package org.bndly.de.rest.websocket.api;

/*-
 * #%L
 * REST Websocket API
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
public final class EventHolder {
	private final Event event;
	private final Object data;

	public static EventHolder create(String namespace, String name) {
		return create(namespace, name, null);
	}
	
	public static EventHolder create(String namespace, String name, Object data) {
		return new EventHolder(new Event(namespace, name), data);
	}
	
	private EventHolder(Event event, Object data) {
		this.event = event;
		this.data = data;
	}

	public Object getData() {
		return data;
	}

	public Event getEvent() {
		return event;
	}
	
}
