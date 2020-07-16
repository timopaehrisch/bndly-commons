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
import org.bndly.common.json.api.Instanciator;
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MapDeSerializer implements Serializer, Deserializer, Instanciator {

	private static final List<Class<? extends Map>> supportedMapTypes;
	
	static {
		supportedMapTypes = new ArrayList<>();
		supportedMapTypes.add(Map.class);
		supportedMapTypes.add(HashMap.class);
		supportedMapTypes.add(LinkedHashMap.class);
		supportedMapTypes.add(Properties.class);
	}
	
	@Override
	public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		if (javaValue == null) {
			return false;
		}
		Class cls = ReflectionUtil.getSimpleClassType(sourceType);
		if (cls == null) {
			return false;
		}
		if (!Map.class.isAssignableFrom(cls)) {
			return false;
		}
		return true;
	}

	@Override
	public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		Map map = (Map) javaValue;
		Type mapEntryType = getMapEntryType(sourceType);
		JSObject jsObject = new JSObject();
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			Type itemType = value == null ? mapEntryType : value.getClass();
			String memberName = conversionContext.memberNameOfMapEntry(sourceType, itemType, key, javaValue);
			if (memberName == null) {
				continue;
			}
			JSValue serialized = conversionContext.serialize(itemType, value);
			if (serialized != null) {
				jsObject.createMember(memberName).setValue(serialized);
			}
		}
		return jsObject;
	}

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (!JSObject.class.isInstance(value) && !JSArray.class.isInstance(value)) {
			return false;
		}
		Class cls = ReflectionUtil.getSimpleClassType(targetType);
		if (cls == null) {
			return false;
		}
		if (!supportedMapTypes.contains(cls)) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		Class cls = ReflectionUtil.getSimpleClassType(targetType);
		Map map = (Map) instantiate(cls, conversionContext, value);
		Type entryType = getMapEntryType(targetType);
		Type keyType = getMapKeyType(targetType);
		if (JSObject.class.isInstance(value)) {
			Set<JSMember> members = ((JSObject) value).getMembers();
			if (members != null) {
				for (JSMember member : members) {
					String memberName = member.getName().getValue();
					JSValue item = member.getValue();
					// try to get a key for the item
					Object deserialized = conversionContext.deserialize(entryType, item);
					if (conversionContext.canKeyMapEntry(targetType, keyType, entryType, item, memberName, deserialized)) {
						Object key = conversionContext.keyOfMapEntry(targetType, keyType, entryType, item, memberName, deserialized);
						map.put(key, deserialized);
					}
				}
			}
		} else if (JSArray.class.isInstance(value)) {
			List<JSValue> items = ((JSArray) value).getItems();
			if (items != null) {
				for (JSValue item : items) {
					// try to get a key for the item
					Object deserialized = conversionContext.deserialize(entryType, item);
					if (conversionContext.canKeyMapEntry(targetType, keyType, entryType, item, null, deserialized)) {
						Object key = conversionContext.keyOfMapEntry(targetType, keyType, entryType, item, null, deserialized);
						map.put(key, deserialized);
					}
				}
			}
		}
		return map;
	}

	@Override
	public boolean canInstantiate(Type desiredType, ConversionContext conversionContext, JSValue jsValue) {
		Class cls = ReflectionUtil.getSimpleClassType(desiredType);
		if (cls == null) {
			return false;
		}
		return supportedMapTypes.contains(cls);
	}

	@Override
	public Map instantiate(Type desiredType, ConversionContext conversionContext, JSValue jsValue) {
		Class cls = ReflectionUtil.getSimpleClassType(desiredType);
		if (Map.class.equals(cls) || HashMap.class.equals(cls)) {
			return new HashMap<>();
		} else if (LinkedHashMap.class.equals(cls)) {
			return new LinkedHashMap();
		} else if (Properties.class.equals(cls)) {
			return new Properties();
		} else {
			throw new IllegalArgumentException("can not instantiate " + desiredType);
		}
	}

	private Type getMapEntryType(Type mapType) {
		return getMapTypeParameter(mapType, 1);
	}

	private Type getMapKeyType(Type mapType) {
		return getMapTypeParameter(mapType, 0);
	}

	private Type getMapTypeParameter(Type mapType, int index) {
		if (ParameterizedType.class.isInstance(mapType)) {
			Type[] args = ((ParameterizedType) mapType).getActualTypeArguments();
			if (args.length == 2) {
				return args[index];
			}
		}
		return null;
	}
	
}
