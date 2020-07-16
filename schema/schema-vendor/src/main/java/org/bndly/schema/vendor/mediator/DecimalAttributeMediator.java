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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.model.DecimalAttribute;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class DecimalAttributeMediator extends AbstractAttributeMediator<DecimalAttribute> {

	private static enum Kind {

		LONG, DOUBLE, DECIMAL
	}

	private Kind getKind(DecimalAttribute attribute) {
		Integer length = attribute.getLength();
		Integer dp = attribute.getDecimalPlaces();
		if (dp == null) {
			dp = 0;
		}
		if (length == null) {
			if (dp == 0) {
				return Kind.LONG;
			} else {
				return Kind.DOUBLE;
			}
		} else {
			return Kind.DECIMAL;
		}
	}
	
	@Override
	public String columnType(DecimalAttribute attribute) {
		Kind kind = getKind(attribute);
		if (kind.equals(Kind.LONG)) {
			return getLongColumnType(attribute);
		} else if (kind.equals(Kind.DOUBLE)) {
			return getDoubleColumnType(attribute);
		} else if (kind.equals(Kind.DECIMAL)) {
			return getDecimalColumnType(attribute);
		} else {
			throw new IllegalStateException("unsupported kind: " + kind);
		}
	}

	@Override
	public int columnSqlType(DecimalAttribute attribute) {
		Kind kind = getKind(attribute);
		if (kind.equals(Kind.LONG)) {
			return getLongColumnSqlType(attribute);
		} else if (kind.equals(Kind.DOUBLE)) {
			return getDoubleColumnSqlType(attribute);
		} else if (kind.equals(Kind.DECIMAL)) {
			return getDecimalColumnSqlType(attribute);
		} else {
			throw new IllegalStateException("unsupported kind: " + kind);
		}
	}

	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, DecimalAttribute attribute, RecordContext recordContext) throws SQLException {
		Integer length = attribute.getLength();
		Integer dp = attribute.getDecimalPlaces();
		if (dp == null) {
			dp = 0;
		}
		if (length == null) {
			if (dp == 0) {
				long l = rs.getLong(columnName);
				if (!rs.wasNull()) {
					return l;
				}
			} else {
				double d = rs.getDouble(columnName);
				if (!rs.wasNull()) {
					return d;
				}
			}
		} else {
			BigDecimal bd = rs.getBigDecimal(columnName);
			if (!rs.wasNull()) {
				return bd;
			}
		}
		return null;
	}

	public static final Class getJavaNativeType(DecimalAttribute attribute) {
		Integer length = attribute.getLength();
		Integer dp = attribute.getDecimalPlaces();
		if (dp == null) {
			dp = 0;
		}
		if (length == null) {
			if (dp == 0) {
				return Long.class;
			} else {
				return Double.class;
			}
		} else {
			return BigDecimal.class;
		}
	}

	@Override
	public Number getAttributeValue(Record record, DecimalAttribute attribute) {
		Integer length = attribute.getLength();
		Integer dp = attribute.getDecimalPlaces();
		if (dp == null) {
			dp = 0;
		}
		Object rawValue = record.getAttributeValue(attribute.getName());
		Class<? extends Number> requiredType;
		if (length == null) {
			if (dp == 0) {
				requiredType = Long.class;
				if (BigDecimal.class.isInstance(rawValue)) {
					return BigDecimal.class.cast(rawValue).longValue();
				}
			} else {
				requiredType = Double.class;
				if (BigDecimal.class.isInstance(rawValue)) {
					return BigDecimal.class.cast(rawValue).doubleValue();
				}
			}
		} else {
			requiredType = BigDecimal.class;
		}
		return requiredType.cast(rawValue);
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, DecimalAttribute attribute, Record record) throws SQLException {
		Kind kind = getKind(attribute);
		Object v = getAttributeValue(record, attribute);
		int sqlType = columnSqlType(attribute);

		if (v == null) {
			ps.setNull(index, sqlType);
		} else {
			if (Kind.LONG.equals(kind)) {
				setLongValue(index, ps, (long) v);
			} else if (Kind.DOUBLE.equals(kind)) {
				setDoubleValue(index, ps, (double) v);
			} else if (Kind.DECIMAL.equals(kind)) {
				setDecimalValue(index, ps, (BigDecimal) v);
			} else {
				throw new IllegalStateException("unsupported kind: " + kind);
			}
		}
	}

	protected String getLongColumnType(DecimalAttribute attribute) {
		return "BIGINT";
	}

	protected String getDoubleColumnType(DecimalAttribute attribute) {
		return "DOUBLE";
	}

	protected String getDecimalColumnType(DecimalAttribute attribute) {
		Integer length = attribute.getLength();
		Integer dp = attribute.getDecimalPlaces();
		if (dp == null) {
			dp = 0;
		}
		String columnType = "DECIMAL";
		columnType += "(";
		columnType += length;
		columnType += ", ";
		columnType += dp;
		columnType += ")";
		return columnType;
	}

	protected int getLongColumnSqlType(DecimalAttribute attribute) {
		return Types.BIGINT;
	}

	protected int getDoubleColumnSqlType(DecimalAttribute attribute) {
		return Types.DOUBLE;
	}

	protected int getDecimalColumnSqlType(DecimalAttribute attribute) {
		return Types.DECIMAL;
	}

	protected void setLongValue(int index, PreparedStatement ps, long v) throws SQLException {
		ps.setLong(index, v);
	}

	protected void setDoubleValue(int index, PreparedStatement ps, double v) throws SQLException {
		ps.setDouble(index, v);
	}

	protected void setDecimalValue(int index, PreparedStatement ps, BigDecimal v) throws SQLException {
		ps.setBigDecimal(index, v);
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, DecimalAttribute attribute, Object rawValue) throws SQLException {
		Number number = (Number) rawValue;
		if (number == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			Kind kind = getKind(attribute);
			if (Kind.LONG.equals(kind)) {
				setLongValue(index, ps, number.longValue());
			} else if (Kind.DOUBLE.equals(kind)) {
				setDoubleValue(index, ps, number.doubleValue());
			} else if (Kind.DECIMAL.equals(kind)) {
				setDecimalValue(index, ps, (BigDecimal) number);
			} else {
				throw new IllegalStateException("unsupported kind: " + kind);
			}
		}
	}

	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final DecimalAttribute attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public Number get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, get());
			}

		};
	}
	
}
