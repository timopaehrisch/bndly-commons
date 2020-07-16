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

import org.bndly.common.data.io.SmartBufferOutputStream;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.JSONAttribute;
import java.io.IOException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JSONAttributeMediator extends BinaryAttributeMediator<JSONAttribute> {

	private final RecordJsonConverter recordJsonConverter;

	public JSONAttributeMediator(LobHandler lobHandler, RecordJsonConverter recordJsonConverter) {
		super(lobHandler);
		this.recordJsonConverter = recordJsonConverter;
	}

	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, BinaryAttribute attribute, RecordContext recordContext) throws SQLException {
		Blob blob = rs.getBlob(columnName);
		if (blob == null) {
			return null;
		}
		JSValue parsed = new JSONParser().parse(blob.getBinaryStream(), "UTF-8");
		if (JSObject.class.isInstance(parsed)) {
			return getRecordJsonConverter().convertJsonToRecord((JSObject) parsed, recordContext);
		} else {
			return null;
		}
	}

	@Override
	public Object getAttributeValue(Record record, JSONAttribute attribute) {
		Object val = record.getAttributeValue(attribute.getName());
		if (val == null || Record.class.isInstance(val) || JSObject.class.isInstance(val)) {
			return val;
		} else {
			throw new SchemaException("value of attribute " + attribute.getName() + " was not a record or a jsobject: " + val.getClass().getName());
		}
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, BinaryAttribute attribute, Record record) throws SQLException {
		SmartBufferOutputStream buf = SmartBufferOutputStream.newInstance();
		Object val = record.getAttributeValue(attribute.getName());
		if (Record.class.isInstance(val)) {
			val = getRecordJsonConverter().convertRecordToJson(new JSObject(), (Record) val);
		}
		if (JSValue.class.isInstance(val)) {
			try {
				new JSONSerializer().serialize((JSValue) val, buf, "UTF-8", true);
				buf.flush();
				binaryStreamSupport.setStreamWithFallback(ps, index, buf.getBufferedDataAsStream());
			} catch (IOException ex) {
				throw new SQLException("could not serialize json", ex);
			}
		}
	}
	
	@Override
	public Object filterValueForPreparedStatementArgument(Object value, JSONAttribute attribute) {
		if (Record.class.isInstance(value)) {
			JSObject val = getRecordJsonConverter().convertRecordToJson(new JSObject(), (Record) value);
			try {
				SmartBufferOutputStream os = SmartBufferOutputStream.newInstance();
				new JSONSerializer().serialize(val, os, "UTF-8");
				os.flush();
				return os.getBufferedDataAsStream();
			} catch (IOException ex) {
				throw new SchemaException("could not convert record to json stream data", ex);
			}
		}
		return super.filterValueForPreparedStatementArgument(value, attribute); //To change body of generated methods, choose Tools | Templates.
	}

	public RecordJsonConverter getRecordJsonConverter() {
		if (recordJsonConverter == null) {
			throw new SchemaException("could not access recordJsonConverter");
		}
		return recordJsonConverter;
	}

	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final JSONAttribute attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				Object value = get();
				if (value == null) {
					ps.setNull(index, columnSqlType(attribute));
				} else if (Record.class.isInstance(value)) {
					JSObject json = getRecordJsonConverter().convertRecordToJson(new JSObject(), (Record) value);
					setJson(index, ps, json);
				} else if (JSObject.class.isInstance(value)) {
					setJson(index, ps, (JSObject) value);
				} else {
					throw new SchemaException("value of attribute " + attribute.getName() + " was not a record or a jsobject: " + value.getClass().getName());
				}
			}
			
			private void setJson(int index, PreparedStatement ps, JSObject json) throws SQLException {
				try {
					SmartBufferOutputStream buf = SmartBufferOutputStream.newInstance();
					new JSONSerializer().serialize(json, buf, "UTF-8", true);
					buf.flush();
					setRawParameterInPreparedStatement(index, ps, attribute, buf.getBufferedDataAsStream());
				} catch (IOException ex) {
					throw new SQLException("could not serialize json", ex);
				}
			}
		};
	}
	
}
