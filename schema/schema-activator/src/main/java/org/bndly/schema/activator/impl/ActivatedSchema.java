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
import org.bndly.schema.activator.api.InstalledSchema;
import static org.bndly.schema.activator.impl.SchemaActivator.registerContainerService;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.SchemaRestBeanProvider;
import org.bndly.schema.definition.parser.api.ParsingException;
import org.bndly.schema.definition.parser.api.SchemaDefinitionIO;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaProvider;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ActivatedSchema {
	private static final Logger LOG = LoggerFactory.getLogger(ActivatedSchema.class);
	
	private final String schemaName;
	private final SchemaConfiguration configuration;
	private final BundleContext bundleContext;
	private final SchemaDefinitionIO schemaDefinitionIO;
	private ContainerRegisteredService<SchemaProvider> schemaProvider;
	private ContainerRegisteredService<InstalledSchema> installedSchema;
	private SchemaBeanProvider schemaBeanProvider;
	private SchemaRestBeanProvider schemaRestBeanProvider;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private ServiceTracker<SchemaBeanProvider, SchemaBeanProvider> schemaBeanProviderTracker;
	private ServiceTracker<SchemaRestBeanProvider, SchemaRestBeanProvider> schemaRestBeanProviderTracker;
	
	public ActivatedSchema(SchemaConfiguration configuration, BundleContext bundleContext, SchemaDefinitionIO schemaDefinitionIO) {
		if (configuration.getName() == null) {
			throw new IllegalArgumentException("schemaName is not allowed to be null");
		}
		this.schemaName = configuration.getName();
		this.configuration = configuration;
		this.bundleContext = bundleContext;
		this.schemaDefinitionIO = schemaDefinitionIO;
	}

	public void init() {
		// register a schemaprovider
		registerSchemaProvider();
		
		// start tracking the services
		if (configuration.getSchemaBeanPackage() != null) {
			// track a schema bean provider
			schemaBeanProviderTracker = new ServiceTracker<SchemaBeanProvider, SchemaBeanProvider>(bundleContext, SchemaBeanProvider.class, null) {
				@Override
				public SchemaBeanProvider addingService(ServiceReference<SchemaBeanProvider> reference) {
					lock.writeLock().lock();
					try {
						SchemaBeanProvider tmp = super.addingService(reference);
						String tmpSchema = new DictionaryAdapter(reference).getString("schema");
						if (schemaBeanProvider == null && schemaName.equals(tmpSchema)) {
							LOG.info("found schema bean provider for schema {}", schemaName);
							schemaBeanProvider = tmp;
							registerInstalledSchema();
						}
						return tmp;
					} finally {
						lock.writeLock().unlock();
					}
				}

				@Override
				public void removedService(ServiceReference<SchemaBeanProvider> reference, SchemaBeanProvider service) {
					lock.writeLock().lock();
					try {
						if (schemaBeanProvider == service) {
							unregisterInstalledSchema();
							LOG.info("removed schema bean provider of schema {}", schemaName);
							schemaBeanProvider = null;
						}
					} finally {
						lock.writeLock().unlock();
					}
				}
				
			};
			LOG.info("started tracking schema bean provider schema {}", schemaName);
			schemaBeanProviderTracker.open();
		}

		if (configuration.getSchemaRestBeanPackage() != null) {
			// track a schema rest bean provider
			schemaRestBeanProviderTracker = new ServiceTracker<SchemaRestBeanProvider, SchemaRestBeanProvider>(bundleContext, SchemaRestBeanProvider.class, null) {
				@Override
				public SchemaRestBeanProvider addingService(ServiceReference<SchemaRestBeanProvider> reference) {
					lock.writeLock().lock();
					try {
						SchemaRestBeanProvider tmp = super.addingService(reference);
						String tmpSchema = new DictionaryAdapter(reference).getString("schema");
						if (schemaRestBeanProvider == null && schemaName.equals(tmpSchema)) {
							LOG.info("found schema rest bean provider for schema {}", schemaName);
							schemaRestBeanProvider = tmp;
							registerInstalledSchema();
						}
						return tmp;
					} finally {
						lock.writeLock().unlock();
					}
				}

				@Override
				public void removedService(ServiceReference<SchemaRestBeanProvider> reference, SchemaRestBeanProvider service) {
					lock.writeLock().lock();
					try {
						if (schemaRestBeanProvider == service) {
							unregisterInstalledSchema();
							LOG.info("removed schema rest bean provider of schema {}", schemaName);
							schemaRestBeanProvider = null;
						}
					} finally {
						lock.writeLock().unlock();
					}
				}
				
			};
			LOG.info("started tracking schema rest bean provider schema {}", schemaName);
			schemaRestBeanProviderTracker.open();
		}
	}

	private void unregisterInstalledSchema() {
		if (installedSchema != null) {
			installedSchema.getServiceRegistration().unregister();
			installedSchema = null;
		}
	}
	
	public void destruct() {
		if (schemaProvider != null) {
			LOG.info("unregistering schema provider {}", schemaName);
			schemaProvider.getServiceRegistration().unregister();
			schemaProvider = null;
		}
		if (schemaRestBeanProviderTracker != null) {
			schemaRestBeanProviderTracker.close();
			schemaRestBeanProviderTracker = null;
		}
		if (schemaBeanProviderTracker != null) {
			schemaBeanProviderTracker.close();
			schemaBeanProviderTracker = null;
		}
	}

	public SchemaConfiguration getConfiguration() {
		return configuration;
	}

	public ContainerRegisteredService<SchemaProvider> getSchemaProvider() {
		return schemaProvider;
	}

	public String getSchemaName() {
		return schemaName;
	}

	private void registerSchemaProvider() {
		String root = configuration.getRoot();
		if (root != null) {
			final Collection<String> extensions = configuration.getExtensions();
			try {
				final Schema schema;
				if (extensions == null || extensions.isEmpty()) {
					schema = schemaDefinitionIO.parse(root);
				} else {
					String[] tmp = new String[extensions.size()];
					int i = 0;
					for (String extension : extensions) {
						tmp[i] = extension;
						i++;
					}
					schema = schemaDefinitionIO.parse(root, tmp);
				}
				
				SimpleSchemaProvider simpleSchemaProvider = new SimpleSchemaProvider(schema);
				LOG.info("registering schema provider {}", schemaName);
				ServiceRegistration<SchemaProvider> reg = registerContainerService(schemaName, SchemaProvider.class, simpleSchemaProvider, bundleContext);
				schemaProvider = new ContainerRegisteredService<>(reg, simpleSchemaProvider);
			} catch (ParsingException e) {
				LOG.error("failed to get schema from config " + configuration.getName(), e);
			}
		}
	}
	
	private void registerInstalledSchema() {
		if (schemaProvider == null || installedSchema != null) {
			return;
		}
		if (schemaBeanProviderTracker != null && schemaBeanProvider == null) {
			return;
		}
		if (schemaRestBeanProviderTracker != null && schemaRestBeanProvider == null) {
			return;
		}
		InstalledSchema is = new InstalledSchema() {
			@Override
			public String getName() {
				return schemaName;
			}

			@Override
			public Schema getSchema() {
				return schemaProvider.getService().getSchema();
			}

			@Override
			public String getSchemaRestBeanPackage() {
				return schemaRestBeanProvider.getSchemaRestBeanPackage();
			}

			@Override
			public String getSchemaBeanPackage() {
				return schemaBeanProvider.getSchemaBeanPackage();
			}

			@Override
			public ClassLoader getSchemaRestBeanClassLoader() {
				return schemaRestBeanProvider.getSchemaRestBeanClassLoader();
			}

			@Override
			public ClassLoader getSchemaBeanClassLoader() {
				return schemaBeanProvider.getSchemaBeanClassLoader();
			}
		};
		LOG.info("registering installed schema {}", schemaName);
		ServiceRegistration<InstalledSchema> reg = registerContainerService(schemaName, InstalledSchema.class, is, bundleContext);
		installedSchema = new ContainerRegisteredService<>(reg, is);
	}
}
