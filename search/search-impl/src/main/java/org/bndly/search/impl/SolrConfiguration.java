package org.bndly.search.impl;

/*-
 * #%L
 * Search Impl
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
		service = SolrConfiguration.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = SolrConfiguration.Configuration.class)
public class SolrConfiguration {
	
	@ObjectClassDefinition(
		name = "Solr Configuration",
		description = "The Solr configuration defines a Solr instance that will be made accessible to clients in the OSGI container."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "The name will be used for the client service registration in the OSGI container."
		)
		String name() default "default";

		@AttributeDefinition(
				name = "URL",
				description = "The URL of the Solr server, that shall be contacted via the client."
		)
		String baseUrl() default "http://localhost:8081/solr";

	}
	
	private String name;
	private String baseUrl;

	@Activate
	public void activate(Configuration configuration) {
		name = configuration.name();
		baseUrl = configuration.baseUrl();
	}
	
	public String getName() {
		return name;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
	
}
