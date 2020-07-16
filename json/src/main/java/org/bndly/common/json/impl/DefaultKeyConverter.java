package org.bndly.common.json.impl;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.KeyConverter;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultKeyConverter implements KeyConverter {

	@Override
	public boolean canCreateJSONKey(Type mapType, Type entryType, ConversionContext conversionContext, Object javaKey, Object javaValue) {
		if (javaKey == null) {
			return false;
		}
		return true;
	}

	@Override
	public String createJSONKey(Type mapType, Type entryType, ConversionContext conversionContext, Object javaKey, Object javaValue) {
		if (String.class.isInstance(javaKey)) {
			return (String) javaKey;
		}
		return javaKey.toString();
	}

	@Override
	public boolean canCreateJavaKey(Type mapType, Type keyType, Type entryType, ConversionContext conversionContext, JSValue jsValue, String jsKey, Object deserializedJavaValue) {
		Class keyCls = ReflectionUtil.getSimpleClassType(keyType);
		if (keyCls == null) {
			return false;
		}
		if (!String.class.isAssignableFrom(keyCls)) {
			return false;
		}
		if (jsKey == null && deserializedJavaValue == null) {
			return false;
		}
		return true;
	}

	@Override
	public Object createJavaKey(Type mapType, Type keyType, Type entryType, ConversionContext conversionContext, JSValue jsValue, String jsKey, Object deserializedJavaValue) {
		if (jsKey != null) {
			return jsKey;
		}
		return deserializedJavaValue.toString();
	}

}
