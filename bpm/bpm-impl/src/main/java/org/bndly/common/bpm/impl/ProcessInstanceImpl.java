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

import org.bndly.common.bpm.api.ProcessInstance;
import org.bndly.common.bpm.api.ProcessVariable;
import java.util.Collection;
import java.util.Date;

public class ProcessInstanceImpl implements ProcessInstance {

	private final String id;
	private final String processName;
	private Date startTime;
	private Date endTime;
	private Collection<ProcessVariable> variables;

	public ProcessInstanceImpl(String id, String processName) {
		this.id = id;
		this.processName = processName;
	}

	@Override
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Override
	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	@Override
	public String getProcessName() {
		return processName;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Collection<ProcessVariable> getVariables() {
		return variables;
	}

	public void setVariables(Collection<ProcessVariable> variables) {
		this.variables = variables;
	}

}
