package org.bndly.rest.data.cache;

/*-
 * #%L
 * REST Data Cache Invalidation
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

import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.api.DataStoreListener;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.cache.api.ContextCacheTransactionProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
@Component(service = CacheInvalidator.class, immediate = true)
public class CacheInvalidator {
	private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidator.class);
	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	@Reference
	private ContextCacheTransactionProvider contextCacheTransactionProvider;
	
	private final List<DataStoreBinding> dataStores = new ArrayList<>();
	private final ReadWriteLock dataStoresLock = new ReentrantReadWriteLock();
	
	private final class DataStoreBinding implements DataStoreListener {
		private final DataStore dataStore;

		private DataStoreBinding(DataStore dataStore) {
			this.dataStore = dataStore;
			dataStore.addListener(this);
		}

		@Override
		public void dataStoreIsReady(DataStore dataStore) {
		}

		@Override
		public void dataStoreClosed(DataStore dataStore) {
			dataStore.removeListener(this);
		}

		@Override
		public Data dataCreated(DataStore dataStore, Data data) {
			// we could pro-active cache the data, but there is currently no need for that
			return data;
		}

		@Override
		public Data dataUpdated(DataStore dataStore, Data data) {
			flushDataFromCache(dataStore, data);
			return data;
		}

		private void flushDataFromCache(DataStore dataStore, Data data) {
			if (cacheTransactionFactory == null) {
				LOG.warn("cann not flush data because, the cacheTransactionFactory is null");
				return;
			}
			StringBuilder prefix = new StringBuilder().append("data").append("/").append(dataStore.getName()).append("/");
			StringBuilder binPath = new StringBuilder(prefix).append("bin");
			StringBuilder viewPath = new StringBuilder(prefix).append("view");
			if (!data.getName().startsWith("/")) {
				binPath.append("/");
				viewPath.append("/");
			}
			binPath.append(data.getName());
			viewPath.append(data.getName());
			String binPathString = binPath.toString();
			String viewPathString = binPath.toString();
			binPathString = removeSelectorsAndExtension(binPathString);
			viewPathString = removeSelectorsAndExtension(viewPathString);
			CacheTransaction existingTransaction = contextCacheTransactionProvider.getCacheTransaction();
			if (existingTransaction != null) {
				existingTransaction.flushRecursive(binPathString);
				existingTransaction.flushRecursive(viewPathString);
			} else {
				try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
					cacheTransaction.flushRecursive(binPathString);
					cacheTransaction.flushRecursive(viewPathString);
				}
			}
		}

		@Override
		public Data dataDeleted(DataStore dataStore, Data data) {
			flushDataFromCache(dataStore, data);
			return data;
		}

		public DataStore getDataStore() {
			return dataStore;
		}

		private String removeSelectorsAndExtension(String pathString) {
			int slashIndex = pathString.lastIndexOf("/");
			int dotIndex;
			if (slashIndex > -1) {
				dotIndex = pathString.indexOf(".", slashIndex);
			} else {
				dotIndex = pathString.indexOf(".");
			}
			if (dotIndex > -1) {
				return pathString.substring(0,dotIndex);
			}
			return pathString;
		}
	}

	@Activate
	public void activate() {
		
	}
	@Deactivate
	public void deactivate() {
		dataStoresLock.writeLock().lock();
		try {
			for (DataStoreBinding dataStoreBinding : dataStores) {
				dataStoreBinding.getDataStore().removeListener(dataStoreBinding);
			}
			dataStores.clear();
		} finally {
			dataStoresLock.writeLock().unlock();
		}
	}
	
	@Reference(
			bind = "addDataStore",
			unbind = "removeDataStore",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = DataStore.class
	)
	public final void addDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				dataStores.add(new DataStoreBinding(dataStore));
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}

	public final void removeDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				Iterator<DataStoreBinding> it = dataStores.iterator();
				while (it.hasNext()) {
					CacheInvalidator.DataStoreBinding next = it.next();
					if (next.getDataStore() == dataStore) {
						dataStore.removeListener(next);
						it.remove();
					}
				}
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}
}
