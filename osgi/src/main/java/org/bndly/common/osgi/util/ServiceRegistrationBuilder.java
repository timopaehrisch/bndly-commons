package org.bndly.common.osgi.util;

/*-
 * #%L
 * OSGI Utilities
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

import org.osgi.framework.Constants;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ServiceRegistrationBuilder<E> {

	private final E service;
	private final Class<E> serviceInterface;
	private final List<Class> serviceInterfaces = new ArrayList<>();
	private final Dictionary<String, Object> props = new Hashtable<>();

	private ServiceRegistrationBuilder(Class<E> serviceInterface, E service) {
		if (service == null) {
			throw new IllegalArgumentException("service is not allowed to be null");
		}
		this.service = service;
		if (serviceInterface == null) {
			throw new IllegalArgumentException("serviceInterface is not allowed to be null");
		}
		this.serviceInterface = serviceInterface;
	}

	public static <F> ServiceRegistrationBuilder<F> newInstance(Class<F> serviceInterface, F service) {
		return new ServiceRegistrationBuilder<F>(serviceInterface, service);
	}
	
	public static <F> ServiceRegistrationBuilder<F> newInstance(F service) {
		Class<? extends F> serviceInterface = (Class<? extends F>) service.getClass();
		return newInstance((Class) serviceInterface, service);
	}
	
	public final ServiceRegistrationBuilder serviceInterface(Class<?> serviceInterface) {
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("service is not implementing " + serviceInterface.getName());
		}
		serviceInterfaces.add(serviceInterface);
		return this;
	}
	
	public final ServiceRegistrationBuilder<E> pid(String pid) {
		return property(Constants.SERVICE_PID, pid);
	}
	
	public final ServiceRegistrationBuilder<E> property(String property, Object value) {
		props.put(property, value);
		return this;
	}
	
	public ServiceRegistration<E> register(BundleContext context) {
		if (props.get(Constants.SERVICE_PID) == null) {
			pid(serviceInterface.getName());
		}
		if (serviceInterfaces.isEmpty()) {
			ServiceRegistration<E> reg = context.registerService(serviceInterface, service, props);
			return reg;
		} else {
			String[] interfacesTemp = new String[serviceInterfaces.size()];
			for (int i = 0; i < serviceInterfaces.size(); i++) {
				Class get = serviceInterfaces.get(i);
				interfacesTemp[i] = get.getName();
			}
			ServiceRegistration reg = context.registerService(interfacesTemp, service, props);
			return reg;
		}
	}
}
