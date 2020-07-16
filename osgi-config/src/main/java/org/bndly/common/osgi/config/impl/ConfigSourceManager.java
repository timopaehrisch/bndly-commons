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

import org.bndly.common.data.api.FolderWatcherFactory;
import org.bndly.common.osgi.config.ConfigSource;
import org.bndly.common.osgi.config.ConfigurationException;
import org.bndly.common.runmode.Runmode;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ConfigSourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigSourceManager.class);
	
	private FolderWatcherFactory folderWatcherFactory;
	
	private ConfigurationAdmin configurationAdmin;
	
	private MetaTypeInformationTracker metaTypeInformationTracker;
	
	private Runmode runmode;
	
	private ConfigurationContextImpl configurationContext;
	private String[] activeRunmodes;
	private FileSystemConfigSource fileSystemConfigSource;
	private final List<ConfigSource> configSources = new ArrayList<>();
	private ServiceRegistration<ConfigSource> fileSytemConfigSourceReg;
	private ConfigSource.ConfigSourceListener listener;
	private ServiceTracker<ConfigSource, ConfigSource> tracker;
	
	public void activate(final BundleContext bundleContext) throws InvalidSyntaxException {
		activeRunmodes = runmode.getActiveRunmodes();
		configurationContext = new ConfigurationContextImpl(configurationAdmin);
		fileSystemConfigSource = new FileSystemConfigSource(metaTypeInformationTracker, folderWatcherFactory, activeRunmodes);
		fileSystemConfigSource.activate(bundleContext);
		fileSytemConfigSourceReg = ServiceRegistrationBuilder.newInstance(ConfigSource.class, fileSystemConfigSource).pid(FileSystemConfigSource.class.getName()).register(bundleContext);
		listener = new ConfigSource.ConfigSourceListener() {
			@Override
			public void itemDeleted(ConfigSource.Item item) {
				LOG.info("item deleted: {}", item);
				try {
					item.uninstall(configurationContext);
				} catch (ConfigurationException ex) {
					LOG.error("could not uninstall item: " + item, ex);
				}
			}

			@Override
			public void itemUpdated(ConfigSource.Item item) {
				LOG.info("item updated: {}", item);
				item.install(configurationContext);
			}

			@Override
			public void itemCreated(ConfigSource.Item item) {
				LOG.info("item created: {}", item);
				item.install(configurationContext);
			}
		};
		tracker = new ServiceTracker<ConfigSource, ConfigSource>(bundleContext, ConfigSource.class, null) {
			@Override
			public ConfigSource addingService(ServiceReference<ConfigSource> reference) {
				ConfigSource configSource = super.addingService(reference);
				configSource.registerListener(listener);
				for (ConfigSource.Item item : configSource.list()) {
					LOG.info("initial item: {}", item);
					item.install(configurationContext);
				}
				return configSource;
			}

			@Override
			public void removedService(ServiceReference<ConfigSource> reference, ConfigSource configSource) {
				configSource.unregisterListener(listener);
			}
			
		};
		tracker.open();
	}

	public void deactivate(BundleContext bundleContext) {
		tracker.close();
		if (fileSytemConfigSourceReg != null) {
			fileSytemConfigSourceReg.unregister();
			fileSytemConfigSourceReg = null;
		}
		fileSystemConfigSource.deactivate(bundleContext);
	}
	
	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

	public void setMetaTypeInformationTracker(MetaTypeInformationTracker metaTypeInformationTracker) {
		this.metaTypeInformationTracker = metaTypeInformationTracker;
	}

	public void setFolderWatcherFactory(FolderWatcherFactory folderWatcherFactory) {
		this.folderWatcherFactory = folderWatcherFactory;
	}

	public void setRunmode(Runmode runmode) {
		this.runmode = runmode;
	}
	
}
