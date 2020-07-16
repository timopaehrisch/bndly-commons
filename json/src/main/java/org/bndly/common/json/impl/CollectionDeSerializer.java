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
import org.bndly.common.json.model.JSValue;
import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CollectionDeSerializer implements Serializer, Deserializer, Instanciator {

	private static final List<Class<? extends Collection>> supportedCollectionTypes;
	
	static {
		supportedCollectionTypes = new ArrayList<>();
		supportedCollectionTypes.add(List.class);
		supportedCollectionTypes.add(Set.class);
		supportedCollectionTypes.add(HashSet.class);
		supportedCollectionTypes.add(ArrayList.class);
		supportedCollectionTypes.add(LinkedList.class);
		supportedCollectionTypes.add(Stack.class);
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
		if (!Collection.class.isAssignableFrom(cls)) {
			return false;
		}
		return true;
	}

	@Override
	public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		Collection col = (Collection) javaValue;
		JSArray arr = new JSArray();
		
		Type collectionParameterType = null;
		if (ParameterizedType.class.isInstance(sourceType)) {
			Type[] args = ((ParameterizedType) sourceType).getActualTypeArguments();
			if (args != null && args.length == 1) {
				collectionParameterType = args[0];
			}
		}
		
		for (Object collectionItem : col) {
			Type itemType = collectionItem != null ? collectionItem.getClass() : collectionParameterType;
			if (itemType != null) {
				JSValue serialized = conversionContext.serialize(itemType, collectionItem);
				if (serialized != null) {
					arr.add(serialized);
				}
			}
		}
		return arr;
	}

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (!JSArray.class.isInstance(value)) {
			return false;
		}
		Class cls = ReflectionUtil.getSimpleClassType(targetType);
		if (cls == null) {
			return false;
		}
		if (!Collection.class.isAssignableFrom(cls)) {
			return false;
		}
		if (!supportedCollectionTypes.contains(cls)) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		Collection collection = instantiate(targetType, conversionContext, value);
		Type itemType = getCollectionParameterType(targetType);
		for (JSValue val : ((JSArray)value)) {
			Object deserialized = conversionContext.deserialize(itemType, val);
			collection.add(deserialized);
		}
		return collection;
	}

	private Type getCollectionParameterType(Type collectionType) {
		if (ParameterizedType.class.isInstance(collectionType)) {
			Type[] args = ((ParameterizedType) collectionType).getActualTypeArguments();
			if (args.length == 1) {
				return args[0];
			}
		}
		return Object.class; // could be anything. we are hitting a raw collection type.
	}
	
	@Override
	public boolean canInstantiate(Type desiredType, ConversionContext conversionContext, JSValue jsValue) {
		Class cls = ReflectionUtil.getSimpleClassType(desiredType);
		if (cls == null) {
			return false;
		}
		return supportedCollectionTypes.contains(cls);
	}

	@Override
	public Collection instantiate(Type desiredType, ConversionContext conversionContext, JSValue jsValue) {
		Integer size = null;
		if (JSArray.class.isInstance(jsValue)) {
			size = ((JSArray) jsValue).size();
		}
		Class cls = ReflectionUtil.getSimpleClassType(desiredType);
		if (ArrayList.class.equals(cls) || List.class.equals(cls)) {
			if (size != null) {
				return new ArrayList<>(size);
			} else {
				return new ArrayList<>();
			}
		} else if (LinkedList.class.equals(cls)) {
			return new LinkedList<>();
		} else if (Stack.class.equals(cls)) {
			return new Stack<>();
		} else if (HashSet.class.equals(cls) || Set.class.equals(cls)) {
			if (size != null) {
				return new HashSet<>(size);
			} else {
				return new HashSet<>();
			}
		} else {
			throw new IllegalArgumentException("could not instantiate " + desiredType);
		}
	}
}
