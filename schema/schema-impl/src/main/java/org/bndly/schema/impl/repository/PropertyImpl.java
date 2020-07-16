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

import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.repository.ModificationNotAllowedException;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.services.Engine;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PropertyImpl extends AbstractRepositoryItem implements Property, IndexManager.IndexContext {
	private final Property.Type type;
	private final String name;
	private final NodeImpl owner;
	private final boolean multiValued;
	private final IndexManager valueIndexManager;
	
	private long index;
	private boolean didLoadValues;
	private List<Value> values;
	private Value value;

	public PropertyImpl(Type type, String name, NodeImpl owner, boolean multiValued, RepositorySessionImpl repository, Record record, Engine engine, RecordContext ctx) {
		super(repository, record, engine, ctx);
		if (type == null) {
			throw new IllegalArgumentException("type is not allowed to be null");
		}
		this.type = type;
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		this.name = name;
		if (owner == null) {
			throw new IllegalArgumentException("owner is not allowed to be null");
		}
		this.owner = owner;
		this.multiValued = multiValued;
		this.valueIndexManager = new IndexManager(this);
		this.index = record.getAttributeValue("parentIndex", Long.class);
	}

	@Override
	public Property.Type getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public NodeImpl getNode() {
		return owner;
	}

	@Override
	public boolean isMultiValued() {
		return multiValued;
	}

	@Override
	public String getString() {
		loadValues();
		if (getType() != Property.Type.STRING) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (String) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (String) value.getValue();
		}
	}

	@Override
	public String[] getStrings() {
		loadValues();
		if (getType() != Property.Type.STRING) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			String[] strings = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (String) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new String[]{(String) value.getValue()};
		}
	}

	@Override
	public Date getDate() {
		loadValues();
		if (getType() != Property.Type.DATE) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (Date) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (Date) value.getValue();
		}
	}

	@Override
	public Date[] getDates() {
		loadValues();
		if (getType() != Property.Type.DATE) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			Date[] strings = new Date[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (Date) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new Date[]{(Date) value.getValue()};
		}
	}

	@Override
	public BigDecimal getDecimal() {
		loadValues();
		if (getType() != Property.Type.DECIMAL) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (BigDecimal) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (BigDecimal) value.getValue();
		}
	}

	@Override
	public BigDecimal[] getDecimals() {
		loadValues();
		if (getType() != Property.Type.DECIMAL) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			BigDecimal[] strings = new BigDecimal[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (BigDecimal) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new BigDecimal[]{(BigDecimal) value.getValue()};
		}
	}

	@Override
	public Long getLong() {
		loadValues();
		if (getType() != Property.Type.LONG) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (Long) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (Long) value.getValue();
		}
	}

	@Override
	public Long[] getLongs() {
		loadValues();
		if (getType() != Property.Type.LONG) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			Long[] longs = new Long[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				longs[i] = (Long) get.getValue();
			}
			return longs;
		} else {
			if (value == null) {
				return null;
			}
			return new Long[]{(Long) value.getValue()};
		}
	}

	@Override
	public Double getDouble() {
		loadValues();
		if (getType() != Property.Type.DOUBLE) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (Double) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (Double) value.getValue();
		}
	}

	@Override
	public Double[] getDoubles() {
		loadValues();
		if (getType() != Property.Type.DOUBLE) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			Double[] doubles = new Double[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				doubles[i] = (Double) get.getValue();
			}
			return doubles;
		} else {
			if (value == null) {
				return null;
			}
			return new Double[]{(Double) value.getValue()};
		}
	}

	@Override
	public Boolean getBoolean() {
		loadValues();
		if (getType() != Property.Type.BOOLEAN) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (Boolean) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (Boolean) value.getValue();
		}
	}

	@Override
	public Boolean[] getBooleans() {
		loadValues();
		if (getType() != Property.Type.BOOLEAN) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			Boolean[] strings = new Boolean[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (Boolean) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new Boolean[]{(Boolean) value.getValue()};
		}
	}

	@Override
	public InputStream getBinary() {
		loadValues();
		if (getType() != Property.Type.BINARY) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (InputStream) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (InputStream) value.getValue();
		}
	}

	@Override
	public InputStream[] getBinaries() {
		loadValues();
		if (getType() != Property.Type.BINARY) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			InputStream[] strings = new InputStream[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (InputStream) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new InputStream[]{(InputStream) value.getValue()};
		}
	}

	@Override
	public Record getEntity() {
		loadValues();
		if (getType() != Property.Type.ENTITY) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			return (Record) values.get(0).getValue();
		} else {
			if (value == null) {
				return null;
			}
			return (Record) value.getValue();
		}
	}

	@Override
	public Record[] getEntities() {
		loadValues();
		if (getType() != Property.Type.ENTITY) {
			return null;
		}
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				return null;
			}
			Record[] strings = new Record[values.size()];
			for (int i = 0; i < values.size(); i++) {
				Value get = values.get(i);
				strings[i] = (Record) get.getValue();
			}
			return strings;
		} else {
			if (value == null) {
				return null;
			}
			return new Record[]{(Record) value.getValue()};
		}
	}

	private void loadValues() {
		if (didLoadValues) {
			return;
		}
		if (getRecord().getId() == null) {
			return;
		}
		final Iterator<Record> valuesIter;
		if (isMultiValued()) {
			valuesIter = getAccessor().query("PICK Value v IF v.property.id=? ORDERBY v.parentIndex", getRecord().getId());
		} else {
			valuesIter = getAccessor().query("PICK Value v IF v.property.id=? LIMIT ?", getRecord().getId(), 1);
		}
		didLoadValues = true;
		if (isMultiValued()) {
			values = new ArrayList<>();
			while (valuesIter.hasNext()) {
				Record valueRecord = valuesIter.next();
				values.add(new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext()));
			}
		} else {
			if (valuesIter.hasNext()) {
				Record valueRecord = valuesIter.next();
				value = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());;
			}
		}
	}

	@Override
	public void setValue(Object rawValue) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be set in read only sessions");
		}
		loadValues();
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				Record valueRecord = getRecordContext().create("Value");
				valueRecord.setAttributeValue("property", getRecord());
				valueRecord.setAttributeValue("parentIndex", 0L);
				value = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
				value.setValue(rawValue);
				value.createPersist(this);
				getRepositoryListenersLock().readLock().lock();
				try {
					for (RepositoryListener repositoryListener : getRepositoryListeners()) {
						repositoryListener.onPropertyChanged(this);
					}
				} finally {
					getRepositoryListenersLock().readLock().unlock();
				}
			} else {
				Value tmpValue = values.get(0);
				tmpValue.setValue(rawValue);
				tmpValue.createPersist(this);
				Iterator<Value> iter = values.iterator();
				while (iter.hasNext()) {
					Value next = iter.next();
					if (next != tmpValue) {
						next.createRemovable(this);
						iter.remove();
					}
				}
				getRepositoryListenersLock().readLock().lock();
				try {
					for (RepositoryListener repositoryListener : getRepositoryListeners()) {
						repositoryListener.onPropertyChanged(this);
					}
				} finally {
					getRepositoryListenersLock().readLock().unlock();
				}
			}
		} else {
			if (value == null) {
				Record valueRecord = getRecordContext().create("Value");
				valueRecord.setAttributeValue("property", getRecord());
				valueRecord.setAttributeValue("parentIndex", 0L);
				value = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
			}
			value.setValue(rawValue);
			value.createPersist(this);
			getRepositoryListenersLock().readLock().lock();
			try {
				for (RepositoryListener repositoryListener : getRepositoryListeners()) {
					repositoryListener.onPropertyChanged(this);
				}
			} finally {
				getRepositoryListenersLock().readLock().unlock();
			}
		}
	}
	
	
	@Override
	public void setValue(int index, Object rawValue) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be set in read only sessions");
		}
		if (!isMultiValued()) {
			throw new ModificationNotAllowedException("setValue with index can only be used on multivalued properties");
		}
		loadValues();
		if (values == null || values.isEmpty()) {
			values = new ArrayList<>(index + 1);
		} else if (values.size() <= index) {
			List<Value> copy = new ArrayList<>(index + 1);
			copy.addAll(values);
			values = copy;
		}
		Value valueItem = values.get(index);
		if (valueItem == null) {
			Record valueRecord = getRecordContext().create("Value");
			valueRecord.setAttributeValue("property", getRecord());
			valueRecord.setAttributeValue("parentIndex", (long)index);
			valueItem = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
			valueItem.setValue(rawValue);
		} else {
			valueItem.setValue(rawValue);
		}
		valueItem.createPersist(this);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onPropertyChanged(this);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
	}

	@Override
	public void setValues(Object... rawValues) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be set in read only sessions");
		}
		loadValues();
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				values = new ArrayList<>(rawValues.length);
				for (int i = 0; i < rawValues.length; i++) {
					Object rawValue = rawValues[i];
					Record valueRecord = getRecordContext().create("Value");
					valueRecord.setAttributeValue("property", getRecord());
					valueRecord.setAttributeValue("parentIndex", (long)i);
					Value v = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
					v.setValue(rawValue);
					v.createPersist(this);
					values.add(v);
				}
				getRepositoryListenersLock().readLock().lock();
				try {
					for (RepositoryListener repositoryListener : getRepositoryListeners()) {
						repositoryListener.onPropertyChanged(this);
					}
				} finally {
					getRepositoryListenersLock().readLock().unlock();
				}
			} else {
				int i = 0;
				Iterator<Value> it = values.iterator();
				while (it.hasNext()) {
					Value next = it.next();
					if (rawValues.length > i) {
						next.setValue(rawValues[i]);
						next.createPersist(this);
					} else {
						it.remove();
						next.createRemovable(this);
					}
					i++;
				}
				if (i < rawValues.length) {
					for (int j = i; j < rawValues.length; j++) {
						Record valueRecord = getRecordContext().create("Value");
						valueRecord.setAttributeValue("property", getRecord());
						valueRecord.setAttributeValue("parentIndex", valueIndexManager.pullNextChildIndex());
						Value v = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
						v.setValue(rawValues[j]);
						v.createPersist(this);
						values.add(v);
					}
				}
				getRepositoryListenersLock().readLock().lock();
				try {
					for (RepositoryListener repositoryListener : getRepositoryListeners()) {
						repositoryListener.onPropertyChanged(this);
					}
				} finally {
					getRepositoryListenersLock().readLock().unlock();
				}
			}
		} else {
			if (rawValues.length > 1) {
				throw new IllegalArgumentException("setting more than one value for a single valued property");
			} else {
				setValue(rawValues[0]);
			}
		}
	}

	@Override
	public void addValue(Object rawValue) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be added in read only sessions");
		}
		loadValues();
		if (isMultiValued()) {
			if (values == null || values.isEmpty()) {
				values = new ArrayList<>();
			}
			Record valueRecord = getRecordContext().create("Value");
			valueRecord.setAttributeValue("property", getRecord());
			valueRecord.setAttributeValue("parentIndex", valueIndexManager.pullNextChildIndex());
			Value v = new Value(this, super.getRepositorySession(), valueRecord, getEngine(), getRecordContext());
			v.setValue(rawValue);
			v.createPersist(this);
			values.add(v);
			getRepositoryListenersLock().readLock().lock();
			try {
				for (RepositoryListener repositoryListener : getRepositoryListeners()) {
					repositoryListener.onPropertyChanged(this);
				}
			} finally {
				getRepositoryListenersLock().readLock().unlock();
			}
		} else {
			setValue(rawValue);
		}
	}

	@Override
	protected void afterRemove() {
		owner.dropProperty(this);
	}

	@Override
	public void remove() throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("properties can not be removed in read only sessions");
		}
		createRemovable(owner);
		getRepositoryListenersLock().readLock().lock();
		try {
			for (RepositoryListener repositoryListener : getRepositoryListeners()) {
				repositoryListener.onPropertyRemoved(this);
			}
		} finally {
			getRepositoryListenersLock().readLock().unlock();
		}
	}

	@Override
	public Object getValue() {
		if (getType() == Property.Type.STRING) {
			return getString();
		} else if (getType() == Property.Type.DECIMAL) {
			return getDecimal();
		} else if (getType() == Property.Type.LONG) {
			return getLong();
		} else if (getType() == Property.Type.DOUBLE) {
			return getDouble();
		} else if (getType() == Property.Type.DATE) {
			return getDate();
		} else if (getType() == Property.Type.BOOLEAN) {
			return getBoolean();
		} else if (getType() == Property.Type.BINARY) {
			return getBinary();
		} else if (getType() == Property.Type.ENTITY) {
			return getEntity();
		} else {
			throw new IllegalStateException("unsupported property type");
		}
	}

	@Override
	public Object[] getValues() {
		if (getType() == Property.Type.STRING) {
			return getStrings();
		} else if (getType() == Property.Type.DECIMAL) {
			return getDecimals();
		} else if (getType() == Property.Type.LONG) {
			return getLongs();
		} else if (getType() == Property.Type.DOUBLE) {
			return getDoubles();
		} else if (getType() == Property.Type.DATE) {
			return getDates();
		} else if (getType() == Property.Type.BOOLEAN) {
			return getBooleans();
		} else if (getType() == Property.Type.BINARY) {
			return getBinaries();
		} else if (getType() == Property.Type.ENTITY) {
			return getEntities();
		} else {
			throw new IllegalStateException("unsupported property type");
		}
	}

	@Override
	public long countChildren() throws RepositoryException {
		if (isTransient() || didLoadValues) {
			if (isMultiValued()) {
				return values == null ? 0 : values.size();
			} else {
				return value == null ? 0 : 1;
			}
		} else {
			Long count = getAccessor().count("COUNT Value v IF v.property.id=?", getRecord().getId());
			if (count == null) {
				return 0;
			}
			return count;
		}
	}

	public void dropValue(Value value) {
		if (isMultiValued()) {
			if (values != null) {
				Iterator<Value> iterator = values.iterator();
				while (iterator.hasNext()) {
					Value next = iterator.next();
					if (next == value) {
						iterator.remove();
					}
				}
			}
		} else {
			this.value = null;
		}
	}

	public void moveValueToIndex(Value value, long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be moved in read only sessions");
		}
		if (value.getProperty() != this) {
			throw new RepositoryException("provided value is not owned by this property");
		}
		if (value.getIndex() == index) {
			return;
		}
		if (isMultiValued()) {
			if (!isTransient()) {
				loadValues();
			}
			if (values != null) {
				Iterator<Value> iterator = values.iterator();
				if (index > values.size() - 1) {
					index = values.size();
				}
				long lowerBorder;
				long upperBorder;
				boolean moveToRight = value.getIndex() < index;
				if (moveToRight) {
					lowerBorder = value.getIndex();
					upperBorder = index;
				} else {
					lowerBorder = index;
					upperBorder = value.getIndex();
				}
				long i = 0;
				while (iterator.hasNext()) {
					Value next = iterator.next();
					if (i >= lowerBorder && i <= upperBorder) {
						if (moveToRight) {
							if (next == value) {
								next.setIndex(index);
								iterator.remove();
							} else {
								next.setIndex(next.getIndex() - 1);
							}
						} else {
							if (next == value) {
								next.setIndex(index);
								iterator.remove();
							} else {
								next.setIndex(next.getIndex() + 1);
							}
						}
					}
					i++;
				}
				values.add((int) index, value);
			}
		} else {
			value.setIndex(0);
		}
	}
	
	public List<Value> getValuesInternal() {
		return values;
	}

	@Override
	public long getIndex() {
		return this.index;
	}

	@Override
	public void moveToIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("values can not be moved in read only sessions");
		}
		owner.movePropertyToIndex(this, index);
	}
	
	public void setIndex(long index) throws RepositoryException {
		if (isReadOnly()) {
			throw new ModificationNotAllowedException("properties can not be moved in read only sessions");
		}
		if (index != this.index) {
			this.index = index;
			getRecord().setAttributeValue("parentIndex", index);
			if (!isTransient()) {
				createPersist(owner);
			}
		}
	}
	
}
