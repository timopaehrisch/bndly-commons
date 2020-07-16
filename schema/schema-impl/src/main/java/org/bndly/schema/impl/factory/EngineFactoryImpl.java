package org.bndly.schema.impl.factory;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.common.datasource.DataSourcePool;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.services.EngineFactory;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.ConstraintRegistryImpl;
import org.bndly.schema.impl.DefaultLobHandler;
import org.bndly.schema.impl.DeployerImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.impl.MediatorRegistryImpl;
import org.bndly.schema.impl.QueryContextFactoryImpl;
import org.bndly.schema.impl.SingleConnectionTransactionTemplateImpl;
import org.bndly.schema.impl.TableRegistryImpl;
import org.bndly.schema.impl.TransactionFactoryImpl;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.impl.TransactionTemplateImpl;
import org.bndly.schema.impl.VirtualAttributeAdapterRegistryImpl;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.impl.nquery.expression.DelegatingExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.EqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.InRangeExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.TypedExpressionStatementHandler;
import org.bndly.schema.impl.vendor.VendorConfigurations;
import org.bndly.schema.vendor.VendorConfiguration;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaProvider;
import org.bndly.schema.model.Type;
import org.bndly.schema.vendor.AttributeMediatorFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
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
@Component(service = EngineFactory.class, immediate = true)
public class EngineFactoryImpl implements EngineFactory {
	private static final Logger LOG = LoggerFactory.getLogger(EngineFactoryImpl.class);
	@Reference
	private DataSourcePool dataSourcePool;
	private final List<SchemaDeploymentListener> deploymentListeners = new ArrayList<>();
	private final ReadWriteLock deploymentListenersLock = new ReentrantReadWriteLock();
	
	private final List<EngineServicesListener> engineServiceListeners = new ArrayList<>();
	private final ReadWriteLock engineServicesLock = new ReentrantReadWriteLock();
	
	@Reference
	private RecordJsonConverter recordJsonConverter;
	private final DelegatingExpressionStatementHandler expressionStatementHandler = new DelegatingExpressionStatementHandler();
	private ComponentContext componentContext;
	private final List<Runnable> lazyInits = new ArrayList<>();
	private final List<ServiceRegistration<VendorConfiguration>> vendorConfigurationRegistrations = new ArrayList<>();

	
	@Reference(
			bind = "addEngineConfiguration", 
			unbind = "removeEngineConfiguration", 
			cardinality = ReferenceCardinality.MULTIPLE, 
			policy = ReferencePolicy.DYNAMIC, 
			service = EngineConfiguration.class
	)
	public void addEngineConfiguration(final EngineConfiguration engineConfiguration) {
		addEngineConfigurationInternal(engineConfiguration, true);
	}
	
