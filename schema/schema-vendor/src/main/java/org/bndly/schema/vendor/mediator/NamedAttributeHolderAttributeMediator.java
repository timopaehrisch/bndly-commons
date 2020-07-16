package org.bndly.schema.vendor.mediator;

/*-
 * #%L
 * Schema Vendor
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

import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.ValueUtil;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryValueProvider;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public abstract class NamedAttributeHolderAttributeMediator<E extends NamedAttributeHolderAttribute> extends AbstractAttributeMediator<E> {

	private final Accessor accessor;

	public NamedAttributeHolderAttributeMediator(Accessor accessor) {
		this.accessor = accessor;
	}

	@Override
	public String columnName(E attribute) {
		String n = super.columnName(attribute);
		n += "_ID";
		return n;
	}

	@Override
	public String columnType(E attribute) {
		return "BIGINT";
	}

	@Override
	public int columnSqlType(E attribute) {
		return Types.BIGINT;
	}
	
	@Override
	public Object getAttributeValue(Record record, E attribute) {
		Object v = record.getAttributeValue(attribute.getName());
		if (v == null) {
			return null;
		}

		Object id;
		if (Record.class.isInstance(v)) {
			Record r = (Record) v;
			Type recordType = r.getType();
			id = r.getId();
			if (id == null) {
				return _getSQLValueForNamedAttributeHolderAttribute(record, this, attribute);
			} else {
				if (recordType != attribute.getNamedAttributeHolder()) {
					// id can be a 'select' for that id
					id = accessor.createIdAsNamedAttributeHolderQuery(attribute.getNamedAttributeHolder(), recordType, r.getId(), r.getContext());
				}
			}
		} else if (Long.class.isInstance(v)) {
			id = Long.class.cast(v);
		} else {
			throw new IllegalStateException("can not figure out if type attribute is present.");
		}
		return id;
	}

	private Object _getSQLValueForNamedAttributeHolderAttribute(Record record, NamedAttributeHolderAttributeMediator<E> mediator, final Attribute attribute) throws SchemaException {
		Object value = record.getAttributeValue(attribute.getName());
		final Object _value = value;
		if (_value != null) {
			if (Long.class.isInstance(_value)) {
				return _value;
			} else if (Record.class.isInstance(_value)) {
				ObjectReference<Object> ref = new ObjectReference<Object>() {

					@Override
					public Object get() {
						Record r = (Record) _value;
						Type recordType = r.getType();
						if (recordType == ((NamedAttributeHolderAttribute) attribute).getNamedAttributeHolder()) {
							return r.getId();
						} else {
							// id can be a 'select' for that id
							return accessor.createIdAsNamedAttributeHolderQuery(((NamedAttributeHolderAttribute) attribute).getNamedAttributeHolder(), r);
						}
					}

				};
				return ref;
			} else if (Query.class.isInstance(_value)) {
				return _value;
			} else {
				throw new SchemaException("can not support " + _value.getClass() + " when building an insert query for a named attribute holder attribute");
			}
		}
		return null;
	}

	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, E attribute, RecordContext recordContext) throws SQLException {
		Long id = rs.getLong(columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			Record r = recordContext.get(attribute.getNamedAttributeHolder().getName(), id);
			if (r == null) {
				return id;
			}
			return r;
		}
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, E attribute, Record record) throws SQLException {
		// the ID might require some transformation to get the ID in the correct table.
		// this depends on the type of the attribute.

		Object id = getAttributeValue(record, attribute);
		if (id == null) {
			ps.setNull(index, Types.BIGINT);
		} else {
			if (Long.class.isInstance(id)) {
				ps.setLong(index, (long) id);
			} else {
				throw new IllegalStateException("unsupported type for id: " + id.getClass().getSimpleName());
			}
		}
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, E attribute, Object rawValue) throws SQLException {
		Number referencedId = (Number) rawValue;
		if (referencedId == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			ps.setLong(index, referencedId.longValue());
		}
	}

	@Override
	public Object filterValueForPreparedStatementArgument(Object value, E attribute) {
		value = ValueUtil.unwrapValue(value);
		return value;
	}

	@Override
	public ValueProvider createValueProviderFor(final Record record, final E attribute) {
		// check if the present value might require some identity conversion because it is a mixin or of a type that has subtypes
		boolean isIdentityConversionRequired = attributeValueRequiresIdentityConversion(record, attribute);
		if (isIdentityConversionRequired) {
			return new QueryValueProvider() {

				@Override
				public Query get() {
					Record valueRecord = record.getAttributeValue(attribute.getName(), Record.class);
					return (Query) accessor.createIdAsNamedAttributeHolderQuery(((NamedAttributeHolderAttribute) attribute).getNamedAttributeHolder(), valueRecord);
				}
			};
		} else {
			return new PreparedStatementValueProvider() {

				@Override
				public Long get() {
					return record.getAttributeValue(attribute.getName(), Long.class);
				}

				@Override
				public void set(int index, PreparedStatement ps) throws SQLException {
					setRawParameterInPreparedStatement(index, ps, attribute, get());
				}
			};
		}
	}

	protected final boolean attributeValueRequiresIdentityConversion(Record record, E attribute) {
		Object value = record.getAttributeValue(attribute.getName());
		if (value == null) {
			return false;
		}
		if (Long.class.isInstance(value)) {
			return false;
		} else if (Record.class.isInstance(value)) {
			Record valueRecord = (Record) value;
			Type valueRecordType = valueRecord.getType();
			if (attribute.getNamedAttributeHolder() != valueRecordType) {
				// if types do not match, we always need a conversion
				return true;
			} else {
				// even if types match, the type may have subtypes, so the used id has be taken from the join_table
				if (valueRecordType.getSubTypes() == null || valueRecordType.getSubTypes().isEmpty()) {
					return false;
				} else {
					return true;
				}
			}
		} else {
			throw new IllegalStateException("unsupported value for named attribtue holder attribute " + attribute.getName() + " in " + record.getType().getName());
		}
	}
}
