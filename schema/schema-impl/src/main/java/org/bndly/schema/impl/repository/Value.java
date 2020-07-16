package org.bndly.schema.impl.repository;

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
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.repository.EntityReference;
import org.bndly.schema.api.repository.IndexedItem;
import org.bndly.schema.api.repository.ModificationNotAllowedException;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.StringAttribute;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Value extends AbstractRepositoryItem implements IndexedItem {
	
	private final PropertyImpl property;
	private Object value;
	private long index;

	public Value(PropertyImpl property, RepositorySessionImpl repository, Record record, Engine engine, RecordContext recordContext) {
		super(repository, record, engine, recordContext);
		if (property == null) {
			throw new IllegalArgumentException("property is not allowed to be null");
		}
		this.property = property;
		if (property.getType() == Property.Type.STRING) {
			Boolean isText = getRecord().getAttributeValue("isText", Boolean.class);
			if (isText != null && isText) {
				value = getRecord().getAttributeValue("textValue", String.class);
			} else {
				value = getRecord().getAttributeValue("stringValue", String.class);
			}
		} else if (property.getType() == Property.Type.DECIMAL) {
			value = getRecord().getAttributeValue("decimalValue", BigDecimal.class);
		} else if (property.getType() == Property.Type.LONG) {
			value = getRecord().getAttributeValue("longValue", Long.class);
		} else if (property.getType() == Property.Type.DOUBLE) {
			value = getRecord().getAttributeValue("doubleValue", Double.class);
		} else if (property.getType() == Property.Type.DATE) {
			value = getRecord().getAttributeValue("dateValue", Date.class);
		} else if (property.getType() == Property.Type.BOOLEAN) {
			value = getRecord().getAttributeValue("booleanValue", Boolean.class);
		} else if (property.getType() == Property.Type.BINARY) {
			value = getRecord().getAttributeValue("binaryValue", InputStream.class);
		} else if (property.getType() == Property.Type.ENTITY) {
			value = getRecord().getAttributeValue("entityValue", Record.class);
		} else {
			throw new IllegalStateException("unsupported property type");
		}
		this.index = record.getAttributeValue("parentIndex", Long.class);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		if (property.getType() == Property.Type.STRING) {
			String stringValue = (String) value;
			Integer maxLength = getRecord().getAttributeDefinition("stringValue", StringAttribute.class).getLength();
			if (stringValue != null && maxLength != null && stringValue.length() > maxLength) {
				getRecord().setAttributeValue("stringValue", null);
				getRecord().setAttributeValue("textValue", stringValue);
				getRecord().setAttributeValue("isText", true);
			} else {
				getRecord().setAttributeValue("stringValue", stringValue);
				getRecord().setAttributeValue("textValue", null);
				getRecord().setAttributeValue("isText", false);
			}
		} else if (property.getType() == Property.Type.DECIMAL) {
			getRecord().setAttributeValue("decimalValue", value);
		} else if (property.getType() == Property.Type.LONG) {
			getRecord().setAttributeValue("longValue", value);
		} else if (property.getType() == Property.Type.DOUBLE) {
			getRecord().setAttributeValue("doubleValue", value);
		} else if (property.getType() == Property.Type.DATE) {
			getRecord().setAttributeValue("dateValue", value);
		} else if (property.getType() == Property.Type.BOOLEAN) {
			getRecord().setAttributeValue("booleanValue", value);
		} else if (property.getType() == Property.Type.BINARY) {
			getRecord().setAttributeValue("binaryValue", value);
		} else if (property.getType() == Property.Type.ENTITY) {
			if (EntityReference.class.isInstance(value)) {
				EntityReference er = (EntityReference) value;
				value = getRecordContext().create(er.getType(), er.getId());
			}
			if (Record.class.isInstance(value)) {
				final Record recordValue = (Record) value;
				if (recordValue.getContext() != getRecordContext()) {
					Long id = recordValue.getId();
					if (id == null) {
						throw new IllegalArgumentException("provided entity value is from a different recordContext and the ID is null.");
					} else {
						Record copy = getRecordContext().create(recordValue.getType().getName(), id);
						copy.setIsReference(true);
						value = copy;
					}
				}
			}
			getRecord().setAttributeValue("entityValue", value);
		} else {
			throw new IllegalStateException("unsupported property type");
		}
		this.value = value;
	}

	@Override
	public void remove() throws RepositoryException {
		createRemovable(property);
	}

	@Override
	protected void afterRemove() {
		property.dropValue(this);
	}

	@Override
	public long getIndex() {
		return index;
	}

	@Override
	public void moveToIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be moved in read only sessions");
		}
		property.moveValueToIndex(this, index);
	}

	public PropertyImpl getProperty() {
		return property;
	}

	public void setIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be moved in read only sessions");
		}
		if (index != this.index) {
			this.index = index;
			getRecord().setAttributeValue("parentIndex", index);
			if (!isTransient()) {
				createPersist(property);
			}
		}
	}
	
}
