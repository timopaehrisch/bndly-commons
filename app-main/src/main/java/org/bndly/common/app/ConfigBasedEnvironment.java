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
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

public class ConfigBasedEnvironment implements Environment {

	private final Map<String, Object> config;

	private static interface ValueTransformer<E> {

		E transform(Object input);
	}

	private static final ValueTransformer<Boolean> BOOLEAN = new ValueTransformer<Boolean>() {
		@Override
		public Boolean transform(Object input) {
			if (Boolean.class.isInstance(input)) {
				return (Boolean) input;
			} else if (String.class.isInstance(input)) {
				return Boolean.valueOf((String) input);
			} else {
				return null;
			}
		}
	};
	
	private static final ValueTransformer<String> STRING = new ValueTransformer<String>() {
		@Override
		public String transform(Object input) {
			if (String.class.isInstance(input)) {
				return (String) input;
			} else if (input != null) {
				return input.toString();
			} else {
				return null;
			}
		}
	};

	public ConfigBasedEnvironment(Map<String, Object> config) {
		this.config = config;
	}

	@Override
	public Path getAutoDeployPath() {
		return null;
	}

	@Override
	public Path getConfigPropertiesPath() {
		String propertiesLocation = getOrDefault(STRING, SharedConstants.CONFIG_PROPERTY_CONFIG_PROPERTIES, null);
		return propertiesLocation == null ? null : Paths.get(propertiesLocation);
	}

	@Override
	public Path getHomeFolder() {
		// check config
		String homeFolderAsString = getOrDefault(STRING, SharedConstants.CONFIG_PROPERTY_HOME_FOLDER, null);

		if (homeFolderAsString == null) {
			// check environment
			homeFolderAsString = getSystemEnvironmentVariable(SharedConstants.ENV_PROPERTY_HOME_FOLDER);
		}
		return homeFolderAsString == null ? null : Paths.get(homeFolderAsString);
	}

	@Override
	public Path getJarPath() {
		return null;
	}

	@Override
	public Boolean needsUnpack() {
		return getOrDefault(BOOLEAN, SharedConstants.CONFIG_PROPERTY_DO_UNPACK_APP_JAR, null);
	}

	@Override
	public Path getApplicationConfigPath() {
		return null;
	}

	@Override
	public Path getJettyHome() {
		return null;
	}

	@Override
	public Path getEmbeddedSolrHome() {
		return null;
	}

	@Override
	public Path getTempFolder() {
		return null;
	}

	@Override
	public Path getLogbackConfigFilePath() {
		return null;
	}

	private <T> T getOrDefault(ValueTransformer<T> transformer, String key, T defaultValue) {
		if (config.containsKey(key)) {
			return transformer.transform(config.get(key));
		} else {
			return defaultValue;
		}
	}
	
	private String getSystemEnvironmentVariable(String key) {
		String var = System.getenv(key.toUpperCase());
		return var;
	}

	@Override
	public Callable<Void> createUnpackCallable(Environment environment, Logger log) {
		return null;
	}
	
}
