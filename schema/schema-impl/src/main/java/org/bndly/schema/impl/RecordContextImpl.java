package org.bndly.schema.impl;

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
import org.bndly.schema.api.RecordList;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RecordContextImpl implements RecordContext {

	private final Engine engine;
	private final Map<RecordContextKey, RecordContextEntry> entries = new HashMap<>();
	private final Map<String, List<RecordContextEntry>> unpersistedEntriesByType = new HashMap<>();
	private final Map<String, Type> typesByName = new HashMap<>();

	public RecordContextImpl(Engine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine is not allowed to be null");
		}
		this.engine = engine;
	}

	@Override
	public Record get(Type type, long id) {
		return get(type.getName(), id);
	}

	@Override
	public Record get(String typeName, long id) {
		RecordContextKey key = new RecordContextKey(typeName, id);
		RecordContextEntry entry = entries.get(key);
		if (entry == null) {
			return null;
		}
		return entry.getRecord();
	}

	@Override
	public Record attach(Record record) {
		RecordContextEntry attached = attachForeign(record);
		Record attachedRecord = attached.getRecord();
		return attachedRecord;
	}

	private RecordContextEntry attachForeign(Record record) {
		if (record.getContext() == this) {
			return ((RecordImpl) record).getRecordContextEntry(); // short cut, because we already have an entry assigned to the record
		}
		RecordContextEntry entry = assertRecordOfEntryMatched(_attach(record, false));
		_attachAndCopyValuesOfRecord(record, entry.getRecord());
		return entry;
	}

	@Override
	public void persisted(Record record) {
		if (record.getContext() != this) {
			throw new IllegalStateException("persisted events should only be triggered for records that live in the handling record context");
		}
		List<RecordContextEntry> l = unpersistedEntriesByType.get(record.getType().getName());
		if (l == null) {
			throw new IllegalStateException(
					"persisted events should only be triggered for records that live in the handling record context. there was no list of unpersisted entries of the provided type."
			);
		}
		RecordContextEntry entry = null;
		Iterator<RecordContextEntry> iterator = l.iterator();
		while (iterator.hasNext()) {
			RecordContextEntry next = iterator.next();
			if (next.getRecord() == record) {
				iterator.remove();
				entry = next;
				break;
			}
		}
		if (entry == null) {
			throw new IllegalStateException("record entry is not allowed to be null, when record has been persisted");
		}
		Long id = record.getId();
		if (id == null) {
			throw new IllegalStateException("id is not allowed to be null, once a record has been persisted");
		}
		RecordContextEntry conflictingEntry = getEntryOf(record);
		if (conflictingEntry != null) {
			throw new IllegalStateException("there was already an entry in the record context with the id of the persisted record");
		}
		entries.put(new RecordContextKey(record), entry);
	}

	private void _attachAndCopyValuesOfRecord(Record sourceRecord, final Record targetRecord) {
		if (sourceRecord == targetRecord) {
			return; // do not run into an infinite loop
		}
		sourceRecord.iteratePresentValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record sourceRecord) {
				if (InverseAttribute.class.isInstance(attribute)) {
					throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
				} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					Object referredRecord = sourceRecord.getAttributeValue(attribute.getName());
					if (Record.class.isInstance(referredRecord)) {
						RecordContextEntry copy = attachForeign((Record) referredRecord);
						targetRecord.setAttributeValue(attribute.getName(), copy.getRecord());
					}
				} else {
					Object val = sourceRecord.getAttributeValue(attribute.getName());
					targetRecord.setAttributeValue(attribute.getName(), val);
				}
			}
		});
	}

	/**
	 * this method attaches a record to the current record context. the
	 * implementation does NOT copy any values, if the attached record is
	 * originating from another record context. use the 
	 * {@link RecordContextImpl#_attachAndCopyValuesOfRecord(org.bndly.schema.api.Record, org.bndly.schema.api.Record) method for copying values.
	 *
	 * @param record the record to attach
	 * @param isDirty true, if the record is not attached to any context yet.
	 * (speeds up internal implementation)
	 * @return a RecordContextEntry with a record that shares the identity of
	 * the input record. the instances of RecordContextEntry.getRecord() and the
	 * parameter record do not have to be the same!
	 */
	private RecordContextEntry _attach(Record record, boolean isDirty /* true, when the record is currently in construction */) {
		boolean shouldExistInThisContext = !isDirty && record.getContext() == this;
		boolean shouldBeReAttachedToThisContext = !isDirty && record.getContext() != this; // is attached to another context
		boolean shouldBeAttachedToThisContext = isDirty; // is dirty and should be attached right away
		RecordContextEntry entry;
		Long id = record.getId();
		if (shouldExistInThisContext) {
			// is already attached to this context!
			if (RecordImpl.class.isInstance(record)) {
				return ((RecordImpl) record).getRecordContextEntry();
			}
			entry = getEntryOf(record);
			if (entry == null) {
				throw new IllegalStateException("record seemed to be attached to current record context, but no context entry could be found.");
			}
			if (entry.getRecord() != record) {
				throw new IllegalStateException("record was attached to current record context, but the entry in the record context did lead to another instance.");
			}
		} else if (shouldBeReAttachedToThisContext) {
			if (id != null) {
				RecordContextKey key = new RecordContextKey(record);
				entry = entries.get(key);
				if (entry == null) {
					RecordImpl r = new RecordImpl(engine.getAccessor());
					r.setType(record.getType());
					r.setId(id);
					entry = new RecordContextEntry(r, this);
					r.setRecordContextEntry(entry);
					entries.put(key, entry);
				}
			} else {
				throw new IllegalStateException("can not attach unpersisted record, because there is no identifier to match it with existing unpersisted entries.");
			}
		} else if (shouldBeAttachedToThisContext) {
			if (id != null) {
				RecordContextKey key = new RecordContextKey(record);
				entry = entries.get(key);
				if (entry == null) {
					entry = new RecordContextEntry(record, this);
					entries.put(key, entry);
					((RecordImpl) record).setRecordContextEntry(entry);
				} else {
					throw new IllegalStateException("there was already a record with the provided key in the context.");
				}
			} else {
				List<RecordContextEntry> l = unpersistedEntriesByType.get(record.getType().getName());
				if (l == null) {
					entry = new RecordContextEntry(record, this);
					((RecordImpl) record).setRecordContextEntry(entry);
					l = new ArrayList<>();
					l.add(entry);
					unpersistedEntriesByType.put(record.getType().getName(), l);
				} else {
					// look for an existing entry
					entry = null;
					Iterator<RecordContextEntry> iterator = l.iterator();
					while (iterator.hasNext()) {
						RecordContextEntry next = iterator.next();
						if (next.getRecord() == record) {
							entry = next;
							break;
						}
					}
					if (entry == null) {
						entry = new RecordContextEntry(record, this);
						l.add(entry);
						((RecordImpl) record).setRecordContextEntry(entry);
					}
				}
				return entry;
			}
		} else {
			throw new IllegalStateException("unknown state for record to attach");
		}
		return entry;
	}

	@Override
	public void detach(Record record) {
		RecordContextKey key = new RecordContextKey(record);
		if (entries.containsKey(key)) {
			RecordContextEntry entry = entries.get(key);
			if (entry != null) {
				// TODO: update references to the entry's record
			}
			entries.remove(key);
		}
	}

	@Override
	public RecordImpl create(Type type) {
		return create(type.getName());
	}

	@Override
	public RecordImpl create(String typeName) {
		RecordImpl r = _lazyRecord(typeName, null);
		return r;
	}

	@Override
	public Record create(Type type, long id) {
		return create(type.getName(), id);
	}

	@Override
	public Record create(String typeName, long id) {
		RecordContextEntry entry = entries.get(new RecordContextKey(typeName, id));
		if (entry == null) {
			RecordImpl r = _lazyRecord(typeName, id);
			return r;
		} else {
			return entry.getRecord();
		}
	}

	private Type getTypeByName(String typeName) {
		TypeTable table = engine.getTableRegistry().getTypeTableByType(typeName);
		Type type;
		if (table == null) {
			if (typesByName.containsKey(typeName)) {
				type = typesByName.get(typeName);
			} else {
				Schema s = engine.getDeployer().getDeployedSchema();
				List<Type> types = s.getTypes();
				for (Type type1 : types) {
					typesByName.put(type1.getName(), type1);
				}
				type = typesByName.get(typeName);
				typesByName.put(typeName, type);
			}
			if (type == null) {
				throw new IllegalArgumentException("can't create new Record for type " + typeName);
			}
		} else {
			type = table.getType();
		}
		return type;
	}

	private RecordImpl _lazyRecord(String typeName, Long id) {
		Accessor accessor = engine.getAccessor();
		Type type = getTypeByName(typeName);
		if (type.isAbstract()) {
			if (id != null) {
				// we first have to look up the real type with the join id
				// we use a new record context, because we don't want to load unwanted records into the current one
				Record rec = accessor.readById(typeName, id, accessor.buildRecordContext());
				if (rec == null) {
					throw new SchemaException("a record should have been created for an abstract type name " + typeName + ", but no real record could be found.");
				}
				type = rec.getType();
				id = rec.getId();
			} else {
				throw new SchemaException(
					"a record should have been created for an abstract type name " + typeName + ", but no id was provided to perform a lookup for the real record."
				);
			}
		}
		RecordImpl r;
		if (type.isVirtual()) {
			r = new VirtualRecordImpl(accessor);
		} else {
			r = new RecordImpl(accessor);
		}
		r.setType(type);
		r.setId(id);
		assertRecordOfEntryMatched(_attach(r, true /* true, because the record is currently in construction */));
		return r;
	}

	private RecordContextEntry assertRecordOfEntryMatched(RecordContextEntry entry) {
		Record rec = entry.getRecord();
		if (!RecordImpl.class.isInstance(rec)) {
			throw new SchemaException("can not assert record of entry is currently correctly attached to the record context");
		}
		RecordContextEntry e = ((RecordImpl) rec).getRecordContextEntry();
		if (e != entry) {
			throw new SchemaException("record of entry has not the entry that was inspected.");
		}
		return entry;
	}

	public String dumpStats() {
		StringBuffer sb = new StringBuffer();
		long unpersistedEntriesSize = unpersistedEntriesSize();
		final long size = size();
		sb.append("total entries: ").append(size).append("\n");
		final long persistedEntriesSize = persistedEntriesSize();
		sb.append("persisted entries: ").append(persistedEntriesSize).append("\n");
		if (size > 0) {
			for (Map.Entry<RecordContextKey, RecordContextEntry> entrySet : entries.entrySet()) {
				RecordContextKey key = entrySet.getKey();
				RecordContextEntry value = entrySet.getValue();
				sb.append("\treferences to ").append(value.getRecord().toString()).append(": ").append(value.referenceCount()).append("\n");
				List<RecordReference> refs = value.getReferences();
				for (RecordReference ref : refs) {
					sb.append("\t\treferenced as ").append(ref.getReferencedAs().getName()).append(" by ").append(ref.getReferencedBy().toString()).append("\n");
				}
			}
		}
		sb.append("unpersisted entries: ").append(unpersistedEntriesSize).append("\n");
		if (unpersistedEntriesSize > 0) {
			for (Map.Entry<String, List<RecordContextEntry>> entrySet : unpersistedEntriesByType.entrySet()) {
				String key = entrySet.getKey();
				List<RecordContextEntry> value = entrySet.getValue();
				if (value != null && !value.isEmpty()) {
					sb.append("\tunpersisted entries of ").append(key).append(": ").append(value.size()).append("\n");
					for (RecordContextEntry value1 : value) {
						sb.append("\tunpersisted entry ").append(value1.getRecord().toString()).append("\n");
					}
				}
			}
		}

		return sb.toString();
	}

	private RecordContextEntry getEntryOf(Record record) {
		Long id = record.getId();
		Type type = record.getType();
		String typeName = type.getName();
		final RecordContextEntry entry;
		if (id == null) {
			List<RecordContextEntry> unpersistedEntriesOfType = unpersistedEntriesByType.get(typeName);
			if (unpersistedEntriesOfType == null) {
				entry = null;
			} else {
				for (RecordContextEntry unpersistedEntry : unpersistedEntriesOfType) {
					if (unpersistedEntry.getRecord() == record) {
						return unpersistedEntry;
					}
				}
				entry = null;
			}
		} else {
			entry = entries.get(new RecordContextKey(record));
		}
		return entry;
	}
	
	@Override
	public boolean isAttached(Record record) {
		RecordContextEntry entry = getEntryOf(record);
		return entry != null;
	}

	@Override
	public Iterator<RecordReference> listReferencesToRecord(Record record) {
		RecordContextEntry entry = getEntryOf(record);
		if (entry == null) {
			return Collections.EMPTY_LIST.iterator();
		} else {
			List<RecordReference> refs = entry.getReferences();
			return refs.iterator();
		}
	}
	
	@Override
	public long unpersistedEntriesSize() {
		long unpersistedEntriesTotal = 0;
		for (Map.Entry<String, List<RecordContextEntry>> entrySet : unpersistedEntriesByType.entrySet()) {
			List<RecordContextEntry> value = entrySet.getValue();
			if (value != null) {
				unpersistedEntriesTotal += value.size();
			}
		}
		return unpersistedEntriesTotal;
	}
	
	@Override
	public long persistedEntriesSize() {
		return entries.size();
	}

	@Override
	public long size() {
		return persistedEntriesSize() + unpersistedEntriesSize();
	}

	@Override
	public Iterator<Record> listPersistedRecordsOfType(String typeName) {
		Type type = getTypeByName(typeName);
		return listPersistedRecordsOfType(type);
	}

	@Override
	public Iterator<Record> listPersistedRecordsOfType(Type type) {
		List<Record> result = null;
		Iterator<RecordContextEntry> iterator = entries.values().iterator();
		while (iterator.hasNext()) {
			RecordContextEntry next = iterator.next();
			if (next.getRecord().getType() == type) {
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(next.getRecord());
			}
		}
		return result == null ? Collections.EMPTY_LIST.iterator() : result.iterator();
	}

	@Override
	public Iterator<Record> listPersistedRecords() {
		Schema ds = engine.getDeployer().getDeployedSchema();
		List<Type> types = ds.getTypes();
		if (types == null) {
			return Collections.EMPTY_LIST.iterator();
		}
		final Iterator<Type> typeIterator = types.iterator();
		return new Iterator<Record>() {

			private Iterator<Record> typeSpecificIterator;

			@Override
			public boolean hasNext() {
				if (typeSpecificIterator != null) {
					if (!typeSpecificIterator.hasNext()) {
						typeSpecificIterator = null;
					}
				}
				if (typeSpecificIterator == null) {
					while (typeIterator.hasNext()) {
						Type next = typeIterator.next();
						typeSpecificIterator = listPersistedRecordsOfType(next);
						if (typeSpecificIterator.hasNext()) {
							return true;
						}
					}
					return false;
				} else {
					return true;
				}
			}

			@Override
			public Record next() {
				return typeSpecificIterator.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported.");
			}
			
		};
	}
	
	@Override
	public RecordList createList(Record owner, String inverseAttributeName) {
		return createList(owner, owner.getAttributeDefinition(inverseAttributeName, InverseAttribute.class));
	}

	@Override
	public RecordList createList(Record owner, InverseAttribute inverseAttribute) {
		return createList(null, owner, inverseAttribute);
	}

	@Override
	public RecordList createList(final RecordListInitializer initializer, Record owner, String inverseAttributeName) {
		return createList(initializer, owner, owner.getAttributeDefinition(inverseAttributeName, InverseAttribute.class));
	}
	
	@Override
	public RecordList createList(final RecordListInitializer initializer, Record owner, InverseAttribute inverseAttribute) {
		final RecordList list = new RecordList(owner, inverseAttribute, new RecordList.NoOpListener() {

			@Override
			public void onItemRemoved(Record record) {
				RecordContextEntry entry = getEntryOf(record);
				// remove reference from child to owner
				entry = entry;
			}

			@Override
			public Record beforeItemAdded(final Record record) {
				RecordContextEntry entry = getEntryOf(record);
				if (entry == null) {
					Record attachedRecord = attach(record);
					return attachedRecord;
				}
				return entry.getRecord();
			}
			
		}) {

			@Override
			protected List<Record> initializeOriginal() {
				List<Record> l = super.initializeOriginal();
				if (initializer != null) {
					Iterator<Record> iter = initializer.initialize();
					while (iter.hasNext()) {
						Record next = iter.next();
						l.add(next);
					}
				}
				return l;
			}
			
		};
		return list;
	}

}
