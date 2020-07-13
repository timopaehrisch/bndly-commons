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

import java.lang.reflect.Method;
import java.util.Map;

public class PropertyAccessor {
	public void addSettersAsProperties(Class<?> type, Map<String, Method> propertyNames) {
		if (type != null) {
			addSettersAsProperties(type.getSuperclass(), propertyNames);
			Method[] methods = type.getDeclaredMethods();
			for (Method method : methods) {
				String methodName = method.getName();

				if (methodName.startsWith("set") && method.getParameterTypes().length == 1) {
					String propertyName = methodName.substring("set".length());
					propertyNames.put(propertyName, method);
				}
			}
			Class<?>[] interfaces = type.getInterfaces();
			if (interfaces != null) {
				for (Class<?> interfaceType : interfaces) {
					addSettersAsProperties(interfaceType, propertyNames);
				}
			}
		}
	}

	public void addGettersAsProperties(Class<?> type, Map<String, Method> propertyNames) {
		if (type != null) {
			addGettersAsProperties(type.getSuperclass(), propertyNames);
			Method[] methods = type.getDeclaredMethods();
			for (Method method : methods) {
				String methodName = method.getName();

				if ((methodName.startsWith("get") || methodName.startsWith("is")) && method.getParameterTypes().length == 0) {
					String propertyName;
					if (methodName.startsWith("get")) {
						propertyName = methodName.substring("get".length());
					} else {
						propertyName = methodName.substring("is".length());
					}
					propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
					propertyNames.put(propertyName, method);
				}
			}
			Class<?>[] interfaces = type.getInterfaces();
			if (interfaces != null) {
				for (Class<?> interfaceType : interfaces) {
					addGettersAsProperties(interfaceType, propertyNames);
				}
			}
		}
	}
}
