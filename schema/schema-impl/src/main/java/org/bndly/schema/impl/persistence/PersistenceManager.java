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
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.impl.IdTypeBinding;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PersistenceManager {

	private final EngineImpl engineImpl;
	private final AccessorImpl accessorImpl;
	private final Transaction transaction;
	private final Set<Record> handledRecords = new HashSet<>();
	private final Map<Record, Runnable> cycleBreakersPerRecord = new LinkedHashMap<>();

	public PersistenceManager(EngineImpl engineImpl, Transaction transaction) {
		this.engineImpl = engineImpl;
		this.accessorImpl = engineImpl.getAccessor();
		this.transaction = transaction;
	}

	public Transaction finalizeTransaction() {
		for (Runnable value : cycleBreakersPerRecord.values()) {
			value.run();
		}
		cycleBreakersPerRecord.clear();
		return transaction;
	}
	
	public boolean isScheduled(Record record) {
		return handledRecords.contains(record);
	}

	public PersistenceManager append(Record record) {
		// if the record is already handled, then do nothing
		if (handledRecords.contains(record)) {
			return this;
		}
		iterateGraphForPersistence(record);
		return this;
	}
	
	/**
	 * Iterates of the attributes of the provided record and creates persistence queries on the transaction. Cycles will be resolved by combining insert and update.
	 *
	 * @param record
	 */
	private boolean iterateGraphForPersistence(Record record) {
		if (record.isReference()) {
			// we just skip here and we do not need to handle a cycle
			return false;
		}
		if (handledRecords.contains(record)) {
			// we detected a cycle. we have to 
			return true;
		}
		handledRecords.add(record);
		
		boolean isInsert = record.getId() == null;
		
		final List<InverseAttribute> inverseAttributes = new ArrayList<>();
		
		// a) during the iteration we drill down the graph and schedule persistence for the referenced records.
		// b) if we encounter an inverse attribute, we collect it, because we have to persist the items after 
		// the current record is persisted. for example the shopping cart needs to be persisted before the 
		// shopping cart items, that refer to the shopping cart.
		record.iteratePresentValues(new RecordAttributeIterator() {
			@Override
			public void handleAttribute(Attribute attribute, final Record currentRecord) {
				if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					NamedAttributeHolderAttribute naha = NamedAttributeHolderAttribute.class.cast(attribute);
					if (currentRecord.isAttributePresent(naha.getName())) {
						Object value = currentRecord.getAttributeValue(naha.getName());
						if (Record.class.isInstance(value)) {
							Record referencedRecord = Record.class.cast(value);
							if (!referencedRecord.isReference()) {
								if(naha.getToOneAttribute() != null) {
									referencedRecord.setAttributeValue(naha.getToOneAttribute(), currentRecord);
								}
								
								boolean isCycle = iterateGraphForPersistence(referencedRecord);
								if (isCycle) {
									// we have to perform an update on the current record at the "end"
									// or in the second part of the transaction, that breaks the cycle
									if (!cycleBreakersPerRecord.containsKey(currentRecord)) {
										cycleBreakersPerRecord.put(currentRecord, new Runnable() {
											@Override
											public void run() {
												updateReferences(currentRecord);
											}
										});
									}
								}
							}
						}
					}
				} else if (InverseAttribute.class.isInstance(attribute)) {
					// these values can only be persisted after the record is persisted due to the inverse nature
					if (!attribute.isVirtual()) {
						inverseAttributes.add((InverseAttribute) attribute);
					}
				}
			}

		});
		if (isInsert) {
			insert(record);
		} else {
			update(record);
		}
		// the inverseAttributes need persistence after the record
		// this means we have to:
		// for each attribute load the list of existing values
		// add each existing value to a pool of values, that might be deleted, because they are no longer referenced
		// for the new list of values we schedule persistence for each item in the list and remove its counterpart from the pool
		// for the remaining items in the pool, we schedule deletion
		for (InverseAttribute inverseAttribute : inverseAttributes) {
			if (record.isAttributePresent(inverseAttribute.getName())) {
				// load all items, that already exist in a new context
				Iterator<Record> existingEntriesAsDefensiveCopy = loadEntriesOfInverseAttribute(inverseAttribute, record, accessorImpl.buildRecordContext());
				Map<IdTypeBinding, Record> existingRecordsThatCouldBeDeleted = new HashMap<>();
				if (existingEntriesAsDefensiveCopy != null) {
					while (existingEntriesAsDefensiveCopy.hasNext()) {
						Record existingEntryRecord = existingEntriesAsDefensiveCopy.next();
						existingRecordsThatCouldBeDeleted.put(new IdTypeBinding(existingEntryRecord), existingEntryRecord);
					}
				}
				// now check what we have as new values, that remain
				List<Record> value = (List<Record>) record.getAttributeValue(inverseAttribute.getName());
				if (value != null) {
					for (Record itemThatHasReference : value) {
						// if the record is not just a reference, it will need persistence
						if (!itemThatHasReference.isReference()) {
							// create a proxy around the record object, so we can deal with its values but use it as a reference
							final boolean isRef = record.isReference();
							if (!isRef) {
								// by marking the current record as a reference, we break the cycle of parent child relations of inverseattributes
								record.setIsReference(true);
							}
							Object shouldBeRecord = itemThatHasReference.getAttributeValue(inverseAttribute.getReferencedAttributeName());
							if (shouldBeRecord != record) {
								// we ensure, that the item has a reference on the record, that we are currently inspecting
								itemThatHasReference.setAttributeValue(inverseAttribute.getReferencedAttributeName(), record);
							}
							boolean itemHadPersistence = itemThatHasReference.getId() != null;
							if (itemHadPersistence) {
								// we do not need to delete item
								existingRecordsThatCouldBeDeleted.remove(new IdTypeBinding(itemThatHasReference));
							}
							// iterate over the item, but the parent is already persisted and this is no issue
							iterateGraphForPersistence(itemThatHasReference);
							if (!isRef) {
								record.setIsReference(false);
							}
						}
					}
				}
				// all missing records should be removed
				for (Map.Entry<IdTypeBinding, Record> entry : existingRecordsThatCouldBeDeleted.entrySet()) {
					Record unreferencedItem = entry.getValue();
					engineImpl.getAccessor().delete(unreferencedItem, transaction);
				}
			}
		}
		return false;
	}
	
	private Iterator<Record> loadEntriesOfInverseAttribute(InverseAttribute inverseAttribute, Record parent, RecordContext targetRecordContext) {
		if (parent.getId() == null) {
			return Collections.EMPTY_LIST.iterator();
		}
		if (targetRecordContext == null) {
			targetRecordContext = parent.getContext();
		}
		Iterator<Record> children = accessorImpl.query(
				"PICK " + inverseAttribute.getReferencedAttributeHolder().getName()
					+ " c IF c." + inverseAttribute.getReferencedAttributeName()
					+ ".id=? AND c." + inverseAttribute.getReferencedAttributeName()
					+ " TYPED ?", targetRecordContext,
				null, 
				parent.getId(), 
				parent.getType().getName()
		);
		return children;
	}

	private void insert(Record record) {
		// build a plain insert query
		accessorImpl.buildInsertQuery(record, transaction);
	}
	private void update(Record record) {
		// build a plain update query
		accessorImpl.buildUpdateQuery(record, transaction);
	}
	private void updateReferences(Record record) {
		// build an update query, that only updates the references
		TypeTableReferenceUpdate.INSTANCE.append(transaction, record, engineImpl);
		// WARNING: also update the unique constraint tables
		UniqueConstraintTableReferenceUpdate.INSTANCE.append(transaction, record, engineImpl);
	}
	
}
