package org.bndly.common.data.impl;

/*-
 * #%L
 * File System Data Impl
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
import org.osgi.service.component.ComponentContext;
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
@Component(service = FolderWatcherConfiguration.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = FolderWatcherConfiguration.Configuration.class)
public class FolderWatcherConfiguration {
	
	@ObjectClassDefinition(name = "Folder Watcher")
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "Name of the watcher"
		)
		String name();

		@AttributeDefinition(
				name = "Folder",
				description = "The path of the watched folder"
		)
		String folder();

		@AttributeDefinition(
				name = "Frequency",
				description = "Frequency of looking for changes in ms"
		)
		int frequency() default 1000;
	}
	
	private DictionaryAdapter adapter;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		this.adapter = new DictionaryAdapter(componentContext.getProperties()).emptyStringAsNull();
	}
	public String getName() {
		return adapter.getString("name");
	}
	public String getFolder() {
		return adapter.getString("folder");
	}
	public Integer getFrequency() {
		return adapter.getInteger("frequency", 1000);
	}
}
