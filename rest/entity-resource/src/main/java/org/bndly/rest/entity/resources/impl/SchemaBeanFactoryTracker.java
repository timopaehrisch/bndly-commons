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

import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.rest.entity.resources.SchemaAdapter;
import org.bndly.schema.api.SchemaRestBeanProvider;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.model.Schema;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaBeanFactoryTracker extends ServiceTracker<SchemaBeanFactory, SchemaBeanFactory> {

	private final SchemaRestBeanProvider schemaRestBeanProvider;
	private ServiceRegistration<SchemaAdapter> schemaAdapterReg;
	private final Lock lock = new ReentrantLock();

	public SchemaBeanFactoryTracker(BundleContext bundleContext, SchemaRestBeanProvider schemaRestBeanProvider) throws InvalidSyntaxException {
		super(bundleContext, bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + SchemaBeanFactory.class.getName() + ")(" + Constants.SERVICE_PID + "=" + SchemaBeanFactory.class.getName() + "." + schemaRestBeanProvider.getSchemaName() + "))"), null);
		this.schemaRestBeanProvider = schemaRestBeanProvider;
	}

	public SchemaRestBeanProvider getSchemaRestBeanProvider() {
		return schemaRestBeanProvider;
	}

	@Override
	public SchemaBeanFactory addingService(ServiceReference<SchemaBeanFactory> reference) {
		lock.lock();
		try {
			SchemaBeanFactory tmp = super.addingService(reference);
			registerSchemaAdapter(tmp);
			return tmp;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void removedService(ServiceReference<SchemaBeanFactory> reference, SchemaBeanFactory service) {
		lock.lock();
		try {
			unregisterSchemaAdapter();
		} finally {
			lock.unlock();
		}
	}
	
	private void unregisterSchemaAdapter() {
		if (schemaAdapterReg != null) {
			schemaAdapterReg.unregister();
			schemaAdapterReg = null;
		}
	}
	
	private void registerSchemaAdapter(final SchemaBeanFactory schemaBeanFactory) {
		if (schemaAdapterReg != null) {
			return;
		}
		SchemaAdapter schemaAdapter = new SchemaAdapter() {

				@Override
				public String getName() {
					return getSchema().getName();
				}

				@Override
				public Schema getSchema() {
					return schemaBeanFactory.getEngine().getDeployer().getDeployedSchema();
				}

				@Override
				public String getSchemaRestBeanPackage() {
					return schemaRestBeanProvider.getSchemaRestBeanPackage();
				}

				@Override
				public String getSchemaBeanPackage() {
					return schemaBeanFactory.getSchemaBeanProvider().getSchemaBeanPackage();
				}

				@Override
				public ClassLoader getSchemaRestBeanClassLoader() {
					return schemaRestBeanProvider.getSchemaRestBeanClassLoader();
				}

				@Override
				public ClassLoader getSchemaBeanClassLoader() {
					return schemaBeanFactory.getSchemaBeanProvider().getSchemaBeanClassLoader();
				}

				@Override
				public SchemaBeanFactory getSchemaBeanFactory() {
					return schemaBeanFactory;
				}

				@Override
				public JSONSchemaBeanFactory getJSONSchemaBeanFactory() {
					return schemaBeanFactory.getJsonSchemaBeanFactory();
				}

				@Override
				public Engine getEngine() {
					return schemaBeanFactory.getEngine();
				}
			};
		schemaAdapterReg = ServiceRegistrationBuilder
				.newInstance(SchemaAdapter.class, schemaAdapter)
				.pid(SchemaAdapter.class.getName() + "." + schemaAdapter.getName())
				.property("schema", schemaAdapter.getName())
				.register(context);
	}
}
