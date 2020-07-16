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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class SingleJarEnvironment implements Environment {

	private Path _jarPath;
	private Path _homeFolder;
	private Path _configPropertiesLocation;
	private Path _autoDeployPath;
	private Path _applicationConfigPath;
	private Path _jettyHome;
	private Path _solrHome;
	private Path _tempFolder;
	private Path _logbackConfigFilePath;

	@Override
	public Path getAutoDeployPath() {
		if (_autoDeployPath == null) {
			_autoDeployPath = getHomeFolder().resolve(SharedConstants.AUTO_DEPLOY_PATH);
		}
		return _autoDeployPath;
	}

	@Override
	public Path getConfigPropertiesPath() {
		if (_configPropertiesLocation == null) {
			_configPropertiesLocation = getHomeFolder().resolve("conf").resolve("config.properties");
		}
		return _configPropertiesLocation;
	}

	@Override
	public Path getApplicationConfigPath() {
		if (_applicationConfigPath == null) {
			_applicationConfigPath = getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_SECURED_CONFIG_LOCATION);
		}
		return _applicationConfigPath;
	}

	@Override
	public Path getJettyHome() {
		if (_jettyHome == null) {
			_jettyHome = getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_JETTY_HOME);
		}
		return _jettyHome;
	}

	@Override
	public Path getEmbeddedSolrHome() {
		if (_solrHome == null) {
			_solrHome = getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_SOLR_HOME);
		}
		return _solrHome;
	}

	@Override
	public Path getTempFolder() {
		if (_tempFolder == null) {
			_tempFolder = getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_JAVA_IO_TEMP);
		}
		return _tempFolder;
	}

	@Override
	public Path getLogbackConfigFilePath() {
		if (_logbackConfigFilePath == null) {
			_logbackConfigFilePath = getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_LOGBACK_CONFIGURATION_FILE);
		}
		return _logbackConfigFilePath;
	}

	/**
	 * In a single jar environment the home folder is equivalent to <code>./framework</code> relative to the executed jar.
	 *
	 * @return
	 */
	@Override
	public Path getHomeFolder() {
		if (_homeFolder == null) {
			_homeFolder = getJarPath().getParent().resolve(SharedConstants.APP_JAR_EXPLODED_FRAMEWORK);
		}
		return _homeFolder;
	}

	@Override
	public Path getJarPath() {
		if (_jarPath == null) {
			URL location = FelixMain.class.getProtectionDomain().getCodeSource().getLocation();
			try {
				_jarPath = Paths.get(location.toURI());
			} catch (URISyntaxException ex) {
				throw new IllegalStateException("could not determine jar path", ex);
			}
		}
		return _jarPath;
	}

	@Override
	public Boolean needsUnpack() {
		return true;
	}

	@Override
	public Callable<Void> createUnpackCallable(final Environment environment, final Logger log) {
		return new DefaultUnpackCallable(environment, log);
	}
}
