package org.bndly.schema.api.services;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.api.DeletionStrategy;
import org.bndly.schema.api.LoadedAttributes;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.listener.QueryByExampleIteratorListener;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Type;
import java.util.Iterator;

public interface Accessor extends DeletionStrategy {

	RecordContext buildRecordContext();

	Record readById(String namedAttributeHolderName, long id, RecordContext recordContext);

	void buildInsertQuery(Record record, Transaction transaction);
	
	long insert(Record record);

	void buildInsertCascadedQuery(Record record, Transaction transaction);

	long insertCascaded(Record record);

	void buildUpdateQuery(Record record, Transaction transaction);
	
	/**
	 * Calling this method is equal to 
	 * {@link #buildUpdateQuery(org.bndly.schema.api.Record, org.bndly.schema.api.Transaction)},
	 * but the id of the record is not null checked. It is assumed, that the record is persisted in
	 * the same transaction as the provided one.
	 * @param record The record, for which an update should be scheduled
	 * @param transaction The transaction, that shall execute the update
	 */
	void buildUpdateQueryPostPersist(Record record, Transaction transaction);

	void update(Record record);
	
	void buildUpdateCascadedQuery(Record record, Transaction transaction);

	void updateCascaded(Record record);
	
	void buildDeleteQuery(Record record, Transaction transaction);

	Object createIdAsNamedAttributeHolderQuery(NamedAttributeHolder namedAttributeHolder, Type sourceType, long id, RecordContext recordContext);

	Object createIdAsNamedAttributeHolderQuery(NamedAttributeHolder namedAttributeHolder, Record record);

	long readIdAsNamedAttributeHolder(NamedAttributeHolder namedAttributeHolder, Type sourceType, long id, RecordContext recordContext);

	QueryByExample queryByExample(String namedAttributeHolderName, RecordContext recordContext);

	void iterate(String typeName, QueryByExampleIteratorListener listener, int batchSize, boolean eager, RecordContext recordContext);

	Iterator<Record> query(String nQuery, Object... queryArgs);

	Iterator<Record> query(String nQuery, RecordContext recordContext, LoadedAttributes loadedAttributes, Object... queryArgs);

	Long count(String nQuery, Object... queryArgs);
}
