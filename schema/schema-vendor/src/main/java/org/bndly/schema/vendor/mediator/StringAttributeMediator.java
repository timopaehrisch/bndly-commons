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
import org.bndly.schema.model.StringAttribute;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class StringAttributeMediator extends AbstractAttributeMediator<StringAttribute> {

	@Override
	public String columnType(StringAttribute attribute) {
		Integer length = attribute.getLength();
		if (length == null) {
			length = 255;
			attribute.setLength(length);
		}
		String columnType;
		if (isLong(attribute)) {
			columnType = "LONGTEXT";
		} else {
			columnType = "VARCHAR";
			columnType += "(" + length + ")";
			attribute.setIsLong(Boolean.FALSE);
		}
		return columnType;
	}

	@Override
	public int columnSqlType(StringAttribute attribute) {
		if (isLong(attribute)) {
			return Types.CLOB;
		} else {
			return Types.VARCHAR;
		}
	}

	protected final boolean isLong(StringAttribute attribute) {
		if (attribute.getIsLong() != null && attribute.getIsLong()) {
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, StringAttribute attribute, RecordContext recordContext) throws SQLException {
		return rs.getString(columnName);
	}

	@Override
	public String getAttributeValue(Record record, StringAttribute attribute) {
		return record.getAttributeValue(attribute.getName(), String.class);
	}

	@Override
	public String filterValueForPreparedStatementArgument(Object value, StringAttribute attribute) {
		if (value == null) {
			return null;
		}
		String string = (String) value;
		if (isLong(attribute)) {
			return (String) value;
		} else {
			if (attribute.getLength() != null && string.length() > attribute.getLength()) {
				string = string.substring(0, attribute.getLength());
			}
			return string;
		}
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, StringAttribute attribute, Record record) throws SQLException {
		String string = (String) getAttributeValue(record, attribute);
		string = (String) filterValueForPreparedStatementArgument(string, attribute);
		if (string == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			if (attribute.getIsLong() != null && attribute.getIsLong()) {
				ps.setClob(index, new StringReader(string));
			} else {
				ps.setString(index, string);
			}
		}
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, StringAttribute attribute, Object rawValue) throws SQLException {
		String string = filterValueForPreparedStatementArgument(rawValue, attribute);
		if (string == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			if (Types.CLOB == columnSqlType(attribute)) {
				ps.setClob(index, new StringReader(string));
			} else {
				ps.setString(index, string);
			}
		}
	}

	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final StringAttribute attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public String get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, get());
			}
			
		};
	}
}
