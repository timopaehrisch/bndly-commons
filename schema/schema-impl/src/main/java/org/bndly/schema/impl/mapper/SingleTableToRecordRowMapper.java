package org.bndly.schema.impl.mapper;

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
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.impl.MediatorRegistryImpl;
import org.bndly.schema.impl.RecordImpl;
import org.bndly.schema.model.Attribute;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SingleTableToRecordRowMapper implements RowMapper<Record> {

	private final Accessor accessor;
	private final TypeTable table;
	private final MediatorRegistryImpl mediatorRegistry;
	private final RecordContext recordContext;

	public SingleTableToRecordRowMapper(TypeTable table, MediatorRegistryImpl mediatorRegistry, Accessor accessor, RecordContext recordContext) {
		this.table = table;
		this.mediatorRegistry = mediatorRegistry;
		this.accessor = accessor;
		this.recordContext = recordContext;
	}

	@Override
	public Record mapRow(ResultSet rs, int i) throws SQLException {
		long id = rs.getLong("id");
		Record r = recordContext.get(table.getType(), id);

		List<AttributeColumn> columns = table.getColumns();
		for (AttributeColumn col : columns) {
			Attribute attribute = col.getAttribute();
			AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(attribute);
			Object value = mediator.extractFromResultSet(rs, col.getColumnName(), attribute, recordContext);
			r.setAttributeValue(attribute.getName(), value);
		}

		if (RecordImpl.class.isInstance(r) && r != null) {
			((RecordImpl) r).setIsDirty(false);
		}
		return r;
	}
}
