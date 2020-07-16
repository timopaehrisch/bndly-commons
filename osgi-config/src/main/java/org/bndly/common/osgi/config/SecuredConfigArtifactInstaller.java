package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.bndly.common.osgi.config.impl.PrefixHandlerProvider;
import org.bndly.common.osgi.config.spi.PrefixHandler;
import org.bndly.common.osgi.util.DictionaryAsMap;
import org.bndly.common.osgi.util.ServicePidUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SecuredConfigArtifactInstaller implements FileConfigArtifactInstaller {

	private static final Logger LOG = LoggerFactory.getLogger(SecuredConfigArtifactInstaller.class);
	public static final String SECURED_VALUE_REGEX = "^\\@\\{([A-Z]+)\\:(.*)\\}$";
	private static final Pattern SECURED_VALUE_PATTERN = Pattern.compile(SECURED_VALUE_REGEX);
	static String SECURED_MARKER = "org.bndly.common.osgi.config.SecuredConfigArtifactInstaller.secured";
	
	private PrefixHandlerProvider prefixHandlerProvider;

	@Override
	public Configuration install(File file, ConfigurationContext configurationContext, ConfigReaderProvider configReaderProvider) throws Exception {
		if (LOG.isInfoEnabled()) {
			LOG.info("install artifact {}", file.getName());
		}
		Configuration configuration = applySecuredConfig(file, configurationContext, configReaderProvider);
		if (LOG.isInfoEnabled()) {
			LOG.info("installed artifact {}", file.getName());
		}
		return configuration;
	}

	private Configuration applySecuredConfig(File file, ConfigurationContext configurationContext, ConfigReaderProvider configReaderProvider) throws ConfigurationException {
		return applySecuredConfigInternal(file, configurationContext, configReaderProvider);
	}
	
	private Configuration applySecuredConfigInternal(final File file, final ConfigurationContext configurationContext, final ConfigReaderProvider configReaderProvider) throws ConfigurationException {
		final ServicePidUtil.PID pid = ServicePidUtil.parseFileName(file.getName());
		ConfigReader reader = configReaderProvider.getConfigReaderForPID(pid.getPid());
		Map<String, ConfigPropertyAdapter> adapters;
		if (reader == null) {
			adapters = Collections.EMPTY_MAP;
		} else {
			adapters = reader.getPropertyAdaptersByPropertyName();
		}
		
		final Hashtable<String, Object> ht = new Hashtable<>();
		// parse the file and filter the values
		try (InputStream is = new FileInputStream(file)) {
			final Properties p = new Properties();
			p.load(is);
			Map<String, Object> strMap = new HashMap<>();
			for (Object k : p.keySet()) {
				// apply security filters on value
				String rawValue = p.getProperty(k.toString());
				Matcher matcher = SECURED_VALUE_PATTERN.matcher(rawValue);
				if (matcher.matches()) {
					final String prefix = matcher.group(1);
					final String value = matcher.group(2);
					PrefixHandler prefixHandler = prefixHandlerProvider.get(prefix);
					if (prefixHandler == null) {
						throw new ConfigurationException("prefix handler for prefix " + prefix + " is missing");
					} else {
						rawValue = prefixHandler.get(value);
					}
				}
				ConfigPropertyAdapter adapter = adapters.get(k.toString());
				Object deserialized;
				if (adapter == null) {
					deserialized = rawValue;
				} else {
					deserialized = adapter.deserialize(rawValue);
				}
				strMap.put(k.toString(), deserialized);
			}
			// TODO: replacement filter for system and evironment properties
			//InterpolationHelper.performSubstitution(strMap, context);
			ht.putAll(strMap);
		} catch (IOException e) {
			LOG.error("failed to read secured config: " + e.getMessage(), e);
			throw new ConfigurationException("could not load secured config " + file.getName(), e);
		}
		
		// once the file is loaded and the values are filtered, we can apply the configs to the configuration admin
		Configuration config = configurationContext.getConfiguration(file, pid);

		Dictionary<String, Object> props = config.getProperties();
		Hashtable<String, Object> old = props != null 
				//? new Hashtable<>(new DictionaryAsMap<>(props)) 
				? new Hashtable<>(new DictionaryAsMap<>(props)) 
				: null;
		if (old != null) {
			old.remove(ConfigurationContext.FILENAME);
			old.remove(SECURED_MARKER);
			old.remove(Constants.SERVICE_PID);
			old.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
		}

		// if the found config differs from the cached one, then update the config in the configAdmin
		if (!ht.equals(old)) {
			configurationContext.linkConfigPropertiesWithFile(ht, file);
			ht.put(SECURED_MARKER, true);
			try {
				config.update(ht);
			} catch (IOException e) {
				throw new ConfigurationException("could not update configuration for " + pid.getPid(), e);
			}
		}
		return config;
	}

	public void setPrefixHandlerProvider(PrefixHandlerProvider prefixHandlerProvider) {
		this.prefixHandlerProvider = prefixHandlerProvider;
	}

}
