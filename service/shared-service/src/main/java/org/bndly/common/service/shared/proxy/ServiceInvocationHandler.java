package org.bndly.common.service.shared.proxy;

/*-
 * #%L
 * Service Shared Impl
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
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Created by ben on 16/06/15.
 */
public final class ServiceInvocationHandler implements InvocationHandler {

	private final Object generatedServiceInstance;
	private final Object customServiceInstance;

	public ServiceInvocationHandler(Object generatedServiceInstance, Object customServiceInstance, Class api) {
		if (generatedServiceInstance == null) {
			throw new IllegalArgumentException("generatedServiceInstance is not allowed to be null");
		}
		this.generatedServiceInstance = generatedServiceInstance;
		this.customServiceInstance = customServiceInstance;
		assertImplementationExists(api);
	}

	private void assertImplementationExists(Class api) {
		if (!api.isInterface()) {
			return;
		}
		Method[] publicMethods = api.getMethods();
		for (Method m : publicMethods) {
			Object that = lookUpThis(m);
			if (that == null) {
				throw new IllegalStateException("no implementation in proxy for method " + m.toString());
			}
		}
		Class[] otherInterfaces = api.getInterfaces();
		for (Class inter : otherInterfaces) {
			assertImplementationExists(inter);
		}
	}

	public Object getGeneratedServiceInstance() {
		return generatedServiceInstance;
	}

	public Object getCustomServiceInstance() {
		return customServiceInstance;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object that = lookUpThis(method);
		try {
			return method.invoke(that, args);
		} catch (UndeclaredThrowableException e) {
			throw e.getUndeclaredThrowable();
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	private Object lookUpThis(Method method) {
		if (method.getDeclaringClass().equals(Object.class)) {
			return this;
		}
		if (method.getDeclaringClass().isAssignableFrom(generatedServiceInstance.getClass())) {
			return generatedServiceInstance;
		} else {
			return customServiceInstance;

		}
	}
}
