package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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

/**
 * A config source is able to list configurations and to provide events on config changes within the source.
 */
public interface ConfigSource {
	
	public interface Item {
		ConfigSource getSource();
		void install(ConfigurationContext configurationContext);
		void uninstall(ConfigurationContext configurationContext) throws ConfigurationException;
	}
	
	public interface ConfigSourceListener {
		void itemDeleted(Item item);
		void itemUpdated(Item item);
		void itemCreated(Item item);
	}
	
	Iterable<Item> list();
	
	void registerListener(ConfigSourceListener listener);
	
	void unregisterListener(ConfigSourceListener listener);
}
