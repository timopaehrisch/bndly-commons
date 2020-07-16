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

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;

/**
 * the resteasy spring integrations seems to have troubles with the usage of the org.activiti.spring.ProcessEngineFactoryBean
 * this factory adapter will be used to allow the retrieval of return types for factory methods.
 */
public final class ServiceFactoryAdapter {
	private ServiceFactoryAdapter() {
	}
	public static RepositoryService getRepositoryService(ProcessEngine processEngine) {
		return processEngine.getRepositoryService();
	}
	public static RuntimeService getRuntimeService(ProcessEngine processEngine) {
		return processEngine.getRuntimeService();
	}
	public static TaskService getTaskService(ProcessEngine processEngine) {
		return processEngine.getTaskService();
	}
	public static HistoryService getHistoryService(ProcessEngine processEngine) {
		return processEngine.getHistoryService();
	}
	public static ManagementService getManagementService(ProcessEngine processEngine) {
		return processEngine.getManagementService();
	}
}
