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

import org.bndly.common.data.api.FolderWatcherFactory;
import org.bndly.common.osgi.config.impl.ConfigSourceManager;
import org.bndly.common.osgi.config.impl.MetaTypeInformationTracker;
import org.bndly.common.osgi.config.impl.MultipleServiceTracker;
import org.bndly.common.osgi.config.impl.MultipleServiceTracker.Dependency;
import org.bndly.common.osgi.config.impl.MultipleServiceTracker.Setter;
import org.bndly.common.osgi.config.impl.MultipleServiceTracker.Wiring;
import static org.bndly.common.osgi.config.impl.MultipleServiceTracker.serviceInterfaceDependency;
import org.bndly.common.osgi.config.impl.PrefixHandlerProvider;
import org.bndly.common.osgi.config.impl.PrefixHandlerTracker;
import org.bndly.common.osgi.config.spi.PrefixHandler;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.common.runmode.Runmode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Activator implements BundleActivator {
	
	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private ServiceRegistration<PrefixHandler> obfuscationPrefixHandlerReg;
	private final List<ServiceTracker> trackers = new ArrayList<>();
	private final PrefixHandlerTracker prefixHandlerTracker = new PrefixHandlerTracker();
	private BundleContext context;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		lock.writeLock().lock();
		try {
			obfuscationPrefixHandlerReg = ServiceRegistrationBuilder
					.newInstance(PrefixHandler.class, new ObfuscationPrefixHandler())
					.pid(ObfuscationPrefixHandler.class.getName())
					.register(context);
			
			createMetaTypeInformationTracker();
			createConfigFolderManager();
			createSecuredPersistenceManagerImpl();
			createPlainConfigArtifactInstaller();
			createSecuredConfigArtifactInstaller();
			
			prefixHandlerTracker.activate(context);
			for (ServiceTracker tracker : trackers) {
				tracker.open();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void createConfigFolderManager() throws InvalidSyntaxException {
		createTracker(new MultipleServiceTracker.Callback() {
					private ConfigSourceManager configSourceManager;
					private ServiceRegistration<ConfigSourceManager> reg;
					
					@Override
					public void onReady(BundleContext context, Wiring wiring) throws Exception {
						configSourceManager = new ConfigSourceManager();
						// wire the dependencies
						wiring.wire(configSourceManager);
						configSourceManager.activate(context);
						LOG.info("activated and registering ConfigSourceManager");
						reg = ServiceRegistrationBuilder
							.newInstance(ConfigSourceManager.class, configSourceManager)
							.pid(ConfigSourceManager.class.getName())
							.register(context);
					}
					
					@Override
					public void onDestroy(BundleContext context) {
						reg.unregister();
						configSourceManager.deactivate(context);
					}
				},
				serviceInterfaceDependency(ConfigurationAdmin.class).wire(new Setter<ConfigSourceManager, ConfigurationAdmin>() {
					@Override
					public void set(ConfigSourceManager target, ConfigurationAdmin trackedService) {
						target.setConfigurationAdmin(trackedService);
					}
				}),
				serviceInterfaceDependency(MetaTypeInformationTracker.class).wire(new Setter<ConfigSourceManager, MetaTypeInformationTracker>() {
					@Override
					public void set(ConfigSourceManager target, MetaTypeInformationTracker trackedService) {
						target.setMetaTypeInformationTracker(trackedService);
					}
				}),
				serviceInterfaceDependency(FolderWatcherFactory.class).wire(new Setter<ConfigSourceManager, FolderWatcherFactory>() {
					@Override
					public void set(ConfigSourceManager target, FolderWatcherFactory trackedService) {
						target.setFolderWatcherFactory(trackedService);
					}
				}),
				serviceInterfaceDependency(Runmode.class).wire(new Setter<ConfigSourceManager, Runmode>() {
					@Override
					public void set(ConfigSourceManager target, Runmode trackedService) {
						target.setRunmode(trackedService);
					}
				})
		);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		lock.writeLock().lock();
		try {
			obfuscationPrefixHandlerReg.unregister();
			obfuscationPrefixHandlerReg = null;
			for (ServiceTracker tracker : trackers) {
				tracker.close();
			}
			trackers.clear();
			prefixHandlerTracker.deactivate(context);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void createTracker(MultipleServiceTracker.Callback callback, Dependency... dependencies) throws InvalidSyntaxException {
		MultipleServiceTracker tracker = MultipleServiceTracker.newInstance(context, callback, dependencies);
		trackers.add(tracker);
	}

	private void createPlainConfigArtifactInstaller() throws InvalidSyntaxException {
		createTracker(
			new MultipleServiceTracker.Callback() {
				private PlainConfigArtifactInstaller plainConfigArtifactInstaller;
				private ServiceRegistration<FileConfigArtifactInstaller> reg;

				@Override
				public void onReady(BundleContext context, Wiring wiring) throws Exception {
					plainConfigArtifactInstaller = new PlainConfigArtifactInstaller();
					LOG.info("activated and registering PlainConfigArtifactInstaller");
					reg = ServiceRegistrationBuilder
							.newInstance(FileConfigArtifactInstaller.class, plainConfigArtifactInstaller)
							.pid(PlainConfigArtifactInstaller.class.getName())
							.property(FileConfigArtifactInstaller.OSGI_PATTERN, "^.*\\.cfg$")
							.register(context);
				}

				@Override
				public void onDestroy(BundleContext context) {
					reg.unregister();
				}
			},
			serviceInterfaceDependency(PersistenceManager.class, "(secured=*)")
		);
	}

	private void createMetaTypeInformationTracker() throws InvalidSyntaxException {
		createTracker(
				new MultipleServiceTracker.Callback() {
					private MetaTypeInformationTracker metaTypeInformationTracker;
					private ServiceRegistration<MetaTypeInformationTracker> reg;
					
					@Override
					public void onReady(BundleContext context, Wiring wiring) throws Exception {
						metaTypeInformationTracker = new MetaTypeInformationTracker();
						// wire the dependencies
						wiring.wire(metaTypeInformationTracker);
						metaTypeInformationTracker.activate(context);
						LOG.info("activated and registering MetaTypeInformationTracker");
						reg = ServiceRegistrationBuilder
							.newInstance(metaTypeInformationTracker)
							.serviceInterface(MetaTypeInformationTracker.class)
							.serviceInterface(ConfigReaderProvider.class)
							.pid(MetaTypeInformationTracker.class.getName())
							.register(context);
					}
					
					@Override
					public void onDestroy(BundleContext context) {
						reg.unregister();
						metaTypeInformationTracker.deactivate(context);
					}
				},
				serviceInterfaceDependency(MetaTypeService.class).wire(new Setter<MetaTypeInformationTracker, MetaTypeService>() {
					@Override
					public void set(MetaTypeInformationTracker target, MetaTypeService trackedService) {
						target.setMetaTypeService(trackedService);
					}
				})
		);
	}

	private void createSecuredConfigArtifactInstaller() throws InvalidSyntaxException {
		createTracker(
				new MultipleServiceTracker.Callback() {
					private SecuredConfigArtifactInstaller securedConfigArtifactInstaller;
					private ServiceRegistration<SecuredConfigArtifactInstaller> reg;
					
					@Override
					public void onReady(BundleContext context, Wiring wiring) throws Exception {
						securedConfigArtifactInstaller = new SecuredConfigArtifactInstaller();
						// wire the dependencies
						wiring.wire(securedConfigArtifactInstaller);
						LOG.info("activated and registering SecuredConfigArtifactInstaller");
						reg = ServiceRegistrationBuilder
							.newInstance(securedConfigArtifactInstaller)
							.serviceInterface(FileConfigArtifactInstaller.class)
							.pid(SecuredConfigArtifactInstaller.class.getName())
							.property(FileConfigArtifactInstaller.OSGI_PATTERN, "^.*\\.scfg$")
							.register(context);
					}
					
					@Override
					public void onDestroy(BundleContext context) {
						reg.unregister();
					}
				},
				serviceInterfaceDependency(PrefixHandlerProvider.class).wire(new Setter<SecuredConfigArtifactInstaller, PrefixHandlerProvider>() {
					@Override
					public void set(SecuredConfigArtifactInstaller target, PrefixHandlerProvider trackedService) {
						target.setPrefixHandlerProvider(trackedService);
					}
				}),
				serviceInterfaceDependency(PersistenceManager.class, "(secured=true)")
		);
	}

	private void createSecuredPersistenceManagerImpl() throws InvalidSyntaxException {
		createTracker(
				new MultipleServiceTracker.Callback() {
					private SecuredPersistenceManagerImpl securedPersistenceManagerImpl;
					
					@Override
					public void onReady(BundleContext context, Wiring wiring) throws Exception {
						securedPersistenceManagerImpl = new SecuredPersistenceManagerImpl();
						// wire the dependencies
						wiring.wire(securedPersistenceManagerImpl);
						LOG.info("activated and registering SecuredPersistenceManagerImpl");
						securedPersistenceManagerImpl.activate(context);
					}
					
					@Override
					public void onDestroy(BundleContext context) {
						securedPersistenceManagerImpl.deactivate();
					}
				},
				serviceInterfaceDependency(ConfigReaderProvider.class).wire(new Setter<SecuredPersistenceManagerImpl, ConfigReaderProvider>() {
					@Override
					public void set(SecuredPersistenceManagerImpl target, ConfigReaderProvider trackedService) {
						target.setConfigReaderProvider(trackedService);
					}
				})
		);
	}
	
}
