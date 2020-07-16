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
import org.bndly.schema.model.DateAttribute;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateAttributeMediator extends AbstractAttributeMediator<DateAttribute> {

	@Override
	public String columnType(DateAttribute attribute) {
		return "TIMESTAMP";
	}

	@Override
	public int columnSqlType(DateAttribute attribute) {
		return Types.TIMESTAMP;
	}

	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, DateAttribute attribute, RecordContext recordContext) throws SQLException {
		Timestamp ts = rs.getTimestamp(columnName, new GregorianCalendar());
		if (rs.wasNull()) {
			return null;
		}
		return new Date(ts.getTime());
	}

	@Override
	public Date getAttributeValue(Record record, DateAttribute attribute) {
		return record.getAttributeValue(attribute.getName(), Date.class);
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, DateAttribute attribute, Record record) throws SQLException {
		Date date = (Date) getAttributeValue(record, attribute);
		setRawParameterInPreparedStatement(index, ps, attribute, date);
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, DateAttribute attribute, Object rawValue) throws SQLException {
		Date date = (Date) rawValue;
		if (date == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			// cut of the milli seconds because there might be rounding issues in mysql.
			long timeInMilliSeconds = date.getTime();
			long timeInSeconds = timeInMilliSeconds / 1000;
			timeInMilliSeconds = timeInSeconds * 1000;
			ps.setTimestamp(index, new Timestamp(timeInMilliSeconds));
		}
	}

	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final DateAttribute attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public Date get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, get());
			}

		};
	}

}
