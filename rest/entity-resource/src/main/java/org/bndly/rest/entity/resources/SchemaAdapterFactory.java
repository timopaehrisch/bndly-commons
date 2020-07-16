package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.rest.entity.resources.impl.SchemaRestBeanProviderTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SchemaAdapterFactory.class, immediate = true)
public class SchemaAdapterFactory {

	private SchemaRestBeanProviderTracker schemaRestBeanProviderTracker;

	public static interface Listener {
		void onSchemaAdapterCreated(SchemaAdapter schemaAdapter);
		void onSchemaAdapterDestroyed(SchemaAdapter schemaAdapter);
	}
	
	
	private final List<Listener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	@Activate
	public void activate(ComponentContext componentContext) {
		schemaRestBeanProviderTracker = new SchemaRestBeanProviderTracker(componentContext.getBundleContext());
		schemaRestBeanProviderTracker.open();
	}
	@Deactivate
	public void deactivate() {
		schemaRestBeanProviderTracker.close();
		schemaRestBeanProviderTracker = null;
	}
	
	public void fireSchemaAdapterCreatedEvent(SchemaAdapter schemaAdapter) {
		listenersLock.readLock().lock();
		try {
			for (SchemaAdapterFactory.Listener listener : listeners) {
				listener.onSchemaAdapterCreated(schemaAdapter);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}
	
	public void fireSchemaAdapterRemovedEvent(SchemaAdapter schemaAdapter) {
		listenersLock.readLock().lock();
		try {
			for (SchemaAdapterFactory.Listener listener : listeners) {
				listener.onSchemaAdapterDestroyed(schemaAdapter);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}
	
}
