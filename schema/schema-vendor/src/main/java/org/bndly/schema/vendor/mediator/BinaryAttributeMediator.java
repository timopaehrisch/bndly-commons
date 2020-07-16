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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.model.BinaryAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class BinaryAttributeMediator<E extends BinaryAttribute> extends AbstractAttributeMediator<E> {

	protected static final class BinaryStreamSupport {
		private boolean binaryStreamSupported = true;
		
		public void setStreamWithFallback(PreparedStatement ps, int index, InputStream stream) throws SQLException {
			if (!binaryStreamSupported) {
				try {
					ReplayableInputStream rpis = ReplayableInputStream.newInstance(stream);
					long length = rpis.getLength();
					if (length < 0) {
						rpis = rpis.doReplay();
						length = rpis.getLength();
					}
					ps.setBinaryStream(index, rpis, length);
				} catch (IOException e) {
					throw new SchemaException("could not fall back to void setBinaryStream(int parameterIndex, java.io.InputStream x, long length) while setting a blob value", e);
				}
			} else {
				try {
					ps.setBinaryStream(index, stream);
				} catch (java.sql.SQLFeatureNotSupportedException e) {
					binaryStreamSupported = false;
					setStreamWithFallback(ps, index, stream);
				}
			}
		}
	}

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	protected final BinaryStreamSupport binaryStreamSupport = new BinaryStreamSupport();
	private final LobHandler lobHandler;

	public BinaryAttributeMediator(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	@Override
	public String columnType(BinaryAttribute attribute) {
		return "LONGBLOB";
	}

	@Override
	public int columnSqlType(BinaryAttribute attribute) {
		return Types.BLOB;
	}
	
	@Override
	public Object extractFromResultSet(ResultSet rs, String columnName, BinaryAttribute attribute, RecordContext recordContext) throws SQLException {
		if (useByteArray(attribute)) {
			return lobHandler.getBlobAsBytes(rs, columnName);
		} else {
			return lobHandler.getBlobAsBinaryStream(rs, columnName);
		}
	}

	@Override
	public Object getAttributeValue(Record record, BinaryAttribute attribute) {
		if (useByteArray(attribute)) {
			return record.getAttributeValue(attribute.getName(), EMPTY_BYTE_ARRAY.getClass());
		} else {
			return record.getAttributeValue(attribute.getName(), InputStream.class);
		}
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, BinaryAttribute attribute, Record record) throws SQLException {
		Object value = record.getAttributeValue(attribute.getName());
		InputStream stream;
		if (value.getClass().isArray()) {
			stream = new ByteArrayInputStream((byte[]) value);
		} else if (InputStream.class.isInstance(value)) {
			stream = (InputStream) value;
		} else {
			throw new IllegalArgumentException("unsupported value for binary attribute: " + value.getClass());
		}
		binaryStreamSupport.setStreamWithFallback(ps, index, stream);
	}

	private boolean useByteArray(BinaryAttribute attribute) {
		return attribute.getAsByteArray() != null && attribute.getAsByteArray();
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, E attribute, Object rawValue) throws SQLException {
		if (rawValue == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			InputStream stream;
			if (rawValue.getClass().isArray()) {
				stream = new ByteArrayInputStream((byte[]) rawValue);
			} else if (InputStream.class.isInstance(rawValue)) {
				stream = (InputStream) rawValue;
			} else {
				throw new IllegalArgumentException("unsupported value for binary attribute: " + rawValue.getClass());
			}
			binaryStreamSupport.setStreamWithFallback(ps, index, stream);
		}
	}

	
	@Override
	public PreparedStatementValueProvider createValueProviderFor(final Record record, final E attribute) {
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				return getAttributeValue(record, attribute);
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, get());
			}
			
		};
	}
	
}
