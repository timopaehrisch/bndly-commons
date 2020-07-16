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

import org.bndly.common.bpm.annotation.ProcessID;
import org.bndly.common.bpm.annotation.ProcessVariable;
import org.bndly.common.bpm.annotation.Resume;
import org.bndly.common.bpm.annotation.ReturnVariable;
import org.bndly.common.bpm.annotation.Signal;
import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.bpm.api.ContextResolver;
import org.bndly.common.bpm.api.ProcessInstanceService;
import org.bndly.common.bpm.datastore.impl.BusinessProcessDataStoreImpl;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.impl.FileSystemDataStoreImpl;
import org.bndly.common.reflection.FieldBeanPropertyWriter;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.bndly.common.bpm.annotation.ProcessDefinition;
import org.testng.annotations.AfterClass;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ProcessInvokerTest implements ProcessInstanceServiceProvider, ContextResolver, JavaDelegate {

	private ProcessInstanceService processInstanceService;
	private ProcessInvokerFactoryImpl processInvokerFactoryImpl;
	private final Map<Class,Object> contextMap = new HashMap<>();
	private Expression myField;
	private Object value;
	private ProcessEngine engine;

	@Override
	public ProcessInstanceService getInstanceServiceByEngineName(String engineName) {
		return processInstanceService;
	}

	@Override
	public <E> E getContext(Class<E> typeOfContextObject) {
		return (E) contextMap.get(typeOfContextObject);
	}

	@Override
	public <E> void setContext(Class<E> typeOfContextObject, E contextObject) {
		contextMap.put(typeOfContextObject, contextObject);
	}

	@Override
	public void clear() {
		contextMap.clear();
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// something happened in activiti
		value = myField.getValue(execution);
	}
	
	@ProcessDefinition("SimpleProcess")
	public static interface SimpleProcess {
		
		void start(@ProcessVariable(name = "myVar") String myVar);
	}
	
	@ProcessDefinition("SplitProcess")
	public static interface SplitProcess {

		@ReturnVariable(ReturnVariable.PROCESS_ID_VAR)
		String start();

		@Resume
		@Signal(activity = "eventgateway1", message = "onCase1")
		void performCase1(@ProcessID String processId);

		@Resume
		@Signal(activity = "eventgateway1", message = "onCase2")
		void performCase2(@ProcessID String processId);
	}
	
	@BeforeClass
	public void setup() {
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
		Map<Object, Object> beans = new HashMap<>();
		beans.put("someTask", this);
		config.setBeans(beans);
		engine = config.buildProcessEngine();
		
		ProcessInstanceServiceImpl processInstanceServiceImpl = new ProcessInstanceServiceImpl(Collections.EMPTY_LIST, new ReentrantReadWriteLock());
		processInstanceServiceImpl.setHistoryService(engine.getHistoryService());
		processInstanceServiceImpl.setRuntimeService(engine.getRuntimeService());
		processInstanceServiceImpl.setEventHandlerRegistry(new EventHandlerRegistry());
		processInstanceService = processInstanceServiceImpl;
		
		ProcessDeploymentServiceImpl processDeploymentService = new ProcessDeploymentServiceImpl("test", Collections.EMPTY_LIST, new ReentrantReadWriteLock());
		processInstanceServiceImpl.setProcessDeploymentService(processDeploymentService);
		processDeploymentService.setRepositoryService(engine.getRepositoryService());
		final FileSystemDataStoreImpl dataStore = new FileSystemDataStoreImpl();
		dataStore.setRoot(Paths.get("src", "test", "resources").toString());
		new FieldBeanPropertyWriter().set("ready", Boolean.TRUE, dataStore);
		BusinessProcessDataStore businessProcessDataStore = new BusinessProcessDataStoreImpl("test") {
			@Override
			protected DataStore getDataStore() {
				return dataStore;
			}
		};
		processDeploymentService.setBusinessProcessDataStore(businessProcessDataStore);
		
		processInvokerFactoryImpl = new ProcessInvokerFactoryImpl();
		new FieldBeanPropertyWriter().set("processInstanceServiceProvider", this, processInvokerFactoryImpl);
		new FieldBeanPropertyWriter().set("contextResolver", this, processInvokerFactoryImpl);
	}
	
	@AfterClass
	public void destroy() {
		engine.close();
	}
	
	@Test
	public void testSimpleInvocation() {
		SimpleProcess simpleProcess = processInvokerFactoryImpl.create(SimpleProcess.class, "test");
		simpleProcess.start("hello world");
		Assert.assertEquals(value, "hello world");
		simpleProcess.start("hello world!");
		Assert.assertEquals(value, "hello world!");
	}
	
	@Test
	public void testSplitInvocation() {
		SplitProcess splitProcess = processInvokerFactoryImpl.create(SplitProcess.class, "test");
		String processId = splitProcess.start();
		Assert.assertNotNull(processId);
		
		String processId2 = splitProcess.start();
		Assert.assertNotNull(processId2);
		
		Assert.assertNotEquals(processId, processId2);
		
		splitProcess.performCase1(processId);
		Assert.assertEquals(value, "assert case1");
		
		splitProcess.performCase2(processId2);
		Assert.assertEquals(value, "assert case2");
	}
}
