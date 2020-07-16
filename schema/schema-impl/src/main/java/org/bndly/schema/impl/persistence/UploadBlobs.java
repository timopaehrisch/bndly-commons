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
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class UploadBlobs implements TransactionAppender {

	public static UploadBlobs INSTANCE = new UploadBlobs();
	
	@Override
	public void append(Transaction tx, final Record record, EngineImpl engine) {
		List<Query> blobQueries = _buildBlobInsertQueries(record, new ValueProvider<Long>() {
			@Override
			public Long get() {
				return record.getId();
			}
		}, engine);
		for (Query query : blobQueries) {
			tx.getQueryRunner().uploadBlob(query);
		}
	}
	
	private List<Query> _buildBlobInsertQueries(Record record, final ValueProvider<Long> idReference, final EngineImpl engine) {
		final List<Query> result = new ArrayList<>();
		final TypeTable table = engine.getTableRegistry().getTypeTableByType(record.getType());
		final Map<String, AttributeColumn> columnsByAttributeName = new HashMap<>();
		for (AttributeColumn attributeColumn : table.getColumns()) {
			columnsByAttributeName.put(attributeColumn.getAttribute().getName(), attributeColumn);
		}

		record.iteratePresentValues(new RecordAttributeIterator() {
			@Override
			public void handleAttribute(final Attribute attribute, final Record record) {
				if (BinaryAttribute.class.isInstance(attribute)) {
					QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
					Update update = qc.update();
					update.table(table.getTableName());
					AttributeColumn column = columnsByAttributeName.get(attribute.getName());
					final Object rawValue = record.getAttributeValue(attribute.getName());
					final AttributeMediator<Attribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
					int sqlType = mediator.columnSqlType(attribute);
					if (rawValue == null) {
						update.setNull(column.getColumnName(), sqlType);
					} else {
						update.set(column.getColumnName(), mediator.createValueProviderFor(record, attribute), sqlType);
					}
					update.where().expression().criteria().field(table.getPrimaryKeyColumn().getColumnName()).equal().value(idReference);
					result.add(qc.build(record.getContext()));
				}
			}
		});
		return result;
	}
	
}
