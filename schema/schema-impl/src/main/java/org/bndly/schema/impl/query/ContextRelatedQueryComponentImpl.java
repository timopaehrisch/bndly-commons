package org.bndly.schema.impl.query;

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

import org.bndly.schema.api.ValueUtil;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.schema.api.ContextRelatedQueryComponent;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public abstract class ContextRelatedQueryComponentImpl implements ContextRelatedQueryComponent {

	private final QueryContextImpl queryContext;
	private final VendorConfiguration vendorConfiguration;

	public ContextRelatedQueryComponentImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		if (queryContext == null) {
			throw new IllegalArgumentException("queryContext is not allowed to be null");
		}
		this.queryContext = queryContext;
		if (vendorConfiguration == null) {
			throw new IllegalArgumentException("vendorConfiguration is not allowed to be null");
		}
		this.vendorConfiguration = vendorConfiguration;
	}

	@Override
	public final QueryContextImpl getQueryContext() {
		return queryContext;
	}

	public final VendorConfiguration getVendorConfiguration() {
		return vendorConfiguration;
	}

	protected PreparedStatementArgumentSetter createFallbackPreparedStatementArgumentSetter(final ValueProvider valueProvider, final Integer sqlType) {
		if (valueProvider == null) {
			throw new IllegalArgumentException("valueProvider is not allowed to be null");
		}
		return new PreparedStatementArgumentSetter() {

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
					if (sqlType == null) {
						ps.setObject(index, ValueUtil.unwrapValue(valueProvider));
					} else {
						if (sqlType == Types.BLOB || sqlType == Types.BINARY) {
							Object v = ValueUtil.unwrapValue(valueProvider);
							if (v == null) {
								ps.setNull(index, sqlType);
							} else {
								ReplayableInputStream ris;
								if (InputStream.class.isInstance(v)) {
									try {
										ris = ReplayableInputStream.newInstance((InputStream) v);
									} catch (IOException ex) {
										throw new IllegalStateException("could not wrap input stream as replayable input stream: " + ex.getMessage(), ex);
									}
								} else if (v.getClass().isArray()) {
									byte[] ba = (byte[]) v;
									ris = ReplayableInputStream.newInstance(ba);
								} else if (JSObject.class.isInstance(v)) {
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									try {
										new JSONSerializer().serialize((JSValue) v, bos, "UTF-8", true);
									} catch (IOException ex) {
										throw new SQLException("could not set json value in query.", ex);
									}
									byte[] ba = bos.toByteArray();
									ris = ReplayableInputStream.newInstance(ba);
								} else {
									throw new IllegalStateException("unable to set value for blob field");
								}
								ps.setBinaryStream(index, ris);
							}
						} else {
							ps.setObject(index, ValueUtil.unwrapValue(valueProvider), sqlType);
						}
					}
			}
		};
	}
	
}
