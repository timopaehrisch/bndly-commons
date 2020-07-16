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
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.UniqueConstraint;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class UniqueConstraintTableInsert implements TransactionAppender {

	public static final UniqueConstraintTableInsert INSTANCE = new UniqueConstraintTableInsert();
	
	@Override
	public void append(Transaction tx, final Record record, EngineImpl engine) {
		_buildTypeUniqueConstraintInsertQueries(record, new ValueProvider<Long>() {
			@Override
			public Long get() {
				return record.getId();
			}
		}, tx, engine);
	}
	
	private void _buildTypeUniqueConstraintInsertQueries(final Record record, ValueProvider<Long> idRef, Transaction transaction, EngineImpl engine) {
		List<UniqueConstraint> constraints = engine.getConstraintRegistry().getUniqueConstraintsForType(record.getType());
		if (constraints != null) {
			for (final UniqueConstraint uniqueConstraint : constraints) {
				final QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
				final Insert insert = qc.insert();

				// insert values into uniqueConstraintTable
				final UniqueConstraintTable table = engine.getTableRegistry().getUniqueConstraintTableByConstraint(uniqueConstraint);
				insert.into(table.getTableName());

				List<AttributeColumn> columns = table.getColumns();
				UniqueConstraint constraint = table.getUniqueConstraint();
				List<Attribute> atts = constraint.getAttributes();
				for (final Attribute attribute : atts) {
					final AttributeMediator<Attribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
					if (mediator.isAttributePresent(record, attribute)) {

						AttributeColumn attributeColumn = null;
						for (AttributeColumn col : columns) {
							if (col.getAttribute() == attribute) {
								attributeColumn = col;
							}
						}
						if (attributeColumn == null) {
							throw new IllegalStateException("could not find column for attribute: " + attribute.getName());
						}
						AccessorImpl.appendValueToInsert(insert, attributeColumn, record, engine.getMediatorRegistry());
					}
				}

				for (AttributeColumn attributeColumn : table.getHolderColumns()) {
					final NamedAttributeHolderAttribute att = (NamedAttributeHolderAttribute) attributeColumn.getAttribute();
					if (att.getNamedAttributeHolder() == record.getType()) {
						final AttributeMediator<NamedAttributeHolderAttribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(att);
						int sqlType = mediator.columnSqlType(att);
						insert.value(attributeColumn.getColumnName(), new PreparedStatementValueProvider() {

							@Override
							public Long get() {
								return record.getId();
							}

							@Override
							public void set(int index, PreparedStatement ps) throws SQLException {
								ps.setLong(index, get());
							}
						}, sqlType);
						break;
					}
				}
				final Query q = qc.build(record.getContext());
				transaction.getQueryRunner().run(q);
			}
		}
	}
	
}
