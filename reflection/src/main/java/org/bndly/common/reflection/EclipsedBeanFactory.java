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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class EclipsedBeanFactory {

	private EclipsedBeanFactory() {
	}

	public static interface Eclipse<T> {

		Eclipse<T> append(T provider);

		Eclipse<T> prepend(T provider);

		Eclipse<T> remove(T provider);

		Eclipse<T> clear();
	}

	public static <T> T createEclipse(Class<T> interfaceType, T... providers) {
		return createEclipse(interfaceType.getClassLoader(), interfaceType, providers);
	}

	public static <T> T createEclipse(ClassLoader classLoader, Class<T> interfaceType, T... providers) {
		if (!interfaceType.isInterface()) {
			throw new IllegalArgumentException(interfaceType + " is not an interface");
		}
		return (T) Proxy.newProxyInstance(classLoader, new Class[]{interfaceType, Eclipse.class}, new EclipseInvocationHandler(interfaceType));
	}

	private static class EclipseInvocationHandler<T> implements InvocationHandler, Eclipse<T> {

		private final Class<T> interfaceType;
		private final List<T> providers = new ArrayList<>();

		public EclipseInvocationHandler(Class<T> interfaceType) {
			this.interfaceType = interfaceType;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (Eclipse.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			} else if (interfaceType.equals(method.getDeclaringClass())) {
				for (T provider : providers) {
					Object result = method.invoke(provider, args);
					if (result != null) {
						return result;
					}
				}
				return null;
			} else {
				return method.invoke(this, args);
			}
		}

		@Override
		public Eclipse<T> append(T provider) {
			if (provider != null) {
				providers.add(provider);
			}
			return this;
		}

		@Override
		public Eclipse<T> prepend(T provider) {
			if (provider != null) {
				providers.add(0, provider);
			}
			return this;
		}

		@Override
		public Eclipse<T> remove(T provider) {
			Iterator<T> iter = providers.iterator();
			while (iter.hasNext()) {
				T next = iter.next();
				if (next == provider) {
					iter.remove();
				}
			}
			return this;
		}

		@Override
		public Eclipse<T> clear() {
			providers.clear();
			return this;
		}

	}
}
