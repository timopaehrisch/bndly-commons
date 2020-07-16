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

import org.bndly.common.osgi.util.DictionaryAdapter;
import javax.sql.DataSource;
import org.activiti.engine.ProcessEngine;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DataSourceTracker extends ServiceTracker<DataSource, DataSource> {

	private final ActivitiEngineConfiguration activitiEngineConfiguration;
	private final ActivitiEngineProvider activitiEngineProvider;
	private DataSource dataSource;
	private String requiredDatasourcePid;
	private ProcessEngine engine;

	public DataSourceTracker(ActivitiEngineConfiguration activitiEngineConfiguration, ActivitiEngineProvider activitiEngineProvider, BundleContext context) {
		super(context, DataSource.class, null);
		this.activitiEngineConfiguration = activitiEngineConfiguration;
		if (activitiEngineConfiguration == null) {
			throw new IllegalArgumentException("activitiEngineConfiguration is not allowed to be null");
		}
		this.activitiEngineProvider = activitiEngineProvider;
		if (activitiEngineProvider == null) {
			throw new IllegalArgumentException("activitiEngineProvider is not allowed to be null");
		}
		requiredDatasourcePid = DataSource.class.getName() + "." + activitiEngineConfiguration.getDatasource();
	}

	@Override
	public DataSource addingService(ServiceReference<DataSource> reference) {
		DataSource ds = super.addingService(reference);
		String pid = new DictionaryAdapter(reference).getString(Constants.SERVICE_PID);
		if (requiredDatasourcePid.equals(pid)) {
			dataSource = ds;
			engine = activitiEngineProvider.createEngine(dataSource, activitiEngineConfiguration);
		}
		return ds;
	}

	@Override
	public void removedService(ServiceReference<DataSource> reference, DataSource service) {
		if (service == dataSource) {
			dataSource = null;
			activitiEngineProvider.destroyEngine(engine);
		}
		super.removedService(reference, service);
	}

	@Override
	public void close() {
		try {
			if (engine != null) {
				activitiEngineProvider.destroyEngine(engine);
			}
			super.close();
		} finally {
			engine = null;
			dataSource = null;
		}
	}

}