	private void addEngineConfigurationInternal(final EngineConfiguration engineConfiguration, boolean allowLazyInit) {
		if (engineConfiguration != null) {
			if (componentContext == null) {
				// lazy init
				if (allowLazyInit) {
					LOG.info("deferring engine service listeners setup until activation of engine factory");
					lazyInits.add(new Runnable() {
						@Override
						public void run() {
							addEngineConfigurationInternal(engineConfiguration, false);
						}
					});
				} else {
					LOG.error("could not set up engine service listener, because component context was null and lazy init is prevented.");
				}
			} else {
				SchemaBeanServicesListener schemaBeanServicesListener = new SchemaBeanServicesListener(componentContext.getBundleContext(), engineConfiguration.getSchema()) {
					
					private SchemaBeanFactory schemaBeanFactory;
					private ServiceRegistration<SchemaBeanFactory> schemaBeanFactoryReg;
					
					@Override
					protected void onReady(Engine engine, SchemaBeanProvider schemaBeanProvider) {
						schemaBeanFactory = new SchemaBeanFactory(schemaBeanProvider);
						JSONSchemaBeanFactory jsbf = new JSONSchemaBeanFactory();
						schemaBeanFactory.setJsonSchemaBeanFactory(jsbf);
						schemaBeanFactory.setEngine(engine);
						registerJavaInterfacesToBeanFactories(schemaBeanProvider, schemaBeanFactory, jsbf, engine.getDeployer().getDeployedSchema());
						schemaBeanFactoryReg = ServiceRegistrationBuilder
								.newInstance(SchemaBeanFactory.class, schemaBeanFactory)
								.pid(SchemaBeanFactory.class.getName() + "." + schemaBeanProvider.getSchemaName())
								.property("schema", schemaBeanProvider.getSchemaName())
								.register(bundleContext);
					}
					
					@Override
					protected void onShutdown(Engine engine, SchemaBeanProvider schemaBeanProvider) {
						schemaBeanFactory = null;
						if (schemaBeanFactoryReg != null) {
							schemaBeanFactoryReg.unregister();
							schemaBeanFactoryReg = null;
						}
					}
					
					private void registerJavaInterfacesToBeanFactories(
							SchemaBeanProvider schemaBeanProvider,
							SchemaBeanFactory schemaBeanFactory,
							JSONSchemaBeanFactory jsonSchemaBeanFactory,
							Schema schema
					) {
						String schemaBeanPackage = schemaBeanProvider.getSchemaBeanPackage();
						List<Type> types = schema.getTypes();
						if (types != null) {
							for (Type type : types) {
								String className = schemaBeanPackage + "." + type.getName();
								try {
									Class<?> clazz = schemaBeanProvider.getSchemaBeanClassLoader().loadClass(className);
									schemaBeanFactory.registerTypeBinding(type.getName(), clazz);
									jsonSchemaBeanFactory.registerTypeBinding(type.getName(), clazz);
								} catch (ClassNotFoundException ex) {
									// if a class can not be found, we silently ignore that
									LOG.warn("could not find class {} to register in schema bean factory");
								}
							}
						}
						List<Mixin> mixins = schema.getMixins();
						if (mixins != null) {
							for (Mixin mixin : mixins) {
								String className = schemaBeanPackage + "." + mixin.getName();
								try {
									Class<?> clazz = schemaBeanProvider.getSchemaBeanClassLoader().loadClass(className);
									schemaBeanFactory.registerTypeBinding(mixin.getName(), clazz);
									jsonSchemaBeanFactory.registerTypeBinding(mixin.getName(), clazz);
								} catch (ClassNotFoundException ex) {
									// if a class can not be found, we silently ignore that
									LOG.warn("could not find class {} to register in schema bean factory");
								}
							}
						}
					}
				};
				EngineServicesListener engineServicesListener = new EngineServicesListener(componentContext.getBundleContext(), engineConfiguration) {
					
					private Engine engine;
					private ServiceRegistration<Engine> engineReg;
					private ListenerTracker listenerTracker;
					
					@Override
					protected void onReady(DataSource dataSource, SchemaProvider schemaProvider) {
						LOG.info("creating schema engine for schema " + schemaProvider.getSchemaName());
						engine = createEngine(
								dataSource,
								engineConfiguration.getSchema(),
								engineConfiguration.getVendorConfig(),
								engineConfiguration.getConnection(),
								engineConfiguration.isValidateOnly(),
								engineConfiguration.isValidationErrorIgnored()
						);
						LOG.info("created schema engine for schema " + schemaProvider.getSchemaName());
						try {
							// start tracking listeners for that particular engine
							listenerTracker = new ListenerTracker(bundleContext, engine, schemaProvider.getSchemaName());
							listenerTracker.open();
							engine.getDeployer().deploy(schemaProvider.getSchema());
							LOG.info("deployed schema to engine " + schemaProvider.getSchemaName());
						} catch (Exception e) {
							LOG.error("failed to deploy schema to engine " + schemaProvider.getSchemaName(), e);
							return;
						}
						engineReg = ServiceRegistrationBuilder
								.newInstance(Engine.class, engine)
								.pid(Engine.class.getName() + "." + schemaProvider.getSchemaName())
								.register(bundleContext);
					}

					@Override
					protected void onShutdown(DataSource dataSource, SchemaProvider schemaProvider) {
						LOG.info("shutting down schema engine for schema " + engineConfiguration.getSchema());
						if (listenerTracker != null) {
							listenerTracker.close();
							listenerTracker = null;
						}
						engine = null;
						if (engineReg != null) {
							engineReg.unregister();
							engineReg = null;
						}
					}

					@Override
					public synchronized void destroy() {
						if (engineReg != null) {
							engineReg.unregister();
							engineReg = null;
						}
						engine = null;
						super.destroy();
					}

					@Override
					protected Engine getEngine() {
						return engine;
					}
					
				};
				engineServicesLock.writeLock().lock();
				try {
					schemaBeanServicesListener.init();
					engineServicesListener.init();
					engineServiceListeners.add(engineServicesListener);
				} catch (InvalidSyntaxException e) {
					LOG.error("could not init engine services listener");
					engineServicesListener.destroy();
					schemaBeanServicesListener.destroy();
				} finally {
					engineServicesLock.writeLock().unlock();
				}
			}
		}
	}
	
