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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *This test is used to play around with Activiti to see how the different types of service tasks work.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ExpressionTest implements JavaDelegate {

	private boolean invoked;
	private boolean didStuff;
	private Expression exampleField;

	@Test
	public void testSimpleExpression() throws IOException {
		// this cast is really bad, because setBeans is not defined in the configuration interface
		ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
		Map<Object, Object> beans = new HashMap<>();
		beans.put("exampleBean", this);
		config.setBeans(beans);
		ProcessEngine engine = config.buildProcessEngine();
		try {
			Path definition = Paths.get("src", "test", "resources", "ExpressionProcess.bpmn");
			Path definition2 = Paths.get("src", "test", "resources", "DelegateExpressionProcess.bpmn");
			try (InputStream is = Files.newInputStream(definition, StandardOpenOption.READ)) {
				try (InputStream is2 = Files.newInputStream(definition2, StandardOpenOption.READ)) {
					Deployment deployment = engine.getRepositoryService().createDeployment()
							.addInputStream(definition.getFileName().toString(), is)
							.addInputStream(definition2.getFileName().toString(), is2)
							.deploy();

				}
			}

			invoked = false;
			didStuff = false;
			exampleField = null;
			ProcessInstance instance = engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionKey("expressionProcess").start();
			Assert.assertTrue(didStuff,"doStuff was not invoked");
			Assert.assertFalse(invoked,"java delegate was invoked");
			Assert.assertEquals(engine.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).variableName("deVar").singleResult().getValue(), 5);

			invoked = false;
			didStuff = false;
			exampleField = null;
			instance = engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionKey("delegateExpressionProcess").start();
			Assert.assertFalse(didStuff,"doStuff was invoked");
			Assert.assertTrue(invoked,"java delegate was not invoked");
			Assert.assertEquals(engine.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).variableName("deVar").singleResult().getValue(), 8);
		} finally {
			engine.close();
		}
	}

	public int doStuff() {
		this.didStuff = true;
		return 5;
	}
	
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		Assert.assertNotNull(exampleField);
		Assert.assertEquals(exampleField.getValue(execution), "exampleFieldValue");
		this.invoked = true;
		execution.setVariable("deVar", 8);
	}
}
