package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

import java.util.HashMap;
import java.util.Map;

import org.bndly.common.bpm.impl.privateapi.EndEventHandler;
import org.bndly.common.bpm.impl.privateapi.ErrorEndEventHandler;
import org.bndly.common.bpm.api.EventHandler;
import org.bndly.common.bpm.impl.privateapi.StartEventHandler;


public class EventHandlerRegistry {
	
	private final Map<String, Map<Class<? extends EventHandler>,EventHandler>> handlers = new HashMap<>(); 
	
	public void register(String processId, EventHandler handler) {
		Map<Class<? extends EventHandler>, EventHandler> handlerMapForProcess = handlers.get(processId);
		if (handlerMapForProcess == null) {
			handlerMapForProcess = new HashMap<>();
			handlers.put(processId, handlerMapForProcess);
		}
		Class<? extends EventHandler> handlerClass = handler.getClass();
		if (EndEventHandler.class.isAssignableFrom(handlerClass)) {
			handlerMapForProcess.put(EndEventHandler.class, handler);
		}
		if (StartEventHandler.class.isAssignableFrom(handlerClass)) {
			handlerMapForProcess.put(StartEventHandler.class, handler);
		}
		if (ErrorEndEventHandler.class.isAssignableFrom(handlerClass)) {
			handlerMapForProcess.put(ErrorEndEventHandler.class, handler);
		}
	}
	
	public EndEventHandler getEndEventHandlerForProcessId(String processId) {
		return getHandlerForProcessByType(processId, EndEventHandler.class);
	}
	
	public ErrorEndEventHandler getErrorEndEventHandlerForProcessId(String processId) {
		return getHandlerForProcessByType(processId, ErrorEndEventHandler.class);
	}
	
	public StartEventHandler getStartEventHandlerForProcessId(String processId) {
		return getHandlerForProcessByType(processId, StartEventHandler.class);
	}

	public <E extends EventHandler> E getHandlerForProcessByType(String processId, Class<E> type) {
		Map<Class<? extends EventHandler>, EventHandler> handlerMapForProcess = handlers.get(processId);
		if (handlerMapForProcess == null) {
			return null;
		}

		EventHandler handler = handlerMapForProcess.get(type);
		if (handler == null) {
			return null;
		}
		
		return type.cast(handler);
	}
	
	public void removeEventHandlersForProcess(String processId) {
		handlers.remove(processId);
	}
}
