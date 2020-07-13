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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public final class InterfaceCollector {

	private InterfaceCollector() {
	}

	public static Class[] collectInterfacesWithAnnotationOfBeanAsArray(Class<? extends Annotation> aClass, Object bean) {
		return collectInterfacesWithAnnotationOfBeanAsArray(aClass, bean.getClass());
	}

	public static Class[] collectInterfacesWithAnnotationOfBeanAsArray(Class<? extends Annotation> aClass, Class<?> type) {
		Set<Class<?>> interfaces = new HashSet<>();
		collectInterfacesOfBean(aClass, type, interfaces);
		Class[] types = new Class[interfaces.size()];
		int i = 0;
		for (Class<?> interfaceType : interfaces) {
			types[i] = interfaceType;
			i++;
		}
		return types;
	}

	public static Class[] collectInterfacesOfBeanAsArray(Object bean) {
		return collectInterfacesOfBeanAsArray(bean.getClass());
	}

	public static Class[] collectInterfacesOfBeanAsArray(Class<?> type) {
		Set<Class<?>> interfaces = collectInterfacesOfBean(type);
		Class[] types = new Class[interfaces.size()];
		int i = 0;
		for (Class<?> interfaceType : interfaces) {
			types[i] = interfaceType;
			i++;
		}
		return types;
	}

	public static Set<Class<?>> collectInterfacesOfBean(Object bean) {
		return collectInterfacesOfBean(bean.getClass());
	}

	public static Set<Class<?>> collectInterfacesOfBean(Class<?> type) {
		Set<Class<?>> t = new HashSet<>();
		collectInterfacesOfBean(type, t);
		return t;
	}

	private static void collectInterfacesOfBean(Class<?> type, Set<Class<?>> interfaces) {
		collectInterfacesOfBean(null, type, interfaces);
	}

	private static void collectInterfacesOfBean(Class<? extends Annotation> annotationType, Class<?> type, Set<Class<?>> interfaces) {
		if (type == null || Object.class.equals(type)) {
			return;
		}
		if (interfaces.contains(type)) {
			return;
		}
		collectInterfacesOfBean(annotationType, type.getSuperclass(), interfaces);
		Class<?>[] ifs = type.getInterfaces();
		for (Class<?> interfaceType : ifs) {
			collectInterfacesOfBean(annotationType, interfaceType, interfaces);
		}
		if (type.isInterface()) {
			if (annotationType == null || type.isAnnotationPresent(annotationType)) {
				interfaces.add(type);
			}
		}
	}
}
