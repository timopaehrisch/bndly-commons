package org.bndly.common.datasource;

/*-
 * #%L
 * Data Source
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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.osgi.util.DictionaryToPropertiesAdapter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.osgi.framework.Bundle;
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
@Component(service = DataSourcePool.class, immediate = true)
public class DataSourcePoolImpl implements DataSourcePool {

	private static final Logger LOG = LoggerFactory.getLogger(DataSourcePoolImpl.class);

	private final List<DataSourceConfiguration> dataSourceConfigurations = new ArrayList<>();
	private final ReadWriteLock dataSourceConfigurationsLock = new ReentrantReadWriteLock();
	
	private final Map<String, RegisteredDataSource> pool = new HashMap<>();
	private DriverBundleListener driverBundleListener;
	
	private final List<Listener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	private final List<RegisteredDriver> registeredDrivers = new ArrayList<>();
	private final ReadWriteLock registeredDriversLock = new ReentrantReadWriteLock();
	private ComponentContext componentContext;
	private boolean active;
	
	@Reference(
			bind = "addDataSourceConfiguration",
			unbind = "removeDataSourceConfiguration",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = DataSourceConfiguration.class
	)
	public void addDataSourceConfiguration(DataSourceConfiguration configuration) {
		if (configuration != null) {
			dataSourceConfigurationsLock.writeLock().lock();
			try {
				dataSourceConfigurations.add(configuration);
				initDataSourceFromConfig(configuration);
			} finally {
				dataSourceConfigurationsLock.writeLock().unlock();
			}
		}
	}
	
	public void removeDataSourceConfiguration(DataSourceConfiguration configuration) {
		if (configuration != null) {
			dataSourceConfigurationsLock.writeLock().lock();
			try {
				Iterator<DataSourceConfiguration> iterator = dataSourceConfigurations.iterator();
				while (iterator.hasNext()) {
					DataSourceConfiguration next = iterator.next();
					if (next == configuration) {
						iterator.remove();
					}
				}
				RegisteredDataSource rds = pool.get(configuration.getName());
				if (rds != null) {
					destructRegisteredDataSource(rds);
				}
				pool.remove(configuration.getName());
			} finally {
				dataSourceConfigurationsLock.writeLock().unlock();
			}
		}
	}


	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
		this.driverBundleListener = new DriverBundleListener() {
			
			Map<Long, List<String>> driverNamesByBundle = new HashMap<>();

			@Override
			protected void onBundleResolved(Bundle bundle) {
				try {
					List<String> sqlDriverNames = getSqlDriverNamesFromBundle(bundle);
					driverNamesByBundle.put(bundle.getBundleId(), sqlDriverNames);
					registerDriverFromBundle(bundle, sqlDriverNames);
				} catch (IOException e) {
					LOG.error("failed to retrieve sql driver name from bundle " + bundle.getSymbolicName(), e);
				}
			}

			@Override
			protected void onBundleUnresolved(Bundle bundle) {
				try {
					List<String> sqlDriverNames = driverNamesByBundle.get(bundle.getBundleId());
					unregisterDriverFromBundle(bundle, sqlDriverNames);
				} finally {
					driverNamesByBundle.remove(bundle.getBundleId());
				}
				
			}
			
		};
		componentContext.getBundleContext().addBundleListener(driverBundleListener);
		
		Bundle[] bundles = componentContext.getBundleContext().getBundles();
		for (Bundle bundle : bundles) {
			int s = bundle.getState();
			if (s == Bundle.ACTIVE || s == Bundle.RESOLVED || s == Bundle.STARTING) {
				try {
					List<String> sqlDriverNames = driverBundleListener.getSqlDriverNamesFromBundle(bundle);
					registerDriverFromBundle(bundle, sqlDriverNames);
				} catch (IOException e) {
					LOG.error("failed to retrieve sql driver name from bundle " + bundle.getSymbolicName(), e);
				}
			}
		}
		
		initDataSourcesFromConfigs();
		
		listenersLock.readLock().lock();
		try {
			for (Listener listener : listeners) {
				listener.onInit(this);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
		active = true;
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		componentContext.getBundleContext().removeBundleListener(driverBundleListener);
		driverBundleListener = null;
		
		for (Map.Entry<String, RegisteredDataSource> entry : pool.entrySet()) {
			RegisteredDataSource registeredDataSource = entry.getValue();
			destructRegisteredDataSource(registeredDataSource);
		}
		pool.clear();
		
		listenersLock.writeLock().lock();
		try {
			for (Listener listener : listeners) {
				listener.onDestruct(this);
			}
			listeners.clear();
		} finally {
			listenersLock.writeLock().unlock();
		}
		
		registeredDriversLock.writeLock().lock();
		try {
			registeredDrivers.clear();
		} finally {
			registeredDriversLock.writeLock().unlock();
		}
		
		this.componentContext = null;
		active = false;
	}
	
	private void destructRegisteredDataSource(RegisteredDataSource registeredDataSource) {
		registeredDataSource.destroy();
		registeredDataSource.getRegisteredDriver().getDataSources().remove(registeredDataSource);
		listenersLock.readLock().lock();
		try {
			for (Listener listener : listeners) {
				listener.onDataSourceDestroyed(this, registeredDataSource.getName(), registeredDataSource.getDataSource(), registeredDataSource.getDatabaseSchemaName());
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}
	
	private void initDataSourceFromConfig(DataSourceConfiguration configuration) {
		if (componentContext == null) {
			LOG.info("skipping creation of data source because component context was null");
			return;
		}
		String name = configuration.getName();
		if (pool.containsKey(name)) {
			LOG.debug("skipping creation of datasource {}, because it was already defined", name);
			return;
		}
		String driverClass = configuration.getDriverClass();
		if (driverClass == null) {
			LOG.warn("no driverClass defined for datasource {}", name);
			return;
		}
		RegisteredDriver driver = getDriverByClassNameAndVersion(driverClass, configuration.getDriverClassVersion());
		if (driver == null) {
			LOG.warn("driver {} not found for datasource {}", driverClass, name);
			return;
		}
		DataSource ds = createDataSourceFromConfig(configuration, driver);
		if (ds == null) {
			LOG.error("could not create datasource " + name);
			return;
		}
		RegisteredDataSource registeredDataSource = new RegisteredDataSource(componentContext.getBundleContext(), name, configuration, driver, ds, configuration.getSchemaName());
		pool.put(name, registeredDataSource);
		registeredDataSource.init();
		driver.getDataSources().add(registeredDataSource);
		listenersLock.readLock().lock();
		try {
			for (Listener listener : listeners) {
				listener.onDataSourceCreated(this, registeredDataSource.getName(), ds, registeredDataSource.getDatabaseSchemaName());
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}
	
	private void initDataSourcesFromConfigs() {
		if (componentContext == null) {
			LOG.warn("skipping creation of data sources because component context was null");
			return;
		}
		dataSourceConfigurationsLock.readLock().lock();
		try {
			for (DataSourceConfiguration dataSourceConfiguration : dataSourceConfigurations) {
				RegisteredDataSource ds = pool.get(dataSourceConfiguration.getName());
				if (ds == null) {
					initDataSourceFromConfig(dataSourceConfiguration);
				}
			}
		} finally {
			dataSourceConfigurationsLock.readLock().unlock();
		}
	}

	private RegisteredDriver getDriverByClassNameAndVersion(String driverClass, String version) {
		registeredDriversLock.readLock().lock();
		try {
			Iterator<RegisteredDriver> iterator = registeredDrivers.iterator();
			while (iterator.hasNext()) {
				RegisteredDriver next = iterator.next();
				if (next.getDriverClassName().equals(driverClass)) {
					if (version == null || next.getVersionString().startsWith(version)) {
						return next;
					}
				}
			}
			return null;
		} finally {
			registeredDriversLock.readLock().unlock();
		}
	}
	
	private void registerDriverFromBundle(Bundle bundle, List<String> sqlDriverNames) {
		if (sqlDriverNames != null) {
			for (String sqlDriverName : sqlDriverNames) {
				RegisteredDriver registeredDriver = new RegisteredDriver(bundle, sqlDriverName);
				LOG.info("found jdbc driver {} in version {}", sqlDriverName, bundle.getVersion().toString());
				registeredDriversLock.writeLock().lock();
				try {
					registeredDrivers.add(registeredDriver);
				} finally {
					registeredDriversLock.writeLock().unlock();
				}
				initDataSourcesFromConfigs();
			}
		}
	}
	private void unregisterDriverFromBundle(Bundle bundle, List<String> sqlDriverNames) {
		registeredDriversLock.writeLock().lock();
		try {
			Iterator<RegisteredDriver> iterator = registeredDrivers.iterator();
			while (iterator.hasNext()) {
				RegisteredDriver next = iterator.next();
				if (next.getBundle() == bundle || next.getBundle().getBundleId() == bundle.getBundleId()) {
					LOG.info("removing jdbc driver {} in version {}", next.getDriverClassName(), next.getBundle().getVersion().toString());
					// now we have to remove data sources that might use the driver
					iterator.remove();
					List<RegisteredDataSource> ds = next.getDataSources();
					for (RegisteredDataSource d : ds) {
						LOG.info("removing datasource {} because driver {} is now unregistered", d.getName(), next.getDriverClassName());
						destructRegisteredDataSource(d);
						if (pool.get(d.getName()) == d) {
							pool.remove(d.getName());
						}
					}
				}
			}
		} finally {
			registeredDriversLock.writeLock().unlock();
		}
	}

	@Override
	public String getDatabaseSchemaNameForDataSource(DataSource dataSource) {
		for (RegisteredDataSource value : pool.values()) {
			if (value.getDataSource() == dataSource) {
				return value.getDatabaseSchemaName();
			}
		}
		return null;
	}

	@Override
	public DataSource getDataSource(String dataSourceName) {
		RegisteredDataSource rds = pool.get(dataSourceName);
		if (rds == null) {
			return null;
		}
		return rds.getDataSource();
	}

	private DataSource createDataSourceFromConfig(DataSourceConfiguration configuration, RegisteredDriver driver) {
		final String driverClassName = configuration.getDriverClass();
		if (driverClassName != null) {
			LOG.info("creating data source with driver class: {}", driverClassName);
			
			Class<?> driverClass;
			try {
				driverClass = driver.getClassLoader().loadClass(driver.getDriverClassName());
			} catch (ClassNotFoundException e) {
				LOG.error("failed to load driver class: " + e.getMessage(), e);
				return null;
			}
			Driver sqlDriver;
			try {
				sqlDriver = (Driver) driverClass.newInstance();
			} catch (Exception e) {
				LOG.error("failed to instantiate driver class: " + e.getMessage(), e);
				return null;
			}
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			try {
				String connectUri = configuration.getJdbcUrl();
				Properties properties = new DictionaryToPropertiesAdapter(configuration.getAdapter()).toStringProperties();
				DriverConnectionFactory connectionFactory = new DriverConnectionFactory(sqlDriver, connectUri, properties);
				PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
				if (configuration.getConnectionInitSqls() != null) {
					poolableConnectionFactory.setConnectionInitSql(configuration.getConnectionInitSqls());
				}
				if (configuration.getConnectionDefaultAutoCommit() != null) {
					poolableConnectionFactory.setDefaultAutoCommit(configuration.getConnectionDefaultAutoCommit());
				}
				if (configuration.getConnectionDefaultCatalog() != null) {
					poolableConnectionFactory.setDefaultCatalog(configuration.getConnectionDefaultCatalog());
				}
				if (configuration.getConnectionDefaultQueryTimeout() != null) {
					poolableConnectionFactory.setDefaultQueryTimeout(configuration.getConnectionDefaultQueryTimeout());
				}
				if (configuration.getConnectionDefaultReadOnly() != null) {
					poolableConnectionFactory.setDefaultReadOnly(configuration.getConnectionDefaultReadOnly());
				}
				if (configuration.getConnectionDefaultTransactionIsolation() != null) {
					poolableConnectionFactory.setDefaultTransactionIsolation(configuration.getConnectionDefaultTransactionIsolation());
				}
				if (configuration.getConnectionDisconnectionSqlCodes() != null) {
					poolableConnectionFactory.setDisconnectionSqlCodes(configuration.getConnectionDisconnectionSqlCodes());
				}
				if (configuration.getConnectionEnableAutoCommitOnReturn() != null) {
					poolableConnectionFactory.setEnableAutoCommitOnReturn(configuration.getConnectionEnableAutoCommitOnReturn());
				}
				if (configuration.getConnectionFastFailValidation() != null) {
					poolableConnectionFactory.setFastFailValidation(configuration.getConnectionFastFailValidation());
				}
				if (configuration.getConnectionMaxConnLifetimeMillis() != null) {
					poolableConnectionFactory.setMaxConnLifetimeMillis(configuration.getConnectionMaxConnLifetimeMillis());
				}
				if (configuration.getConnectionMaxOpenPreparedStatements() != null) {
					poolableConnectionFactory.setMaxOpenPrepatedStatements(configuration.getConnectionMaxOpenPreparedStatements());
				}
				if (configuration.getConnectionPoolStatements() != null) {
					poolableConnectionFactory.setPoolStatements(configuration.getConnectionPoolStatements());
				}
				if (configuration.getConnectionRollbackOnReturn() != null) {
					poolableConnectionFactory.setRollbackOnReturn(configuration.getConnectionRollbackOnReturn());
				}
				if (configuration.getConnectionValidationQuery() != null) {
					poolableConnectionFactory.setValidationQuery(configuration.getConnectionValidationQuery());
				}
				if (configuration.getConnectionValidationQueryTimeout() != null) {
					poolableConnectionFactory.setValidationQueryTimeout(configuration.getConnectionValidationQueryTimeout());
				}
				
				
				
				
				GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
				if (configuration.getPoolBlockWhenExhausted() != null) {
					connectionPool.setBlockWhenExhausted(configuration.getPoolBlockWhenExhausted());
				}
				if (configuration.getPoolEvictionPolicyClassName() != null) {
					connectionPool.setEvictionPolicyClassName(configuration.getPoolEvictionPolicyClassName());
				}
				if (configuration.getPoolLifo() != null) {
					connectionPool.setLifo(configuration.getPoolLifo());
				}
				if (configuration.getPoolMaxIdle() != null) {
					connectionPool.setMaxIdle(configuration.getPoolMaxIdle());
				}
				if (configuration.getPoolMaxTotal() != null) {
					connectionPool.setMaxTotal(configuration.getPoolMaxTotal());
				}
				if (configuration.getPoolMaxWaitMillis() != null) {
					connectionPool.setMaxWaitMillis(configuration.getPoolMaxWaitMillis());
				}
				if (configuration.getPoolMinEvictableIdleTimeMillis() != null) {
					connectionPool.setMinEvictableIdleTimeMillis(configuration.getPoolMinEvictableIdleTimeMillis());
				}
				if (configuration.getPoolMinIdle() != null) {
					connectionPool.setMinIdle(configuration.getPoolMinIdle());
				}
				if (configuration.getPoolNumTestsPerEvictionRun() != null) {
					connectionPool.setNumTestsPerEvictionRun(configuration.getPoolNumTestsPerEvictionRun());
				}
				if (configuration.getPoolSoftMinEvictableIdleTimeMillis() != null) {
					connectionPool.setSoftMinEvictableIdleTimeMillis(configuration.getPoolSoftMinEvictableIdleTimeMillis());
				}
				if (configuration.getPoolTestOnBorrow() != null) {
					connectionPool.setTestOnBorrow(configuration.getPoolTestOnBorrow());
				}
				if (configuration.getPoolTestOnCreate() != null) {
					connectionPool.setTestOnCreate(configuration.getPoolTestOnCreate());
				}
				if (configuration.getPoolTestOnReturn() != null) {
					connectionPool.setTestOnReturn(configuration.getPoolTestOnReturn());
				}
				if (configuration.getPoolTestWhileIdle() != null) {
					connectionPool.setTestWhileIdle(configuration.getPoolTestWhileIdle());
				}
				if (configuration.getPoolTimeBetweenEvictionRunsMillis() != null) {
					connectionPool.setTimeBetweenEvictionRunsMillis(configuration.getPoolTimeBetweenEvictionRunsMillis());
				}
				poolableConnectionFactory.setPool(connectionPool);
				PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(connectionPool);
				return dataSource;
			} catch (Exception e) {
				LOG.error("failed to create data source: " + e.getMessage(), e);
				return null;
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}
		}
		return null;
	}

	private <E> void pipePropertiesOfTypeToDataSource(E dataSource, Class<E> inspectedType, DictionaryAdapter props, String prefix) {
		if (inspectedType != null && !Object.class.equals(inspectedType)) {
			Method[] publicMethods = inspectedType.getMethods();
			if (publicMethods != null) {
				for (Method publicMethod : publicMethods) {
					Class<?>[] parameterTypes = publicMethod.getParameterTypes();
					if (parameterTypes != null && parameterTypes.length == 1) {
						Class<?> pt = parameterTypes[0];
						String name = publicMethod.getName();
						if (name.startsWith("set")) {
							name = name.substring("set".length());
							name = name.substring(0, 1).toLowerCase() + name.substring(1);
						}
						String prefixedName = prefix + name;
						Object val = null;
						boolean supported = false;
						if (Long.class.equals(pt) || long.class.equals(pt)) {
							supported = true;
							val = props.getLong(prefixedName);
						} else if (Integer.class.equals(pt) || int.class.equals(pt)) {
							supported = true;
							val = props.getInteger(prefixedName);
						} else if (Float.class.equals(pt) || float.class.equals(pt)) {
							supported = true;
							val = props.getFloat(prefixedName);
						} else if (Double.class.equals(pt) || double.class.equals(pt)) {
							supported = true;
							val = props.getDouble(prefixedName);
						} else if (String.class.equals(pt)) {
							supported = true;
							val = props.getString(prefixedName);
						}
						
						if (val != null) {
							if (supported && (val != null || !pt.isPrimitive())) {
								try {
									publicMethod.invoke(dataSource, val);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
									LOG.error("failed to write config value for data source: " + name, ex);
								}
							}
						}
					}
				}
			}
			pipePropertiesOfTypeToDataSource(dataSource, inspectedType.getSuperclass(), props, prefix);
		}
	}

	@Override
	public void addListener(Listener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
				if (active) {
					listener.onInit(this);
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeListener(Listener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				Iterator<Listener> iterator = listeners.iterator();
				while (iterator.hasNext()) {
					DataSourcePool.Listener next = iterator.next();
					if (next == listener) {
						iterator.remove();
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	
	
}
