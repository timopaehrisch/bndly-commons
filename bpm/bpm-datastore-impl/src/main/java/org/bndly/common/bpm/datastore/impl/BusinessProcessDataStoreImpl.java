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

import org.bndly.common.bpm.api.BusinessProcessDataStoreEventListener;
import org.bndly.common.bpm.api.BusinessProcessData;
import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class BusinessProcessDataStoreImpl implements BusinessProcessDataStore {

	private static final Logger LOG = LoggerFactory.getLogger(BusinessProcessDataStoreFactoryImpl.class);

	protected final String processEngineAlias;
	private final List<BusinessProcessDataStoreEventListener> eventListeners = new ArrayList<>();

	public BusinessProcessDataStoreImpl(String processEngineAlias) {
		if (processEngineAlias == null) {
			throw new IllegalArgumentException("processEngineAlias is not allowed to be null");
		}
		this.processEngineAlias = processEngineAlias;
	}

	protected abstract DataStore getDataStore();

	protected List<BusinessProcessDataStoreEventListener> getEventListeners() {
		return eventListeners;
	}

	@Override
	public BusinessProcessData newInstance() {
		return new BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl();
	}

	@Override
	public void update(BusinessProcessData foundData) {
		if (foundData == null) {
			LOG.warn("can not update process data when process data is null");
			return;
		}
		LOG.info("updating process {}", foundData.getName());
		BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl d = BusinessProcessDataStoreFactoryImpl.assertDataType(foundData);
		DataStore ds = getDataStore();
		if (ds == null) {
			LOG.warn("could not find data store with name " + processEngineAlias);
			return;
		}
		ds.update((BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl) foundData);
	}

	@Override
	public List<BusinessProcessData> list() {
		LOG.info("listing all processes");
		DataStore ds = getDataStore();
		if (ds == null) {
			LOG.warn("could not find data store with name " + processEngineAlias);
			return Collections.EMPTY_LIST;
		}
		List<Data> d = ds.list();
		List<BusinessProcessData> result = null;
		if (d != null) {
			result = new ArrayList<>();
			for (Data data : d) {
				BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl instance = BusinessProcessDataStoreFactoryImpl.mapDataToBusinessProcessData(data, this);
				result.add(instance);
			}
		}
		LOG.info("found {} processes", result == null ? 0 : result.size());
		return result;
	}

	@Override
	public BusinessProcessData findByName(String resourceName) {
		if (resourceName == null) {
			LOG.warn("can not look up process data when resourceName is null");
			return null;
		}
		LOG.info("looking up process {}", resourceName);
		DataStore ds = getDataStore();
		if (ds == null) {
			LOG.warn("could not find data store with name " + processEngineAlias);
			return null;
		}
		Data d = ds.findByName(resourceName);
		if (d == null) {
			LOG.warn("could not find process {}", resourceName);
			return null;
		}
		BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl instance = BusinessProcessDataStoreFactoryImpl.mapDataToBusinessProcessData(d, this);
		return instance;
	}

	@Override
	public BusinessProcessData create(BusinessProcessData d) {
		if (d == null) {
			LOG.warn("could not create process when data parameter is null");
			return d;
		}
		LOG.info("creating process data {}", d.getName());
		BusinessProcessDataStoreFactoryImpl.BusinessProcessDataImpl data = BusinessProcessDataStoreFactoryImpl.assertDataType(d);
		DataStore ds = getDataStore();
		if (ds == null) {
			LOG.warn("could not find data store with name " + processEngineAlias);
			return null;
		}
		ds.create(data);
		return data;
	}

	@Override
	public void onUpdatedBPMNData(BusinessProcessData businessProcessData) {
		try {
			for (BusinessProcessDataStoreEventListener eventListener : getEventListeners()) {
				eventListener.onUpdatedBPMNData(this, businessProcessData);
			}
		} catch (Exception ex) {
			LOG.error("event listener of business process data store threw an exception while handling 'updated' event: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void onDeletedBPMNData(BusinessProcessData businessProcessData) {
		try {
			for (BusinessProcessDataStoreEventListener eventListener : getEventListeners()) {
				eventListener.onDeletedBPMNData(this, businessProcessData);
			}
		} catch (Exception ex) {
			LOG.error("event listener of business process data store threw an exception while handling 'deleted' event: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void onCreatedBPMNData(BusinessProcessData businessProcessData) {
		try {
			for (BusinessProcessDataStoreEventListener eventListener : getEventListeners()) {
				eventListener.onCreatedBPMNData(this, businessProcessData);
			}
		} catch (Exception ex) {
			LOG.error("event listener of business process data store threw an exception while handling 'created' event: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void addListener(BusinessProcessDataStoreEventListener listener) {
		if (listener != null) {
			getEventListeners().add(listener);
		}
	}

	@Override
	public void removeListener(BusinessProcessDataStoreEventListener listener) {
		if (listener != null) {
			getEventListeners().remove(listener);
		}
	}

}
