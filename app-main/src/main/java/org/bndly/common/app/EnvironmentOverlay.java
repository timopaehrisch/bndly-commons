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

public class EnvironmentOverlay implements Environment {

	private final Iterable<Environment> delegatedEnvironments;

	public EnvironmentOverlay(Iterable<Environment> delegatedEnvironments) {
		this.delegatedEnvironments = delegatedEnvironments;
	}
	
	@Override
	public Path getAutoDeployPath() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path autoDeployPath = delegatedEnvironment.getAutoDeployPath();
			if (autoDeployPath != null) {
				return autoDeployPath;
			}
		}
		return null;
	}

	@Override
	public Path getConfigPropertiesPath() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path configPropertiesPath = delegatedEnvironment.getConfigPropertiesPath();
			if (configPropertiesPath != null) {
				return configPropertiesPath;
			}
		}
		return null;
	}

	@Override
	public Path getHomeFolder() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path homeFolder = delegatedEnvironment.getHomeFolder();
			if (homeFolder != null) {
				return homeFolder;
			}
		}
		return null;
	}

	@Override
	public Path getJarPath() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path jarPath = delegatedEnvironment.getJarPath();
			if (jarPath != null) {
				return jarPath;
			}
		}
		return null;
	}

	@Override
	public Boolean needsUnpack() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Boolean needsUnpack = delegatedEnvironment.needsUnpack();
			if (needsUnpack != null) {
				return needsUnpack;
			}
		}
		return null;
	}

	@Override
	public Path getApplicationConfigPath() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path applicationConfigPath = delegatedEnvironment.getApplicationConfigPath();
			if (applicationConfigPath != null) {
				return applicationConfigPath;
			}
		}
		return null;
	}

	@Override
	public Path getJettyHome() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path jettyHome = delegatedEnvironment.getJettyHome();
			if (jettyHome != null) {
				return jettyHome;
			}
		}
		return null;
	}

	@Override
	public Path getEmbeddedSolrHome() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path embeddedSolrHome = delegatedEnvironment.getEmbeddedSolrHome();
			if (embeddedSolrHome != null) {
				return embeddedSolrHome;
			}
		}
		return null;
	}

	@Override
	public Path getTempFolder() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path tempFolder = delegatedEnvironment.getTempFolder();
			if (tempFolder != null) {
				return tempFolder;
			}
		}
		return null;
	}

	@Override
	public Path getLogbackConfigFilePath() {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Path logbackConfigFilePath = delegatedEnvironment.getLogbackConfigFilePath();
			if (logbackConfigFilePath != null) {
				return logbackConfigFilePath;
			}
		}
		return null;
	}

	@Override
	public Callable<Void> createUnpackCallable(Environment environment, Logger log) {
		for (Environment delegatedEnvironment : delegatedEnvironments) {
			Callable<Void> unpackCallable = delegatedEnvironment.createUnpackCallable(environment, log);
			if (unpackCallable != null) {
				return unpackCallable;
			}
		}
		return null;
	}
	
}
