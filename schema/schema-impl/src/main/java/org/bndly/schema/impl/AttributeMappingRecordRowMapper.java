package org.bndly.schema.impl;

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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.model.Attribute;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class AttributeMappingRecordRowMapper implements RowMapper<Record> {

	private final Map<String, AttributeColumnToTableBinding> attributeMappings;
	private final Map<String, TypeTable> tablesByIdColumnAlias;
	private final MediatorRegistryImpl mediatorRegistry;
	private final Accessor accessor;
	private final RecordContext recordContext;

	public AttributeMappingRecordRowMapper(
			Map<String, AttributeColumnToTableBinding> attributeMappings, 
			Map<String, TypeTable> tablesByIdColumnAlias, 
			MediatorRegistryImpl mediatorRegistry, 
			Accessor accessor, 
			RecordContext recordContext
	) {
		this.attributeMappings = attributeMappings;
		this.tablesByIdColumnAlias = tablesByIdColumnAlias;
		this.mediatorRegistry = mediatorRegistry;
		this.accessor = accessor;
		this.recordContext = recordContext;
	}

	@Override
	public Record mapRow(ResultSet rs, int i) throws SQLException {
		Record r = null;
		if (attributeMappings.isEmpty()) {
			// this will be hard
		} else {
			// first of, we have to find the correct type for the matching row
			// this can be done by finding the id column that has a non-null value
			// the id column is mapped 1:1 to a type specific table
			for (Map.Entry<String, TypeTable> entry : tablesByIdColumnAlias.entrySet()) {
				String idColumnAlias = entry.getKey();
				TypeTable typeTable = entry.getValue();
				Long id = rs.getLong(idColumnAlias);
				if (!rs.wasNull()) {
					r = recordContext.get(typeTable.getType(), id);
					for (Map.Entry<String, AttributeColumnToTableBinding> e : attributeMappings.entrySet()) {
						String alias = e.getKey();
						AttributeColumnToTableBinding attributeColumnToTableBinding = e.getValue();
						if (attributeColumnToTableBinding.getTable() == typeTable) {
							AttributeColumn col = attributeColumnToTableBinding.getAttributeColumn();
							if (col != null) {
								Attribute att = col.getAttribute();
								AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(att);
								Object value = mediator.extractFromResultSet(rs, alias, att, recordContext);
								r.setAttributeValue(att.getName(), value);
							}
						}
					}
					break;
				} else {
					continue;
				}
			}
		}
		if (r == null) {
			throw new IllegalStateException("could not map row to record.");
		}
		if (RecordImpl.class.isInstance(r)) {
			((RecordImpl) r).setIsDirty(false);
		}
		return r;
	}

}
