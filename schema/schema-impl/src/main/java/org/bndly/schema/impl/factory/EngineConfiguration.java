package org.bndly.schema.impl.factory;

/*-
 * #%L
 * Schema Impl
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
import org.bndly.schema.vendor.VendorConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = EngineConfiguration.Configuration.class)
public class EngineConfiguration {

	private ServiceTracker<VendorConfiguration, VendorConfiguration> tracker;
	private ServiceRegistration<EngineConfiguration> reg;
	
	@ObjectClassDefinition(
			name = "Schema Engine Configuration",
			description = "The schema engine configuration binds a schema to a datasource, where the schema should be deployed."
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Schema name",
				description = "The technical name of the schema. This value should only contain [a-z] symbols."
		)
		String schema() default "myschema";

		@AttributeDefinition(
				name = "Datasource name",
				description = "The name of the datasource to use for the schema deployment."
		)
		String datasource() default "mydatasource";

		@AttributeDefinition(
				name = "Dialect",
				description = "The dialect to use for the datasource. The built in defaults are: mysql, h2, postgres and mariadb."
		)
		String dialect() default "mysql";
		
		@AttributeDefinition(
				name = "Connection",
				description = "The type of connections handling",
				options = {
					@Option(label = "Pooled connection", value = "pooled"),
					@Option(label = "Single connection", value = "single")
				}
		)
		String connection() default "pooled";

		@AttributeDefinition(
				name = "Schema validation only",
				description = "If checked, the schema will only be validated"
		)
		boolean validateOnly() default false;

		@AttributeDefinition(
				name = "Ignore schema validation errors",
				description = "If checked, the validation of the schema will not throw exceptions."
		)
		boolean ignoreValidationErrors() default false;

	}

	private String schema;
	private String datasource;
	private String dialect;
	private String connection;
	private boolean validateOnly;
	private boolean ignoreValidationErrors;
	private VendorConfiguration vendorConfig;

	@Activate
	public void activate(Configuration configuration, final BundleContext bundleContext) throws InvalidSyntaxException {
		schema = configuration.schema();
		datasource = configuration.datasource();
		dialect = configuration.dialect();
		connection = configuration.connection();
		validateOnly = configuration.validateOnly();
		// track the dialect's vendor configuration. if found, register the engine configuration as a service.
		tracker = new ServiceTracker<VendorConfiguration, VendorConfiguration>(bundleContext, bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + VendorConfiguration.class.getName() + ")(name=" + dialect + "))"), null) {
			
			@Override
			public VendorConfiguration addingService(ServiceReference<VendorConfiguration> reference) {
				VendorConfiguration tmp = super.addingService(reference);
				register(tmp, bundleContext);
				return tmp;
			}

			@Override
			public void removedService(ServiceReference<VendorConfiguration> reference, VendorConfiguration service) {
				unregister(service);
			}

		};
		tracker.open();
	}
	
	@Deactivate
	public void deactivate() {
		tracker.close();
	}

	private void unregister(VendorConfiguration tmp) {
		if(reg != null && vendorConfig == tmp) {
			reg.unregister();
			reg = null;
			vendorConfig = null;
		}
	}
	
	private void register(VendorConfiguration tmp, BundleContext context) {
		if (reg == null) {
			vendorConfig = tmp;
			reg = ServiceRegistrationBuilder.newInstance(EngineConfiguration.class, this)
					.property("schema", schema)
					.property("datasource", datasource)
					.property("dialect", dialect)
					.property("connection", connection)
					.property("validateOnly", validateOnly)
					.register(context);
		}
	}
	
	public String getSchema() {
		return schema;
	}

	public String getDatasource() {
		return datasource;
	}

	public String getDialect() {
		return dialect;
	}

	public VendorConfiguration getVendorConfig() {
		return vendorConfig;
	}

	public String getConnection() {
		return connection;
	}

	public boolean isValidateOnly() {
		return validateOnly;
	}

	public boolean isValidationErrorIgnored() {
		return ignoreValidationErrors;
	}

}
