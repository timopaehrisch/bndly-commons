package org.bndly.common.service.client.dao;

/*-
 * #%L
 * Service Client
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

import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.ServiceFactory;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.rest.schema.beans.SchemaList;
import org.bndly.rest.schema.beans.TypeBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaBeanDaoFactory {

	private Future<?> initFuture; // holds the current init future
	private Future<?> finishedInitFuture; // holds the last init future, that has finished

	public static interface Listener {
		void onSchemaLoaded(SchemaBean schemaBean);
		void onDaoCreated(DefaultDao dao, String typeName);
		void onMissingBeanClass(String typeName, Class<?> listRestBean, Class<?> restBean, Class<?> restReferenceBean);
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(SchemaBeanDaoFactory.class);
	private final String schemaName;
	private final String schemaRestBeanPackage;
	private final Map<String, DefaultDao> daosByTypeName = new HashMap<>();
	private final Map<Class<?>, DefaultDao> daosByRestBeanType = new HashMap<>();

	private ServiceFactory serviceFactory;
	private final ExecutorService initExecutor = Executors.newSingleThreadExecutor();
	private JAXBMessageClassProvider messageClassProvider;

	public SchemaBeanDaoFactory(String schemaName, String schemaRestBeanPackage) {
		if (schemaName == null) {
			throw new IllegalArgumentException("schemaName is not allowed to be null");
		}
		this.schemaName = schemaName;
		if (schemaRestBeanPackage == null) {
			throw new IllegalArgumentException("schemaRestBeanPackage is not allowed to be null");
		}
		this.schemaRestBeanPackage = schemaRestBeanPackage;
	}

	public Future<?> init(final Listener... listeners) {
		Runnable initRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (messageClassProvider == null) {
						LOG.error("no messageClassProvider configured");
						return;
					}
					Collection<Class<?>> classes = messageClassProvider.getJAXBMessageClasses();
					Map<String, Class> classesByName = new HashMap<>();
					for (Class<?> cls : classes) {
						classesByName.put(cls.getName(), cls);
					}

					HATEOASClient<SchemaList> client = serviceFactory.getServiceClient("schema", SchemaList.class);
					SchemaList schemaList = client.getWrappedBean();
					final String packagePrefix = schemaRestBeanPackage + ".";
					for (SchemaBean schemaBean : schemaList) {
						if (schemaBean.getName().equals(schemaName)) {
							for (Listener listener : listeners) {
								listener.onSchemaLoaded(schemaBean);
							}
							for (final TypeBean typeBean : schemaBean.getTypes()) {
								if (typeBean.follow("primaryResource") != null) {
									final String typeName = typeBean.getName();
									final Class<?> listRestBean = classesByName.get(packagePrefix + typeName + "ListRestBean");
									final Class<?> restBean = classesByName.get(packagePrefix + typeName + "RestBean");
									final Class<?> restReferenceBean = classesByName.get(packagePrefix + typeName + "ReferenceRestBean");

									if (listRestBean == null || restBean == null || restReferenceBean == null) {
										LOG.warn("could not find all required xml beans for type {} of schema {}", typeName, schemaBean.getName());
										for (Listener listener : listeners) {
											listener.onMissingBeanClass(typeName, listRestBean, restBean, restReferenceBean);
										}
										continue;
									}
									final DefaultDaoImpl dao = new DefaultDaoImpl(listRestBean, restBean, restReferenceBean);
									Initializer initializer = new Initializer() {
										private boolean isInitialized = false;

										@Override
										public void init() throws ClientException {
											Object primaryResource = serviceFactory.createClient(typeBean).follow("primaryResource").execute(listRestBean);
											dao.setPrimaryResource(primaryResource);
											isInitialized = true;
										}

										@Override
										public boolean isInitialized() {
											return isInitialized;
										}
									};
									dao.setInitializer(initializer);

									dao.setServiceFactory(serviceFactory);
									daosByTypeName.put(typeName, dao);
									daosByRestBeanType.put(restBean, dao);
									for (Listener listener : listeners) {
										listener.onDaoCreated(dao, typeName);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		};
		Future<?> initFutureTmp = initExecutor.submit(initRunnable);
		initFuture = initFutureTmp;
		return initFutureTmp;
	}
	
	public void destroy() {
		if (!initExecutor.isShutdown()) {
			LOG.info("shutting down init executor");
			initExecutor.shutdownNow();
		}
	}

	public void joinAndKeepExecutorServiceAlive() {
		join(false);
	}
	
	public void join() {
		join(true);
	}
	
	private void join(boolean destroyOnJoin) {
		// wait for init thread
		try {
			if (finishedInitFuture != initFuture && initFuture != null) {
				try {
					LOG.info("waiting for initialization to finish");
					initFuture.get();
					finishedInitFuture = initFuture;
					LOG.info("initialization finished");
				} catch (InterruptedException ex) {
					LOG.warn("initialization was aborted");
				} catch (ExecutionException ex) {
					LOG.error("initialization failed", ex);
				}
			}
		} finally {
			// for legacy reasons
			if (destroyOnJoin) {
				destroy();
			}
		}
	}

	public DefaultDao getDaoForRestBeanType(final Class<?> restBeanType) {
		if (restBeanType == null) {
			throw new IllegalArgumentException("missing restBeanType argument");
		}
		return new LazyDefaultDao() {
			@Override
			protected DefaultDao waitForRealDao() {
				join();

				DefaultDao dao = daosByRestBeanType.get(restBeanType);
				if (dao == null) {
					throw new IllegalStateException("could not find a DAO for the restBeanType: " + restBeanType);
				}
				return dao;
			}
		};
	}

	public void setServiceFactoryListeners(List list) {
		list.add(this);
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		this.messageClassProvider = messageClassProvider;
	}

}
