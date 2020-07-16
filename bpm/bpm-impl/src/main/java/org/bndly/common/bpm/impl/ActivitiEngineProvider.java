package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

import org.bndly.common.bpm.api.BusinessProcessData;
import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.bpm.api.BusinessProcessDataStoreEventListener;
import org.bndly.common.bpm.api.BusinessProcessDataStoreFactory;
import org.bndly.common.bpm.api.EngineProviderListener;
import org.bndly.common.bpm.api.ProcessDeploymentListener;
import org.bndly.common.bpm.api.ProcessDeploymentService;
import org.bndly.common.bpm.api.ProcessInstanceService;
import org.bndly.common.bpm.api.ProcessInvocationListener;
import org.bndly.common.bpm.api.ProcessVariableType;
import org.bndly.common.bpm.api.ServiceTaskClassProvider;
import org.bndly.common.bpm.exception.ProcessDeploymentException;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.sql.DataSource;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.scripting.ScriptBindingsFactory;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.engine.impl.variable.VariableType;
import org.activiti.engine.impl.variable.VariableTypes;
import org.activiti.image.ProcessDiagramGenerator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {ActivitiEngineProvider.class, ProcessInstanceServiceProvider.class}, immediate = true)
public class ActivitiEngineProvider implements ProcessInstanceServiceProvider {

	private static final Logger LOG = LoggerFactory.getLogger(ActivitiEngineProvider.class);
	private final ClassLoader myClassLoader = new ProxyClassLoader(ActivitiEngineProvider.class.getClassLoader());
	private ServiceTracker javaDelegateTracker;
	private JavaDelegateTracker customizer;
	private ComponentContext componentContext;

	private class ProxyClassLoader extends ClassLoader {

		public ProxyClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			serviceTaskClassProvidersLock.readLock().lock();
			try {
				for (ServiceTaskClassProvider serviceTaskClassProvider : serviceTaskClassProviders) {
					Class<?> cls = serviceTaskClassProvider.getServiceTaskClassByName(name);
					if (cls != null) {
						return cls;
					}
				}
			} finally {
				serviceTaskClassProvidersLock.readLock().unlock();
			}
			return super.findClass(name);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			serviceTaskClassProvidersLock.readLock().lock();
			try {
				for (ServiceTaskClassProvider serviceTaskClassProvider : serviceTaskClassProviders) {
					Class<?> cls = serviceTaskClassProvider.getServiceTaskClassByName(name);
					if (cls != null) {
						return cls;
					}
				}
			} finally {
				serviceTaskClassProvidersLock.readLock().unlock();
			}
			return super.loadClass(name);
		}

