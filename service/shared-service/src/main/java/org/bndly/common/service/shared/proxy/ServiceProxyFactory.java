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

import org.bndly.common.service.shared.api.ProxyAware;
import java.lang.reflect.Proxy;

/**
 * Created by ben on 16/06/15.
 */
public class ServiceProxyFactory {

	public <E> E getInstance(Class<E> serviceClass, Object generatedServiceInstance, Object customServiceInstance) {
		return getInstance(serviceClass.getClassLoader(), serviceClass,generatedServiceInstance, customServiceInstance);
	}

	public <E> E getInstance(ClassLoader customClassLoader, Class<E> serviceClass, Object generatedServiceInstance, Object customServiceInstance) {
		E proxy = (E) Proxy.newProxyInstance(
				customClassLoader, new Class[]{serviceClass}, new ServiceInvocationHandler(generatedServiceInstance, customServiceInstance, serviceClass)
		);
		if (ProxyAware.class.isInstance(customServiceInstance)) {
			((ProxyAware) customServiceInstance).setThisProxy(proxy);
		}
		return proxy;
	}

}
