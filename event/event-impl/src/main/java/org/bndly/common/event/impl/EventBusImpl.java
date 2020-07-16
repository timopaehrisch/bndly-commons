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
import org.bndly.common.event.api.EventBus;
import org.bndly.common.event.api.ListenerIterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = EventBus.class, immediate = true)
public class EventBusImpl implements EventBus {

	private final Map<Class<?>, List<Object>> listeners = new HashMap<>();
	@Reference
	private ApplicationContextResolver applicationContextResolver;

	@Override
	public <E> void registerEvent(Class<E> eventInterfaceType) {
		if (applicationContextResolver != null) {
			List list = applicationContextResolver.resolveObjectsOfType(eventInterfaceType);
			if (list == null) {
				list = Collections.EMPTY_LIST;
			}
			listeners.put(eventInterfaceType, list);
		}
	}

	@Override
	public <E> void fireEvent(Class<E> eventInterfaceType, ListenerIterator<E> iterator) {
		List<Object> list = listeners.get(eventInterfaceType);
		if (list != null) {
			for (Object object : list) {
				iterator.fireEvent((E) object);
			}
		} else {
			// no known listeners
		}
	}

	public void setApplicationContextResolver(ApplicationContextResolver applicationContextResolver) {
		this.applicationContextResolver = applicationContextResolver;
	}

}
