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

import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.SchemaProvider;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class EngineServicesListener {
	private static final Logger LOG = LoggerFactory.getLogger(EngineServicesListener.class);
	
	protected final BundleContext bundleContext;
	private final EngineConfiguration engineConfiguration;
	private final String dataSourceName;
	private final String schemaName;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private ServiceTracker<DataSource, DataSource> dataSourceTracker;
	private ServiceTracker<SchemaProvider, SchemaProvider> schemaProviderTracker;
	
	private DataSource dataSource;
	private SchemaProvider schemaProvider;
	private boolean ready;
	private boolean shutdown;

	public EngineServicesListener(BundleContext bundleContext, EngineConfiguration engineConfiguration) {
		if (bundleContext == null) {
			throw new IllegalArgumentException("bundleContext is not allowed to be null");
		}
		this.bundleContext = bundleContext;
		if (engineConfiguration == null) {
			throw new IllegalArgumentException("engineConfiguration is not allowed to be null");
		}
		this.engineConfiguration = engineConfiguration;
		
		this.dataSourceName = engineConfiguration.getDatasource();
		if (dataSourceName == null || dataSourceName.isEmpty()) {
			throw new IllegalArgumentException("dataSourceName is not allowed to be null or empty");
		}
		
		this.schemaName = engineConfiguration.getSchema();
		if (schemaName == null || schemaName.isEmpty()) {
			throw new IllegalArgumentException("schemaName is not allowed to be null or empty");
		}
	}

	public final EngineConfiguration getEngineConfiguration() {
		return engineConfiguration;
	}
	
	public synchronized void init() throws InvalidSyntaxException {
		String dataSourceFilterString = "(&(objectClass=" + DataSource.class.getName() + ")(" + Constants.SERVICE_PID + "=" + DataSource.class.getName() + "." + dataSourceName + "))";
		dataSourceTracker = new ServiceTracker<DataSource, DataSource>(
				bundleContext, 
				bundleContext.createFilter(dataSourceFilterString), 
				new ServiceTrackerCustomizer<DataSource, DataSource>() {
					@Override
					public DataSource addingService(ServiceReference<DataSource> sr) {
						lock.writeLock().lock();
						try {
							dataSource = bundleContext.getService(sr);
							if (dataSource != null && schemaProvider != null) {
								onReadyInternal(dataSource, schemaProvider);
							}
							return dataSource;
						} finally {
							lock.writeLock().unlock();
						}
					}

					@Override
					public void modifiedService(ServiceReference<DataSource> sr, DataSource t) {
					}

					@Override
					public void removedService(ServiceReference<DataSource> sr, DataSource t) {
						lock.writeLock().lock();
						try {
							dataSource = null;
							onShutdownInternal(dataSource, schemaProvider);
						} finally {
							lock.writeLock().unlock();
						}
					}

				}
		);
		dataSourceTracker.open();
		String schemaProviderFilterString = "(&(objectClass=" + SchemaProvider.class.getName() + ")(" + Constants.SERVICE_PID + "=" + SchemaProvider.class.getName() + "." + schemaName + "))";
		schemaProviderTracker = new ServiceTracker<SchemaProvider, SchemaProvider>(
				bundleContext, 
				bundleContext.createFilter(schemaProviderFilterString), 
				new ServiceTrackerCustomizer<SchemaProvider, SchemaProvider>() {
					@Override
					public SchemaProvider addingService(ServiceReference<SchemaProvider> sr) {
						lock.writeLock().lock();
						try {
							schemaProvider = bundleContext.getService(sr);
							if (dataSource != null && schemaProvider != null) {
								onReadyInternal(dataSource, schemaProvider);
							}
							return schemaProvider;
						} finally {
							lock.writeLock().unlock();
						}
					}

					@Override
					public void modifiedService(ServiceReference<SchemaProvider> sr, SchemaProvider t) {
					}

					@Override
					public void removedService(ServiceReference<SchemaProvider> sr, SchemaProvider t) {
						lock.writeLock().lock();
						try {
							schemaProvider = null;
							onShutdownInternal(dataSource, schemaProvider);
						} finally {
							lock.writeLock().unlock();
						}
					}
				}
		);
		schemaProviderTracker.open();
	}
	public synchronized void destroy() {
		lock.writeLock().lock();
		try {
			if (dataSourceTracker != null) {
				dataSourceTracker.close();
				dataSourceTracker = null;
			}
			dataSource = null;
			if (schemaProviderTracker != null) {
				schemaProviderTracker.close();
				schemaProviderTracker = null;
			}
			schemaProvider = null;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void onReadyInternal(DataSource dataSource, SchemaProvider schemaProvider) {
		if (!ready) {
			ready = true;
			shutdown = false;
			LOG.info("engine for schema {} should become ready now", schemaName);
			onReady(dataSource, schemaProvider);
		}
	}
	
	private void onShutdownInternal(DataSource dataSource, SchemaProvider schemaProvider) {
		if (!shutdown && ready) {
			shutdown = true;
			ready = false;
			LOG.info("engine for schema {} should shut down now", schemaName);
			onShutdown(dataSource, schemaProvider);
		}
	}
	protected abstract void onReady(DataSource dataSource, SchemaProvider schemaProvider);
	protected abstract void onShutdown(DataSource dataSource, SchemaProvider schemaProvider);
	protected abstract Engine getEngine();
}
