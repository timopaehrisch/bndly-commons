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

import org.bndly.schema.api.QueryBasedRecordListInitializer;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.RecordList;
import org.bndly.schema.api.RecordValue;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordImpl implements Record {
	private static final Logger LOG = LoggerFactory.getLogger(RecordImpl.class);
	
	private final Accessor accessor; // this makes a record an active record. yikes.
	private final Map<String, RecordValue> values = new HashMap<>();
	private Map<String, Attribute> attributes;
	private Type type;
	private Long id;
	private boolean isReference;
	private boolean isDirty;
	private RecordContextEntry recordContextEntry;

	public RecordImpl(Accessor accessor) {
		if (accessor == null) {
			throw new IllegalArgumentException("can't create a record without accessor object");
		}
		this.accessor = accessor;
	}

	public final RecordContextEntry getRecordContextEntry() {
		return recordContextEntry;
	}

	public final void setRecordContextEntry(RecordContextEntry recordContextEntry) {
		if (recordContextEntry == null) {
			throw new IllegalArgumentException("can't create a record without recordContextEntry object");
		}
		if (this.recordContextEntry != null) {
			throw new IllegalArgumentException("can't set another recordContextEntry object");
		}
		this.recordContextEntry = recordContextEntry;
	}

	@Override
	public RecordContextImpl getContext() {
		if (this.recordContextEntry == null) {
			throw new IllegalStateException("created a record without setting the recordContextEntry");
		}
		return recordContextEntry.getRecordContext();
	}

	@Override
	public final void dropAttribute(String attributeName) {
		dropAttribute(attributeName, true);
	}

	private void dropAttribute(String attributeName, boolean removeValue) {
		Attribute att = assertAttributeIsKnown(attributeName);
		RecordValue val = values.get(att.getName());
		if (val != null) {
			// remove it
			Object realValue = val.getValue();
			if (Record.class.isInstance(realValue)) {
				RecordContextEntry entry = ((RecordImpl) realValue).getRecordContextEntry();
				List<RecordContext.RecordReference> refs = entry.getReferences();
				Iterator<RecordContext.RecordReference> iterator = refs.iterator();
				while (iterator.hasNext()) {
					RecordContext.RecordReference ref = iterator.next();
					if (ref.getReferencedBy() == this && ref.getReferencedAs() == att) {
						iterator.remove();
					}
				}
			} else if (RecordList.class.isInstance(realValue)) {
				RecordList rl = (RecordList) realValue;
				if (rl.didInitialize()) {
					Iterator<Record> iterator = rl.iterator();
					while (iterator.hasNext()) {
						Record item = iterator.next();
						if (RecordImpl.class.isInstance(item)) {
							RecordContextEntry entry = ((RecordImpl) item).getRecordContextEntry();
							List<RecordContext.RecordReference> refs = entry.getReferences();
						}
					}
				}
			}
			if (removeValue) {
				values.remove(att.getName());
			}
		}
	}

	@Override
	public final void dropAttributes() {
		Iterator<String> attributeNameIterator = values.keySet().iterator();
		while (attributeNameIterator.hasNext()) {
			String attributeName = attributeNameIterator.next();
			// beware of concurrent modifications
			dropAttribute(attributeName, false);
		}
		values.clear();
	}

	@Override
	public boolean isReference() {
		return isReference;
	}

	@Override
	public void setIsReference(boolean isReference) {
		this.isReference = isReference;
	}

	@Override
	public final boolean isDirty() {
		return isDirty;
	}

	public final void setIsDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
		List<Attribute> tmp = SchemaUtil.collectAttributes(type);
		this.attributes = new HashMap<>();
		for (Attribute attribute : tmp) {
			this.attributes.put(attribute.getName(), attribute);
		}
	}

	@Override
	public final <E> E setAttributeValue(String attributeName, E value) {
		Attribute attribute = assertAttributeIsKnown(attributeName);
		// first drop existing value, because we want a cleaned up record context
		dropAttribute(attributeName);
		if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
			NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attribute;
			if (value != null) {
				if (!Record.class.isInstance(value) && !Long.class.isInstance(value)) {
					throw new IllegalArgumentException("can not set a named attribute holder attribute to values other than Long or Record.");
				}
				if (Record.class.isInstance(value)) {
					value = (E) getContext().attach((Record) value);
					List<RecordContext.RecordReference> refs = ((RecordImpl) value).getRecordContextEntry().getReferences();
					refs.add(new RecordContextEntry.RecordReferenceImpl(this, attribute));
				}
			}
		} else if (InverseAttribute.class.isInstance(attribute)) {
			if (value != null) {
				if (!RecordList.class.isInstance(value)) {
					throw new SchemaException("inverse attributes should be set as RecordList!");
				}
				RecordList rl = (RecordList) value;
				if (rl.getContext() != getContext()) {
					throw new SchemaException("record list is from a different record context!");
				}
				Iterator<Record> iter = rl.iterator();
				NamedAttributeHolderAttribute attributeInChild = ((InverseAttribute) attribute).getReferencedAttribute();
				List<RecordContext.RecordReference> refs = getRecordContextEntry().getReferences();
				while (iter.hasNext()) {
					Record next = iter.next();
					refs.add(new RecordContextEntry.RecordReferenceImpl(next, attributeInChild));
				}
			}
		}
		values.put(attributeName, new RecordValueImpl(this, attribute, value));
		if (!attribute.isVirtual()) {
			setIsDirty(true);
		}
		return value;
	}

	@Override
	public boolean isAttributePresent(String attributeName) {
		assertAttributeIsKnown(attributeName);
		return values.containsKey(attributeName);
	}

	@Override
	public final <E> E getAttributeValue(String attributeName, Class<E> desiredType) {
		Attribute att = assertAttributeIsKnown(attributeName);
		RecordValue v = values.get(attributeName);
		final E returnValue;
		if (v == null) {
			// value is not defined
			if (InverseAttribute.class.isInstance(att)) {
				if (att.isVirtual()) {
					returnValue = null;
				} else {
					RecordList found = null;
					if (this.id != null) {
						final InverseAttribute ia = InverseAttribute.class.cast(att);
						if (LOG.isDebugEnabled()) {
							LOG.debug("loading inverse attribute: " + ia.getName() + " in " + type.getName() + " for record " + id + " in instance " + hashCode());
						}
						final String attName = ia.getReferencedAttributeName();
						final Record parent = this;
						QueryBasedRecordListInitializer initializer = new QueryBasedRecordListInitializer(
								accessor, 
								"PICK " + ia.getReferencedAttributeHolder().getName() + " x IF x." + attName + ".id=? AND x." + attName + " TYPED ?",
								parent.getContext(), 
								id, 
								type.getName()
						) {

							@Override
							public Record onIterated(Record iteratedRecord) {
								iteratedRecord.setAttributeValue(ia.getReferencedAttributeName(), parent);
								return super.onIterated(iteratedRecord);
							}

						};

						found = getContext().createList(initializer, parent, ia);
					}
					setAttributeValue(attributeName, found);
					returnValue = desiredType.cast(found);
				}
			} else {
				returnValue = null;
			}
		} else {
			Object raw = v.getValue();
			if (raw == null) {
				returnValue = null;
			} else {
				if (NamedAttributeHolderAttribute.class.isInstance(att)) {
					if (Record.class.isInstance(raw)) {
						Record rawRec = (Record) raw;
						Long rId = rawRec.getId();
						if (Long.class.isAssignableFrom(desiredType)) {
							if (rId == null) {
								LOG.debug("attribute {} in {} referenced a record with an id", attributeName, this);
								return null;
							}
							long transformedId = accessor.readIdAsNamedAttributeHolder(
									((NamedAttributeHolderAttribute) att).getNamedAttributeHolder(), rawRec.getType(), rId, getContext()
							);
							returnValue = desiredType.cast((Long) transformedId);
						} else if (Query.class.isAssignableFrom(desiredType)) {
							returnValue = (E) accessor.createIdAsNamedAttributeHolderQuery(
									((NamedAttributeHolderAttribute) att).getNamedAttributeHolder(), rawRec.getType(), rId, getContext()
							);
						} else {
							returnValue = (E) raw;
						}
					} else if (Long.class.isInstance(raw)) {
						if (Record.class.isAssignableFrom(desiredType)) {
							Record rec = accessor.readById(((NamedAttributeHolderAttribute) att).getNamedAttributeHolder().getName(), (Long) raw, getContext());
							setAttributeValue(attributeName, rec);
							returnValue = (E) rec;
						} else {
							returnValue = desiredType.cast(raw);
						}
					} else {
						returnValue = (E) raw;
					}
				} else {
					try {
						returnValue = desiredType.cast(raw);
					} catch (ClassCastException e) {
						throw e;
					}
				}
			}
		}
		return returnValue;
	}

	@Override
	public boolean isAttributeDefined(String attributeName) {
		Attribute attribute = null;
		if (attributes != null) {
			attribute = attributes.get(attributeName);
		}
		if (attribute == null) {
			return false;
		}
		return true;
	}

	private Attribute assertAttributeIsKnown(String attributeName) throws IllegalStateException {
		Attribute attribute = null;
		if (attributes != null) {
			attribute = attributes.get(attributeName);
		}
		if (attribute == null) {
			throw new IllegalStateException("unknown attribute " + attributeName + " for type " + type.getName());
		}
		return attribute;
	}

	@Override
	public Object getAttributeValue(String attributeName) {
		return getAttributeValue(attributeName, Object.class);
	}

	@Override
	public boolean isVirtualAttribute(String attributeName) {
		return assertAttributeIsKnown(attributeName).isVirtual();
	}

	@Override
	public Attribute getAttributeDefinition(String attributeName) {
		return getAttributeDefinition(attributeName, Attribute.class);
	}

	@Override
	public <E extends Attribute> E getAttributeDefinition(String attributeName, Class<E> definitionType) {
		Attribute att = assertAttributeIsKnown(attributeName);
		return (E) att;
	}

	@Override
	public void iterateValues(RecordAttributeIterator listener) {
		for (Attribute attribute : attributes.values()) {
			listener.handleAttribute(attribute, this);
		}
	}

	@Override
	public void iteratePresentValues(RecordAttributeIterator listener) {
		for (Attribute attribute : attributes.values()) {
			if (isAttributePresent(attribute.getName())) {
				listener.handleAttribute(attribute, this);
			}
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Record: ").append(type.getName()).append("[");
		if (id == null) {
			sb.append("hash-").append(hashCode());
		} else {
			sb.append(id);
		}
		sb.append("]");
		if (isReference) {
			sb.append("[REF]");
		}
		String stringValue = sb.toString();
		return stringValue;
	}

}
