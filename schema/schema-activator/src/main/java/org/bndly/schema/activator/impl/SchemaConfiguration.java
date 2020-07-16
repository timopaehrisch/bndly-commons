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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = SchemaConfiguration.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = SchemaConfiguration.Configuration.class)
public class SchemaConfiguration {

	private Configuration configuration;

	@ObjectClassDefinition(
		name = "Schema Configuration",
		description = "A schema configuration binds schema definition files to Java packages of according 'rest beans' and 'schema beans'"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "The name of the schema."
		)
		String name();

		@AttributeDefinition(
				name = "Root file",
				description = "The path to the root definition file of the schema"
		)
		String root();

		@AttributeDefinition(
				name = "Schema RestBean Package",
				description = "The name of the schema."
		)
		String schemaRestBeanPackage();

		@AttributeDefinition(
				name = "Schema Bean Package",
				description = "The name of the schema."
		)
		String schemaBeanPackage();

		@AttributeDefinition(
				name = "Extensions",
				description = "The paths to the extensions of the root schema"
		)
		String[] extensions();
		
	}
	
	@Activate
	public void activate(Configuration configuration) {
		this.configuration = configuration;
	}

	public String getName() {
		return configuration.name();
	}
	
	public String getRoot() {
		return configuration.root();
	}

	public String getSchemaRestBeanPackage() {
		return configuration.schemaRestBeanPackage();
	}

	public String getSchemaBeanPackage() {
		return configuration.schemaBeanPackage();
	}

	public Collection<String> getExtensions() {
		String[] exts = configuration.extensions();
		if (exts == null || exts.length == 0) {
			return Collections.EMPTY_LIST;
		} else {
			return Arrays.asList(exts);
		}
	}
}
