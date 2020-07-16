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

import org.bndly.common.bpm.api.BusinessProcess;
import org.activiti.engine.repository.ProcessDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ActivitiProcessDefinitionAsBusinessProcess implements BusinessProcess {
	private final ProcessDefinition processDefinition;

	public ActivitiProcessDefinitionAsBusinessProcess(ProcessDefinition processDefinition) {
		if (processDefinition == null) {
			throw new IllegalArgumentException("processDefinition is not allowed to be null");
		}
		this.processDefinition = processDefinition;
	}

	@Override
	public String getName() {
		return processDefinition.getName();
	}

	@Override
	public String getKey() {
		return processDefinition.getKey();
	}

	@Override
	public String getId() {
		return processDefinition.getId();
	}

	@Override
	public Integer getVersion() {
		return processDefinition.getVersion();
	}

	@Override
	public String getDeploymentId() {
		return processDefinition.getDeploymentId();
	}

	@Override
	public String getResourceName() {
		return processDefinition.getResourceName();
	}

	@Override
	public String getCategory() {
		return processDefinition.getCategory();
	}

	@Override
	public String getDiagramResourceName() {
		return processDefinition.getDiagramResourceName();
	}

	@Override
	public String getDescription() {
		return processDefinition.getDescription();
	}
	
}
