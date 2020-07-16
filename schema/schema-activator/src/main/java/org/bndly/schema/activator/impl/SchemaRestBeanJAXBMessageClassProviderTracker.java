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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import static org.bndly.schema.activator.impl.SchemaActivator.registerContainerService;
import org.bndly.schema.api.SchemaRestBeanProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks JAXBMessageClassProviders and registers them as SchemaRestBeanProvider instances.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaRestBeanJAXBMessageClassProviderTracker extends ServiceTracker<JAXBMessageClassProvider, JAXBMessageClassProvider>{

	private static final Logger LOG = LoggerFactory.getLogger(SchemaRestBeanJAXBMessageClassProviderTracker.class);
	
	private  class KnownJAXBMessageClassProvider {
		private final ServiceReference<JAXBMessageClassProvider> serviceReference;
		private final JAXBMessageClassProvider messageClassProvider;
		private final DictionaryAdapter dictionaryAdapter;
		private ServiceRegistration schemaRestBeanProviderReg;

		public KnownJAXBMessageClassProvider(ServiceReference<JAXBMessageClassProvider> serviceReference, JAXBMessageClassProvider messageClassProvider) {
			this.serviceReference = serviceReference;
			this.messageClassProvider = messageClassProvider;
			dictionaryAdapter = new DictionaryAdapter(serviceReference);
		}
		
		public String getSchemaName() {
			return dictionaryAdapter.getString("schema");
		}
		
		public String getSchemaRestBeanPackage() {
			return dictionaryAdapter.getString("schemaRestBeanPackage");
		}
		
		public void init() {
			if (schemaRestBeanProviderReg == null) {
				JAXBSchemaRestBeanProvider schemaRestBeanProvider = new JAXBSchemaRestBeanProvider(messageClassProvider, getSchemaName(), getSchemaRestBeanPackage());
				schemaRestBeanProviderReg = registerContainerService(getSchemaName(), SchemaRestBeanProvider.class, schemaRestBeanProvider, context);
			}
		}
		
		public void destruct() {
			if (schemaRestBeanProviderReg != null) {
				schemaRestBeanProviderReg.unregister();
				schemaRestBeanProviderReg = null;
			}
		}
	}
	
	private final Map<String, KnownJAXBMessageClassProvider> providersBySchemaName = new HashMap<>();
	private final ReadWriteLock providersBySchemaNameLock = new ReentrantReadWriteLock();
	
	public SchemaRestBeanJAXBMessageClassProviderTracker(BundleContext bundleContext) throws InvalidSyntaxException {
		super(bundleContext, bundleContext.createFilter("(schemaRestBeanProvider=true)"), null);
	}
	
	@Override
	public JAXBMessageClassProvider addingService(ServiceReference<JAXBMessageClassProvider> reference) {
		JAXBMessageClassProvider instance = super.addingService(reference);
		providersBySchemaNameLock.writeLock().lock();
		try {
			KnownJAXBMessageClassProvider knownJAXBMessageClassProvider = new KnownJAXBMessageClassProvider(reference, instance);
			KnownJAXBMessageClassProvider old = providersBySchemaName.put(knownJAXBMessageClassProvider.getSchemaName(), knownJAXBMessageClassProvider);
			if (old != null) {
				LOG.warn("overwriting schema rest bean message class provider");
				old.destruct();
			}
			knownJAXBMessageClassProvider.init();
		} finally {
			providersBySchemaNameLock.writeLock().unlock();
		}
		return instance;
	}

	@Override
	public void removedService(ServiceReference<JAXBMessageClassProvider> reference, JAXBMessageClassProvider service) {
		String schemaName = new KnownJAXBMessageClassProvider(reference, service).getSchemaName();
		providersBySchemaNameLock.writeLock().lock();
		try {
			KnownJAXBMessageClassProvider old = providersBySchemaName.remove(schemaName);
			if (old.messageClassProvider != service) {
				providersBySchemaName.put(schemaName, old);
			} else {
				old.destruct();
			}
		} finally {
			providersBySchemaNameLock.writeLock().unlock();
		}
		super.removedService(reference, service);
	}
	
}
