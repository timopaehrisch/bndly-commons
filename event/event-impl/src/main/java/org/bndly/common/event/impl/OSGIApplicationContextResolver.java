package org.bndly.common.event.impl;

/*-
 * #%L
 * Event Impl
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

import org.bndly.common.event.api.ApplicationContextResolver;
import java.util.ArrayList;
import java.util.List;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ApplicationContextResolver.class, immediate = true)
public class OSGIApplicationContextResolver implements ApplicationContextResolver {
	private ComponentContext componentContext;

	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
	}
	@Deactivate
	public void deactivate() {
		componentContext = null;
	}
	
	@Override
	public <E> List<E> resolveObjectsOfType(Class<E> type) {
		Object[] services = componentContext.locateServices(type.getName());
		List<E> result = null;
		if (services != null) {
			for (Object service : services) {
				if (type.isInstance(service)) {
					if (result == null) {
						result = new ArrayList<>();
					}
					result.add((E) service);
				}
			}
		}
		return result;
	}
	
}
