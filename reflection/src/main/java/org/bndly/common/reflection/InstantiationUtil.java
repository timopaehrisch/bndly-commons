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

import java.lang.reflect.Proxy;

public final class InstantiationUtil {

	private InstantiationUtil() {
	}

	public static <E> E instantiateType(Class<E> type) {
		return instantiateType(type, null);
	}

	public static <E> E instantiateType(Class<E> type, ReflectivePojoValueProvider valueProvider) {
		E e = null;
		if (type.isInterface()) {
			if (valueProvider == null) {
				valueProvider = new MapBasedReflectivePojoValueProvider();
			}
			e = InstantiationUtil.instantiateDomainModelInterface(type, valueProvider);
		} else {
			try {
				e = type.newInstance();
			} catch (Exception ex) {
				// ignore this. the caller has to care for null values.
			}
		}
		return e;
	}

	public static <E> E instantiateDomainModelInterface(final Class<E> interfaceType) {
		return instantiateDomainModelInterface(interfaceType, new MapBasedReflectivePojoValueProvider());
	}

	public static <E> E instantiateDomainModelInterface(final Class<E> interfaceType, ReflectivePojoValueProvider valueProvider) {
		return instantiateDomainModelInterface(interfaceType.getClassLoader(), interfaceType, valueProvider);
	}
	
	public static <E> E instantiateDomainModelInterface(ClassLoader classLoader, final Class<E> interfaceType, ReflectivePojoValueProvider valueProvider) {
		E e = (E) Proxy.newProxyInstance(classLoader, new Class[]{interfaceType}, new PojoInvocationHandler(interfaceType, valueProvider));
		return e;
	}
}