	public void removeEngineConfiguration(EngineConfiguration engineConfiguration) {
		if (engineConfiguration != null) {
			engineServicesLock.writeLock().lock();
			try {
				Iterator<EngineServicesListener> iterator = engineServiceListeners.iterator();
				while (iterator.hasNext()) {
					EngineServicesListener next = iterator.next();
					if (next.getEngineConfiguration() == engineConfiguration) {
						next.destroy();
						iterator.remove();
					}
				}
			} finally {
				engineServicesLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "registerSchemaDeploymentListener", 
			unbind = "unregisterSchemaDeploymentListener", 
			cardinality = ReferenceCardinality.MULTIPLE, 
			policy = ReferencePolicy.DYNAMIC, 
			service = SchemaDeploymentListener.class
	)
	public void registerSchemaDeploymentListener(SchemaDeploymentListener listener) {
		if (listener != null) {
			deploymentListenersLock.writeLock().lock();
			try {
				deploymentListeners.add(0, listener);
				engineServicesLock.readLock().lock();
				try {
					for (EngineServicesListener engineServicesListener : engineServiceListeners) {
						Engine engine = engineServicesListener.getEngine();
						if (engine != null) {
							listener.schemaDeployed(engine.getDeployer().getDeployedSchema(), engine);
						}
					}
				} finally {
					engineServicesLock.readLock().unlock();
				}
			} finally {
				deploymentListenersLock.writeLock().unlock();
			}
		}
	}

	public void unregisterSchemaDeploymentListener(SchemaDeploymentListener listener) {
		if (listener != null) {
			deploymentListenersLock.writeLock().lock();
			try {
				deploymentListeners.remove(listener);
			} finally {
				deploymentListenersLock.writeLock().unlock();
			}
		}
	}

	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
		
		expressionStatementHandler.addExpressionStatementHandler(new EqualExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new GreaterEqualExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new GreaterExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new LowerEqualExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new LowerExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new InRangeExpressionStatementHandler());
		expressionStatementHandler.addExpressionStatementHandler(new TypedExpressionStatementHandler());
		
		// register the built-in vendor configurations
		registerVendorConfig(EngineFactory.DIALECT_H2, VendorConfigurations.H2, componentContext.getBundleContext());
		registerVendorConfig(EngineFactory.DIALECT_MARIADB, VendorConfigurations.MARIADB, componentContext.getBundleContext());
		registerVendorConfig(EngineFactory.DIALECT_MYSQL, VendorConfigurations.MYSQL, componentContext.getBundleContext());
		registerVendorConfig(EngineFactory.DIALECT_MYSQL8, VendorConfigurations.MYSQL8, componentContext.getBundleContext());
		registerVendorConfig(EngineFactory.DIALECT_POSTGRES, VendorConfigurations.POSTGRES, componentContext.getBundleContext());
		try {
			for (Runnable lazyInit : lazyInits) {
				lazyInit.run();
			}
		} catch (Exception ex) {
			LOG.error("failed to activate engine factory: " + ex.getMessage(), ex);
		} finally {
			lazyInits.clear();
		}
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		engineServicesLock.writeLock().lock();
		try {
			for (EngineServicesListener engineServicesListener : engineServiceListeners) {
				engineServicesListener.destroy();
			}
			engineServiceListeners.clear();
		} finally {
			engineServicesLock.writeLock().unlock();
		}
		expressionStatementHandler.clear();
		deploymentListenersLock.writeLock().lock();
		try {
			deploymentListeners.clear();
		} finally {
			deploymentListenersLock.writeLock().unlock();
		}
		for (ServiceRegistration<VendorConfiguration> reg : vendorConfigurationRegistrations) {
			reg.unregister();
		}
		vendorConfigurationRegistrations.clear();
		this.componentContext = null;
	}
	
//	@Override
	public Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration) {
		return createEngine(dataSourceName, schemaName, vendorConfiguration, CONNECTION_POOLED);
	}
	
//	@Override
	public Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration, String connectionStrategy) {
		return createEngine(dataSourceName, schemaName, vendorConfiguration, connectionStrategy, false);
	}
	
