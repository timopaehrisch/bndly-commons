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
import org.bndly.common.bpm.api.BusinessProcessData;
import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.bpm.api.NamedBusinessProcessData;
import org.bndly.common.bpm.api.ProcessDeploymentListener;
import org.bndly.common.bpm.api.ProcessDeploymentService;
import org.bndly.common.bpm.exception.ProcessDeploymentException;
import org.bndly.common.data.io.ReplayableInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;

public class ProcessDeploymentServiceImpl implements ProcessDeploymentService {

	private RepositoryService repositoryService;
	private BusinessProcessDataStore businessProcessDataStore;
	private final List<ProcessDeploymentListener> listeners;
	private final ReadWriteLock lock;
	private final String engineName;

	public ProcessDeploymentServiceImpl(String engineName, List<ProcessDeploymentListener> listeners, ReadWriteLock lock) {
		if (listeners == null) {
			throw new IllegalArgumentException("listeners is not allowed to be null");
		}
		this.listeners = listeners;
		if (engineName == null) {
			throw new IllegalArgumentException("engineName is not allowed to be null");
		}
		this.engineName = engineName;
		if (lock == null) {
			throw new IllegalArgumentException("lock is not allowed to be null");
		}
		this.lock = lock;
	}

	@Override
	public void undeploy(BusinessProcess p) {
		repositoryService.deleteDeployment(p.getDeploymentId());
		lock.readLock().lock();
		try {
			for (ProcessDeploymentListener listener : listeners) {
				listener.onProcessDefinitionUndeployment(engineName, p);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public BusinessProcess deploy(BusinessProcess p) throws ProcessDeploymentException {
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
		String processName = p.getName();
		deploymentBuilder = deploymentBuilder.name(processName);
		String resourceName = p.getResourceName();
		BusinessProcessData d = p.getData();
		if (d != null) {
			ReplayableInputStream inputStream;
			try {
				inputStream = ReplayableInputStream.replayIfPossible(d.getInputStream());
			} catch (IOException ex) {
				throw new ProcessDeploymentException(processName, "could not replay input stream to make sure that originally read data is not lost: " + ex.getMessage(), ex);
			}
			if (inputStream != null) {
				deploymentBuilder = deploymentBuilder.addInputStream(resourceName, inputStream);
				final Deployment deployment = deploymentBuilder.deploy();
				lock.readLock().lock();
				try {
					for (ProcessDeploymentListener listener : listeners) {
						listener.onProcessDefinitionDeployment(engineName, p);
					}
				} finally {
					lock.readLock().unlock();
				}
				ProcessDefinition processDefinition = getProcessDefinitionByDepolymentId(deployment.getId());
				assignImageData(processDefinition, true);
				return mapProcessDefinitionToBusinessProcess(processDefinition);
			} else {
				throw new ProcessDeploymentException(processName, "could not deploy business process, because the data object did not contain any process definition data");
			}
		} else {
			throw new ProcessDeploymentException(processName, "could not deploy business process, because the process definition did not contain a data object");
		}
	}

	@Override
	public BusinessProcess deploy(BusinessProcessData businessProcessData) throws ProcessDeploymentException {
		BusinessProcessImpl bp = new BusinessProcessImpl();
		String n = businessProcessData.getName();
		int i = n.lastIndexOf("/");
		int j = n.lastIndexOf(".");
		String processName;
		if (i > -1 && j > -1 && j > i) {
			processName = n.substring(i + 1, j);
		} else {
			if (i > -1) {
				processName = n.substring(i + 1);
			} else {
				processName = n.substring(0, j);
			}
		}
		bp.setResourceName(businessProcessData.getName());
		bp.setName(processName);
		bp.setData(businessProcessData);
		return deploy(bp);
	}

	@Override
	public List<BusinessProcess> getDeployedProcesses() {
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		List<ProcessDefinition> list = query.list();
		Map<String, BusinessProcess> processesByKey = new HashMap<>();
		for (ProcessDefinition processDefinition : list) {
			BusinessProcess p = mapProcessDefinitionToBusinessProcess(processDefinition);
			BusinessProcess mapEntry = processesByKey.get(p.getKey());
			if (mapEntry == null) {
				processesByKey.put(p.getKey(), p);
			} else {
				if (mapEntry.getVersion() < p.getVersion()) {
					processesByKey.put(p.getKey(), p);
				}
			}
		}
		List<BusinessProcess> result = new ArrayList<>();
		for (Map.Entry<String, BusinessProcess> entry : processesByKey.entrySet()) {
			BusinessProcess businessProcess = entry.getValue();
			result.add(businessProcess);
		}
		return result;
	}

	@Override
	public BusinessProcess getDeployedProcess(String processDefinitionId) {
		BusinessProcessImpl p = new BusinessProcessImpl();
		p.setId(processDefinitionId);
		return getDeployedProcess(p);
	}

	@Override
	public BusinessProcess getDeployedProcess(BusinessProcess p) {
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		if (p.getId() != null) {
			query.processDefinitionId(p.getId());
		}
		if (p.getDeploymentId() != null) {
			query.deploymentId(p.getDeploymentId());
		}
		ProcessDefinition processDefinition = query.singleResult();
		return mapProcessDefinitionToBusinessProcess(processDefinition);
	}
	
	private BusinessProcess mapProcessDefinitionToBusinessProcess(final ProcessDefinition processDefinition) {
		return new ActivitiProcessDefinitionAsBusinessProcess(processDefinition) {
			@Override
			public BusinessProcessData getData() {
				return getDefinitionData(processDefinition);
			}
			
			@Override
			public BusinessProcessData getImageData() {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
		};
	}

	private void assignImageData(ProcessDefinition processDefinition, boolean replace) {
		assignImageData(processDefinition, null, replace);
	}
	
	private void assignImageData(ProcessDefinition processDefinition, BusinessProcessImpl p, boolean replace) {
		String resourceName;
		resourceName = processDefinition.getDiagramResourceName();
		BusinessProcessData foundData = businessProcessDataStore.findByName(resourceName);
		if (foundData == null || replace) {
			ReplayableInputStream is;
			try {
				InputStream diagramData = repositoryService.getProcessDiagram(processDefinition.getId());
				if (diagramData != null) {
					is = ReplayableInputStream.newInstance(diagramData);
				} else {
					is = null;
				}
			} catch (IOException ex) {
				throw new IllegalStateException("could not create replayable input stream for process diagram: " + ex.getMessage(), ex);
			}
			if (is != null) {
				if (foundData != null) {
					foundData.setInputStream(is);
					foundData.setContentType("image/png");
					foundData.setUpdatedOn(new Date());
					assertDataNameIsNotEmpty(foundData, resourceName);
					businessProcessDataStore.update(foundData);
				} else {
					BusinessProcessData d = businessProcessDataStore.newInstance();
					d.setInputStream(is);
					d.setContentType("image/png");
					d.setCreatedOn(new Date());
					assertDataNameIsNotEmpty(d, resourceName);
					foundData = businessProcessDataStore.create(d);
				}
			}
		}
		if (p != null) {
			p.setImageData(foundData);
		}
	}

	private void assignImageDataWithReplace(ProcessDefinition processDefinition, BusinessProcessImpl p) {
		assignImageData(processDefinition, p, true);
	}

	private void assignImageDataWithoutReplace(ProcessDefinition processDefinition, BusinessProcessImpl p) {
		assignImageData(processDefinition, p, false);
	}

	private BusinessProcessData getDefinitionData(ProcessDefinition processDefinition) {
		String resourceName = processDefinition.getResourceName();
		BusinessProcessData foundData = businessProcessDataStore.findByName(resourceName);
		if (foundData == null) {
			ReplayableInputStream is;
			try {
				is = ReplayableInputStream.newInstance(repositoryService.getProcessModel(processDefinition.getId()));
			} catch (IOException ex) {
				throw new IllegalStateException("could not create replayable input stream for process definition data: " + ex.getMessage(), ex);
			}
			BusinessProcessData d = businessProcessDataStore.newInstance();
			d.setInputStream(is);
			d.setContentType("application/xml");
			d.setCreatedOn(new Date());
			foundData = businessProcessDataStore.create(d);
		}
		return foundData;
	}
	
	@Override
	public BusinessProcess getDeployedProcessByProcessName(String processName) {
		ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processName).latestVersion().singleResult();
		if (processDef == null) {
			return null;
		}
		return mapProcessDefinitionToBusinessProcess(processDef);
	}

	@Override
	public boolean isDeployedProcess(String processName) {
		ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processName).latestVersion().singleResult();
		boolean isDeployed = processDef != null;
		return isDeployed;
	}

	@Override
	public BusinessProcess autoDeploy(String processName) throws ProcessDeploymentException {
		String resourceName = processName + ".bpmn";
		final BusinessProcessData data = businessProcessDataStore.findByName(resourceName);
		if (data == null || data.getInputStream() == null) {
			return null;
		}
		Deployment deployment = repositoryService.createDeployment()
				.name(processName)
				.addInputStream(resourceName, data.getInputStream())
				.deploy();
		ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
		if (processDef != null) {
			assignImageData(processDef, true);
		}
		return new BusinessProcessWrapper(mapProcessDefinitionToBusinessProcess(processDef)) {
			@Override
			public BusinessProcessData getData() {
				return data;
			}

		};
	}

	private ProcessDefinition getProcessDefinitionByDepolymentId(String id) {
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(id).singleResult();
		return processDefinition;
	}

	private void assertDataNameIsNotEmpty(BusinessProcessData d, String resourceName) {
		if (NamedBusinessProcessData.class.isInstance(d)) {
			NamedBusinessProcessData nd = NamedBusinessProcessData.class.cast(d);
			if (nd.getName() == null) {
				nd.setName(resourceName);
			}
		}
	}

	public void setBusinessProcessDataStore(BusinessProcessDataStore businessProcessDataStore) {
		this.businessProcessDataStore = businessProcessDataStore;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}
}
