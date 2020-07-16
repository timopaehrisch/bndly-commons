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

import org.bndly.schema.api.Logic;
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RollbackHandler;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.impl.UpdateQuery;
import org.bndly.schema.impl.UpdateQueryImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import java.util.List;

/**
 * This transaction appender will append an INSERT statement for the type table of the provided record to the transaction
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeTableInsert implements TransactionAppender {

	public static final TypeTableInsert INSTANCE = new TypeTableInsert();
	
	@Override
	public void append(Transaction tx, final Record record, EngineImpl engine) {
		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Insert insert = qc.insert();

		// insert values into typeTable
		final TypeTable table = engine.getTableRegistry().getTypeTableByType(record.getType());
		insert.into(table.getTableName());

		List<AttributeColumn> columns = table.getColumns();
		for (AttributeColumn attributeColumn : columns) {
			final Attribute attribute = attributeColumn.getAttribute();
			if (record.isAttributePresent(attribute.getName()) && !BinaryAttribute.class.isInstance(attribute)) {
				AccessorImpl.appendValueToInsert(insert, attributeColumn, record, engine.getMediatorRegistry());
			}
		}

		Query q = qc.build(record.getContext());
		UpdateQuery uq = new UpdateQueryImpl(q) {

			@Override
			public AttributeColumn getPrimaryKeyColumn() {
				return table.getPrimaryKeyColumn();
			}
		};
		final ObjectReference<Long> idRef = tx.getQueryRunner().number(uq, uq.getPrimaryKeyColumn().getColumnName());

		tx.add(new Logic() {

			@Override
			public void execute(Transaction transaction) {
				record.setId(idRef.get());
			}
		});
		tx.afterRollback(new RollbackHandler() {
			@Override
			public void didRollback(Transaction transaction) {
				record.setId(null);
			}
		});
	}
	
}
