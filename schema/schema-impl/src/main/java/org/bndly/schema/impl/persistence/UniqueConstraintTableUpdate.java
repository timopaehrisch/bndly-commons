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
import org.bndly.schema.api.db.UniqueConstraintTable;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.UniqueConstraint;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class UniqueConstraintTableUpdate implements TransactionAppender {

	public static final UniqueConstraintTableUpdate INSTANCE = new UniqueConstraintTableUpdate();
	
	@Override
	public void append(Transaction tx, Record record, EngineImpl engine) {
		_buildUniqueConstraintUpdateQueries(record, tx, engine);
	}
	
	private void _buildUniqueConstraintUpdateQueries(final Record record, Transaction transaction, EngineImpl engine) {
		List<UniqueConstraint> constraints = engine.getConstraintRegistry().getUniqueConstraintsForType(record.getType());
		if (constraints != null) {
			for (final UniqueConstraint uniqueConstraint : constraints) {
				UniqueConstraintTable table = engine.getTableRegistry().getUniqueConstraintTableByConstraint(uniqueConstraint);
				QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
				Update update = qc.update();
				update.table(table.getTableName());

				// update values in uniqueConstraintTable
				List<AttributeColumn> columns = table.getColumns();
				UniqueConstraint constraint = table.getUniqueConstraint();
				List<Attribute> atts = constraint.getAttributes();
				boolean shouldRunQuery = false;
				for (final Attribute attribute : atts) {
					final AttributeMediator<Attribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
					if (mediator.isAttributePresent(record, attribute)) {
						Object value = mediator.getAttributeValue(record, attribute);
						AttributeColumn attributeColumn = null;
						for (AttributeColumn col : columns) {
							if (col.getAttribute() == attribute) {
								attributeColumn = col;
							}
						}
						if (attributeColumn == null) {
							throw new IllegalStateException("could not find column for attribute: " + attribute.getName());
						}
						update.set(attributeColumn.getColumnName(), mediator.createValueProviderFor(record, attribute), mediator.columnSqlType(attribute));
						shouldRunQuery = true;
					}
				}
				if (shouldRunQuery) {
					AttributeColumn col = engine.getConstraintRegistry().getJoinColumnForTypeInUniqueConstraintTable(uniqueConstraint, record.getType());
					String recordIdField = col.getColumnName();
					update.where().expression().criteria().field(recordIdField).equal().value(AccessorImpl.createRecordIdPreparedStatementValueProvider(col, record, engine.getMediatorRegistry()));
					Query q = qc.build(record.getContext());
					transaction.getQueryRunner().run(q);
				}
			}
		}
	}
}
