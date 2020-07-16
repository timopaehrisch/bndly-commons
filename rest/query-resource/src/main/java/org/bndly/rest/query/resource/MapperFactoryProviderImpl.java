package org.bndly.rest.query.resource;

/*-
 * #%L
 * REST Query Resource
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

import org.bndly.common.mapper.MapperFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = MapperFactoryProvider.class)
public class MapperFactoryProviderImpl implements MapperFactoryProvider {

	private ServiceTracker tracker;
	private final Map<String, MapperFactory> knownMapperFactories = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	@Activate
	public void activate(ComponentContext componentContext) {
		tracker = new ServiceTracker<MapperFactory,MapperFactory>(componentContext.getBundleContext(), MapperFactory.class, null) {
			@Override
			public MapperFactory addingService(ServiceReference<MapperFactory> reference) {
				MapperFactory instance = super.addingService(reference);
				String schemaName = getSchemaName(reference);
				if (schemaName != null) {
					lock.writeLock().lock();
					try {
						knownMapperFactories.put(schemaName, instance);
					} finally {
						lock.writeLock().unlock();
					}
				}
				return instance;
			}

			@Override
			public void removedService(ServiceReference<MapperFactory> reference, MapperFactory instance) {
				String schemaName = getSchemaName(reference);
				if (schemaName != null) {
					lock.writeLock().lock();
					try {
						knownMapperFactories.remove(schemaName);
					} finally {
						lock.writeLock().unlock();
					}
				}
				super.removedService(reference, instance);
			}

			String getSchemaName(ServiceReference serviceReference) {
				Object pid = serviceReference.getProperty(Constants.SERVICE_PID);
				if (!String.class.isInstance(pid)) {
					return null;
				}
				String pidString = ((String) pid);
				int i = pidString.lastIndexOf(".");
				if (i > -1) {
					return pidString.substring(i + 1);
				}
				return null;
			}
			
		};
		tracker.open();
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		tracker.close();
	}
	
	@Override
	public MapperFactory getMapperFactoryForSchema(String schemaName) {
		lock.readLock().lock();
		try {
			return knownMapperFactories.get(schemaName);
		} finally {
			lock.readLock().unlock();
		}
	}
	
}
