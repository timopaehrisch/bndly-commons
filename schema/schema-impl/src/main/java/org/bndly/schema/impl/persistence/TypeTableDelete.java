package org.bndly.schema.impl.persistence;

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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Delete;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import static org.bndly.schema.impl.AccessorImpl.createRecordIdPreparedStatementValueProvider;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeTableDelete implements TransactionAppender {

	public static final TypeTableDelete INSTANCE = new TypeTableDelete();
	
	@Override
	public void append(final Transaction tx, Record record, final EngineImpl engine) {
		Query q = _buildDeleteQuery(record, engine);
		tx.getQueryRunner().run(q);
		record.iterateValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attribute;
					Boolean deleteOrphans = naha.getDeleteOrphans();
					if (deleteOrphans != null && deleteOrphans) {
						Record parentRecord = record.getAttributeValue(attribute.getName(), Record.class);
						if(parentRecord != null) {
							append(tx, parentRecord, engine);
						}
					}
				}
			}
		});
	}
	
	private Query _buildDeleteQuery(Record record, EngineImpl engine) {
		Long id = record.getId();
		if (id == null) {
			throw new SchemaException("can not delete a record without id");
		}
		TypeTable table = engine.getTableRegistry().getTypeTableByType(record.getType());
		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Delete delete = qc.delete();
		delete.from(table.getTableName())
				.where()
					.expression()
					.criteria()
						.field(table.getPrimaryKeyColumn().getColumnName())
						.equal()
						.value(createRecordIdPreparedStatementValueProvider(table.getPrimaryKeyColumn(), record, engine.getMediatorRegistry()));
		Query q = qc.build(record.getContext());
		return q;
	}
}
