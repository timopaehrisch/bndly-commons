package org.bndly.common.reflection;

/*-
 * #%L
 * Reflection
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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MapBasedReflectivePojoValueProvider implements ReflectivePojoValueProvider {

	private final Map<String, Object> values;

	public MapBasedReflectivePojoValueProvider() {
		this(new HashMap<String, Object>());
	}

	public MapBasedReflectivePojoValueProvider(Map<String, Object> values) {
		this.values = values;
	}
	
	@Override
	public Object get(String propertyName, Type desiredType) {
		Object v = get(propertyName);
		if (v == null) {
			return null;
		}
		Class simpleType = ReflectionUtil.getSimpleClassType(desiredType);
		if (simpleType.isInstance(v)) {
			return simpleType.cast(v);
		}
		return null;
	}

	private Object get(String propertyName) {
		return values.get(propertyName);
	}

	@Override
	public void set(String propertyName, Type requiredType, Object value) {
		Class simpleType = ReflectionUtil.getSimpleClassType(requiredType);
		if (simpleType.isInstance(value) || value == null) {
			set(propertyName, value);
		}
	}

	private void set(String propertyName, Object value) {
		values.put(propertyName, value);
	}
	
}
