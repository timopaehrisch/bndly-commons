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

import org.bndly.common.bpm.annotation.ReturnVariable;
import org.bndly.common.bpm.api.BusinessProcess;
import org.bndly.common.bpm.api.ProcessDeploymentService;
import org.bndly.common.bpm.api.ProcessInstance;
import org.bndly.common.bpm.api.ProcessInstanceService;
import org.bndly.common.bpm.api.ProcessInvocationListener;
import org.bndly.common.bpm.api.ProcessVariable;
import org.bndly.common.bpm.api.ProcessVariableAdapter;
import org.bndly.common.bpm.api.ReturnValueHandler;
import org.bndly.common.bpm.api.TypedProcessVariable;
import org.bndly.common.bpm.exception.ProcessDeploymentException;
import org.bndly.common.bpm.impl.privateapi.UltimateEventHandler;
import org.bndly.common.bpm.exception.ProcessErrorException;
import org.bndly.common.bpm.exception.ProcessInvocationException;

import static org.bndly.common.bpm.impl.ProcessInvoker.PROCESS_INSTANCE_PROCESS_NAME_VAR;
import static org.bndly.common.bpm.impl.ProcessInvoker.UNIQUE_PROCESS_INSTANCE_ID_VAR;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessInstanceServiceImpl implements ProcessInstanceService, ProcessVariableAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessInstanceServiceImpl.class);

	private HistoryService historyService;
	private RuntimeService runtimeService;
	private EventHandlerRegistry eventHandlerRegistry;
	private ProcessDeploymentService processDeploymentService;
	private final List<ProcessVariableAdapter> processVariableAdapters = new ArrayList<>();
	private final List<ProcessInvocationListener> listeners;
	private final ReadWriteLock lock;
	private static final ThreadLocal<EventHandlerRegistry> currentEventHandlerRegistry = new ThreadLocal<>();

	public ProcessInstanceServiceImpl(List<ProcessInvocationListener> listeners, ReadWriteLock lock) {
		this.listeners = listeners;
		this.lock = lock;
	}

	/**
	 * returns the current event handler registry based on the current thread.
	 *
	 * @return the current event handler registry or null, when called from another thread than the one that called startProcess or resumeProcess.
	 */
	public static EventHandlerRegistry getCurrentEventHandlerRegistry() {
		return currentEventHandlerRegistry.get();
	}

	@Override
	public void registerVariableAdapter(ProcessVariableAdapter processVariableAdapter) {
		if (processVariableAdapter != null) {
			processVariableAdapters.add(processVariableAdapter);
		}
	}

	@Override
	public void unregisterVariableAdapter(ProcessVariableAdapter processVariableAdapter) {
		Iterator<ProcessVariableAdapter> iterator = processVariableAdapters.iterator();
		while (iterator.hasNext()) {
			ProcessVariableAdapter next = iterator.next();
			if (next == processVariableAdapter) {
				iterator.remove();
			}
		}
	}

	@Override
	public List<ProcessInstance> listProcessInstances(int offset, int size) {
		List<ProcessInstance> result = new ArrayList<>();
		List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().desc().listPage(offset, size);
		if (historicInstances != null) {
			for (HistoricProcessInstance historicProcessInstance : historicInstances) {
				result.add(mapActivitiProcessInstance(historicProcessInstance));
			}
		}
		return result;
	}

	@Override
	public ProcessInstanceImpl startProcess(String processName, Collection<ProcessVariable> processVariables, ReturnValueHandler returnValueHandler) {
		try {
			currentEventHandlerRegistry.set(eventHandlerRegistry);
			Map<String, Object> params = convertProcessVariablesToMap(processVariables);
			BusinessProcess deployedProcess = processDeploymentService.getDeployedProcessByProcessName(processName);
			boolean isDeployedProcess = deployedProcess != null;
			String id;
			if (!isDeployedProcess) {
				LOG.info("auto deploying process {}, because it is not deployed already", processName);
				try {
					deployedProcess = processDeploymentService.autoDeploy(processName);
					if (deployedProcess == null) {
						throw new ProcessInvocationException("could not auto deploy process '" + processName + "'");
					}
				} catch (ProcessDeploymentException e) {
					throw new ProcessInvocationException("could not auto deploy process '" + processName + "'", e);
				}
			}
			id = deployedProcess.getId();

			String uniqueProcessInstanceId = UUID.randomUUID().toString();
			params.put(UNIQUE_PROCESS_INSTANCE_ID_VAR, uniqueProcessInstanceId);
			params.put(PROCESS_INSTANCE_PROCESS_NAME_VAR, processName);
			final ObjectReferenceHolder<String> errorEndHolder = new ObjectReferenceHolder<>();
			final ObjectReferenceHolder<String> processInstanceIdHolder = new ObjectReferenceHolder<>();
			final ObjectReferenceHolder<Object> regularEndHolder = new ObjectReferenceHolder<>();

			String returnVariableName = null;
			Class<?> returnType = Void.class;
			if (returnValueHandler != null) {
				returnVariableName = returnValueHandler.getReturnVariableName();
				returnType = returnValueHandler.getReturnVariableType();
			}

			boolean ended = false;
			boolean suspended = false;
			String activityId = null;
			try {
				registerEventHandler(uniqueProcessInstanceId, errorEndHolder, processInstanceIdHolder, returnVariableName, processName, returnType, regularEndHolder);
				lock.readLock().lock();
				try {
					for (ProcessInvocationListener listener : listeners) {
						try {
							listener.beforeInvocation();
						} catch (Exception e) {
							LOG.error("process invocation listener threw exception: " + e.getMessage(), e);
						}
					}
				} finally {
					lock.readLock().unlock();
				}
				org.activiti.engine.runtime.ProcessInstance processInstance = runtimeService.startProcessInstanceById(id, params);
				ended = processInstance.isEnded();
				suspended = processInstance.isSuspended();
				activityId = processInstance.getActivityId();

				// If the process stopped at an event gateway, the activityId of processInstance will be null.
				// Instead, the processInstance will be marked as scoped and it will contain a single execution of type ScopeExecution,
				// holsing the desired activity id.
				if(activityId == null) {
					ExecutionEntity executionEntity = ExecutionEntity.class.cast(processInstance);
					if(executionEntity.isScope()) {
						List<ExecutionEntity> executions = executionEntity.getExecutions();
						if(executions != null && executions.size() == 1) {
							activityId = executions.get(0).getActivityId();
						}
					}
				}
			} catch (BpmnError e) {
				if (errorEndHolder.isNull()) {
					errorEndHolder.setRef(e.getErrorCode());
				}
				throwProcessExceptionOnErrorEnd(errorEndHolder, processInstanceIdHolder, processName);
			} finally {
				eventHandlerRegistry.removeEventHandlersForProcess(uniqueProcessInstanceId);
				lock.readLock().lock();
				try {
					for (ProcessInvocationListener listener : listeners) {
						try {
							listener.afterInvocation();
						} catch (Exception e) {
							LOG.error("process invocation listener threw exception: " + e.getMessage(), e);
						}
					}
				} finally {
					lock.readLock().unlock();
				}
			}

			throwProcessExceptionOnErrorEnd(errorEndHolder, processInstanceIdHolder, processName);

			ProcessInstanceImpl proc = new ProcessInstanceImpl(processInstanceIdHolder.getRef(), processName);
			proc.setVariables(new ArrayList<ProcessVariable>());
			Object returnObject;
			if (!returnType.equals(Void.class) && !returnType.equals(void.class)) {
				// for a quick match, we could check by instance. to be sure, check with equals
				if (returnVariableName == ReturnVariable.PROCESS_ID_VAR || ReturnVariable.PROCESS_ID_VAR.equals(returnVariableName)) {
					returnObject = processInstanceIdHolder.getRef();
				} else if (regularEndHolder.isNull()) {
					// if the end value has not been set yet by the eventHandler, then do it now!
					HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
							.processInstanceId(processInstanceIdHolder.getRef())
							.finished()
							.includeProcessVariables()
							.singleResult(); // TODO: there might be more than one result. handle that case!
					Map<String, Object> variables = null;
					if (historicInstance != null) {
						variables = historicInstance.getProcessVariables();
					} else {
						List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceIdHolder.getRef()).list();
						if (!executions.isEmpty()) {
							for (Execution execution : executions) {
								String executionActivityId = execution.getActivityId();
								if (executionActivityId != null && executionActivityId.equals(activityId)) {
									variables = runtimeService.getVariables(execution.getId());
									break;
								}
							}
						}
					}
					if (variables != null) {
						for (Map.Entry<String, Object> entry : variables.entrySet()) {
							String variableName = entry.getKey();
							Object variableValue = entry.getValue();
							proc.getVariables().add(new ProcessVariableImpl(variableName, variableValue));
						}
						writeReturnValueToReferenceHolder(variables, returnVariableName, returnType, regularEndHolder, processName);
					}
					returnObject = regularEndHolder.getRef();
				} else {
					returnObject = regularEndHolder.getRef();
				}
			} else {
				returnObject = null;
			}

			if (returnValueHandler != null) {
				returnValueHandler.didReturn(returnObject, proc);
			}
			return proc;
		} finally {
			currentEventHandlerRegistry.remove();
		}
	}

	private void throwProcessExceptionOnErrorEnd(
			final ObjectReferenceHolder<String> errorEndHolder, 
			ObjectReferenceHolder<String> processInstanceIdHolder, 
			String processName
	) throws ProcessErrorException {
		if (!errorEndHolder.isNull()) {
			String processInstanceId = processInstanceIdHolder.getRef();
			String message = "instance " + processInstanceId + " of process " + processName + " ran into an error end state. state name: " + errorEndHolder.getRef();
			throw new ProcessErrorException(message, errorEndHolder.getRef(), processName, processInstanceId);
		}
	}

	private interface ResumeProcessAction {
		void invoke(RuntimeService runtimeService, String executionId);
	}

	@Override
	public void resumeProcess(ProcessInstance instance, String eventName, ReturnValueHandler returnValueHandler) {
		final ResumeProcessAction action = new ResumeProcessAction() {
			@Override
			public void invoke(RuntimeService runtimeService, String executionId) {
				runtimeService.signal(executionId);
			}
		};

		resumeProcess(instance, eventName, action, returnValueHandler);
	}

	@Override
	public void resumeProcess(ProcessInstance instance, String activityId, final String messageId, ReturnValueHandler returnValueHandler) {

		final ResumeProcessAction action = new ResumeProcessAction() {
			@Override
			public void invoke(RuntimeService runtimeService, String executionId) {
				runtimeService.messageEventReceived(messageId, executionId);
			}
		};

		resumeProcess(instance, activityId, action, returnValueHandler);
	}

	private void resumeProcess(ProcessInstance instance, String activityId, ResumeProcessAction resumeAction, ReturnValueHandler returnValueHandler) {
		try {
			currentEventHandlerRegistry.set(eventHandlerRegistry);
			final ObjectReferenceHolder<String> errorEndHolder = new ObjectReferenceHolder<>();
			final ObjectReferenceHolder<String> processInstanceIdHolder = new ObjectReferenceHolder<>();
			final ObjectReferenceHolder<Object> regularEndHolder = new ObjectReferenceHolder<>();
			final String processInstanceId = instance.getId();
			String returnVariableName = null;
			Class<?> returnType = Void.class;
			if (returnValueHandler != null) {
				returnVariableName = returnValueHandler.getReturnVariableName();
				returnType = returnValueHandler.getReturnVariableType();
			}

			Execution execution = runtimeService.createExecutionQuery()
					.processInstanceId(processInstanceId).activityId(activityId)
					.singleResult();
			String executionId = execution.getId();
			String uniqueProcessInstanceId = (String) runtimeService.getVariable(executionId, UNIQUE_PROCESS_INSTANCE_ID_VAR);
			String processName = (String) runtimeService.getVariable(executionId, PROCESS_INSTANCE_PROCESS_NAME_VAR);

			registerEventHandler(uniqueProcessInstanceId, errorEndHolder, processInstanceIdHolder, returnVariableName, processName, returnType, regularEndHolder);

			Map<String, Object> params = convertProcessVariablesToMap(instance.getVariables());
			runtimeService.setVariables(executionId, params);

			try {
				lock.readLock().lock();
				try {
					for (ProcessInvocationListener listener : listeners) {
						try {
							listener.beforeInvocation();
						} catch (Exception e) {
							LOG.error("process invocation listener threw exception: " + e.getMessage(), e);
						}
					}
				} finally {
					lock.readLock().unlock();
				}
				// now that all variables in the process are set, the process can resume its execution
				resumeAction.invoke(runtimeService, executionId);
			} catch (BpmnError e) {
				if (errorEndHolder.isNull()) {
					errorEndHolder.setRef(e.getErrorCode());
				}
				throwProcessExceptionOnErrorEnd(errorEndHolder, processInstanceIdHolder, processName);
			} finally {
				// execution will be synchronous, hence, now we can remove the eventHandlers, because we wont be notified of any other end events
				eventHandlerRegistry.removeEventHandlersForProcess(uniqueProcessInstanceId);
				lock.readLock().lock();
				try {
					for (ProcessInvocationListener listener : listeners) {
						try {
							listener.afterInvocation();
						} catch (Exception e) {
							LOG.error("process invocation listener threw exception: " + e.getMessage(), e);
						}
					}
				} finally {
					lock.readLock().unlock();
				}
			}

			// because we wrap the process as a java method invocation, we will now look in the process instance for the variables we were looking for.
			throwProcessExceptionOnErrorEnd(errorEndHolder, processInstanceIdHolder, processName);

			ProcessInstanceImpl proc = new ProcessInstanceImpl(processInstanceIdHolder.getRef(), processName);
			proc.setVariables(new ArrayList<ProcessVariable>());
			Object returnObject;
			if (!returnType.equals(Void.class) && !returnType.equals(void.class)) {
				if (returnVariableName == ReturnVariable.PROCESS_ID_VAR || ReturnVariable.PROCESS_ID_VAR.equals(returnVariableName)) {
					returnObject = processInstanceIdHolder.getRef();
				} else if (regularEndHolder.isNull()) {
					// if the end value has not been set yet by the eventHandler, then do it now!
					Map<String, Object> variables;
					try {
						variables = runtimeService.getVariables(execution.getId());
					} catch (ActivitiObjectNotFoundException e) {
						HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
								.processInstanceId(instance.getId())
								.includeProcessVariables()
								.singleResult();
						variables = historicInstance.getProcessVariables();
					}
					for (Map.Entry<String, Object> entry : variables.entrySet()) {
						String variableName = entry.getKey();
						Object variableValue = entry.getValue();
						proc.getVariables().add(new ProcessVariableImpl(variableName, variableValue));
					}
					writeReturnValueToReferenceHolder(variables, returnVariableName, returnType, regularEndHolder, processName);
					returnObject = regularEndHolder.getRef();
				} else {
					returnObject = regularEndHolder.getRef();
				}
			} else {
				returnObject = null;
			}

			if (returnValueHandler != null) {
				returnValueHandler.didReturn(returnObject, proc);
			}
		} finally {
			currentEventHandlerRegistry.remove();
		}
	}

	@Override
	public ProcessInstanceImpl getProcessInstance(String processInstanceId) {
		org.activiti.engine.runtime.ProcessInstance proc = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		if (proc == null) {
			HistoricProcessInstance hProc = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
			return mapActivitiProcessInstance(hProc);
		}
		return mapActivitiProcessInstance(proc);
	}

	private void registerEventHandler(
			String uniqueProcessInstanceId, 
			final ObjectReferenceHolder<String> errorEndHolder, 
			final ObjectReferenceHolder<String> processInstanceIdHolder, 
			final String returnVariableName, 
			final String processName, 
			final Class<?> returnType, 
			final ObjectReferenceHolder<Object> regularEndHolder
	) {
		eventHandlerRegistry.register(uniqueProcessInstanceId, new UltimateEventHandler() {
			@Override
			public void handleErrorEnd(String endActivityId, String endActivityName, String errorCode, DelegateExecution execution) {
				errorEndHolder.setRef(errorCode);
			}

			@Override
			public void handleStart(String startActivityId, String startActivityName, DelegateExecution execution) {
				String processInstanceId = execution.getProcessInstanceId();
				processInstanceIdHolder.setRef(processInstanceId);
			}

			@Override
			public void handleEnd(String endActivityId, String endActivityName, DelegateExecution execution) {
				writeReturnValueToReferenceHolder(execution.getVariables(), returnVariableName, returnType, regularEndHolder, processName);
			}
		});
	}

	private void writeReturnValueToReferenceHolder(
			Map<String, Object> vars, 
			String returnVariableName, 
			Class<?> returnType, ObjectReferenceHolder<Object> regularEndHolder, 
			String processName
	) throws ProcessInvocationException {
		if (returnVariableName != null) {
			if (!vars.containsKey(returnVariableName)) {
				throw new ProcessInvocationException("could not find the variable " + returnVariableName + " for process " + processName);
			}
			Object var = vars.get(returnVariableName);
			if (var != null) {
				if (!returnType.isAssignableFrom(var.getClass())) {
					throw new ProcessInvocationException(
						"could find the variable " + returnVariableName + " for process " + processName
						+ ", but the type " + var.getClass().getName() + " was not assignable to the returntype "
						+ returnType.getName()
					);
				}
			}
			regularEndHolder.setRef(var);

		} else if (!Void.class.equals(returnType) && !void.class.equals(returnType)) {
			Object tmp = null;
			boolean found = false;
			for (String key : vars.keySet()) {
				Object var = vars.get(key);
				if (var != null) {
					if (returnType.isAssignableFrom(var.getClass())) {
						if (tmp == null) {
							found = true;
							tmp = var;
						} else {
							throw new ProcessInvocationException(
								"could not find a unique return object for process " + processName 
								+ ", because there were multiple instances by the type " + returnType.getName()
							);
						}
					}
				}
			}
			if (!found) {
				throw new ProcessInvocationException("could not find a any return object for process " + processName + " with the type " + returnType.getName());
			}
			regularEndHolder.setRef(tmp);
		}
	}

	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public void setEventHandlerRegistry(EventHandlerRegistry eventHandlerRegistry) {
		this.eventHandlerRegistry = eventHandlerRegistry;
	}

	public void setProcessDeploymentService(ProcessDeploymentService processDeploymentService) {
		this.processDeploymentService = processDeploymentService;
	}

	private Map<String, Object> convertProcessVariablesToMap(Collection<ProcessVariable> processVariables) {
		Map<String, Object> result = new HashMap<>();
		if (processVariables != null) {
			for (ProcessVariable processVariable : processVariables) {
				if (processVariable.getName() != null) {
					result.put(processVariable.getName(), processVariable.getValue());
				}
			}
		}
		return result;
	}

	private Collection<ProcessVariable> convertExecutionVariablesToProcessVariables(Map<String, Object> executionVariables) {
		List<ProcessVariable> result = new ArrayList<>();
		if (executionVariables != null) {
			for (Map.Entry<String, Object> entry : executionVariables.entrySet()) {
				String name = entry.getKey();
				Object value = entry.getValue();
				result.add(new ProcessVariableImpl(name, value));
			}
		}
		return result;
	}

	private ProcessInstanceImpl mapActivitiProcessInstance(HistoricProcessInstance processInstance) {
		BusinessProcess processDef = processDeploymentService.getDeployedProcess(processInstance.getProcessDefinitionId());
		final ProcessInstanceImpl instance = new ProcessInstanceImpl(processInstance.getId(), processDef.getName());
		instance.setStartTime(processInstance.getStartTime());
		instance.setEndTime(processInstance.getEndTime());
		instance.setVariables(new LazyCollection<ProcessVariable>() {
			@Override
			protected void doInit() {
				List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).list();
				for (final HistoricVariableInstance historicVariableInstance : historicVariables) {
					add(new TypedProcessVariable() {
						@Override
						public String getName() {
							return historicVariableInstance.getVariableName();
						}

						@Override
						public String getType() {
							return historicVariableInstance.getVariableTypeName();
						}

						@Override
						public Object getValue() {
							return historicVariableInstance.getValue();
						}
					});
				}
			}
		});
		return instance;
	}
	
	private ProcessInstanceImpl mapActivitiProcessInstance(org.activiti.engine.runtime.ProcessInstance processInstance) {
		ProcessInstanceImpl instance = new ProcessInstanceImpl(processInstance.getId(), processInstance.getProcessDefinitionId());
		Execution execution = runtimeService.createExecutionQuery().processInstanceId(instance.getId()).singleResult();
		Map<String, Object> executionVariables = runtimeService.getVariables(execution.getId());
		Collection<ProcessVariable> vars = convertExecutionVariablesToProcessVariables(executionVariables);
		instance.setVariables(vars);
		return instance;
	}

	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}

	@Override
	public boolean doesSupport(ProcessVariable variable) {
		return true;
	}

	@Override
	public String getType(ProcessVariable variable) {
		for (ProcessVariableAdapter processVariableAdapter : processVariableAdapters) {
			if (processVariableAdapter.doesSupport(variable)) {
				return processVariableAdapter.getType(variable);
			}
		}
		Object v = variable.getValue();
		if (v == null) {
			return null;
		} else {
			return v.getClass().getSimpleName();
		}
	}

}
