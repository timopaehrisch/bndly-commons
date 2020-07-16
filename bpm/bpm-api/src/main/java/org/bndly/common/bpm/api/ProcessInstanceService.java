package org.bndly.common.bpm.api;

/*-
 * #%L
 * BPM API
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

import java.util.Collection;
import java.util.List;

public interface ProcessInstanceService {
    public ProcessInstance startProcess(String processName, Collection<ProcessVariable> processVariables, ReturnValueHandler returnValueHandler);
    public void resumeProcess(ProcessInstance instance, String eventName, ReturnValueHandler returnValueHandler);
    public void resumeProcess(ProcessInstance instance, String activityId, String messageId, ReturnValueHandler returnValueHandler);
    public ProcessInstance getProcessInstance(String processInstanceId);
    public List<ProcessInstance> listProcessInstances(int offset, int size);
    public void registerVariableAdapter(ProcessVariableAdapter processVariableAdapter);
    public void unregisterVariableAdapter(ProcessVariableAdapter processVariableAdapter);
//    public void registerListener(ProcessInvocationListener listener);
//    public void unregisterListener(ProcessInvocationListener listener);
}
