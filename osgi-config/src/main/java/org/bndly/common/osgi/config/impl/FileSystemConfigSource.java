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

import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.FolderWatcher;
import org.bndly.common.data.api.FolderWatcherFactory;
import org.bndly.common.data.api.FolderWatcherListener;
import org.bndly.common.osgi.config.AbstractConfigSource;
import org.bndly.common.osgi.config.ConfigSource;
import org.bndly.common.osgi.config.ConfigurationContext;
import org.bndly.common.osgi.config.ConfigurationException;
import org.bndly.common.osgi.config.FileConfigArtifactInstaller;
import org.bndly.common.osgi.util.DictionaryAdapter;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FileSystemConfigSource extends AbstractConfigSource implements ConfigSource, FolderWatcherListener{

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemConfigSource.class);
	
	private static final String RUNMODE_FOLDER_PREFIX = "runmode-";
	private static final String PROPERTY_LEGACY_CONFIG_LOCATION = "felix.fileinstall.dir";
	private static final String PROPERTY_CONFIG_LOCATION = "bndly.application.config.dir";
	
	private final MetaTypeInformationTracker metaTypeInformationTracker;
	private final FolderWatcherFactory folderWatcherFactory;
	private final String[] activeRunmodes;
	private final ConfigSource source = this;
	private final List<PatternCheckingConfigInstaller> installers = new ArrayList<>();
	private final ReadWriteLock trackerLock = new ReentrantReadWriteLock();
	private final List<DefferedConfigurationInstallation> queuedConfigInstallations = new ArrayList<>();
	private final Map<String, InstalledConfig> installedConfigsByFilePath = new HashMap<>();
	private final ReadWriteLock installedConfigsLock = new ReentrantReadWriteLock();
	private final FileFilter configFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return isConfig(pathname);
		}
	};
	
	private List<FolderWatcher> watchers;
	private ServiceTracker<FileConfigArtifactInstaller, FileConfigArtifactInstaller> tracker;

	public FileSystemConfigSource(MetaTypeInformationTracker metaTypeInformationTracker, FolderWatcherFactory folderWatcherFactory, String[] activeRunmodes) {
		this.metaTypeInformationTracker = metaTypeInformationTracker;
		this.folderWatcherFactory = folderWatcherFactory;
		this.activeRunmodes = activeRunmodes;
	}
	
	public void activate(BundleContext bundleContext) throws InvalidSyntaxException {
		String foldersToWatch = bundleContext.getProperty(PROPERTY_CONFIG_LOCATION);
		if (foldersToWatch == null) {
			foldersToWatch = bundleContext.getProperty(PROPERTY_LEGACY_CONFIG_LOCATION);
		}
		tracker = new ServiceTracker<FileConfigArtifactInstaller, FileConfigArtifactInstaller>(bundleContext, bundleContext.createFilter("(&(objectclass=" + FileConfigArtifactInstaller.class.getName() + ")(" + FileConfigArtifactInstaller.OSGI_PATTERN + "=*))"), null) {
			@Override
			public FileConfigArtifactInstaller addingService(ServiceReference<FileConfigArtifactInstaller> reference) {
				trackerLock.writeLock().lock();
				try {
					final FileConfigArtifactInstaller installer = super.addingService(reference);
					String pattern = new DictionaryAdapter(reference).getString(FileConfigArtifactInstaller.OSGI_PATTERN);
					PatternCheckingConfigInstaller patternCheckingInstaller = new PatternCheckingConfigInstaller(installer, pattern, installedConfigsLock, installedConfigsByFilePath);
					installers.add(patternCheckingInstaller);
					Iterator<DefferedConfigurationInstallation> iterator = queuedConfigInstallations.iterator();
					while (iterator.hasNext()) {
						DefferedConfigurationInstallation defferedConfigurationInstallation = iterator.next();
						defferedConfigurationInstallation.install(iterator);
					}
					installConfigsFromWatchedFolders(patternCheckingInstaller);
					return installer;
				} finally {
					trackerLock.writeLock().unlock();
				}
			}

			@Override
			public void removedService(ServiceReference<FileConfigArtifactInstaller> reference, FileConfigArtifactInstaller service) {
				trackerLock.writeLock().lock();
				try {
					Iterator<PatternCheckingConfigInstaller> iter = installers.iterator();
					while (iter.hasNext()) {
						if (iter.next().getConfigInstaller() == service) {
							iter.remove();
						}
					}
				} finally {
					trackerLock.writeLock().unlock();
				}
			}
			
		};
		watchers = new ArrayList<>();
		if (foldersToWatch == null) {
			LOG.warn("no folders defined to watch for configurations");
		} else {
			for (String folderToWatch : foldersToWatch.split(",")) {
				LOG.info("watching {} for configs", folderToWatch);
				FolderWatcher watcher = folderWatcherFactory.createFolderWatcher(folderToWatch, "config");
				watcher.addListener(this);
				watchers.add(watcher);
			}
		}
		tracker.open();
	}
	
	public void deactivate(BundleContext bundleContext) {
		for (FolderWatcher watcher : watchers) {
			watcher.removeListener(this);
			watcher.stop();
			LOG.info("stopped watching {} for configs", watcher.getWatchedFolder());
		}
		watchers.clear();
		tracker.close();
		queuedConfigInstallations.clear();
	}
	
	private Item createItem(final File configFile) {
		return new Item() {
			@Override
			public ConfigSource getSource() {
				return source;
			}

			@Override
			public void install(ConfigurationContext configurationContext) {
				installOrUpdateConfig(configurationContext, configFile, true);
			}

			@Override
			public void uninstall(ConfigurationContext configurationContext) throws ConfigurationException {
				configurationContext.deleteConfig(configFile);
			}
			
			@Override
			public String toString() {
				return FileSystemConfigSource.class.getSimpleName() + "(" + configFile + ")";
			}

		};
	}
	
	@Override
	public Iterable<Item> list() {
		List<Item> list = new ArrayList<>();
		for (FolderWatcher watcher : watchers) {
			File wf = watcher.getWatchedFolder();
			Path wfPath = wf.toPath();
			File[] listFiles = wf.listFiles(configFilter);
			for (File configFile : listFiles) {
				list.add(createItem(configFile));
			}
			for (String activeRunmode : activeRunmodes) {
				Path runModeFolder = wfPath.resolve(RUNMODE_FOLDER_PREFIX + activeRunmode);
				if (Files.exists(runModeFolder) && Files.isDirectory(runModeFolder)) {
					listFiles = runModeFolder.toFile().listFiles(configFilter);
					for (File configFile : listFiles) {
						list.add(createItem(configFile));
					}
				}
			}
		}
		return list;
	}
	
	private void installConfigsFromWatchedFolders(PatternCheckingConfigInstaller installer) {
		installedConfigsLock.writeLock().lock();
		try {
			for (FolderWatcher watcher : watchers) {
				File wf = watcher.getWatchedFolder();
				Path wfPath = wf.toPath();
				FileFilter filter = new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						if (installedConfigsByFilePath.containsKey(pathname.toPath().toAbsolutePath().toString())) {
							return false;
						}
						if (!configFilter.accept(pathname)) {
							return false;
						}
						return true;
					}
				};
				File[] listFiles = wf.listFiles(filter);
				for (File configFile : listFiles) {
					fireCreatedEvent(createItem(configFile));
				}
				for (String activeRunmode : activeRunmodes) {
					Path runModeFolder = wfPath.resolve(RUNMODE_FOLDER_PREFIX + activeRunmode);
					if (Files.exists(runModeFolder) && Files.isDirectory(runModeFolder)) {
						listFiles = runModeFolder.toFile().listFiles(filter);
						for (File configFile : listFiles) {
							fireCreatedEvent(createItem(configFile));
						}
					}
				}
			}
		} finally {
			installedConfigsLock.writeLock().unlock();
		}
	}
	
	private boolean isConfig(File file) {
		return isConfig(file.getName());
	}
	
	private boolean isConfig(Data data) {
		return isConfig(data.getName());
	}
	
	private boolean isConfig(String dataName) {
		trackerLock.readLock().lock();
		try {
			for (PatternCheckingConfigInstaller installer : installers) {
				if (installer.canHandle(dataName)) {
					return true;
				}
			}
			return false;
		} finally {
			trackerLock.readLock().unlock();
		}
	}
	
	private static final Pattern RUNMODE_PATTERN = Pattern.compile("^(.+)[\\/|\\\\].+$");

	private boolean isConfigForActiveRunmodes(Data data) {
		String runmodeFromData = getRunmodeFromData(data);
		if (runmodeFromData == null) {
			return false;
		}
		for (String activeRunmode : activeRunmodes) {
			if (runmodeFromData.equals(activeRunmode)) {
				return true;
			}
		}
		return false;
	}

	private String getRunmodeFromData(Data data) {
		String name = data.getName();
		if (!name.startsWith(RUNMODE_FOLDER_PREFIX)) {
			return null;
		}
		String nameWithoutPrefix = name.substring(RUNMODE_FOLDER_PREFIX.length());
		Matcher matcher = RUNMODE_PATTERN.matcher(nameWithoutPrefix);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);
	}
	
	private void deleteConfig(Data data) {
		LOG.info("deleting config {}", data.getName());
	}
	
	private boolean isRunModeFolder(File subFolder) {
		String folderName = subFolder.toPath().getFileName().toString();
		return folderName.startsWith(RUNMODE_FOLDER_PREFIX);
	}
	
	private interface DefferedConfigurationInstallation {
		void install(Iterator<DefferedConfigurationInstallation> iterator);
	}

	private boolean installOrUpdateConfig(final ConfigurationContext configurationContext, final File file, boolean canDefer) {
		LOG.debug("installing config {}", file.getName());
		trackerLock.readLock().lock();
		try {
			for (PatternCheckingConfigInstaller installer : installers) {
				if (installer.canHandle(file)) {
					installConfig(configurationContext, file, installer);
					return true;
				}
			}
		} finally {
			trackerLock.readLock().unlock();
		}
		trackerLock.writeLock().lock();
		try {
			// until we get the write lock, a new installer may be there
			for (PatternCheckingConfigInstaller installer : installers) {
				if (installer.canHandle(file)) {
					installConfig(configurationContext, file, installer);
					return true;
				}
			}
			if (canDefer) {
				LOG.debug("deferring installation of {} because no installer is available", file.getName());
				queuedConfigInstallations.add(new DefferedConfigurationInstallation() {
					@Override
					public void install(Iterator<DefferedConfigurationInstallation> iterator) {
						if (installOrUpdateConfig(configurationContext, file, false)) {
							LOG.debug("successfully installed deffered configuration {}", file.getName());
							iterator.remove();
						}
					}

				});
			}
			return false;
		} finally {
			trackerLock.writeLock().unlock();
		}
	}
	
	private void installConfig(ConfigurationContext configurationContext, final File file, PatternCheckingConfigInstaller installer) {
		try {
			installer.install(file, configurationContext, metaTypeInformationTracker);
		} catch (Exception e) {
			LOG.error("failed to install configuration", e);
		}
	}
	
	@Override
	public void newData(FolderWatcher folderWatcher, Data data, File file) {
		if (isConfig(data)) {
			if (isConfigForActiveRunmodes(data)) {
				fireCreatedEvent(createItem(file));
			}
		} else {
			// we might find files, that are configs, but we yet do not know that, because the configinstallers will tell us. those may can come at any time
		}
	}

	@Override
	public void updatedData(FolderWatcher folderWatcher, Data data, File file) {
		if (isConfig(data)) {
			if (isConfigForActiveRunmodes(data)) {
				fireUpdatedEvent(createItem(file));
			}
		}
	}

	@Override
	public void deletedData(FolderWatcher folderWatcher, Data data, File file) {
		if (isConfig(data)) {
			if (isConfigForActiveRunmodes(data)) {
				deleteConfig(data);
			} else {
				LOG.info("detected deletion of config from an inactive runmode");
			}
		}
	}

	@Override
	public void newSubFolder(FolderWatcher folderWatcher, File subFolder) {
		if (isRunModeFolder(subFolder)) {
			// ???
		}
	}

	@Override
	public void deletedSubFolder(FolderWatcher folderWatcher, File subFolder) {
		if (isRunModeFolder(subFolder)) {
			// ???
		}
	}

	@Override
	public void shuttingDown(FolderWatcher folderWatcher) {
	}
}
