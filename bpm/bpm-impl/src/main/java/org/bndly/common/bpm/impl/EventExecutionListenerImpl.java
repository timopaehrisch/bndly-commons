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

import org.bndly.common.bpm.impl.privateapi.EndEventHandler;
import org.bndly.common.bpm.impl.privateapi.ErrorEndEventHandler;
import org.bndly.common.bpm.impl.privateapi.StartEventHandler;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

public class EventExecutionListenerImpl implements ExecutionListener {

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String eventName = execution.getEventName();
		String activityId = execution.getCurrentActivityId();
		String activityName = execution.getCurrentActivityName();
		boolean isEndEvent = "end".equals(eventName);
		boolean isStartEvent = "start".equals(eventName);
		if (isEndEvent || isStartEvent) {
			EventHandlerRegistry eventHandlerRegistry = ProcessInstanceServiceImpl.getCurrentEventHandlerRegistry();
			if (eventHandlerRegistry != null) {
				String uniqueProcessInstanceId = (String) execution.getVariable(ProcessInvoker.UNIQUE_PROCESS_INSTANCE_ID_VAR);
				if (isEndEvent) {
					ErrorEndEventActivityBehavior errorDefinition = extractErrorDefinition(execution);
					// is it an error end event?
					if (errorDefinition != null) {
						ErrorEndEventHandler handler = eventHandlerRegistry.getErrorEndEventHandlerForProcessId(uniqueProcessInstanceId);
						if (handler != null) {
							String errorCode = errorDefinition.getErrorCode();
							handler.handleErrorEnd(activityId, activityName, errorCode, execution);
						}
					} else {
						// regular end
						EndEventHandler handler = eventHandlerRegistry.getEndEventHandlerForProcessId(uniqueProcessInstanceId);
						if (handler != null) {
							handler.handleEnd(activityId, activityName, execution);
						}
					}
				} else if (isStartEvent) {
					StartEventHandler handler = eventHandlerRegistry.getStartEventHandlerForProcessId(uniqueProcessInstanceId);
					if (handler != null) {
						handler.handleStart(activityId, activityName, execution);
					}
				}
			}
		}
	}

	private ErrorEndEventActivityBehavior extractErrorDefinition(DelegateExecution execution) {
		if (ExecutionEntity.class.isAssignableFrom(execution.getClass())) {
			ActivityImpl activity = ((ExecutionEntity) execution).getActivity();
			ActivityBehavior behavior = activity.getActivityBehavior();
			if (ErrorEndEventActivityBehavior.class.isAssignableFrom(behavior.getClass())) {
				return ErrorEndEventActivityBehavior.class.cast(behavior);
			}
		}

		return null;

	}

}
