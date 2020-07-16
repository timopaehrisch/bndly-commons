package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ServiceReference {
	private final String name;
	private final Class serviceType;
	private final Object service;

	private ServiceReference(String name, Class serviceType, Object service) {
		this.name = name;
		this.serviceType = serviceType;
		this.service = service;
	}
	
	public static ServiceReference buildByName(String name, Object service) {
		if (name == null || service == null) {
			throw new IllegalArgumentException("name and service are not allowed to be null");
		}
		return new ServiceReference(name, null, service);
	}

	public static ServiceReference buildByType(Class serviceType, Object service) {
		if (serviceType == null || service == null) {
			throw new IllegalArgumentException("serviceType and service are not allowed to be null");
		}
		return new ServiceReference(null, serviceType, service);
	}

	public String getName() {
		return name;
	}

	public Class getServiceType() {
		return serviceType;
	}

	public Object getService() {
		return service;
	}
	
}
