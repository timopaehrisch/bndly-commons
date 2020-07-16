package org.bndly.schema.impl.db;

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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.model.Attribute;

public final class AttributeColumnImpl implements AttributeColumn {

	private final Attribute attribute;
	private final String columnName;
	private final String columnType;
	private final Table table;
	private final boolean requiresIndex;
	private final boolean primaryKey;

	public AttributeColumnImpl(Attribute attribute, String columnName, String columnType, Table table, boolean requiresIndex, boolean primaryKey) {
		this.attribute = attribute;
		this.columnName = columnName;
		this.columnType = columnType;
		this.table = table;
		this.requiresIndex = requiresIndex;
		this.primaryKey = primaryKey;
	}

	@Override
	public final Table getTable() {
		return table;
	}

	@Override
	public final Attribute getAttribute() {
		return attribute;
	}

	@Override
	public final String getColumnName() {
		return columnName;
	}

	@Override
	public final String getColumnType() {
		return columnType;
	}

	@Override
	public final boolean requiresIndex() {
		return requiresIndex;
	}

	@Override
	public boolean isPrimaryKey() {
		return primaryKey;
	}

}
