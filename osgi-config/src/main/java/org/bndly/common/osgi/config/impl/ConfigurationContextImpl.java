package org.bndly.common.osgi.config.impl;

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

import org.bndly.common.osgi.config.ConfigurationContext;
import org.bndly.common.osgi.config.ConfigurationException;
import org.bndly.common.osgi.util.ServicePidUtil;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ConfigurationContextImpl implements ConfigurationContext {

	private final ConfigurationAdmin configAdmin;

	public ConfigurationContextImpl(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	public String toConfigKey(File f) {
		return f.getAbsoluteFile().toURI().toString();
	}

	@Override
	public void linkConfigPropertiesWithFile(Hashtable<String, Object> ht, File file) {
		linkConfigPropertiesWithConfigurationKey(ht, toConfigKey(file));
	}
	
	@Override
	public void linkConfigPropertiesWithConfigurationKey(Hashtable<String, Object> ht, String configurationKey) {
		ht.put(ConfigurationContext.FILENAME, configurationKey);
	}

	@Override
	public Configuration getConfiguration(File file, ServicePidUtil.PID pid) throws ConfigurationException {
		String fileName = toConfigKey(file);
		return getConfiguration(fileName, pid);
	}
	
	@Override
	public Configuration getConfiguration(String configurationKey, ServicePidUtil.PID pid) throws ConfigurationException {
		Configuration oldConfiguration = findExistingConfiguration(configurationKey);
		if (oldConfiguration != null) {
			return oldConfiguration;
		} else {
			try {
				if (pid.getFactoryPid() != null) {
					return configAdmin.createFactoryConfiguration(pid.getPid(), null);
				} else {
					return configAdmin.getConfiguration(pid.getPid(), null);
				}
			} catch (IOException e) {
				throw new ConfigurationException("could not look up config for " + pid.getPid(), e);
			}
		}
	}

	@Override
	public Configuration findExistingConfiguration(String configurationKey) throws ConfigurationException {
		String filter = "(" + FILENAME + "=" + escapeFilterValue(configurationKey) + ")";
		try {
			Configuration[] configurations = configAdmin.listConfigurations(filter);
			if (configurations != null && configurations.length > 0) {
				return configurations[0];
			} else {
				return null;
			}
		} catch (InvalidSyntaxException | IOException e) {
			throw new ConfigurationException("could not list configurations for filter " + filter, e);
		}
	}

	@Override
	public void deleteConfig(File file) throws ConfigurationException {
		ServicePidUtil.PID pid = ServicePidUtil.parseFileName(file.getName());
		Configuration config = getConfiguration(file, pid);
		try {
			config.delete();
		} catch (IOException e) {
			throw new ConfigurationException("could not delete configuration for " + pid.getPid(), e);
		}
	}
	
	@Override
	public void deleteConfig(String configurationKey) throws ConfigurationException {
		Configuration config = findExistingConfiguration(configurationKey);
		if (config == null) {
			// nothing to do
			return;
		}
		try {
			config.delete();
		} catch (IOException e) {
			throw new ConfigurationException("could not delete configuration for " + config.getPid(), e);
		}
	}

	static String escapeFilterValue(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ('(' == c || ')' == c || '=' == c || '*' == c) {
				sb.append('\\').append(c);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}
