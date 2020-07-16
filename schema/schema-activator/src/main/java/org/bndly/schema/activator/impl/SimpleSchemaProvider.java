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

import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaProvider;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class SimpleSchemaProvider implements SchemaProvider {
	private final Schema schema;

	public SimpleSchemaProvider(Schema schema) {
		if (schema == null) {
			throw new IllegalArgumentException("schema is not allowed to be null");
		}
		this.schema = schema;
	}

	@Override
	public String getSchemaName() {
		return getSchema().getName();
	}
	
	@Override
	public final Schema getSchema() {
		return schema;
	}
	
}
