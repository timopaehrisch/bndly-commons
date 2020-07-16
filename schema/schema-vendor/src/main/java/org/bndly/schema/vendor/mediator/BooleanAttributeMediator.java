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
import org.bndly.schema.model.BooleanAttribute;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class BooleanAttributeMediator extends AbstractAttributeMediator<BooleanAttribute> {

	@Override
	public String columnType(BooleanAttribute attribute) {
		return "BOOLEAN";
	}

	@Override
	public int columnSqlType(BooleanAttribute attribute) {
		return Types.BOOLEAN;
	}
	
	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, BooleanAttribute attribute, RecordContext recordContext) throws SQLException {
		return rs.getBoolean(columnName);
	}

	@Override
	public Boolean getAttributeValue(Record record, BooleanAttribute attribute) {
		return record.getAttributeValue(attribute.getName(), Boolean.class);
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, BooleanAttribute attribute, Record record) throws SQLException {
		Boolean b = getAttributeValue(record, attribute);
		if (b == null) {
			ps.setNull(index, Types.BOOLEAN);
		} else {
			ps.setBoolean(index, b);
		}
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, BooleanAttribute attribute, Object rawValue) throws SQLException {
		Boolean b = (Boolean) rawValue;
		if (b == null) {
			ps.setNull(index, Types.BOOLEAN);
		} else {
			ps.setBoolean(index, b);
		}
	}

	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final BooleanAttribute attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public Boolean get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, get());
			}

		};
	}

}
