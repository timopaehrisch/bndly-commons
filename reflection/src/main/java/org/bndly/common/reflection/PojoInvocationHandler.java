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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PojoInvocationHandler implements InvocationHandler {
	
	private final Class interfaceType;
	private final ReflectivePojoValueProvider valueProvider;
	private final Map<String, Method> propertyAccessors;

	public PojoInvocationHandler(Class interfaceType, ReflectivePojoValueProvider valueProvider) {
		if (interfaceType == null) {
			throw new IllegalArgumentException("interfaceType is not allowed to be null");
		}
		if (valueProvider == null) {
			throw new IllegalArgumentException("valueProvider is not allowed to be null");
		}
		this.interfaceType = interfaceType;
		this.valueProvider = valueProvider;
		propertyAccessors = new HashMap<>();
		new PropertyAccessor().addGettersAsProperties(interfaceType, propertyAccessors);
	}

	@Override
	public Object invoke(Object o, Method method, Object[] os) throws Throwable {
		String methodName = method.getName();

		if ("toString".equals(methodName)) {
			return interfaceType.getSimpleName() + " dynamic instance";
		} else if ("equals".equals(methodName) && os.length == 1) {
			return _equals(os[0]);
		} else if ("hashCode".equals(methodName)) {
			return _hashCode();
		}

		if (methodName.startsWith("get") || methodName.startsWith("is")) {
			String propertyName;
			if (methodName.startsWith("get")) {
				propertyName = methodName.substring("get".length());
			} else {
				propertyName = methodName.substring("is".length());
			}
			propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
			return valueProvider.get(propertyName, method.getGenericReturnType());
		} else if (methodName.startsWith("set") && os.length == 1) {
			String propertyName = methodName.substring("set".length());
			propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
			valueProvider.set(propertyName, method.getGenericParameterTypes()[0], os[0]);
			return null;
		}

		throw new UnsupportedOperationException(methodName + " not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	private boolean _equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		if (!interfaceType.isAssignableFrom(o.getClass())) {
			return false;
		}
		for (Map.Entry<String, Method> entry : propertyAccessors.entrySet()) {
			String property = entry.getKey();
			Method accessor = entry.getValue();

			Object thisValue = valueProvider.get(property, accessor.getReturnType());
			try {
				Object otherValue = accessor.invoke(o);
				if (thisValue == null) {
					if (otherValue != null) {
						return false;
					}
				} else {
					if (!thisValue.equals(otherValue)) {
						return false;
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				// ignore exceptions during the invocation
			}
		}
		return true;
	}

	private int _hashCode() {
		int hash = 5;
		for (Map.Entry<String, Method> entry : propertyAccessors.entrySet()) {
			String property = entry.getKey();
			Object propertyValue = valueProvider.get(property, entry.getValue().getReturnType());
			hash = 41 * hash + (propertyValue != null ? propertyValue.hashCode() : 0);
		}
		return hash;
	}
}
