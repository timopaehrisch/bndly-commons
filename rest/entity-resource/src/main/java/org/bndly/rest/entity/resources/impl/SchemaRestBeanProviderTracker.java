package org.bndly.rest.entity.resources.impl;

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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.schema.api.SchemaRestBeanProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaRestBeanProviderTracker extends ServiceTracker<SchemaRestBeanProvider, SchemaRestBeanProvider> {

	private static final Logger LOG = LoggerFactory.getLogger(SchemaRestBeanProviderTracker.class);
	private final List<SchemaBeanFactoryTracker> trackers = new ArrayList<>();
	private final Lock trackersLock = new ReentrantLock();
	
	public SchemaRestBeanProviderTracker(BundleContext bundleContext) {
		super(bundleContext, SchemaRestBeanProvider.class, null);
	}

	@Override
	public SchemaRestBeanProvider addingService(ServiceReference<SchemaRestBeanProvider> reference) {
		SchemaRestBeanProvider tmp = super.addingService(reference);
		trackersLock.lock();
		try {
			String schemaName = new DictionaryAdapter(reference).getString("schema");
			if (schemaName != null) {
				SchemaBeanFactoryTracker tracker = new SchemaBeanFactoryTracker(context, tmp);
				trackers.add(tracker);
				tracker.open();
			}
			return tmp;
		} catch (InvalidSyntaxException ex) {
			LOG.error("could not create schema bean factory tracker", ex);
			return tmp;
		} finally {
			trackersLock.unlock();
		}
	}

	@Override
	public void removedService(ServiceReference<SchemaRestBeanProvider> reference, SchemaRestBeanProvider service) {
		trackersLock.lock();
		try {
			Iterator<SchemaBeanFactoryTracker> iterator = trackers.iterator();
			while (iterator.hasNext()) {
				SchemaBeanFactoryTracker next = iterator.next();
				if (next.getSchemaRestBeanProvider() == service) {
					iterator.remove();
					next.close();
				}
			}
		} finally {
			trackersLock.unlock();
		}
	}
	
}
