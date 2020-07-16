package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface Environment {
	Path getAutoDeployPath();

	/**
	 * Gets the path to the <code>config.properties</code> for the OSGI container.
	 * @return 
	 */
	Path getConfigPropertiesPath();
	
	/**
	 * Gets the path to the folder, that contains the configuration files for the OSGI components.
	 * @return 
	 */
	Path getApplicationConfigPath();

	/**
	 * Gets the path to the Jetty home folder, where the configuration XMLs for an embedded Jetty are stored.
	 * @return 
	 */
	Path getJettyHome();
	
	/**
	 * Gets the path to the home folder of an embedded Solr.
	 * @return 
	 */
	Path getEmbeddedSolrHome();
	
	
	/**
	 * Gets the path to use for temporary data, that shall be used by the JVM.
	 * @return 
	 */
	Path getTempFolder();
	
	/**
	 * Gets the path to the logback configuration xml file, that shall be made available via a system property.
	 * @return 
	 */
	Path getLogbackConfigFilePath();
	
	Path getHomeFolder();

	Path getJarPath();
	
	Boolean needsUnpack();

	Callable<Void> createUnpackCallable(Environment environment, Logger log);
}