		@Override
		public InputStream getResourceAsStream(String name) {
			// activiti is using the classloader resource loading to generate the
			// diagrams. hence we have to chain the activiti bundle classloaders 
			// into the resource resolution.
			InputStream s = ProcessDiagramGenerator.class.getClassLoader().getResourceAsStream(name);
			if (s != null) {
				return s;
			}
			return super.getResourceAsStream(name);
		}

	}

	private final List<ServiceTaskClassProvider> serviceTaskClassProviders = new ArrayList<>();
	private final ReadWriteLock serviceTaskClassProvidersLock = new ReentrantReadWriteLock();

	@Reference
	private BusinessProcessDataStoreFactory bpDataStoreFactory;

	// this list is copied to individual knownprocessengine instances. they have to manually created/destroyed because they share the same life cycle
	private final List<ActivitiEngineConfiguration> engineConfigurations = new ArrayList<>();
	private final ReadWriteLock engineConfigurationsLock = new ReentrantReadWriteLock();
	private final Map<Integer, DataSourceTracker> dataSourceTrackers = new HashMap<>();
	
	// this list is used only internally. these listeners will not appear in any other place
	private final List<EngineProviderListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	// list is copied. this means items have to be manually added/removed from referencing elements upon 'addProcessVariableType' and 'removeProcessVariableType'
	private final List<ProcessVariableType> variableTypes = new ArrayList<>();
	private final ReadWriteLock variableTypesLock = new ReentrantReadWriteLock();
	
	// list is only referenced. never copied
	private final List<ProcessInvocationListener> invocationListeners = new ArrayList<>();
	private final ReadWriteLock invocationListenersLock = new ReentrantReadWriteLock();
	
	// list is only referenced. never copied
	private final List<ProcessDeploymentListener> deploymentListeners = new ArrayList<>();
	private final ReadWriteLock deploymentListenersLock = new ReentrantReadWriteLock();
	
	private final Map<String, KnownProcessEngine> engines = new HashMap<>();
	private final ReadWriteLock enginesLock = new ReentrantReadWriteLock();
	
	private boolean isActivated;
	private final ReadWriteLock isActivatedLock = new ReentrantReadWriteLock();

	@Reference(
			bind = "addActivitiEngineConfiguration",
			unbind = "removeActivitiEngineConfiguration",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ActivitiEngineConfiguration.class
	)
	public void addActivitiEngineConfiguration(ActivitiEngineConfiguration configuration) {
		if (configuration != null) {
			engineConfigurationsLock.writeLock().lock();
			try {
				engineConfigurations.add(configuration);
				isActivatedLock.readLock().lock();
				try {
					if (isActivated) {
						activateConfiguration(configuration, componentContext);
					}
				} finally {
					isActivatedLock.readLock().unlock();
				}
			} finally {
				engineConfigurationsLock.writeLock().unlock();
			}
		}
	}

	public void removeActivitiEngineConfiguration(ActivitiEngineConfiguration configuration) {
		if (configuration != null) {
			engineConfigurationsLock.writeLock().lock();
			try {
				Iterator<ActivitiEngineConfiguration> iterator = engineConfigurations.iterator();
				while (iterator.hasNext()) {
					ActivitiEngineConfiguration next = iterator.next();
					if (next == configuration) {
						iterator.remove();
					}
				}
				deactivateConfiguration(configuration);
			} finally {
				engineConfigurationsLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "addServiceTaskClassProvider",
			unbind = "removeServiceTaskClassProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ServiceTaskClassProvider.class
	)
	public void addServiceTaskClassProvider(ServiceTaskClassProvider serviceTaskClassProvider) {
		if (serviceTaskClassProvider != null) {
			serviceTaskClassProvidersLock.writeLock().lock();
			try {
				serviceTaskClassProviders.add(serviceTaskClassProvider);
			} finally {
				serviceTaskClassProvidersLock.writeLock().unlock();
			}
		}
	}

	public void removeServiceTaskClassProvider(ServiceTaskClassProvider serviceTaskClassProvider) {
		if (serviceTaskClassProvider != null) {
			serviceTaskClassProvidersLock.writeLock().lock();
			try {
				serviceTaskClassProviders.remove(serviceTaskClassProvider);
			} finally {
				serviceTaskClassProvidersLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "addEngineProviderListener",
			unbind = "removeEngineProviderListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = EngineProviderListener.class
	)
	public void addEngineProviderListener(EngineProviderListener engineProviderListener) {
		if (engineProviderListener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(engineProviderListener);
				for (EngineProviderListener listener : listeners) {
					enginesLock.readLock().lock();
					try {
						for (KnownProcessEngine value : engines.values()) {
							listener.createdEngine(value.getEngineName(), value.getProcessInstanceService(), value.getProcessDeploymentService(), value.bpDataStore);
						}
					} finally {
						enginesLock.readLock().unlock();
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	public void removeEngineProviderListener(EngineProviderListener engineProviderListener) {
		if (engineProviderListener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(engineProviderListener);
				for (EngineProviderListener listener : listeners) {
					enginesLock.readLock().lock();
					try {
						for (KnownProcessEngine value : engines.values()) {
							listener.destroyedEngine(value.getEngineName(), value.getProcessInstanceService(), value.getProcessDeploymentService(), value.bpDataStore);
						}
					} finally {
						enginesLock.readLock().unlock();
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	@Reference(
			bind = "addProcessVariableType",
			unbind = "removeProcessVariableType",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ProcessVariableType.class
	)
	public void addProcessVariableType(ProcessVariableType variableType) {
		if (variableType != null) {
			variableTypesLock.writeLock().lock();
			try {
				variableTypes.add(variableType);
				appendVariableTypeToEngines(variableType);
			} finally {
				variableTypesLock.writeLock().unlock();
			}
		}
	}

	public void removeProcessVariableType(ProcessVariableType variableType) {
		if (variableType != null) {
			variableTypesLock.writeLock().lock();
			try {
				variableTypes.remove(variableType);
				removeVariableTypeFromEngines(variableType);
			} finally {
				variableTypesLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "registerProcessInvocationListener",
			unbind = "unregisterProcessInvocationListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ProcessInvocationListener.class
	)
	public void registerProcessInvocationListener(ProcessInvocationListener listener) {
		if (listener != null) {
			invocationListenersLock.writeLock().lock();
			try {
				invocationListeners.add(listener);
			} finally {
				invocationListenersLock.writeLock().unlock();
			}
		}
	}

	public void unregisterProcessInvocationListener(ProcessInvocationListener listener) {
		invocationListenersLock.writeLock().lock();
		try {
			Iterator<ProcessInvocationListener> iterator = invocationListeners.iterator();
			while (iterator.hasNext()) {
				ProcessInvocationListener next = iterator.next();
				if (next == listener) {
					iterator.remove();
				}
			}
		} finally {
			invocationListenersLock.writeLock().unlock();
		}
	}
	
	@Reference(
			bind = "registerProcessDeploymentListener",
			unbind = "unregisterProcessDeploymentListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ProcessDeploymentListener.class
	)
	public void registerProcessDeploymentListener(ProcessDeploymentListener listener) {
		if (listener != null) {
			deploymentListenersLock.writeLock().lock();
			try {
				deploymentListeners.add(listener);
			} finally {
				deploymentListenersLock.writeLock().unlock();
			}
		}
	}

	public void unregisterProcessDeploymentListener(ProcessDeploymentListener listener) {
		deploymentListenersLock.writeLock().lock();
		try {
			Iterator<ProcessDeploymentListener> iterator = deploymentListeners.iterator();
			while (iterator.hasNext()) {
				ProcessDeploymentListener next = iterator.next();
				if (next == listener) {
					iterator.remove();
				}
			}
		} finally {
			deploymentListenersLock.writeLock().unlock();
		}
	}

	private void appendVariableTypeToEngines(ProcessVariableType processVariableType) {
		enginesLock.readLock().lock();
		try {
			for (Map.Entry<String, KnownProcessEngine> entry : engines.entrySet()) {
				KnownProcessEngine knownProcessEngine = entry.getValue();
				appendVariableTypeToConfig(knownProcessEngine, processVariableType);
			}
		} finally {
			enginesLock.readLock().unlock();
		}
	}

	private void appendVariableTypeToConfig(KnownProcessEngine knownProcessEngine, ProcessVariableType processVariableType) {
		if (VariableType.class.isInstance(processVariableType)) {
			if (ProcessEngineConfigurationImpl.class.isInstance(knownProcessEngine.getConfiguration())) {
				String typeName = ((VariableType) processVariableType).getTypeName();
				LOG.info("adding process variable type {} to process engine {}", typeName, knownProcessEngine.getEngineName());
				ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) knownProcessEngine.getConfiguration();
				VariableTypes types = config.getVariableTypes();
				int index = types.getTypeIndex(SerializableType.TYPE_NAME);
				if (index >= 0) {
					types.addType((VariableType) processVariableType, index);
				} else {
					types.addType((VariableType) processVariableType);
				}
			}
		}
	}

	private void removeVariableTypeFromEngines(ProcessVariableType processVariableType) {
		enginesLock.readLock().lock();
		try {
			for (Map.Entry<String, KnownProcessEngine> entry : engines.entrySet()) {
				String engineName = entry.getKey();
				KnownProcessEngine knownProcessEngine = entry.getValue();
				removeVariableTypeFromConfig(knownProcessEngine, processVariableType);
			}
		} finally {
			enginesLock.readLock().unlock();
		}
	}

	private void removeVariableTypeFromConfig(KnownProcessEngine knownProcessEngine, ProcessVariableType processVariableType) {
		if (VariableType.class.isInstance(processVariableType)) {
			if (ProcessEngineConfigurationImpl.class.isInstance(knownProcessEngine.getConfiguration())) {
				String typeName = ((VariableType) processVariableType).getTypeName();
				LOG.info("removing process variable type {} from process engine {}", typeName, knownProcessEngine.getEngineName());
				VariableTypes types = ((ProcessEngineConfigurationImpl) knownProcessEngine.getConfiguration()).getVariableTypes();
				int index = types.getTypeIndex(typeName);
				if (index >= 0) {
					types.removeType((VariableType) processVariableType);
				}
			}
		}
	}

	public ProcessEngine createEngine(final DataSource datasource, final ActivitiEngineConfiguration configuration) {
		if (datasource == null) {
			LOG.info("not data source for engine '{}' provided", configuration.getName());
			return null;
		}
		String engineName = configuration.getName();
		LOG.info("creating process engine '{}'", engineName);
		ProcessEngine engine = createEngine(configuration.getSchema(), engineName, datasource, configuration);
		enginesLock.readLock().lock();
		try {
			KnownProcessEngine kpe = engines.get(engineName);
			variableTypesLock.readLock().lock();
			try {
				LOG.info("adding {} process variable types to process engine {}", variableTypes.size(), kpe.getEngineName());
				for (ProcessVariableType processVariableType : variableTypes) {
					appendVariableTypeToConfig(kpe, processVariableType);
				}
			} finally {
				variableTypesLock.readLock().unlock();
			}
			listenersLock.readLock().lock();
			try {
				for (EngineProviderListener engineProviderListener : listeners) {
					engineProviderListener.createdEngine(engineName, kpe.getProcessInstanceService(), kpe.getProcessDeploymentService(), kpe.bpDataStore);
				}
			} finally {
				listenersLock.readLock().unlock();
			}
			return engine;
		} finally {
			enginesLock.readLock().unlock();
		}
	}
	
	public void destroyEngine(ProcessEngine engine) {
		enginesLock.writeLock().lock();
		try {
			Iterator<KnownProcessEngine> iterator = engines.values().iterator();
			while (iterator.hasNext()) {
				KnownProcessEngine next = iterator.next();
				if (next.getEngine() == engine) {
					iterator.remove();
					turnOffKnownProcessengine(next);
				}
			}
		} finally {
			enginesLock.writeLock().unlock();
		}
	}
	
	public ProcessEngine createEngine(String schema, String engineName, DataSource datasource, ActivitiEngineConfiguration configuration) {
		if (schema != null) {
			if ("DB_SCHEMA_UPDATE_TRUE".equals(schema)) {
				schema = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE;
			} else if ("DB_SCHEMA_UPDATE_FALSE".equals(schema)) {
				schema = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE;
			} else if ("DB_SCHEMA_UPDATE_CREATE_DROP".equals(schema)) {
				schema = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;
			} else {
				schema = null;
			}
		}
		if (schema == null) {
			schema = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE;
		}
		ProcessEngineConfiguration config = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
		config = config.setClassLoader(myClassLoader);
		if (ProcessEngineConfigurationImpl.class.isInstance(config)) {
			ProcessEngineConfigurationImpl configImpl = (ProcessEngineConfigurationImpl) config;
			configImpl.setProcessEngineName(engineName);
			Map<String, JavaDelegate> map = customizer.createBeanMap(engineName);
			configImpl.setBeans((Map) map);
			if (configuration.getAsyncEnabled()) {
				configImpl.setAsyncExecutorEnabled(true);
				DefaultAsyncJobExecutor asyncExecutor = new DefaultAsyncJobExecutor();
				if (configuration.getAsyncCorePoolSize() != null) {
					asyncExecutor.setCorePoolSize(configuration.getAsyncCorePoolSize());
				}
				if (configuration.getAsyncMaxPoolSize() != null) {
					asyncExecutor.setMaxPoolSize(configuration.getAsyncMaxPoolSize());
				}
				if (configuration.getAsyncQueueSize() != null) {
					asyncExecutor.setQueueSize(configuration.getAsyncQueueSize());
				}
				configImpl.setAsyncExecutor(asyncExecutor);
			}
			((ProcessEngineConfigurationImpl) config).addConfigurator(new AbstractProcessEngineConfigurator() {
				@Override
				public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
					ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
					Thread.currentThread().setContextClassLoader(ActivitiEngineProvider.this.getClass().getClassLoader());
					try {
						ScriptingEngines scriptingEngines = new ScriptingEngines(new ScriptEngineManager() {
							@Override
							public ScriptEngine getEngineByName(String shortName) {
								ScriptEngine engine = super.getEngineByName(shortName);
								if (engine != null) {
									return engine;
								}
								LOG.info("loading script engine factories via service loader for 'getEngineByName({})'", shortName);
								Iterator<ScriptEngineFactory> iterator = ServiceLoader.load(ScriptEngineFactory.class).iterator();
								while (iterator.hasNext()) {
									ScriptEngineFactory scriptEngineFactory = iterator.next();
									if (scriptEngineFactory.getEngineName().equals(shortName)) {
										return scriptEngineFactory.getScriptEngine();
									}
								}
								LOG.error("failed to lookup script engine by name {}", shortName);
								return null;
							}

							@Override
							public ScriptEngine getEngineByExtension(String extension) {
								ScriptEngine engine = super.getEngineByExtension(extension);
								if (engine != null) {
									return engine;
								}
								LOG.info("loading script engine factories via service loader for 'getEngineByExtension({})'", extension);
								Iterator<ScriptEngineFactory> iterator = ServiceLoader.load(ScriptEngineFactory.class).iterator();
								while (iterator.hasNext()) {
									ScriptEngineFactory scriptEngineFactory = iterator.next();
									List<String> extensions = scriptEngineFactory.getExtensions();
									if (extensions == null) {
										continue;
									}
									if (extensions.contains(extension)) {
										return scriptEngineFactory.getScriptEngine();
									}
								}
								LOG.error("failed to lookup script engine by extension {}", extension);
								return null;
							}

							@Override
							public ScriptEngine getEngineByMimeType(String mimeType) {
								ScriptEngine engine = super.getEngineByMimeType(mimeType);
								if (engine != null) {
									return engine;
								}
								LOG.info("loading script engine factories via service loader for 'getEngineByMimeType({})'", mimeType);
								Iterator<ScriptEngineFactory> iterator = ServiceLoader.load(ScriptEngineFactory.class).iterator();
								while (iterator.hasNext()) {
									ScriptEngineFactory scriptEngineFactory = iterator.next();
									List<String> mimeTypes = scriptEngineFactory.getMimeTypes();
									if (mimeTypes == null) {
										continue;
									}
									if (mimeTypes.contains(mimeType)) {
										return scriptEngineFactory.getScriptEngine();
									}
								}
								LOG.error("failed to lookup script engine by extension {}", mimeType);
								return null;
							}

							@Override
							public List<ScriptEngineFactory> getEngineFactories() {
								List<ScriptEngineFactory> result = new ArrayList<>(super.getEngineFactories());
								LOG.info("loading script engine factories via service loader for 'getEngineFactories'");
								Iterator<ScriptEngineFactory> iterator = ServiceLoader.load(ScriptEngineFactory.class).iterator();
								while (iterator.hasNext()) {
									ScriptEngineFactory scriptEngineFactory = iterator.next();
									result.add(scriptEngineFactory);
								}
								return Collections.unmodifiableList(result);
							}
						});
						processEngineConfiguration.setScriptingEngines(scriptingEngines);
					} finally {
						Thread.currentThread().setContextClassLoader(ctxtLoader);
					}
				}

				@Override
				public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
					processEngineConfiguration.getScriptingEngines().setScriptBindingsFactory(new ScriptBindingsFactory(processEngineConfiguration.getResolverFactories()));
				}
			});
		} else {
			LOG.warn("ProcessEngineConfiguration is not a ProcessEngineConfigurationImpl, which is required for setting a map of beans visible to the service task expressions.");
		}
		ProcessEngine engine = config.setDataSource(datasource).setDatabaseSchemaUpdate(schema).buildProcessEngine();
		KnownProcessEngine kpe = new KnownProcessEngine(configuration, config, engine, engineName, datasource);
		enginesLock.writeLock().lock();
		try {
			engines.put(engineName, kpe);
			kpe.registerAsOsgiService();
		} finally {
			enginesLock.writeLock().unlock();
		}
		LOG.info("created BPM engine {}", engineName);
		return engine;
	}

	private class KnownProcessEngine {

		private final ActivitiEngineConfiguration engineConfiguration;
		private final ProcessEngineConfiguration configuration;
		private final ProcessEngine engine;
		private final String engineName;
		private final DataSource dataSource;
		private final ProcessInstanceServiceImpl processInstanceService;
		private final ProcessDeploymentServiceImpl deploymentService;
		private final List<ProcessVariableType> variableTypes = new ArrayList<>();
		private final BusinessProcessDataStore bpDataStore;
		private final BusinessProcessDataStoreEventListener installProcessesListener;
		private ServiceRegistration<ProcessEngine> reg;

		public KnownProcessEngine(ActivitiEngineConfiguration engineConfiguration, ProcessEngineConfiguration configuration, ProcessEngine engine, String engineName, DataSource dataSource) {
			this.engineConfiguration = engineConfiguration;
			this.configuration = configuration;
			this.engine = engine;
			this.engineName = engineName;
			this.dataSource = dataSource;
			processInstanceService = new ProcessInstanceServiceImpl(invocationListeners, invocationListenersLock);
			deploymentService = new ProcessDeploymentServiceImpl(engineName, deploymentListeners, deploymentListenersLock);

			processInstanceService.setHistoryService(engine.getHistoryService());
			processInstanceService.setRuntimeService(engine.getRuntimeService());
			EventHandlerRegistry eventHandlerRegistry = new EventHandlerRegistry();
			processInstanceService.setEventHandlerRegistry(eventHandlerRegistry);
			processInstanceService.setProcessDeploymentService(deploymentService);

			bpDataStore = bpDataStoreFactory.create(engineName);
			
			installProcessesListener = new BusinessProcessDataStoreEventListener() {

				@Override
				public void onUpdatedBPMNData(BusinessProcessDataStore businessProcessDataStore, BusinessProcessData businessProcessData) {
					ReplayableInputStream is = businessProcessData.getInputStream();
					if (is != null) {
						try {
							LOG.info("deploying process from data {} to engine {}", businessProcessData.getName(), getEngineName());
							getProcessDeploymentService().deploy(businessProcessData);
						} catch (ProcessDeploymentException ex) {
							LOG.error("failed to update process definition: " + ex.getMessage(), ex);
						}
					} else {
						LOG.info(
							"skipping deployment of process from data {} to engine {} because the data did not contain an inputstream", 
							businessProcessData.getName(), 
							getEngineName()
						);
					}
				}

				@Override
				public void onDeletedBPMNData(BusinessProcessDataStore businessProcessDataStore, BusinessProcessData businessProcessData) {
				}

				@Override
				public void onCreatedBPMNData(BusinessProcessDataStore businessProcessDataStore, BusinessProcessData businessProcessData) {
					onUpdatedBPMNData(businessProcessDataStore, businessProcessData); // update and create shall have the same effect.
				}
			};
			bpDataStore.addListener(installProcessesListener);
			
			deploymentService.setBusinessProcessDataStore(bpDataStore);
			deploymentService.setRepositoryService(engine.getRepositoryService());
		}

		public ActivitiEngineConfiguration getEngineConfiguration() {
			return engineConfiguration;
		}

		public List<ProcessVariableType> getVariableTypes() {
			return variableTypes;
		}

		public ProcessEngineConfiguration getConfiguration() {
			return configuration;
		}

		public DataSource getDataSource() {
			return dataSource;
		}

		public ProcessEngine getEngine() {
			return engine;
		}

		public String getEngineName() {
			return engineName;
		}

		private ProcessInstanceService getProcessInstanceService() {
			return processInstanceService;
		}

		private ProcessDeploymentService getProcessDeploymentService() {
			return deploymentService;
		}

		private void registerAsOsgiService() {
			if (reg == null && engine != null && componentContext != null) {
				reg = ServiceRegistrationBuilder.newInstance(ProcessEngine.class, engine)
						.pid(ProcessEngine.class.getName() + "." + getEngineName())
						.property("name", getEngineName())
						.register(componentContext.getBundleContext());
			}
		}

		private void unregisterOsgiService() {
			if (reg != null) {
				reg.unregister();
				reg = null;
			}
		}
	}

	@Activate
	public void activate(ComponentContext componentContext) {
		isActivatedLock.writeLock().lock();
		try {
			try {
				LOG.info("activating process engine provider");
				this.componentContext = componentContext;
				customizer = new JavaDelegateTracker(componentContext.getBundleContext());
				javaDelegateTracker = new ServiceTracker(componentContext.getBundleContext(), JavaDelegate.class, customizer);
				javaDelegateTracker.open();

				engineConfigurationsLock.writeLock().lock();
				try {
					for (ActivitiEngineConfiguration configuration : engineConfigurations) {
						activateConfiguration(configuration, componentContext);
					}
				} finally {
					engineConfigurationsLock.writeLock().unlock();
				}
				this.isActivated = true;
			} finally {
				LOG.info("activated process engine provider");
			}
		} finally {
			isActivatedLock.writeLock().unlock();
		}
	}

	private void deactivateConfiguration(ActivitiEngineConfiguration configuration) {
		int hc = System.identityHashCode(configuration);
		engineConfigurationsLock.writeLock().lock();
		try {
			DataSourceTracker tracker = dataSourceTrackers.get(hc);
			if (tracker != null) {
				tracker.close();
				dataSourceTrackers.remove(hc);
			}
		} finally {
			engineConfigurationsLock.writeLock().unlock();
		}
	}
	
	private void activateConfiguration(ActivitiEngineConfiguration configuration, ComponentContext componentContext) {
		if (componentContext == null) {
			return;
		}
		int hc = System.identityHashCode(configuration);
		engineConfigurationsLock.writeLock().lock();
		try {
			DataSourceTracker tracker = dataSourceTrackers.get(hc);
			if (tracker == null) {
				tracker = new DataSourceTracker(configuration, this, componentContext.getBundleContext());
				dataSourceTrackers.put(hc, tracker);
				tracker.open();
			}
		} finally {
			engineConfigurationsLock.writeLock().unlock();
		}
	}

	@Deactivate
	public void deactivate() {
		isActivatedLock.writeLock().lock();
		try {
			LOG.info("deactivating process engine provider");
			if (javaDelegateTracker != null) {
				javaDelegateTracker.close();
				javaDelegateTracker = null;
			}
			customizer = null;
			engineConfigurationsLock.writeLock().lock();
			try {
				engineConfigurations.clear();
				for (DataSourceTracker value : dataSourceTrackers.values()) {
					value.close();
				}
				dataSourceTrackers.clear();
			} finally {
				engineConfigurationsLock.writeLock().unlock();
			}
			enginesLock.writeLock().lock();
			try {
				for (Map.Entry<String, KnownProcessEngine> entry : engines.entrySet()) {
					KnownProcessEngine knownProcessEngine = entry.getValue();
					turnOffKnownProcessengine(knownProcessEngine);
				}
				engines.clear();
			} finally {
				enginesLock.writeLock().unlock();
			}
			invocationListenersLock.writeLock().lock();
			try {
				invocationListeners.clear();
			} finally {
				invocationListenersLock.writeLock().unlock();
			}
			deploymentListenersLock.writeLock().lock();
			try {
				deploymentListeners.clear();
			} finally {
				deploymentListenersLock.writeLock().unlock();
			}
			isActivated = false;
		} finally {
			isActivatedLock.writeLock().unlock();
		}
	}

	private void turnOffKnownProcessengine(KnownProcessEngine knownProcessEngine) {
		knownProcessEngine.unregisterOsgiService();
		variableTypesLock.readLock().lock();
		try {
			for (ProcessVariableType processVariableType : variableTypes) {
				removeVariableTypeFromConfig(knownProcessEngine, processVariableType);
			}
		} finally {
			variableTypesLock.readLock().unlock();
		}
		LOG.info("closing process engine {}", knownProcessEngine.getEngineName());
		try {
			knownProcessEngine.getEngine().close();
		} catch (Exception e) {
			LOG.error("failed to close activiti process engine: " + e.getMessage(), e);
		}
		knownProcessEngine.bpDataStore.removeListener(knownProcessEngine.installProcessesListener);
		listenersLock.readLock().lock();
		try {
			for (EngineProviderListener engineProviderListener : listeners) {
				try {
					engineProviderListener.destroyedEngine(
						knownProcessEngine.getEngineName(), 
						knownProcessEngine.getProcessInstanceService(), 
						knownProcessEngine.getProcessDeploymentService(), 
						knownProcessEngine.bpDataStore
					);
				} catch (Exception e) {
					LOG.error("process engine provider listener threw an exception while deactivating engine provider: " + e.getMessage(), e);
				}
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}
	
	@Override
	public ProcessInstanceService getInstanceServiceByEngineName(String engineName) {
		enginesLock.readLock().lock();
		try {
			KnownProcessEngine engine = engines.get(engineName);
			if (engine != null) {
				return engine.getProcessInstanceService();
			}
			return null;
		} finally {
			enginesLock.readLock().unlock();
		}
	}

	public ProcessDeploymentService getDeploymentServiceByEngineName(String engineName) {
		enginesLock.readLock().lock();
		try {
			KnownProcessEngine engine = engines.get(engineName);
			if (engine != null) {
				return engine.getProcessDeploymentService();
			}
			return null;
		} finally {
			enginesLock.readLock().unlock();
		}
	}

	public void setBpDataStoreFactory(BusinessProcessDataStoreFactory bpDataStoreFactory) {
		this.bpDataStoreFactory = bpDataStoreFactory;
	}

}
