package org.bndly.schema.activator.impl;

/*-
 * #%L
 * Schema Activator
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

import org.bndly.schema.definition.parser.api.SchemaDefinitionIO;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaConfigurationTracker extends ServiceTracker<SchemaConfiguration, SchemaConfiguration>{

	private final List<ActivatedSchema> activatedSchemas = new ArrayList<>();
	private final ReadWriteLock activatedSchemasLock = new ReentrantReadWriteLock();
	private final SchemaDefinitionIO schemaDefinitionIO;
	
	public SchemaConfigurationTracker(BundleContext bundleContext, SchemaDefinitionIO schemaDefinitionIO) {
		super(bundleContext, SchemaConfiguration.class, null);
		this.schemaDefinitionIO = schemaDefinitionIO;
	}

	@Override
	public SchemaConfiguration addingService(ServiceReference<SchemaConfiguration> reference) {
		SchemaConfiguration config = super.addingService(reference);
		activatedSchemasLock.writeLock().lock();
		try {
			ActivatedSchema activatedSchema = new ActivatedSchema(config, context, schemaDefinitionIO);
			activatedSchema.init();
			activatedSchemas.add(activatedSchema);
		} finally {
			activatedSchemasLock.writeLock().unlock();
		}
		return config;
	}

	@Override
	public void removedService(ServiceReference<SchemaConfiguration> reference, SchemaConfiguration configuration) {
		activatedSchemasLock.writeLock().lock();
		try {
			Iterator<ActivatedSchema> iterator = activatedSchemas.iterator();
			while (iterator.hasNext()) {
				ActivatedSchema next = iterator.next();
				if (next.getConfiguration() == configuration) {
					iterator.remove();
					next.destruct();
				}
			}
		} finally {
			activatedSchemasLock.writeLock().unlock();
		}
	}
	
}
