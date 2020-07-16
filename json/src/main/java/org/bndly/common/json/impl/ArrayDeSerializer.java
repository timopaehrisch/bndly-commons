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
import org.bndly.common.json.api.Deserializer;
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSValue;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ArrayDeSerializer implements Serializer, Deserializer {

	@Override
	public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		return javaValue != null && javaValue.getClass().isArray();
	}

	@Override
	public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		Object[] array = (Object[]) javaValue;
		JSArray jsarray = new JSArray();
		Type genType = getGenericTypeParameterOf(sourceType);
		for (Object arrayItem : array) {
			Type itemType = arrayItem == null ? genType : arrayItem.getClass();
			if (itemType != null) {
				JSValue item = conversionContext.serialize(itemType, arrayItem);
				jsarray.add(item);
			}
		}
		return jsarray;
	}

	private Type getGenericTypeParameterOf(Type sourceType) {
		Type genType;
		if (Class.class.isInstance(sourceType)) {
			genType = ((Class) sourceType).getComponentType();
		} else {
			GenericArrayType gat = (GenericArrayType) sourceType;
			genType = gat.getGenericComponentType();
		}
		return genType;
	}

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (!JSArray.class.isInstance(value)) {
			return false;
		}
		if (!Class.class.isInstance(targetType)) {
			return false;
		}
		if (!((Class) targetType).isArray()) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		JSArray val = (JSArray) value;
		List<JSValue> items = val.getItems();
		Type genType = getGenericTypeParameterOf(targetType);
		Object[] array = (Object[]) Array.newInstance((Class<?>) genType, items == null ? 0 : items.size());
		if (items != null) {
			for (int i = 0; i < items.size(); i++) {
				// convert the item
				JSValue item = items.get(i);
				Object deserialized = conversionContext.deserialize(genType, item);
				array[i] = deserialized;
			}
		}
		return array;
	}
	
}
