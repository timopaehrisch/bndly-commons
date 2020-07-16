package org.bndly.common.velocity.datastore;

/*-
 * #%L
 * Velocity DataStore
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

import org.bndly.common.data.api.DataStore;
import org.bndly.common.velocity.api.VelocityDataProvider;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = VelocityDataProviderFactoryImpl.class, immediate = true)
public class VelocityDataProviderFactoryImpl {

	private BundleContext bc;

	private class RegisteredDataStore {

		private final DataStore dataStore;
		private final VelocityDataProviderImpl dataProviderImpl;
		private ServiceRegistration<VelocityDataProvider> reg;

		public RegisteredDataStore(DataStore dataStore) {
			this.dataStore = dataStore;
			dataProviderImpl = new VelocityDataProviderImpl(dataStore);
		}
	}

	private final List<RegisteredDataStore> dataStores = new ArrayList<>();
	private final ReadWriteLock dataStoresLock = new ReentrantReadWriteLock();

	@Activate
	public void activate(ComponentContext componentContext) {
		BundleContext bc = componentContext.getBundleContext();
		this.bc = bc;
		dataStoresLock.readLock().lock();
		try {
			for (RegisteredDataStore dataStore : dataStores) {
				if (dataStore.reg == null) {
					registerOsgiService(dataStore);
				}
			}
		} finally {
			dataStoresLock.readLock().unlock();
		}
	}

	@Deactivate
	public void deactivate() {
		dataStoresLock.writeLock().lock();
		try {
			for (RegisteredDataStore dataStore : dataStores) {
				if (dataStore.reg != null) {
					dataStore.reg.unregister();
				}
			}
			dataStores.clear();
		} finally {
			dataStoresLock.writeLock().unlock();
		}
		this.bc = null;
	}

	@Reference(
			bind = "addDataStore",
			unbind = "removeDataStore",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = DataStore.class
	)
	public void addDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				RegisteredDataStore rds = new RegisteredDataStore(dataStore);
				dataStores.add(rds);
				if (bc != null) {
					registerOsgiService(rds);
				}
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}

	public void removeDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				Iterator<RegisteredDataStore> iter = dataStores.iterator();
				while (iter.hasNext()) {
					RegisteredDataStore next = iter.next();
					if (next.dataStore == dataStore) {
						iter.remove();
						if (next.reg != null) {
							next.reg.unregister();
							next.reg = null;
						}
					}
				}
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}

	private void registerOsgiService(RegisteredDataStore rds) {
		Dictionary<String, Object> serviceFactoryProps = new Hashtable<>();
		String pid = VelocityDataProvider.class.getName() + "." + rds.dataStore.getName();
		serviceFactoryProps.put("service.pid", pid);
		ServiceRegistration<VelocityDataProvider> reg = bc.registerService(VelocityDataProvider.class, rds.dataProviderImpl, serviceFactoryProps);
		rds.reg = reg;
	}
}