//	@Override
	public Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration, String connectionStrategy, boolean validateOnly) {
		DataSource ds = dataSourcePool.getDataSource(dataSourceName);
		if (ds == null) {
			throw new IllegalStateException("could not create engine, because no datasource could be found for " + dataSourceName);
		}
		return createEngine(ds, schemaName, vendorConfiguration, connectionStrategy, validateOnly, false);
	}
	
	public Engine createEngine(DataSource dataSource, String schemaName, final VendorConfiguration vendorConfiguration, String connectionStrategy, boolean validateOnly, boolean validationErrorIgnored) {
		if (connectionStrategy == null) {
			connectionStrategy = CONNECTION_SINGLE;
		}

		if (vendorConfiguration == null) {
			throw new SchemaException("missing dialect for schema " + schemaName);
		}

		EngineImpl engineImpl = new EngineImpl();
		TransactionTemplate transactionTemplate;
		if (CONNECTION_POOLED.equals(connectionStrategy)) {
			TransactionTemplateImpl templateImpl = new TransactionTemplateImpl(vendorConfiguration, engineImpl);
			templateImpl.setDataSource(dataSource);
			templateImpl.setCloseConnectionAfterUsage(true);
			transactionTemplate = templateImpl;
		} else if (CONNECTION_SINGLE.equals(connectionStrategy)) {
			SingleConnectionTransactionTemplateImpl templateImpl = new SingleConnectionTransactionTemplateImpl(vendorConfiguration, engineImpl);
			templateImpl.setDataSource(dataSource);
			transactionTemplate = templateImpl;
		} else {
			LOG.warn("unsupported connection strategy: {}. falling back to single connection.", connectionStrategy);
			SingleConnectionTransactionTemplateImpl templateImpl = new SingleConnectionTransactionTemplateImpl(vendorConfiguration, engineImpl);
			templateImpl.setDataSource(dataSource);
			transactionTemplate = templateImpl;
		}
		
		String databaseSchemaName = dataSourcePool.getDatabaseSchemaNameForDataSource(dataSource);

		AccessorImpl accessorImpl = new AccessorImpl();
		accessorImpl.setExpressionStatementHandler(expressionStatementHandler);
		engineImpl.setAccessor(accessorImpl);
		TransactionFactoryImpl queryRunnerImpl = new TransactionFactoryImpl();
		engineImpl.setQueryRunner(queryRunnerImpl);
		QueryContextFactoryImpl queryContextFactoryImpl = new QueryContextFactoryImpl();
		engineImpl.setQueryContextFactory(queryContextFactoryImpl);
		VirtualAttributeAdapterRegistryImpl virtualAttributeAdapterRegistryImpl = new VirtualAttributeAdapterRegistryImpl();
		engineImpl.setVirtualAttributeAdapterRegistry(virtualAttributeAdapterRegistryImpl);
		ConstraintRegistryImpl constraintRegistryImpl = new ConstraintRegistryImpl();
		engineImpl.setConstraintRegistry(constraintRegistryImpl);
		DeployerImpl deployerImpl = new DeployerImpl(engineImpl);
		if (validateOnly) {
			deployerImpl.setValidateOnly(true);
		}
		if (validationErrorIgnored) {
			deployerImpl.setValidationErrorIgnored(true);
		}
		engineImpl.setDeployer(deployerImpl);
		TableRegistryImpl tableRegistryImpl = new TableRegistryImpl();
		tableRegistryImpl.setVendorConfiguration(vendorConfiguration);
		engineImpl.setTableRegistry(tableRegistryImpl);
		MediatorRegistryImpl mediatorRegistry = new MediatorRegistryImpl();
		engineImpl.setMediatorRegistry(mediatorRegistry);
		engineImpl.setVendorConfiguration(vendorConfiguration);

		LobHandler lobHandler = new DefaultLobHandler(vendorConfiguration, engineImpl);
		CryptoProvider cryptoProvider = createCryptoProvider();
		Map<Class<? extends Attribute>, AttributeMediator<?>> mediatorMap = new HashMap<>();
		AttributeMediatorFactory attributeMediatorFactory = vendorConfiguration.getAttributeMediatorFactory();
		mediatorMap.put(org.bndly.schema.model.StringAttribute.class, attributeMediatorFactory.createStringAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.DecimalAttribute.class, attributeMediatorFactory.createDecimalAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.BooleanAttribute.class, attributeMediatorFactory.createBooleanAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.DateAttribute.class, attributeMediatorFactory.createDateAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.InverseAttribute.class, attributeMediatorFactory.createInverseAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.TypeAttribute.class, attributeMediatorFactory.createTypeAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.MixinAttribute.class, attributeMediatorFactory.createMixinAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.BinaryAttribute.class, attributeMediatorFactory.createBinaryAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.JSONAttribute.class, attributeMediatorFactory.createJSONAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorMap.put(org.bndly.schema.model.CryptoAttribute.class, attributeMediatorFactory.createCryptoAttributeMediator(accessorImpl, recordJsonConverter, lobHandler, cryptoProvider));
		mediatorRegistry.setMediators(mediatorMap);

		tableRegistryImpl.setMediatorRegistry(mediatorRegistry);
		deployerImpl.setMediatorRegistry(mediatorRegistry);
		deployerImpl.setConstraintRegistry(constraintRegistryImpl);
		deployerImpl.setListeners(deploymentListeners, deploymentListenersLock);
		deployerImpl.setTableRegistry(tableRegistryImpl);
		deployerImpl.setDatabaseSchemaName(databaseSchemaName);
		deployerImpl.setTransactionTemplate(transactionTemplate);
		deployerImpl.setVendorConfiguration(vendorConfiguration);

		accessorImpl.setEngine(engineImpl);
		accessorImpl.setMediatorRegistry(mediatorRegistry);

		queryRunnerImpl.setTransactionTemplate(transactionTemplate);
		queryRunnerImpl.setEngineImpl(engineImpl);

		queryContextFactoryImpl.setVendorConfiguration(vendorConfiguration);
		queryContextFactoryImpl.setAccessor(accessorImpl);
		queryContextFactoryImpl.setMediatorRegistry(mediatorRegistry);
		queryContextFactoryImpl.setTableRegistry(tableRegistryImpl);
		
		return engineImpl;
	}
	
	private CryptoProvider createCryptoProvider() {
		return new CryptoProvider() {

			@Override
			public InputStream createDecryptingStream(InputStream stream, CryptoAttribute attribute) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public InputStream createEncryptingStream(InputStream inputStream, CryptoAttribute attribute) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public String base64Encode(InputStream stream) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public InputStream createBase64DecodingStream(String base64String) {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
		};
	}

	private void registerVendorConfig(String dialect, VendorConfiguration config, BundleContext context) {
		vendorConfigurationRegistrations.add(ServiceRegistrationBuilder.newInstance(VendorConfiguration.class, config).property("name", dialect).register(context));
	}

}
