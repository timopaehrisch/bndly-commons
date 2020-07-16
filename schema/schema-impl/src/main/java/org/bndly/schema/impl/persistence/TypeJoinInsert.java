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
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeJoinInsert implements TransactionAppender {

	public static TypeJoinInsert INSTANCE = new TypeJoinInsert();
	
	@Override
	public void append(Transaction tx, final Record record, EngineImpl engine) {
		_buildTypeJoinTableInsertQueries(record, new ValueProvider<Long>() {
			@Override
			public Long get() {
				return record.getId();
			}
		}, tx, engine);
	}
	
	private void _buildTypeJoinTableInsertQueries(final Record record, ValueProvider<Long> idRef, Transaction transaction, EngineImpl engine) {
		Type currentSuperType = record.getType();
		Type currentSubType = currentSuperType;
		ValueProvider<Long> currentIdReferenceOfSubType = idRef;
		while (currentSuperType != null) {

			if (currentSuperType.getSubTypes() != null && !currentSuperType.getSubTypes().isEmpty()) {
				// this can be the case if the type has sub types
				JoinTable joinTable = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(currentSuperType);
				currentIdReferenceOfSubType = insertRecordIdIntoJoinTable(joinTable, currentSubType, currentIdReferenceOfSubType, record.getContext(), transaction, engine);
			} else {
				// this can be the case if the type has no sub types
			}

			// what ever the case, insert values into the mixin join tables too
			List<Mixin> mixins = currentSuperType.getMixins();
			if (mixins != null && !mixins.isEmpty()) {
				for (Mixin mixin : mixins) {
					if (mixin.isVirtual()) {
						continue;
					}
					JoinTable joinTable = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(mixin);
					// the id of the mixin is not so relevant.
					// hence we drop it here
					insertRecordIdIntoJoinTable(joinTable, currentSuperType, currentIdReferenceOfSubType, record.getContext(), transaction, engine);
				}
			}

			currentSubType = currentSuperType;
			currentSuperType = currentSuperType.getSuperType();
		}
	}
	
	private ValueProvider<Long> insertRecordIdIntoJoinTable(
			JoinTable joinTable, 
			NamedAttributeHolder namedAttributeHolder, 
			final ValueProvider<Long> recordId, 
			RecordContext recordContext, 
			Transaction transaction,
			EngineImpl engine
	) {
		Query q = _buildInsertRecordIdIntoJoinTableQuery(joinTable, namedAttributeHolder, recordId, recordContext, engine);
		return transaction.getQueryRunner().number(q, joinTable.getPrimaryKeyColumn().getColumnName());
	}
	
	private Query _buildInsertRecordIdIntoJoinTableQuery(JoinTable joinTable, NamedAttributeHolder namedAttributeHolder, final ValueProvider<Long> recordId, RecordContext recordContext, EngineImpl engine) {
		List<AttributeColumn> joinTableColumns = joinTable.getColumns();
		AttributeColumn colToFill = null;
		NamedAttributeHolderAttribute attribute = null;
		for (AttributeColumn attributeColumn : joinTableColumns) {
			attribute = (NamedAttributeHolderAttribute) attributeColumn.getAttribute();
			if (attribute.getNamedAttributeHolder().getName().equals(namedAttributeHolder.getName())) {
				colToFill = attributeColumn;
				break;
			}
		}
		if (colToFill != null) {
			QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
			Insert insert = qc.insert();
			insert.into(joinTable.getTableName());
			final AttributeMediator<NamedAttributeHolderAttribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
			int sqlType = mediator.columnSqlType(attribute);
			insert.value(colToFill.getColumnName(), new PreparedStatementValueProvider() {

				@Override
				public Long get() {
					return recordId.get();
				}

				@Override
				public void set(int index, PreparedStatement ps) throws SQLException {
					ps.setLong(index, (long) get());
				}
			}, sqlType);
			final Query q = qc.build(recordContext);

			return q;
		} else {
			throw new SchemaException("could not find column to fill in join table");
		}
	}
}
