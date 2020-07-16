package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.graph.CompiledBeanIteratorProviderImpl;
import org.bndly.common.graph.EntityCollectionDetector;
import org.bndly.common.graph.NoOpGraphListener;
import org.bndly.common.graph.ReferenceDetector;
import org.bndly.common.graph.TypeBasedReferenceDetector;
import org.bndly.common.mapper.DomainCollectionAdapter;
import org.bndly.common.mapper.MapCollectionTypeAdapter;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.common.mapper.NullReturningMissingMapperHandler;
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.service.shared.GenericResourceServiceImpl;
import org.bndly.common.service.shared.ReferableResourceCollectionDetector;
import org.bndly.common.service.shared.ReferableResourceDetector;
import org.bndly.common.service.shared.proxy.ServiceProxyFactory;
import org.bndly.common.service.client.dao.DefaultDao;
import org.bndly.common.service.client.dao.SchemaBeanDaoFactory;
import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.client.api.BackendAccountProvider;
import org.bndly.rest.client.api.ExceptionThrower;
import org.bndly.rest.client.api.HATEOASClientFactory;
import org.bndly.rest.client.api.LanguageProvider;
import org.bndly.rest.client.api.MessageClassesProvider;
import org.bndly.rest.client.api.ServiceFactory;
import org.bndly.rest.client.impl.hateoas.HATEOASClientFactoryImpl;
import org.bndly.rest.client.impl.hateoas.ServiceFactoryImpl;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.common.service.decorator.InvocationServiceDecorator;
import org.bndly.common.service.decorator.api.DecoratedProxyFactory;
import org.bndly.common.service.decorator.wrapper.ServiceProxyFactoryImpl;
import org.bndly.common.service.shared.GraphCycleUtilImpl;
import org.bndly.common.service.shared.api.GraphCycleUtil;
import org.bndly.rest.client.api.RequestInterceptor;
import org.bndly.rest.client.api.ResponseInterceptor;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.impl.hateoas.ExceptionThrowerImpl;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.rest.schema.beans.TypeBean;
import org.bndly.ssl.api.KeyStoreAccessProvider;
import org.bndly.ssl.impl.KeyStoreAccessProviderImpl;
import org.bndly.ssl.impl.SSLHttpClientFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Resource;
import org.apache.http.client.HttpClient;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ClientSetup {

	private static final BeanGraphIteratorListener NOOP = new NoOpGraphListener() {

		@Override
		public Class getIterationContextType() {
			return null;
		}
	};

	private static final Logger LOG = LoggerFactory.getLogger(ClientSetup.class);
	private String systemPropertyPrefix;
	private String hostUrl;
	private String keystoreLocation;
	private String keystorePassword;
	private boolean ignoreHost;
	private String backendAccountSecret;
	private String backendAccountName;
	private String defaultLanguage;
	private String sslProtocol;

	private Base64Service base64Service;
	private SSLHttpClientFactory httpClientFactory;
	private KeyStoreAccessProvider keyStoreAccessProvider;
	private HttpClient httpClient;
	private MessageClassesProvider messageClassesProvider;
	private ExceptionThrower exceptionThrower;
	private BackendAccountProvider backendAccountProvider;
	private LanguageProvider languageProvider;
	private HATEOASClientFactory hateoasClientFactory;
	private ServiceFactory serviceFactory;
	private DecoratedProxyFactory decoratedProxyFactory;
	private ServiceProxyFactory serviceProxyFactory;
	private BeanGraphIteratorListener createResourceServiceGraphListener;
	private BeanGraphIteratorListener readResourceServiceGraphListener;
	private BeanGraphIteratorListener updateResourceServiceGraphListener;
	private RewiringGraphListener rewiringGraphListener;
	private AcyclicGraphListener acyclicGraphListener;

	private List<ResponseInterceptor> responseInterceptors;
	private List<RequestInterceptor> requestInterceptors;
	private boolean didRegisterRequestResponseInterceptors;
	
	private final List<Runnable> destructionRunnables = new ArrayList<>();
	
	private final List<ServiceReference> serviceReferences = new ArrayList<>();
	private final List<SchemaReference> schemaReferences = new ArrayList<>();
	private final List<SchemaServiceConstructionGuide> schemaServiceConstructionGuides = new ArrayList<>();
	private final List<SchemaServiceStub> schemaServiceStubs = new ArrayList<>();
	private final List<JAXBMessageClassProvider> messageClassProviders = new ArrayList<>();
	private final Map<String, SchemaBeanDaoFactory> schemaBeanDaoFactoriesBySchemaName = new LinkedHashMap<>();
	private final Map<String, CompiledBeanIteratorProviderImpl> compiledBeanIteratorProvidersBySchemaName = new HashMap<>();
	private final Map<String, GraphCycleUtilImpl> graphCycleUtilsBySchemaName = new HashMap<>();
	private final Map<String, MapperFactory> mapperFactoriesBySchemaName = new LinkedHashMap<>();
	private final Map<String, Object> servicesByName = new HashMap<>();
	private final Map<Class, Object> servicesByType = new HashMap<>();
	private ThreadPoolExecutor threadPoolExecutor;
	private Integer corePoolSize;
	private Integer maximumPoolSize;
	private Integer keepAliveTime;
	private BlockingQueue<Runnable> blockingQueue;
	private Integer queueCapacity;
	private RejectedExecutionHandler rejectedExecutionHandler;
	private Boolean joinAtEndOfInitEnabled;

	private ClassLoader classLoader;
	private final ReadWriteLock exceptionThrowerStrategiesLock = new ReentrantReadWriteLock();
	private final List<ExceptionThrower.Strategy> exceptionThrowerStrategies = new ArrayList<>(ExceptionThrowerImpl.DEFAULT_STRATEGIES);

	public ClientSetup() {
		this(ClientSetup.class.getClassLoader());
	}

	public ClientSetup(ClassLoader customClassLoader) {
		this.classLoader = customClassLoader;
	}

	public ClientSetup init() throws ClassNotFoundException {
		/*
		 1. init connection to app container
		 2. load schemas
		 3. for each schema, services might be created
		 4. custom service stubs might be put into the client setup in advance
		 */
		final Map<String, List<String>> typeNamesBySchemaName = new HashMap<>();
		JAXBMessageClassProvider joinedJAXBMessageClassProvider = null;
		for (final SchemaReference schemaReference : schemaReferences) {
			if (joinedJAXBMessageClassProvider == null) {
				joinedJAXBMessageClassProvider = buildJoinedJAXBMessageClassProvider();
			}
			final SchemaBeanDaoFactory daoFactory = new SchemaBeanDaoFactory(schemaReference.getName(), schemaReference.getRestBeanPackage());
			destructionRunnables.add(new Runnable() {
				@Override
				public void run() {
					daoFactory.destroy();
				}
			});
			daoFactory.setServiceFactory(assertServiceFactoryExists());
			daoFactory.setMessageClassProvider(joinedJAXBMessageClassProvider);
			if (!schemaServiceConstructionGuides.isEmpty()) {
				daoFactory.init(new SchemaBeanDaoFactory.Listener() {

					@Override
					public void onSchemaLoaded(SchemaBean schemaBean) {
						onSchemaLoadedInternal(schemaBean, schemaReference);
					}

					@Override
					public void onDaoCreated(DefaultDao dao, String typeName) {
						List<String> listOfTypeNames = typeNamesBySchemaName.get(schemaReference.getName());
						if (listOfTypeNames == null) {
							listOfTypeNames = new ArrayList<>();
							typeNamesBySchemaName.put(schemaReference.getName(), listOfTypeNames);
						}
						listOfTypeNames.add(typeName);
					}

					@Override
					public void onMissingBeanClass(String typeName, Class<?> listRestBean, Class<?> restBean, Class<?> restReferenceBean) {
					}
					
				});
				// if we fully dynamically create services, we have to wait for the asynchronously created daos
				daoFactory.join();
			} else {
				daoFactory.init(new SchemaBeanDaoFactory.Listener() {

					@Override
					public void onSchemaLoaded(SchemaBean schemaBean) {
						onSchemaLoadedInternal(schemaBean, schemaReference);
					}

					@Override
					public void onDaoCreated(DefaultDao dao, String typeName) {
					}

					@Override
					public void onMissingBeanClass(String typeName, Class<?> listRestBean, Class<?> restBean, Class<?> restReferenceBean) {
					}
				});
			}
			schemaBeanDaoFactoriesBySchemaName.put(schemaReference.getName(), daoFactory);
		}

		List<ServiceInitializer> initializers = null;
		
		// generate schema service stubs with the schema service construction guides.
		// the guide provides the conventions for service class and interface names.
		List<SchemaServiceStub> generatedStubs = new ArrayList<>();
		for (SchemaServiceConstructionGuide guide : schemaServiceConstructionGuides) {
			List<String> typeNames = typeNamesBySchemaName.get(guide.getSchemaName());
			if (typeNames != null) {
				for (String typeName : typeNames) {
					SchemaServiceStub generateStub = new SchemaServiceStub();
					String customServiceClassImplementationName = 
							guide.getServiceImplementationPackage() 
							+ "." + guide.getCustomServicePrefix() + typeName + guide.getServiceImplementationSuffix();
					String defaultServiceClassImplementationName = 
							guide.getServiceImplementationPackage() 
							+ "." + guide.getDefaultServicePrefix() + typeName + guide.getServiceImplementationSuffix();
					String fullApiClassName = guide.getServiceApiPackage() + "." + typeName + guide.getServiceSuffix();
					generateStub.setSchemaName(guide.getSchemaName());
					try {
						generateStub.setCustomServiceClassName(customServiceClassImplementationName);
					} catch (ClassNotFoundException e) {
						// custom services are optional
					}
					generateStub.setGenericServiceClassName(defaultServiceClassImplementationName);
					generateStub.setFullApiClassName(fullApiClassName);

					boolean skip = false;
					for (SchemaServiceStub schemaServiceStub : schemaServiceStubs) {
						if (schemaServiceStub.getSchemaName().equals(guide.getSchemaName())) {
							if (
								generateStub.getFullApiClass().equals(schemaServiceStub.getFullApiClass()) 
								|| generateStub.getFullApiClass().isInstance(schemaServiceStub.getFullApi())
							) {
								skip = true;
								break;
							}
						}
					}
					if (!skip) {
						generatedStubs.add(generateStub);
					}
				}
			}
		}

		schemaServiceStubs.addAll(generatedStubs);
		// for all schema service stubs, we will initialize the service instances
		for (SchemaServiceStub schemaServiceStub : schemaServiceStubs) {
			SchemaBeanDaoFactory daoFactory = schemaBeanDaoFactoriesBySchemaName.get(schemaServiceStub.getSchemaName());
			Object fullApi = schemaServiceStub.getFullApi();
			Class fullApiClass = schemaServiceStub.getFullApiClass();
			// if there is an API for the service and the service instance does not exist yet...
			if (fullApi == null) {
				if (fullApiClass != null) {
					// ... create the composed service by looking up the generic service (generated)
					// and the custom service (not generated, but optional)
					Object genericService = schemaServiceStub.getGenericService();
					if (genericService == null) {
						Class genericServiceClass = schemaServiceStub.getGenericServiceClass();
						if (genericServiceClass != null) {
							genericService = InstantiationUtil.instantiateType(genericServiceClass);
							if (genericService != null && GenericResourceServiceImpl.class.isInstance(genericService)) {
								GenericResourceServiceImpl tmp = (GenericResourceServiceImpl) genericService;

								MapperFactory mapperFactory = assertMapperFactoryExistsForSchemaName(schemaServiceStub.getSchemaName());
								DefaultDao dao = daoFactory.getDaoForRestBeanType(tmp.getRestBeanType());
								final String schmeaBeanPackageName = findSchmeaBeanPackageName(tmp.getModelClass());
								tmp.setClientFactory(assertHATEOASClientFactoryExists());
								tmp.setMapperFactory(mapperFactory);
								tmp.setResourceDAO(dao);
								EntityCollectionDetector entityCollectionDetector = new ReferableResourceCollectionDetector(tmp.getModelClass().getPackage().getName());
								ReferenceDetector referenceDetector = new ReferableResourceDetector();
								TypeBasedReferenceDetector typeBasedReferenceDetector = new TypeBasedReferenceDetector() {
									@Override
									public boolean isReferencable(Class<?> type) {
										if (type.isPrimitive()) {
											return false;
										}
										if (type.getPackage().getName().equals(schmeaBeanPackageName)) {
											return true;
										}
										for (Class<?> aInterface : type.getInterfaces()) {
											if (aInterface.getPackage().getName().equals(schmeaBeanPackageName)) {
												return true;
											}
										}
										return false;
									}

									@Override
									public boolean isReferencableField(Field field) {
										return isReferencable(field.getType());
									}
								};
								tmp.setEntityCollectionDetector(entityCollectionDetector);
								tmp.setReferenceDetector(referenceDetector);
								CompiledBeanIteratorProviderImpl compiledBeanIteratorProvider = assertCompiledBeanIteratorProviderExists(schemaServiceStub.getSchemaName(), entityCollectionDetector, typeBasedReferenceDetector);
								tmp.setCompiledBeanIteratorProvider(compiledBeanIteratorProvider);
								tmp.setGraphCycleUtil(assertGraphCycleUtilExists(schemaServiceStub.getSchemaName(), compiledBeanIteratorProvider));
								tmp.setCreateResourceServiceGraphListener(assertCreateResourceServiceGraphListenerExists());
								tmp.setReadResourceServiceGraphListener(assertReadResourceServiceGraphListenerExists());
								tmp.setUpdateResourceServiceGraphListener(assertUpdateResourceServiceGraphListenerExists());
							}
						}
					}
					Object customService = schemaServiceStub.getCustomService();
					Class customServiceClass = schemaServiceStub.getCustomServiceClass();
					if (customService == null) {
						if (customServiceClass != null) {
							customService = InstantiationUtil.instantiateType(customServiceClass);
							if (customService != null) {
								// look for setters with @ServiceReference annotations. those will be auto-wired at the end of the init method
								if (initializers == null) {
									initializers = new ArrayList<>();
								}
								inspectServiceForSetters(customService, customServiceClass, initializers);
							}
						}
					} else {
						if (customServiceClass == null) {
							customServiceClass = customService.getClass();
						}
						if (initializers == null) {
							initializers = new ArrayList<>();
						}
						inspectServiceForSetters(customService, customServiceClass, initializers);
					}
					// if we have at least a generic service instance, we can but it behind the service API
					if (genericService != null) {
						// create a full api instance
						ServiceProxyFactory spf = assertServiceProxyFactoryExists();

						fullApi = spf.getInstance(classLoader, fullApiClass, genericService, customService);
					}
				}
			}
			if (fullApi != null && fullApiClass != null) {
				// this is for encapsulating the entire service layer. creating decorated services!
				DecoratedProxyFactory dpf = assertDecoratedProxyFactoryExists();

				fullApi = dpf.decorateService(classLoader,fullApi, fullApiClass);
				servicesByType.put(fullApiClass, fullApi);
				try {
					Field nameField = fullApiClass.getField("NAME");
					if (ReflectionUtil.fieldIsStatic(nameField) && nameField.getType().equals(String.class)) {
						String name = (String) nameField.get(null);
						if (name != null) {
							servicesByName.put(name, fullApi);
						}
					}
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
					// skip this
				}
			}
			schemaServiceStub.setFullApi(fullApi);
		}

		if (initializers != null) {
			ServiceInitializer.Context ctx = new ServiceInitializer.Context() {

				@Override
				public Object resolveReferencedServiceByName(String nameOfService) {
					return getServiceByName(nameOfService);
				}

				@Override
				public Object resolveReferencedServiceByType(Class typeOfService) {
					return getServiceByType(typeOfService);
				}
			};
			for (ServiceInitializer initializer : initializers) {
				initializer.init(ctx);
			}
		}

		if (assertJoinAtEndOfInitEnabledIsDefined()) {
			joinWithAllSchemaBeanDaoFactories();
		}
		return this;
	}

	private CompiledBeanIteratorProviderImpl assertCompiledBeanIteratorProviderExists(String schemaName, EntityCollectionDetector entityCollectionDetector, TypeBasedReferenceDetector typeBasedReferenceDetector) {
		CompiledBeanIteratorProviderImpl compiledBeanIteratorProvider = compiledBeanIteratorProvidersBySchemaName.get(schemaName);
		if (compiledBeanIteratorProvider == null) {
			compiledBeanIteratorProvider = new CompiledBeanIteratorProviderImpl();
			compiledBeanIteratorProvider.setEntityCollectionDetector(entityCollectionDetector);
			compiledBeanIteratorProvider.setReferenceDetector(typeBasedReferenceDetector);
			compiledBeanIteratorProvidersBySchemaName.put(schemaName, compiledBeanIteratorProvider);
		}
		return compiledBeanIteratorProvider;
	}

	private GraphCycleUtil assertGraphCycleUtilExists(String schemaName, CompiledBeanIteratorProviderImpl compiledBeanIteratorProvider) {
		GraphCycleUtilImpl graphCycleUtilImpl = graphCycleUtilsBySchemaName.get(schemaName);
		if (graphCycleUtilImpl == null) {
			graphCycleUtilImpl = new GraphCycleUtilImpl();
			graphCycleUtilImpl.setAcyclicGraphListener(assertAcyclicGraphListenerExists());
			graphCycleUtilImpl.setRewiringGraphListener(assertRewiringGraphListenerExists());
			graphCycleUtilImpl.setCompiledBeanIteratorProvider(compiledBeanIteratorProvider);
			graphCycleUtilsBySchemaName.put(schemaName, graphCycleUtilImpl);
		}
		return graphCycleUtilImpl;
	}
	
	private String findSchmeaBeanPackageName(Class modelClass) {
		if (modelClass == null) {
			return null;
		}
		if (!modelClass.isInterface()) {
			return null;
		}
		return modelClass.getPackage().getName();
	}
	
	public void destroy() {
		for (Runnable destructionRunnable : destructionRunnables) {
			destructionRunnable.run();
		}
	}
	
	private void joinWithAllSchemaBeanDaoFactories() {
		for (SchemaBeanDaoFactory schemaBeanDaoFactory : schemaBeanDaoFactoriesBySchemaName.values()) {
			// this join method will also shut down the internal executor service thread of the schema bean dao factory
			schemaBeanDaoFactory.join();
		}
	}
	
	private void onSchemaLoadedInternal(SchemaBean schemaBean, SchemaReference schemaReference) {
		List<TypeBean> types = schemaBean.getTypes();
		if (types != null) {
			for (TypeBean type : types) {
				String typeName = type.getName();
				//ClassLoader cl = getClass().getClassLoader();
				try {
					Class<?> modelImplClass = classLoader.loadClass(schemaReference.getModelImplPackage() + "." + typeName + MODEL_CLASS_SUFFIX);
					Class<?> listRestBeanClass = classLoader.loadClass(schemaReference.getRestBeanPackage() + "." + typeName + "ListRestBean");
					Class<?> restBeanClass = classLoader.loadClass(schemaReference.getRestBeanPackage() + "." + typeName + "RestBean");
					Class<?> referenceRestBeanClass = classLoader.loadClass(schemaReference.getRestBeanPackage() + "." + typeName + "ReferenceRestBean");
					MapperFactory mapperFactory = assertMapperFactoryExistsForSchemaName(schemaBean.getName());

					mapperFactory.buildAutoMappers(restBeanClass, modelImplClass);
					mapperFactory.buildAutoMappers(referenceRestBeanClass, modelImplClass);
					mapperFactory.buildCollection(listRestBeanClass, restBeanClass);
				} catch (ClassNotFoundException e) {
					LOG.warn("Could not find class! Message: " + e.getMessage());
				}
			}
		}
	}
	private static final String MODEL_CLASS_SUFFIX = "Impl";
	
	public List<SchemaServiceStub> getSchemaServiceStubs() {
		return Collections.unmodifiableList(schemaServiceStubs);
	}
	
	public Object getServiceByName(String nameOfService) {
		Object s = servicesByName.get(nameOfService);
		if (s == null) {
			for (ServiceReference serviceReference : serviceReferences) {
				if (nameOfService.equals(serviceReference.getName())) {
					return serviceReference.getService();
				}
			}
		}
		return s;
	}
	
	public <T> T getServiceByType(Class<T> typeOfService) {
		T t = (T) servicesByType.get(typeOfService);
		if (t == null) {
			for (ServiceReference serviceReference : serviceReferences) {
				if (typeOfService.equals(serviceReference.getServiceType()) || typeOfService.isInstance(serviceReference.getService())) {
					return (T) serviceReference.getService();
				}
			}
		}
		return t;
	}

	public MapperFactory assertMapperFactoryExistsForSchemaName(String schemaName) {
		MapperFactory mapperFactory = mapperFactoriesBySchemaName.get(schemaName);
		if (mapperFactory == null) {
			mapperFactory = new MapperFactory();
			mapperFactoriesBySchemaName.put(schemaName, mapperFactory);

			mapperFactory.registerMapper(new AtomLinkMapper());
			mapperFactory.registerMapper(new ReversedAtomLinkMapper());
			mapperFactory.setMissingMapperHandler(new NullReturningMissingMapperHandler());
			mapperFactory.setAmbiguityResolver(new DefaultMapperAmbiguityResolver());
			mapperFactory.addPreInterceptor(new MarkAsReferenceMappingPreInterceptor());
			mapperFactory.registerMapper(new ListRestBeanMapper());
			mapperFactory.setTypeInstanceBuilder(new DefaultTypeInstanceBuilder());
			mapperFactory.setComplexTypeDetector(new DefaultComplexTypeDetector());
			mapperFactory.register(new DomainCollectionAdapter());
			MapCollectionTypeAdapter mapCollectionTypeAdapter = new MapCollectionTypeAdapter();
			mapperFactory.register(mapCollectionTypeAdapter);
			mapCollectionTypeAdapter.addKeyBuilder(new AtomLinkKeyBuilder());
			mapperFactory.register(new ListRestBeanCollectionAdapter());
		}
		return mapperFactory;
	}

	public ServiceProxyFactory assertServiceProxyFactoryExists() {
		if (serviceProxyFactory == null) {
			serviceProxyFactory = new ServiceProxyFactory();
		}
		return serviceProxyFactory;
	}

	public BeanGraphIteratorListener assertRewiringGraphListenerExists() {
		if (rewiringGraphListener == null) {
			rewiringGraphListener = new RewiringGraphListener();
		}
		return rewiringGraphListener;
	}

	public BeanGraphIteratorListener assertAcyclicGraphListenerExists() {
		if (acyclicGraphListener == null) {
			acyclicGraphListener = new AcyclicGraphListener();
		}
		return acyclicGraphListener;
	}

	public BeanGraphIteratorListener assertUpdateResourceServiceGraphListenerExists() {
		if (updateResourceServiceGraphListener == null) {
			updateResourceServiceGraphListener = assertAcyclicGraphListenerExists();
		}
		return updateResourceServiceGraphListener;
	}
	
	public BeanGraphIteratorListener assertReadResourceServiceGraphListenerExists() {
		if (readResourceServiceGraphListener == null) {
			readResourceServiceGraphListener = assertRewiringGraphListenerExists();
		}
		return readResourceServiceGraphListener;
	}

	public BeanGraphIteratorListener assertCreateResourceServiceGraphListenerExists() {
		if (createResourceServiceGraphListener == null) {
			createResourceServiceGraphListener = assertAcyclicGraphListenerExists();
		}
		return createResourceServiceGraphListener;
	}

	public DecoratedProxyFactory assertDecoratedProxyFactoryExists() {
		if (decoratedProxyFactory == null) {
			decoratedProxyFactory = buildDecoratedProxyFactory();
		}
		return decoratedProxyFactory;
	}

	public ServiceFactory assertServiceFactoryExists() {
		if (serviceFactory == null) {
			serviceFactory = buildServiceFactory();
		}
		return serviceFactory;
	}
	
	public HATEOASClientFactory assertHATEOASClientFactoryExists() {
		if (hateoasClientFactory == null) {
			HATEOASClientFactoryImpl tmp = new HATEOASClientFactoryImpl(
				assertHttpClientExists(), 
				assertMessageClassesProviderExists(), 
				assertBackendAccountProviderExists(), 
				assertLanguageProviderExists(), 
				assertExceptionThrowerExists(), 
				base64Service
			);
			tmp.setThreadPoolExecutor(assertThreadPoolExecutorExists());
			this.hateoasClientFactory = tmp;
		}
		assertRequestResponseInterceptorsAreRegistered();
		return hateoasClientFactory;
	}

	public LanguageProvider assertLanguageProviderExists() {
		if (languageProvider == null) {
			languageProvider = buildLanguageProvider();
		}
		return languageProvider;
	}

	public BackendAccountProvider assertBackendAccountProviderExists() {
		if (backendAccountProvider == null) {
			backendAccountProvider = buildBackendAccountProvider();
		}
		return backendAccountProvider;
	}

	public ExceptionThrower assertExceptionThrowerExists() {
		if (exceptionThrower == null) {
			exceptionThrower = new ExceptionThrowerImpl(exceptionThrowerStrategies) {
				@Override
				public void throwException(Object error, int statusCode, String httpMethod, String url) throws ClientException {
					exceptionThrowerStrategiesLock.readLock().lock();
					try {
						super.throwException(error, statusCode, httpMethod, url);
					} finally {
						exceptionThrowerStrategiesLock.readLock().unlock();
					}
				}
				
			};
		}
		return exceptionThrower;
	}

	public MessageClassesProvider assertMessageClassesProviderExists() {
		if (messageClassesProvider == null) {
			messageClassesProvider = buildMessageClassesProvider();
		}
		return messageClassesProvider;
	}

	public HttpClient assertHttpClientExists() {
		if (httpClient == null) {
			httpClient = buildHttpClient();
		}
		return httpClient;
	}

	public KeyStoreAccessProvider assertKeyStoreAccessProviderExists() {
		if (keyStoreAccessProvider == null) {
			keyStoreAccessProvider = buildKeyStoreAccessProvider();
		}
		return keyStoreAccessProvider;
	}

	public SSLHttpClientFactory assertSSLHttpClientFactoryExists() {
		if (httpClientFactory == null) {
			httpClientFactory = new SSLHttpClientFactory();
		}
		return httpClientFactory;
	}

	public String getSystemPropertyPrefix() {
		return systemPropertyPrefix == null ? "" : systemPropertyPrefix;
	}

	private String getStringProperty(String key, String defaultValue) {
		String propertyValue = System.getProperty(key);
		if (propertyValue == null) {
			return defaultValue;
		}
		return propertyValue;
	}

	private Boolean getBooleanProperty(String key, Boolean defaultValue) {
		String propertyValue = System.getProperty(key);
		if (propertyValue == null) {
			return defaultValue;
		}
		return Boolean.valueOf(propertyValue);
	}

	///////////////////////////////////////
	// START - build default services 
	///////////////////////////////////////
	public DecoratedProxyFactory buildDecoratedProxyFactory() {
		ServiceProxyFactoryImpl tmp = new ServiceProxyFactoryImpl();
		tmp.registerDecorator(new InvocationServiceDecorator());
		return tmp;
	}

	public ServiceFactory buildServiceFactory() {
		final ServiceFactoryImpl tmp = new ServiceFactoryImpl();
		tmp.setClientFactory(assertHATEOASClientFactoryExists());
		tmp.setHostUrl(getStringProperty(getSystemPropertyPrefix() + "host.url", hostUrl));
		tmp.setRootResource(new Services());
		destructionRunnables.add(new Runnable() {
			@Override
			public void run() {
				tmp.destroy();
			}
		});
		tmp.init();
		return tmp;
	}

	public HttpClient buildHttpClient() {
		return assertSSLHttpClientFactoryExists().build(assertKeyStoreAccessProviderExists(), getBooleanProperty("ssl.ignore.host", ignoreHost));
	}

	public LanguageProvider buildLanguageProvider() {
		DefaultLanguageSetterAndProvider tmp = new DefaultLanguageSetterAndProvider();
		tmp.setDefaultLanguage(getStringProperty(getSystemPropertyPrefix() + "defaultlanguage", defaultLanguage));
		return tmp;
	}

	public BackendAccountProvider buildBackendAccountProvider() {
		DefaultBackendAccountProvider tmp = new DefaultBackendAccountProvider();
		tmp.setBackendAccountName(getStringProperty(getSystemPropertyPrefix() + "backendaccountname", backendAccountName));
		tmp.setBackendAccountSecret(getStringProperty(getSystemPropertyPrefix() + "backendaccountsecret", backendAccountSecret));
		return tmp;
	}

	public KeyStoreAccessProvider buildKeyStoreAccessProvider() {
		KeyStoreAccessProviderImpl tmp = new KeyStoreAccessProviderImpl();
		tmp.setKeyStoreLocation(getStringProperty("ssl.keyStoreLocation", keystoreLocation));
		tmp.setKeyStorePassword(getStringProperty("ssl.keyStorePassword", keystorePassword));
		tmp.setSecureSocketProtocol(getStringProperty("ssl.protocol", sslProtocol));
		return tmp;
	}

	public JAXBMessageClassProvider buildJoinedJAXBMessageClassProvider() {
		JAXBMessageClassProvider prov = new JAXBMessageClassProvider() {

			@Override
			public Collection<Class<?>> getJAXBMessageClasses() {
				HashSet<Class<?>> hashSet = new HashSet<>();
				for (JAXBMessageClassProvider messageClassProvider : messageClassProviders) {
					Collection<Class<?>> classes = messageClassProvider.getJAXBMessageClasses();
					hashSet.addAll(classes);
				}
				return hashSet;
			}
		};
		return prov;
	}

	public MessageClassesProvider buildMessageClassesProvider() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		ArrayList<Class<?>> errorClasses = new ArrayList<>();
		for (JAXBMessageClassProvider jaxbMessageClassProvider : messageClassProviders) {
			for (Class<?> classType : jaxbMessageClassProvider.getJAXBMessageClasses()) {
				if (ErrorRestBean.class.isAssignableFrom(classType)) {
					errorClasses.add(classType);
				}
				classes.add(classType);
			}
		}
		final Class[] _classes = classes.toArray(new Class[classes.size()]);
		final Class[] _errorClasses = errorClasses.toArray(new Class[errorClasses.size()]);
		return new MessageClassesProvider() {

			@Override
			public Class<?>[] getAllUseableMessageClasses() {
				return _classes;
			}

			@Override
			public Class<?>[] getAllErrorMessageClasses() {
				return _errorClasses;
			}
		};
	}

	///////////////////////////////////////
	// END - build default services 
	///////////////////////////////////////
	
	public ClientSetup addExceptionThrowerStrategy(ExceptionThrower.Strategy strategy) {
		if(strategy != null) {
			exceptionThrowerStrategiesLock.writeLock().lock();
			try {
				exceptionThrowerStrategies.add(strategy);
			} finally {
				exceptionThrowerStrategiesLock.writeLock().unlock();
			}
		}
		return this;
	}
	
	public ClientSetup removeExceptionThrowerStrategy(ExceptionThrower.Strategy strategy) {
		if (strategy != null) {
			exceptionThrowerStrategiesLock.writeLock().lock();
			try {
				Iterator<ExceptionThrower.Strategy> iterator = exceptionThrowerStrategies.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == strategy) {
						iterator.remove();
					}

				}
			} finally {
				exceptionThrowerStrategiesLock.writeLock().unlock();
			}
		}
		return this;
	}
	
	public ClientSetup addJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			messageClassProviders.add(messageClassProvider);
		}
		return this;
	}

	public ClientSetup removeJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			Iterator<JAXBMessageClassProvider> iterator = messageClassProviders.iterator();
			while (iterator.hasNext()) {
				JAXBMessageClassProvider next = iterator.next();
				if (next == messageClassProvider) {
					iterator.remove();
				}
			}
		}
		return this;
	}

	public ClientSetup addServiceReference(ServiceReference serviceReference) {
		if (serviceReference != null) {
			serviceReferences.add(serviceReference);
		}
		return this;
	}

	public ClientSetup removeServiceReference(ServiceReference serviceReference) {
		if (serviceReference != null) {
			Iterator<ServiceReference> iterator = serviceReferences.iterator();
			while (iterator.hasNext()) {
				ServiceReference next = iterator.next();
				if (next == serviceReference) {
					iterator.remove();
				}
			}
		}
		return this;
	}
	
	public ClientSetup addSchemaReference(SchemaReference schemaReference) {
		if (schemaReference != null) {
			schemaReferences.add(schemaReference);
		}
		return this;
	}

	public ClientSetup removeSchemaReference(SchemaReference schemaReference) {
		if (schemaReference != null) {
			Iterator<SchemaReference> iterator = schemaReferences.iterator();
			while (iterator.hasNext()) {
				SchemaReference next = iterator.next();
				if (next == schemaReference) {
					iterator.remove();
				}
			}
		}
		return this;
	}
	
	public ClientSetup addSchemaServiceConstructionGuide(SchemaServiceConstructionGuide schemaServiceConstructionGuide) {
		if (schemaServiceConstructionGuide != null) {
			schemaServiceConstructionGuides.add(schemaServiceConstructionGuide);
		}
		return this;
	}

	public ClientSetup removeSchemaServiceConstructionGuide(SchemaServiceConstructionGuide schemaServiceConstructionGuide) {
		if (schemaServiceConstructionGuide != null) {
			Iterator<SchemaServiceConstructionGuide> iterator = schemaServiceConstructionGuides.iterator();
			while (iterator.hasNext()) {
				SchemaServiceConstructionGuide next = iterator.next();
				if (next == schemaServiceConstructionGuide) {
					iterator.remove();
				}
			}
		}
		return this;
	}

	public ClientSetup addSchemaServiceStub(SchemaServiceStub schemaServiceStub) {
		if (schemaServiceStub != null) {
			schemaServiceStubs.add(schemaServiceStub);
		}
		return this;
	}

	public ClientSetup removeSchemaServiceStub(SchemaServiceStub schemaServiceStub) {
		if (schemaServiceStub != null) {
			Iterator<SchemaServiceStub> iterator = schemaServiceStubs.iterator();
			while (iterator.hasNext()) {
				SchemaServiceStub next = iterator.next();
				if (next == schemaServiceStub) {
					iterator.remove();
				}
			}
		}
		return this;
	}

	public ClientSetup setSchemaServiceConstructionGuides(List<SchemaServiceConstructionGuide> schemaServiceConstructionGuides) {
		this.schemaServiceConstructionGuides.clear();
		if (schemaServiceConstructionGuides != null) {
			this.schemaServiceConstructionGuides.addAll(schemaServiceConstructionGuides);
		}
		return this;
	}
	
	public ClientSetup setSchemaReferences(List<SchemaReference> schemaReferences) {
		this.schemaReferences.clear();
		if (schemaReferences != null) {
			this.schemaReferences.addAll(schemaReferences);
		}
		return this;
	}
	
	public ClientSetup setServiceReferences(List<ServiceReference> serviceReferences) {
		this.serviceReferences.clear();
		if (serviceReferences != null) {
			this.serviceReferences.addAll(serviceReferences);
		}
		return this;
	}

	public ClientSetup setSchemaServiceStubs(List<SchemaServiceStub> schemaServiceStubs) {
		this.schemaServiceStubs.clear();
		if (schemaServiceStubs != null) {
			this.schemaServiceStubs.addAll(schemaServiceStubs);
		}
		return this;
	}

	public ClientSetup setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
		return this;
	}

	public ClientSetup setKeystoreLocation(String keystoreLocation) {
		this.keystoreLocation = keystoreLocation;
		return this;
	}

	public ClientSetup setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
		return this;
	}

	public ClientSetup setIgnoreHost(boolean ignoreHost) {
		this.ignoreHost = ignoreHost;
		return this;
	}

	public ClientSetup setBackendAccountSecret(String backendAccountSecret) {
		this.backendAccountSecret = backendAccountSecret;
		return this;
	}

	public ClientSetup setBackendAccountName(String backendAccountName) {
		this.backendAccountName = backendAccountName;
		return this;
	}

	public ClientSetup setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
		return this;
	}

	public ClientSetup setSslProtocol(String sslProtocol) {
		this.sslProtocol = sslProtocol;
		return this;
	}

	public ClientSetup setBase64Service(Base64Service base64Service) {
		this.base64Service = base64Service;
		return this;
	}

	public ClientSetup setMessageClassProviders(List<JAXBMessageClassProvider> messageClassProviders) {
		this.messageClassProviders.clear();
		if (messageClassProviders != null) {
			this.messageClassProviders.addAll(messageClassProviders);
		}
		return this;
	}

	public ClientSetup setHttpClientFactory(SSLHttpClientFactory httpClientFactory) {
		this.httpClientFactory = httpClientFactory;
		return this;
	}

	public ClientSetup setKeyStoreAccessProvider(KeyStoreAccessProvider keyStoreAccessProvider) {
		this.keyStoreAccessProvider = keyStoreAccessProvider;
		return this;
	}

	public ClientSetup setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
		return this;
	}

	public ClientSetup setMessageClassesProvider(MessageClassesProvider messageClassesProvider) {
		this.messageClassesProvider = messageClassesProvider;
		return this;
	}

	public ClientSetup setExceptionThrower(ExceptionThrower exceptionThrower) {
		this.exceptionThrower = exceptionThrower;
		return this;
	}

	public ClientSetup setBackendAccountProvider(BackendAccountProvider backendAccountProvider) {
		this.backendAccountProvider = backendAccountProvider;
		return this;
	}

	public ClientSetup setLanguageProvider(LanguageProvider languageProvider) {
		this.languageProvider = languageProvider;
		return this;
	}

	public ClientSetup setHateoasClientFactory(HATEOASClientFactory hateoasClientFactory) {
		this.hateoasClientFactory = hateoasClientFactory;
		return this;
	}

	public ClientSetup setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
		return this;
	}

	public ClientSetup setDecoratedProxyFactory(DecoratedProxyFactory decoratedProxyFactory) {
		this.decoratedProxyFactory = decoratedProxyFactory;
		return this;
	}

	public ClientSetup setCreateResourceServiceGraphListener(BeanGraphIteratorListener createResourceServiceGraphListener) {
		this.createResourceServiceGraphListener = createResourceServiceGraphListener;
		return this;
	}

	public ClientSetup setReadResourceServiceGraphListener(BeanGraphIteratorListener readResourceServiceGraphListener) {
		this.readResourceServiceGraphListener = readResourceServiceGraphListener;
		return this;
	}

	public ClientSetup setUpdateResourceServiceGraphListener(BeanGraphIteratorListener updateResourceServiceGraphListener) {
		this.updateResourceServiceGraphListener = updateResourceServiceGraphListener;
		return this;
	}

	public ClientSetup setSystemPropertyPrefix(String systemPropertyPrefix) {
		this.systemPropertyPrefix = systemPropertyPrefix;
		return this;
	}

	public ClientSetup setServiceProxyFactory(ServiceProxyFactory serviceProxyFactory) {
		this.serviceProxyFactory = serviceProxyFactory;
		return this;
	}

	public ClientSetup setMapperFactoriesBySchemaName(Map<String, MapperFactory> mapperFactoriesBySchemaName) {
		this.mapperFactoriesBySchemaName.clear();
		if (mapperFactoriesBySchemaName != null) {
			this.mapperFactoriesBySchemaName.putAll(mapperFactoriesBySchemaName);
		}
		return this;
	}

	private void inspectServiceForSetters(final Object customService, Class customServiceClass, List<ServiceInitializer> initializers) {
		if (customService == null || customServiceClass == null) {
			return;
		}
		Method[] methods = customServiceClass.getMethods();
		for (final Method method : methods) {
			Resource resourceAnnotation = method.getAnnotation(Resource.class);
			org.bndly.common.service.shared.api.ServiceReference serviceReferenceAnnotation = method.getAnnotation(org.bndly.common.service.shared.api.ServiceReference.class);
			if (method.getName().startsWith("set") && (resourceAnnotation != null || serviceReferenceAnnotation != null)) {
				Class<?>[] pt = method.getParameterTypes();
				if (pt.length == 1) {
					final Class<?> referencedType = pt[0];
					final String name;
					if (resourceAnnotation != null) {
						name = resourceAnnotation.name();
					} else {
						name = "";
					}
					final ServiceInitializer initializer;
					if (name.isEmpty()) {
						// look up by type
						initializer = new ServiceInitializer() {

							@Override
							public void init(ServiceInitializer.Context context) {
								Object referenced = context.resolveReferencedServiceByType(referencedType);
								try {
									method.invoke(customService, referenced);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
									throw new ServiceInitializationException(
										customService, "failed to wire service reference by type: " + referencedType.getName(), ex
									);
								}
							}
						};
					} else {
						// look up by name
						initializer = new ServiceInitializer() {

							@Override
							public void init(ServiceInitializer.Context context) {
								Object referenced = context.resolveReferencedServiceByName(name);
								try {
									method.invoke(customService, referenced);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
									throw new ServiceInitializationException(
										customService, "failed to wire service reference by type: " + referencedType.getName(), ex
									);
								}
							}
						};
					}
					initializers.add(initializer);
				}
			}
		}
		Class superCls = customServiceClass.getSuperclass();
		if (!Object.class.equals(superCls)) {
			inspectServiceForSetters(customService, superCls, initializers);
		}
	}

	public ClientSetup setServicesByName(Map<String, Object> servicesByName) {
		this.servicesByName.clear();
		if (servicesByName != null) {
			this.servicesByName.putAll(servicesByName);
		}
		return this;
	}

	public ThreadPoolExecutor assertThreadPoolExecutorExists() {
		if (threadPoolExecutor == null) {
			threadPoolExecutor = new ThreadPoolExecutor(
					assertCorePoolSizeIsDefined(), 
					assertMaximumPoolSizeIsDefined(), 
					assertKeepAliveTimeIsDefined(), 
					TimeUnit.SECONDS, 
					assertBlockingQueueExists(), 
					assertRejectedExecutionHandlerExists()
			);
		}
		return threadPoolExecutor;
	}

	public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}
	
	public int assertCorePoolSizeIsDefined() {
		if (corePoolSize == null) {
			corePoolSize = 10;
		}
		return corePoolSize;
	}

	public void setCorePoolSize(Integer corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int assertMaximumPoolSizeIsDefined() {
		if (maximumPoolSize == null) {
			maximumPoolSize = 100;
		}
		return maximumPoolSize;
	}

	public void setMaximumPoolSize(Integer maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}

	public int assertKeepAliveTimeIsDefined() {
		if (keepAliveTime == null) {
			keepAliveTime = 60;
		}
		return keepAliveTime;
	}

	public void setKeepAliveTime(Integer keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}

	public void setJoinAtEndOfInitEnabled(boolean joinAtEndOfInitEnabled) {
		this.joinAtEndOfInitEnabled = joinAtEndOfInitEnabled;
	}
	
	public boolean assertJoinAtEndOfInitEnabledIsDefined() {
		if (joinAtEndOfInitEnabled == null) {
			joinAtEndOfInitEnabled = true;
		}
		return joinAtEndOfInitEnabled;
	}
	
	public int assertQueueCapacityIsDefined() {
		if (queueCapacity == null) {
			queueCapacity = assertMaximumPoolSizeIsDefined();
		}
		return queueCapacity;
	}

	public void setBlockingQueueCapacity(Integer queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public BlockingQueue<Runnable> assertBlockingQueueExists() {
		if (blockingQueue == null) {
			blockingQueue = new ArrayBlockingQueue<>(assertQueueCapacityIsDefined());
		}
		return blockingQueue;
	}

	public void setBlockingQueue(BlockingQueue<Runnable> blockingQueue) {
		this.blockingQueue = blockingQueue;
	}
	
	public RejectedExecutionHandler assertRejectedExecutionHandlerExists() {
		if (rejectedExecutionHandler == null) {
			rejectedExecutionHandler = new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					try {
						executor.getQueue().put(r);
					} catch (InterruptedException ex) {
						LOG.info("interrupted while trying to schedule work");
					}
				}
			};
		}
		return rejectedExecutionHandler;
	}

	public void setRequestInterceptors(List<RequestInterceptor> requestInterceptors) {
		this.requestInterceptors = requestInterceptors;
	}

	public void setResponseInterceptors(List<ResponseInterceptor> responseInterceptors) {
		this.responseInterceptors = responseInterceptors;
	}

	private void assertRequestResponseInterceptorsAreRegistered() {
		if (didRegisterRequestResponseInterceptors) {
			return;
		}
		didRegisterRequestResponseInterceptors = true;
		if (hateoasClientFactory == null) {
			return;
		}
		if (requestInterceptors != null) {
			for (RequestInterceptor requestInterceptor : requestInterceptors) {
				hateoasClientFactory.addRequestInterceptor(requestInterceptor);
			}
		}
		if (responseInterceptors != null) {
			for (ResponseInterceptor responseInterceptor : responseInterceptors) {
				hateoasClientFactory.addResponseInterceptor(responseInterceptor);
			}
		}
	}
	
}
