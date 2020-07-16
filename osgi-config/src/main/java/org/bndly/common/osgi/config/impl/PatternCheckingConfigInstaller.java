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

import org.bndly.common.osgi.config.ConfigReaderProvider;
import org.bndly.common.osgi.config.ConfigurationContext;
import org.bndly.common.osgi.config.FileConfigArtifactInstaller;
import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.service.cm.Configuration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class PatternCheckingConfigInstaller implements FileConfigArtifactInstaller {
	
	private final ReadWriteLock installedConfigsLock;
	private final Map<String, InstalledConfig> installedConfigsByFilePath;
	private final FileConfigArtifactInstaller configInstaller;
	private final Pattern compiledPattern;

	public PatternCheckingConfigInstaller(FileConfigArtifactInstaller configInstaller, String pattern, ReadWriteLock installedConfigsLock, Map<String, InstalledConfig> installedConfigsByFilePath) {
		this.configInstaller = configInstaller;
		this.compiledPattern = Pattern.compile(pattern);
		this.installedConfigsLock = installedConfigsLock;
		this.installedConfigsByFilePath = installedConfigsByFilePath;
	}

	@Override
	public Configuration install(File file, ConfigurationContext configurationContext, ConfigReaderProvider configReaderProvider) throws Exception {
		installedConfigsLock.writeLock().lock();
		try {
			if (canHandle(file)) {
				Configuration config = configInstaller.install(file, configurationContext, configReaderProvider);
				installedConfigsByFilePath.put(file.toPath().toAbsolutePath().toString(), new InstalledConfig(file, configInstaller, config));
				return config;
			}
			return null;
		} finally {
			installedConfigsLock.writeLock().unlock();
		}
	}

	public boolean canHandle(String dataName) {
		Matcher matcher = compiledPattern.matcher(dataName);
		return matcher.matches();
	}

	public boolean canHandle(File file) {
		return canHandle(file.toPath().getFileName().toString());
	}

	public FileConfigArtifactInstaller getConfigInstaller() {
		return configInstaller;
	}
	
}
