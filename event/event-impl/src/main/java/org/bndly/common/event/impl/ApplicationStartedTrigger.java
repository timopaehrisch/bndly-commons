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

import org.bndly.common.event.api.ApplicationStarted;
import org.bndly.common.event.api.EventBus;
import org.bndly.common.event.api.ListenerIterator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ApplicationStartedTrigger.class, immediate = true)
public class ApplicationStartedTrigger {

	@Reference
	private EventBus eventBus;

	public void onApplicationEvent() {
		eventBus.fireEvent(ApplicationStarted.class, new ListenerIterator<ApplicationStarted>() {
			@Override
			public void fireEvent(ApplicationStarted listener) {
				listener.applicationHasStarted();
			}
		});
	}

	@Activate
	public void init() {
		eventBus.registerEvent(ApplicationStarted.class);
		onApplicationEvent();
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

}
