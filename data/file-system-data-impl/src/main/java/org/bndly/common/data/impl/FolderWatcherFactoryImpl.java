package org.bndly.common.data.impl;

/*-
 * #%L
 * File System Data Impl
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
import org.bndly.common.data.api.FolderWatcher;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = FolderWatcherFactory.class, immediate = true)
public class FolderWatcherFactoryImpl implements FolderWatcherFactory {

	private final List<FolderWatcherConfiguration> configurations = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final List<RegisteredWatcher> watchers = new ArrayList<>();
	private final List<Runnable> lazyInits = new ArrayList<>();

	private static final Logger LOG = LoggerFactory.getLogger(FolderWatcherFactoryImpl.class);
	private ComponentContext componentContext;

	private class RegisteredWatcher {

		private final FolderWatcherConfiguration configuration;
		private final ServiceRegistration reg;
		private final FolderWatcher watcher;

		public RegisteredWatcher(FolderWatcherConfiguration configuration, ServiceRegistration reg, FolderWatcher watcher) {
			this.configuration = configuration;
			this.reg = reg;
			this.watcher = watcher;
		}

		public ServiceRegistration getReg() {
			return reg;
		}

		public FolderWatcher getWatcher() {
			return watcher;
		}

		public FolderWatcherConfiguration getConfiguration() {
			return configuration;
		}

	}

	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
		for (Runnable lazyInit : lazyInits) {
			lazyInit.run();
		}
		lazyInits.clear();
	}

	@Deactivate
	public void deactivate() {
		lock.writeLock().lock();
		try {
			List<RegisteredWatcher> tmp = new ArrayList<>(watchers);
			for (RegisteredWatcher folderWatcher : tmp) {
				folderWatcher.getWatcher().stop();
				if (folderWatcher.getReg() != null) {
					folderWatcher.getReg().unregister();
				}
			}
			watchers.clear();
			lazyInits.clear();
			configurations.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Reference(
			bind = "addFolderWatcherConfiguration",
			unbind = "removeFolderWatcherConfiguration",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = FolderWatcherConfiguration.class
	)
	public void addFolderWatcherConfiguration(FolderWatcherConfiguration configuration) {
		if (configuration != null) {
			lock.writeLock().lock();
			try {
				configurations.add(configuration);
				createFolderWatcherWithConfig(configuration);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeFolderWatcherConfiguration(FolderWatcherConfiguration configuration) {
		if (configuration != null) {
			lock.writeLock().lock();
			try {
				Iterator<FolderWatcherConfiguration> iterator = configurations.iterator();
				while (iterator.hasNext()) {
					FolderWatcherConfiguration next = iterator.next();
					if (next == configuration) {
						iterator.remove();
					}
				}
				Iterator<RegisteredWatcher> watcherIterator = watchers.iterator();
				while (watcherIterator.hasNext()) {
					RegisteredWatcher watcher = watcherIterator.next();
					if (watcher.getConfiguration() == configuration) {
						watcher.getWatcher().stop();
						if (watcher.getReg() != null) {
							watcher.getReg().unregister();
						}
						watcherIterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public FolderWatcher createFolderWatcher(String folderLocation, String name) {
		NIOFolderWatcherImpl folderWatcherImpl = new NIOFolderWatcherImpl(name);
		folderWatcherImpl.setFolderLocation(folderLocation);
		folderWatcherImpl.start();
		return folderWatcherImpl;
	}

	private void createFolderWatcherWithConfig(final FolderWatcherConfiguration configuration) {
		if (componentContext == null) {
			lazyInits.add(new Runnable() {

				@Override
				public void run() {
					createFolderWatcherWithConfig(configuration);
				}
			});
			return;
		}

		String folder = configuration.getFolder();
		if (folder == null) {
			return;
		}
		String name = configuration.getName();
		if (name == null) {
			return;
		}
		FolderWatcher watcher = createFolderWatcher(folder, name);
		Dictionary localprops = new Hashtable<>();
		localprops.put("service.pid", FolderWatcher.class.getName() + "." + name);
		ServiceRegistration<?> reg = null;
		reg = componentContext.getBundleContext().registerService(FolderWatcher.class, watcher, localprops);
		lock.writeLock().lock();
		try {
			watchers.add(new RegisteredWatcher(configuration, reg, watcher));
		} finally {
			lock.writeLock().unlock();
		}
	}

}
