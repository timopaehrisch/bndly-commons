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

import org.bndly.common.osgi.util.ServicePidUtil;
import java.io.File;
import java.util.Hashtable;
import org.osgi.service.cm.Configuration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ConfigurationContext {
	
	public static final String FILENAME = "org.bndly.common.osgi.config.secured.filename";
	
	/**
	 * Will find an existing configuration or creates a new one based on the provided PID.
	 * @param file the file, that is the source of the configuration to retrieve.
	 * @param pid the persistence identifier of the configuration
	 * @return a configuration. never null.
	 * @throws ConfigurationException if not configuration can be found or created
	 */
	Configuration getConfiguration(File file, ServicePidUtil.PID pid) throws ConfigurationException;
	
	/**
	 * Will find an existing configuration or creates a new one based on the provided PID.
	 * The <code>configurationKey</code> parameter will be used in an LDAP filter to find an existing configuration: 
	 * <code>(org.bndly.common.osgi.config.secured.filename=__configurationKey__)</code>
	 * @param configurationKey a natural key that is used to query the configuration admin for an existing configuration
	 * @param pid the persistence identifier of the configuration
	 * @return a configuration. never null.
	 * @throws ConfigurationException if not configuration can be found or created
	 */
	Configuration getConfiguration(String configurationKey, ServicePidUtil.PID pid) throws ConfigurationException;
	
	/**
	 * Will find a configuration based on the configuration key. 
	 * @param configurationKey a natural key that is used to query the configuration admin for an existing configuration
	 * @return a configuration or null, if no configuration exists for the configuration key.
	 * @throws ConfigurationException  if the lookup can not be performed
	 */
	Configuration findExistingConfiguration(String configurationKey) throws ConfigurationException;

	void linkConfigPropertiesWithFile(Hashtable<String, Object> ht, File file);
	
	void linkConfigPropertiesWithConfigurationKey(Hashtable<String, Object> ht, String configurationKey);
	
	void deleteConfig(File file) throws ConfigurationException;
	
	void deleteConfig(String configurationKey) throws ConfigurationException;
}
