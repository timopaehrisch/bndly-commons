package org.bndly.common.bpm.datastore.impl;

/*-
 * #%L
 * BPM DataStore Impl
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
import org.bndly.common.bpm.api.BusinessProcessDataStoreFactory;
import org.bndly.common.bpm.api.NamedBusinessProcessData;
import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.api.DataStoreListener;
import org.bndly.common.data.api.NoOpDataStoreListener;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.api.SimpleData;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = BusinessProcessDataStoreFactory.class)
@Designate(ocd = BusinessProcessDataStoreFactoryImpl.Configuration.class)
public class BusinessProcessDataStoreFactoryImpl implements BusinessProcessDataStoreFactory {
	
	private static final Logger LOG = LoggerFactory.getLogger(BusinessProcessDataStoreFactoryImpl.class);
	
	@ObjectClassDefinition(
			name = "Business Process Datastore Factory",
			description = "This factory creates business process datastores from regular datastores"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Fire Data Store Events",
				description = "Set this value to true and the events of the DataStore will also be fired by the BusinessProcessDataStore instances"
		)
		boolean fireDataStoreEvents() default true;
	}

	private final Map<String, DataStoreBinding> dataStores = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private Boolean fireDataStoreEvents;

	@Activate
	public void activate(Configuration configuration) {
		fireDataStoreEvents = configuration.fireDataStoreEvents();
	}
	
	@Deactivate
	public void deactivate() {
		
	}
	
	private DataStoreBinding isEventingEnabled(DataStore dataStore, Data data) {
		if (fireDataStoreEvents) {
			lock.readLock().lock();
			try {
				DataStoreBinding binding = dataStores.get(dataStore.getName());
				if (binding != null) {
					if (data.getName().endsWith(".bpmn")) {
						return binding;
					}
				}
			} finally {
				lock.readLock().unlock();
			}
		}
		return null;
	}
	
	private class DataStoreBinding {
		private DataStore dataStore;
		private final BusinessProcessDataStore businessProcessDataStore;
		private final DataStoreListener dataStoreListener;

		public DataStoreBinding(BusinessProcessDataStore bpDataStore) {
			this(null, bpDataStore);
		}
		
		public DataStoreBinding(DataStore dataStore, BusinessProcessDataStore bpDataStore) {
			this.dataStore = dataStore;
			this.businessProcessDataStore = bpDataStore;
			dataStoreListener = new NoOpDataStoreListener() {

				@Override
				public Data dataCreated(DataStore dataStore, Data data) {
					replay(data);
					BusinessProcessData mapped = mapDataToBusinessProcessData(data, businessProcessDataStore);
					if (mapped != null && isEventingEnabled(dataStore, data) != null) {
						businessProcessDataStore.onCreatedBPMNData(mapped);
					}
					return super.dataCreated(dataStore, data);
				}

				@Override
				public Data dataDeleted(DataStore dataStore, Data data) {
					replay(data);
					BusinessProcessData mapped = mapDataToBusinessProcessData(data, businessProcessDataStore);
					if (mapped != null && isEventingEnabled(dataStore, data) != null) {
						businessProcessDataStore.onDeletedBPMNData(mapped);
					}
					return super.dataDeleted(dataStore, data);
				}

				@Override
				public Data dataUpdated(DataStore dataStore, Data data) {
					replay(data);
					BusinessProcessData mapped = mapDataToBusinessProcessData(data, businessProcessDataStore);
					if (mapped != null && isEventingEnabled(dataStore, data) != null) {
						businessProcessDataStore.onUpdatedBPMNData(mapped);
					}
					return super.dataUpdated(dataStore, data);
				}
				
				private void replay(Data data) {
					try {
						ReplayableInputStream.replayIfPossible(data.getInputStream());
					} catch (IOException ex) {
						LOG.error("failed to replay data input stream: " + ex.getMessage(), ex);
					}
				}
			};
		}

		public BusinessProcessDataStore getBusinessProcessDataStore() {
			return businessProcessDataStore;
		}

		public DataStore getDataStore() {
			return dataStore;
		}

		public void setDataStore(DataStore dataStore) {
			this.dataStore = dataStore;
		}
		
	}
	
	public static class BusinessProcessDataImpl extends SimpleData implements NamedBusinessProcessData {

		public BusinessProcessDataImpl() {
			super(null);
		}

	}

	protected static BusinessProcessDataImpl assertDataType(BusinessProcessData foundData) throws IllegalStateException {
		BusinessProcessDataImpl d;
		if (BusinessProcessDataImpl.class.isInstance(foundData)) {
			d = (BusinessProcessDataImpl) foundData;
		} else {
			throw new IllegalStateException("unsupported parameter type: " + foundData.getClass());
		}
		return d;
	}

	@Override
	public BusinessProcessDataStore create(String processEngineAlias) {
		if (processEngineAlias == null) {
			return null;
		}
		lock.writeLock().lock();
		try {
			DataStoreBinding ds = dataStores.get(processEngineAlias);
			if (ds != null) {
				return ds.getBusinessProcessDataStore();
			} else {
				LOG.info("creating a proxy business process data store for '{}', because the real data store is not there yet.", processEngineAlias);
				// return a proxy instance. that will look up the data store on demand.
				final DataStoreBinding dataStoreBinding = new DataStoreBinding(new BusinessProcessDataStoreImpl(processEngineAlias) {

					@Override
					protected DataStore getDataStore() {
						lock.readLock().lock();
						try {
							DataStoreBinding ds = dataStores.get(processEngineAlias);
							return ds == null ? null : ds.getDataStore();
						} finally {
							lock.readLock().unlock();
						}
					}

				});
				dataStores.put(processEngineAlias, dataStoreBinding);
				return dataStoreBinding.getBusinessProcessDataStore();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	protected static BusinessProcessDataImpl mapDataToBusinessProcessData(Data d, BusinessProcessDataStore businessProcessDataStore) {
		BusinessProcessDataImpl instance = (BusinessProcessDataImpl) businessProcessDataStore.newInstance();
		instance.setInputStream(d.getInputStream());
		instance.setContentType(d.getContentType());
		instance.setCreatedOn(d.getCreatedOn());
		instance.setUpdatedOn(d.getUpdatedOn());
		instance.setName(d.getName());
		return instance;
	}

	@Reference(
			bind = "addDataStore",
			unbind = "removeDataStore",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = DataStore.class
	)
	public void addDataStore(final DataStore dataStore) {
		if (dataStore != null) {
			LOG.info("creating a business process data store for '{}', because there was a new datastore in the application.", dataStore.getName());
			BusinessProcessDataStore cr = new BusinessProcessDataStoreImpl(dataStore.getName()) {

				@Override
				protected DataStore getDataStore() {
					return dataStore;
				}

			};
			lock.writeLock().lock();
			try {
				DataStoreBinding binding = dataStores.get(dataStore.getName());
				if (binding == null) {
					binding = new DataStoreBinding(dataStore, cr);
					dataStores.put(dataStore.getName(), binding);
				} else {
					if (binding.getDataStore() != null) {
						binding.getDataStore().removeListener(binding.dataStoreListener);
					}
					binding.setDataStore(dataStore);
					binding.getDataStore().addListener(binding.dataStoreListener);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeDataStore(DataStore dataStore) {
		if (dataStore != null) {
			lock.writeLock().lock();
			try {
				DataStoreBinding binding = dataStores.get(dataStore.getName());
				if (binding != null) {
					if (binding.getDataStore() != null) {
						binding.getDataStore().removeListener(binding.dataStoreListener);
					}
				}
				dataStores.remove(dataStore.getName());
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void setFireDataStoreEvents(Boolean fireDataStoreEvents) {
		this.fireDataStoreEvents = fireDataStoreEvents;
	}

	
}
