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

import org.bndly.common.data.io.IOUtils;
import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.model.CryptoAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CryptoAttributeMediator extends AbstractAttributeMediator<CryptoAttribute> {

	private final BinaryAttributeMediator.BinaryStreamSupport binaryStreamSupport = new BinaryAttributeMediator.BinaryStreamSupport();
	private final CryptoProvider cryptoProvider;
	private final LobHandler lobHandler;

	public CryptoAttributeMediator(CryptoProvider cryptoProvider, LobHandler lobHandler) {
		this.cryptoProvider = cryptoProvider;
		this.lobHandler = lobHandler;
	}
	
	

	public CryptoProvider getCryptoProvider() {
		if (cryptoProvider == null) {
			throw new IllegalArgumentException("cryptoProvider is not allowed to be null");
		}
		return cryptoProvider;
	}

	public LobHandler getLobHandler() {
		if (lobHandler == null) {
			throw new IllegalArgumentException("lobHandler is not allowed to be null");
		}
		return lobHandler;
	}
	
	@Override
	public String columnType(CryptoAttribute attribute) {
		return "LONGBLOB";
	}

	@Override
	public int columnSqlType(CryptoAttribute attribute) {
		return Types.BLOB;
	}
	
	@Override
	public String extractFromResultSet(ResultSet rs, String columnName, CryptoAttribute attribute, RecordContext recordContext) throws SQLException {
		InputStream stream = getLobHandler().getBlobAsBinaryStream(rs, columnName);
		if (stream != null) {
			if (attribute.isAutoDecrypted()) {
				// use the crypto provider to decode the stream
				try (InputStream decodingStream = getCryptoProvider().createDecryptingStream(stream, attribute)) {
					if (attribute.isPlainString()) {
						// create string from bytes of stream with utf-8 encoding
						return IOUtils.readToString(decodingStream, "UTF-8");
					} else {
						// create string from bytes by base64 encode it
						return getCryptoProvider().base64Encode(decodingStream);
					}
				} catch (IOException e) {
					throw new SchemaException("could not decrypt crypto attribute", e);
				}
			} else {
				// just read the stream and base64 encode it
				return getCryptoProvider().base64Encode(stream);
			}
		}
		return null;
	}

	@Override
	public String getAttributeValue(Record record, CryptoAttribute attribute) {
		return record.getAttributeValue(attribute.getName(), String.class);
	}

	@Override
	public void setParameterInPreparedStatement(int index, PreparedStatement ps, CryptoAttribute attribute, Record record) throws SQLException {
		String value = getAttributeValue(record, attribute);
		setRawParameterInPreparedStatement(index, ps, attribute, value);
	}

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, CryptoAttribute attribute, Object rawValue) throws SQLException {
		if (rawValue == null) {
			ps.setNull(index, columnSqlType(attribute));
		} else {
			if (!String.class.isInstance(rawValue)) {
				throw new SchemaException("rawValue was not a string while setting parameter in prepared statement");
			}
			String rawString = (String) rawValue;
			final InputStream stream;
			if (attribute.isAutoDecrypted()) {
				// encrypt the raw value with the crypto provider
				try {
					if (attribute.isPlainString()) {
						byte[] bytes = rawString.getBytes("UTF-8");
						stream = getCryptoProvider().createEncryptingStream(new ByteArrayInputStream(bytes), attribute);
					} else {
						InputStream decoded = getCryptoProvider().createBase64DecodingStream(rawString);
						stream = getCryptoProvider().createEncryptingStream(decoded, attribute);
					}
				} catch (IOException e) {
					throw new SchemaException("could not encrypt input value", e);
				}
			} else {
				// assume, that the rawValue is already base64 encoded encrypted data
				// remove base64, so we get the raw encrypted stream
				stream = getCryptoProvider().createBase64DecodingStream(rawString);
			}
			// set the encrypted bytes as a stream
			binaryStreamSupport.setStreamWithFallback(ps, index, stream);
		}
	}

	@Override
	public ValueProvider createValueProviderFor(final Record record, final CryptoAttribute attribute) {
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
