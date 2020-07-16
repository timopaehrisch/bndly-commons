package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.services.Engine;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaResourceInstance implements Resource {

	private final Engine engine;
	private final ResourceURI uri;
	private final ResourceProvider provider;
	private Record record;

	public SchemaResourceInstance(ResourceURI uri, ResourceProvider provider, Engine engine, Record record) {
		this.uri = uri;
		this.engine = engine;
		this.provider = provider;
		if (record == null) {
			List<String> elements = uri.getPath().getElements();
			String typeName = null;
			if (elements.size() > 1) {
				typeName = elements.get(1);
			}
			Long id = null;
			if (elements.size() > 2) {
				try {
					id = new Long(elements.get(2));
				} catch (NumberFormatException e) {
					// ignore this. then we select a list
				}
			}

			if (typeName != null) {
				TypeTable table = engine.getTableRegistry().getTypeTableByType(typeName);
				if (table != null) {
					if (id != null) {
						record = engine.getAccessor().readById(typeName, id, engine.getAccessor().buildRecordContext());
					}
				}
			}
		}
		this.record = record;
	}

	public SchemaResourceInstance(ResourceURI uri, ResourceProvider provider, Engine engine) {
		this(uri, provider, engine, null);
	}

	public Record getRecord() {
		return record;
	}

	@Override
	public ResourceURI getURI() {
		return uri;
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}
}
