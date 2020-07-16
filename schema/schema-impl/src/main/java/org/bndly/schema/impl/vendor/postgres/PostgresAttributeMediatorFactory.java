package org.bndly.schema.impl.vendor.postgres;

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

import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.vendor.mediator.BinaryAttributeMediator;
import org.bndly.schema.vendor.mediator.CryptoAttributeMediator;
import org.bndly.schema.vendor.mediator.JSONAttributeMediator;
import org.bndly.schema.vendor.mediator.StringAttributeMediator;
import org.bndly.schema.vendor.def.DefaultAttributeMediatorFactory;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.StringAttribute;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresAttributeMediatorFactory extends DefaultAttributeMediatorFactory {

	@Override
	public BinaryAttributeMediator<BinaryAttribute> createBinaryAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new BinaryAttributeMediator<BinaryAttribute>(lobHandler){
			@Override
			public String columnType(BinaryAttribute attribute) {
				return "BYTEA";
			}
			
			@Override
			public int columnSqlType(BinaryAttribute attribute) {
				return Types.BINARY;
			}
		};
	}

	@Override
	public JSONAttributeMediator createJSONAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new JSONAttributeMediator(lobHandler, recordJsonConverter){

			@Override
			public Object extractFromResultSet(ResultSet rs, String columnName, BinaryAttribute attribute, RecordContext recordContext) throws SQLException {
				InputStream stream = rs.getBinaryStream(columnName);
				if (stream == null) {
					return null;
				}
				JSValue parsed = new JSONParser().parse(stream, "UTF-8");
				if (JSObject.class.isInstance(parsed)) {
					return getRecordJsonConverter().convertJsonToRecord((JSObject) parsed, recordContext);
				} else {
					return null;
				}
			}

			@Override
			public String columnType(BinaryAttribute attribute) {
				return "BYTEA";
			}

			@Override
			public int columnSqlType(BinaryAttribute attribute) {
				return Types.BINARY;
			}

		};
	}

	@Override
	public StringAttributeMediator createStringAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new StringAttributeMediator() {
			@Override
			public String columnType(StringAttribute attribute) {
				Integer length = attribute.getLength();
				if (length == null) {
					length = 255;
					attribute.setLength(length);
				}
				String columnType;
				if (isLong(attribute)) {
					columnType = "TEXT";
				} else {
					columnType = "VARCHAR";
					columnType += "(" + length + ")";
					attribute.setIsLong(Boolean.FALSE);
				}
				return columnType;
			}

			@Override
			public int columnSqlType(StringAttribute attribute) {
				return Types.VARCHAR;
			}

		};
	}

	@Override
	public CryptoAttributeMediator createCryptoAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new CryptoAttributeMediator(cryptoProvider, lobHandler){
			@Override
			public String columnType(CryptoAttribute attribute) {
				return "BYTEA";
			}
			
			@Override
			public int columnSqlType(CryptoAttribute attribute) {
				return Types.BINARY;
			}
		};
	}
	
	
}
