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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeTableReferenceUpdate implements TransactionAppender {

	public static final TypeTableReferenceUpdate INSTANCE = new TypeTableReferenceUpdate();
	
	@Override
	public void append(Transaction tx, Record record, EngineImpl engine) {
		TypeTable table = engine.getTableRegistry().getTypeTableByType(record.getType());
		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Update update = qc.update();
		update.table(table.getTableName());
		boolean hasAttributesPresent = false;
		for (AttributeColumn attributeColumn : table.getColumns()) {
			final Attribute attribute = attributeColumn.getAttribute();
			if (!NamedAttributeHolderAttribute.class.isInstance(attribute)) {
				continue;
			}
			final AttributeMediator<Attribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
			if (mediator.isAttributePresent(record, attribute) && !BinaryAttribute.class.isInstance(attribute)) {
				update.set(attributeColumn.getColumnName(), mediator.createValueProviderFor(record, attribute), mediator.columnSqlType(attribute));
				hasAttributesPresent = true;
			}
		}
		if (hasAttributesPresent) {
			update.where()
					.expression()
					.criteria()
						.field(table.getPrimaryKeyColumn().getColumnName())
						.equal()
						.value(AccessorImpl.createRecordIdPreparedStatementValueProvider(table.getPrimaryKeyColumn(), record, engine.getMediatorRegistry()))
				;
			Query q = qc.build(record.getContext());
			tx.getQueryRunner().run(q);
		}
	}
	
}
