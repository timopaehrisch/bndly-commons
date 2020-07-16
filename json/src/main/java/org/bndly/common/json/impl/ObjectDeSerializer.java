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
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ObjectDeSerializer implements Serializer, Deserializer {

	private static final Logger LOG = LoggerFactory.getLogger(ObjectDeSerializer.class);
	
	@Override
	public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		return javaValue != null;
	}

	@Override
	public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		Class<? extends Object> cls = javaValue.getClass();
		JSObject obj = new JSObject();
		serializePublicGetters(sourceType, conversionContext, javaValue, cls, obj, new HashSet<String>());
		return obj;
	}

	private void serializePublicGetters(Type sourceType, ConversionContext conversionContext, Object that, Class<? extends Object> cls, JSObject obj, Set<String> invokedGetters) {
		if (Object.class.equals(cls) || cls == null) {
			return;
		}
		Method[] publicMethods = cls.getMethods();
		if (publicMethods != null) {
			for (Method publicMethod : publicMethods) {
				if (Object.class.equals(publicMethod.getDeclaringClass())) {
					// skip the reflection methods
					continue;
				}
				String methodName = publicMethod.getName();
				if (invokedGetters.contains(methodName)) {
					continue;
				}
				invokedGetters.add(methodName);
				Class<?> rt = publicMethod.getReturnType();
				boolean isGet = methodName.startsWith("get");
				boolean isIs = methodName.startsWith("is");
				if ((isGet || isIs) && !rt.equals(Void.class) && getParameterCount(publicMethod) == 0) {
					boolean ia = publicMethod.isAccessible();
					try {
						publicMethod.setAccessible(true);
						Object propertyValue = publicMethod.invoke(that);
						Type propertyType = publicMethod.getGenericReturnType();
						JSValue serializedObjectProperty = conversionContext.serialize(propertyType, propertyValue);
						if (serializedObjectProperty != null) {
							String propertyName = isGet ? methodName.substring(3) : methodName.substring(2);
							propertyName = lowercaseFirstChar(propertyName);
							obj.createMember(propertyName).setValue(serializedObjectProperty);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
						// invoking the public getter failed.
						LOG.error("could not get property: " + ex.getMessage(), ex);
					} finally {
						publicMethod.setAccessible(ia);
					}
				}
			}
		}
		serializePublicGetters(sourceType, conversionContext, that, cls.getSuperclass(), obj, invokedGetters);
	}

	public static String lowercaseFirstChar(String propertyName) {
		propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
		return propertyName;
	}

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (!JSObject.class.isInstance(value)) {
			return false;
		}
		if (!conversionContext.canInstantiate(targetType, value)) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		Object instance = conversionContext.newInstance(targetType, value);
		if (instance != null) {
			Class<? extends Object> cls = instance.getClass();
			deserializePublicSetters(targetType, conversionContext, (JSObject) value, cls, instance);
		}
		return instance;
	}

	private static int getParameterCount(Method method) {
		Class<?>[] pt = method.getParameterTypes();
		if (pt == null) {
			return 0;
		}
		return pt.length;
	}

	static void deserializePublicSetters(Type targetType, ConversionContext conversionContext, JSObject value, Class<? extends Object> cls, Object instance) {
		if (Object.class.equals(cls) || cls == null) {
			return;
		}
		Method[] publicMethods = cls.getMethods();
		if (publicMethods != null) {
			for (Method publicMethod : publicMethods) {
				if (Object.class.equals(publicMethod.getDeclaringClass())) {
					// skip the reflection methods
					continue;
				}
				if (getParameterCount(publicMethod) != 1) {
					continue;
				}
				String methodName = publicMethod.getName();
				boolean isSet = methodName.startsWith("set");
				if (!isSet) {
					continue;
				}
				Type pt = publicMethod.getGenericParameterTypes()[0];
				String propertyName = methodName.substring(3);
				String propertyNameLC = lowercaseFirstChar(propertyName);
				JSMember member = value.getMember(propertyNameLC);
				if (member == null) {
					member = value.getMember(propertyName);
				}
				if (member != null) {
					Object v = conversionContext.deserialize(pt, member.getValue());
					try {
						publicMethod.invoke(instance, v);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
						// invoking a public setter failed.
						LOG.error("could not set property: " + ex.getMessage(), ex);
					}
				}
			}
		}
		deserializePublicSetters(targetType, conversionContext, value, cls.getSuperclass(), instance);
	}
	
}
