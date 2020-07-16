package org.bndly.schema.impl.vendor.mysql;

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

import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.vendor.mediator.DateAttributeMediator;
import org.bndly.schema.vendor.def.DefaultAttributeMediatorFactory;
import org.bndly.schema.model.DateAttribute;
import java.sql.Types;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MySQLAttributeMediatorFactory extends DefaultAttributeMediatorFactory {

	@Override
	public DateAttributeMediator createDateAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new DateAttributeMediator() {
			@Override
			public String columnType(DateAttribute attribute) {
				return "DATETIME";
			}

			@Override
			public int columnSqlType(DateAttribute attribute) {
				return Types.TIMESTAMP;
			}
			
		};
	}
	
}
