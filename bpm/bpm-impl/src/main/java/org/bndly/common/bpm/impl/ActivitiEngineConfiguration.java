package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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
import org.osgi.service.metatype.annotations.Option;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = ActivitiEngineConfiguration.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = ActivitiEngineConfiguration.Configuration.class)
public class ActivitiEngineConfiguration {
	
	@ObjectClassDefinition(
		name = "Activiti Engine Configuration",
		description = "This configuration defines an instance of the Activiti process engine."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "The name of the Activiti process engine instance"
		)
		String name();

		@AttributeDefinition(
				name = "Datasource",
				description = "The name of the datasource to use"
		)
		String datasource();

		@AttributeDefinition(
				name = "Schema Creation",
				description = "Mode of how to create the database schema",
				options = {
					@Option(value = "DB_SCHEMA_UPDATE_TRUE", label = "Update if necessary"),
					@Option(value = "DB_SCHEMA_UPDATE_FALSE", label = "Fail on mismatching schema"),
					@Option(value = "DB_SCHEMA_UPDATE_CREATE_DROP", label = "Create on start, drop on end")
				}
		)
		String schema();

		@AttributeDefinition(
				name = "Aynchronous execution enabled",
				description = "If this value is set to true, asynchronous executions are enabled. Those are required for timer events."
		)
		boolean asyncEnabled() default false;

		@AttributeDefinition(
				name = "Aynchronous core thread pool size",
				description = "The amount of threads to permanently keep in the thread pool."
		)
		int asyncCorePoolSize() default -1;

		@AttributeDefinition(
				name = "Aynchronous max thread pool size",
				description = "The maximum amount of threads to hold in the thread pool."
		)
		int asyncMaxPoolSize() default -1;

		@AttributeDefinition(
				name = "Aynchronous execution queue size",
				description = "The size of the queue for pending executions."
		)
		int asyncQueueSize() default -1;
	}
	
	private String name;
	private String datasource;
	private String schema;
	private Boolean asyncEnabled;
	private Integer asyncCorePoolSize;
	private Integer asyncMaxPoolSize;
	private Integer asyncQueueSize;

	@Activate
	public void activate(Configuration configuration) {
		name = configuration.name();
		datasource = configuration.datasource();
		schema = configuration.schema();
		asyncEnabled = configuration.asyncEnabled();
		asyncCorePoolSize = minusOneToNull(configuration.asyncCorePoolSize());
		asyncMaxPoolSize = minusOneToNull(configuration.asyncMaxPoolSize());
		asyncQueueSize = minusOneToNull(configuration.asyncQueueSize());
	}

	private Integer minusOneToNull(int intValue) {
		return intValue == -1 ? null : intValue;
	}

	public String getName() {
		return name;
	}

	public String getDatasource() {
		return datasource;
	}

	public String getSchema() {
		return schema;
	}

	public boolean getAsyncEnabled() {
		return asyncEnabled;
	}

	public Integer getAsyncCorePoolSize() {
		return asyncCorePoolSize;
	}

	public Integer getAsyncMaxPoolSize() {
		return asyncMaxPoolSize;
	}

	public Integer getAsyncQueueSize() {
		return asyncQueueSize;
	}
	
}
