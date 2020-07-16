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

import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.query.ValueProvider;

class KeyValue {

	private final String key;
	private final ValueProvider value;
	private final Integer sqlType;
	private final PreparedStatementArgumentSetter argumentSetter;

	public KeyValue(String key, ValueProvider value, Integer sqlType, PreparedStatementArgumentSetter argumentSetter) {
		if (key == null) {
			throw new IllegalArgumentException("key is not allowed to be null");
		}
		this.key = key;
		if (value == null) {
			throw new IllegalArgumentException("value is not allowed to be null");
		}
		this.value = value;
		this.sqlType = sqlType;
		if (argumentSetter == null) {
			throw new IllegalArgumentException("argument setter is not allowed to be null");
		}
		this.argumentSetter = argumentSetter;
	}

	public String getKey() {
		return key;
	}

	public ValueProvider getValue() {
		return value;
	}

	public Integer getSqlType() {
		return sqlType;
	}

	public PreparedStatementArgumentSetter getArgumentSetter() {
		return argumentSetter;
	}

}
