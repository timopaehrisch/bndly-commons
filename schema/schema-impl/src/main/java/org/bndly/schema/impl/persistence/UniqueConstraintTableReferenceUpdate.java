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
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.UniqueConstraint;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class UniqueConstraintTableReferenceUpdate implements TransactionAppender {

	public static final UniqueConstraintTableReferenceUpdate INSTANCE = new UniqueConstraintTableReferenceUpdate();
	
	@Override
	public void append(Transaction tx, Record record, EngineImpl engine) {
		List<UniqueConstraint> constraints = engine.getConstraintRegistry().getUniqueConstraintsForType(record.getType());
		if (constraints == null) {
			return;
		}
		for (UniqueConstraint constraint : constraints) {
			List<Attribute> affectedAttributes = constraint.getAttributes();
			for (Attribute affectedAttribute : affectedAttributes) {
				if (NamedAttributeHolderAttribute.class.isInstance(affectedAttribute)) {
					// schedule the update of the constraint
					scheduleUpdateForConstraint(constraint, tx, record, engine);
					break;
				}
			}
		}
	}

	private void scheduleUpdateForConstraint(UniqueConstraint constraint, Transaction tx, Record record, EngineImpl engine) {
		UniqueConstraintTable table = engine.getTableRegistry().getUniqueConstraintTableByConstraint(constraint);
		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Update update = qc.update();
		update.table(table.getTableName());

		// update values in uniqueConstraintTable
		List<AttributeColumn> columns = table.getColumns();
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
					throw new SchemaException("could not find column for attribute: " + attribute.getName());
				}
				update.set(attributeColumn.getColumnName(), mediator.createValueProviderFor(record, attribute), mediator.columnSqlType(attribute));
				shouldRunQuery = true;
			}
		}
		if (shouldRunQuery) {
			AttributeColumn col = engine.getConstraintRegistry().getJoinColumnForTypeInUniqueConstraintTable(constraint, record.getType());
			String recordIdField = col.getColumnName();
			update.where().expression().criteria().field(recordIdField).equal().value(AccessorImpl.createRecordIdPreparedStatementValueProvider(col, record, engine.getMediatorRegistry()));
			Query q = qc.build(record.getContext());
			tx.getQueryRunner().run(q);
		}
	}
	
}
