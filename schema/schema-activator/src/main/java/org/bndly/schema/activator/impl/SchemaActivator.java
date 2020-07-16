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

import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.schema.definition.parser.api.SchemaDefinitionIO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SchemaActivator.class, immediate = true)
public class SchemaActivator {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaActivator.class);
	@Reference
	private SchemaDefinitionIO schemaDefinitionIO;
	
	private SchemaRestBeanJAXBMessageClassProviderTracker schemaRestBeanJAXBMessageClassProviderTracker;
	private SchemaBeanBundleTracker schemaBeanBundleTracker;
	private SchemaConfigurationTracker schemaConfigurationTracker;
	
	@Activate
	public void activate(final ComponentContext componentContext) {
		try {
			schemaConfigurationTracker = new SchemaConfigurationTracker(componentContext.getBundleContext(), schemaDefinitionIO);
			schemaRestBeanJAXBMessageClassProviderTracker = new SchemaRestBeanJAXBMessageClassProviderTracker(componentContext.getBundleContext());
			schemaBeanBundleTracker = new SchemaBeanBundleTracker(componentContext.getBundleContext());
			schemaConfigurationTracker.open();
			schemaRestBeanJAXBMessageClassProviderTracker.open();
			schemaBeanBundleTracker.open();
		} catch (Exception e) {
			LOG.error("activation of schema activator failed: " + e.getMessage(), e);
		}
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		if (schemaBeanBundleTracker != null) {
			schemaBeanBundleTracker.close();
			schemaBeanBundleTracker = null;
		}
		if (schemaRestBeanJAXBMessageClassProviderTracker != null) {
			schemaRestBeanJAXBMessageClassProviderTracker.close();
			schemaRestBeanJAXBMessageClassProviderTracker = null;
		}
		if (schemaConfigurationTracker != null) {
			schemaConfigurationTracker.close();
			schemaConfigurationTracker = null;
		}
	}
	
	static <E> ServiceRegistration<E> registerContainerService(String schemaName, Class<E> api, E instance, BundleContext bc, Class... interfaces) {
		ServiceRegistrationBuilder<E> builder;
		if (interfaces != null && interfaces.length > 0) {
			builder = ServiceRegistrationBuilder.newInstance(api, instance);
			builder.serviceInterface(api);
			for (Class aInterface : interfaces) {
				builder.serviceInterface(aInterface);
			}
		} else {
			builder = ServiceRegistrationBuilder.newInstance(api, instance);
		}
		return builder
				.pid(api.getName() + "." + schemaName)
				.property("schema", schemaName)
				.register(bc);
	}
	
}
