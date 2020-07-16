package org.bndly.rest.bpm.resources;

/*-
 * #%L
 * REST BPM Resource
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

import org.bndly.common.bpm.api.EngineProviderListener;
import org.bndly.common.bpm.api.ProcessInstance;
import org.bndly.common.bpm.api.ProcessInstanceService;
import org.bndly.common.bpm.api.ProcessVariable;
import org.bndly.common.bpm.api.TypedProcessVariable;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.bpm.beans.BusinessProcessInstance;
import org.bndly.rest.bpm.beans.BusinessProcessInstances;
import org.bndly.rest.bpm.beans.BusinessProcessVariable;
import org.bndly.rest.bpm.beans.BusinessProcessVariables;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { BusinessProcessInstanceResource.class, EngineProviderListener.class })
@Path("bpm/instances")
public class BusinessProcessInstanceResource extends AbstractProcessEngineAwareResource {

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	
	@Activate
	public void activate() {
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}

	@GET
	@AtomLinks({
		@AtomLink(rel = "businessProcessInstances", target = Services.class),
		@AtomLink(rel = "list", target = BusinessProcessInstance.class),
		@AtomLink(target = BusinessProcessInstances.class)
	})
	public Response listProcessInstances() {
		BusinessProcessInstances result = new BusinessProcessInstances();
		int offset = 0;
		int size = 20;
		for (Map.Entry<String, Engine> entry : knownEngines.entrySet()) {
			String engineName = entry.getKey();
			Engine engine = entry.getValue();
			ProcessInstanceService processInstanceService = engine.getInstanceService();
			List<ProcessInstance> instances = processInstanceService.listProcessInstances(offset, size);
			for (ProcessInstance processInstance : instances) {
				BusinessProcessInstance mappedInstance = map(processInstance, engineName);
				result.add(mappedInstance);
			}
		}
		return Response.ok(result);
	}

	@POST
	@AtomLink(target = BusinessProcessInstances.class)
	public Response startProcess(BusinessProcessInstance businessProcessInstance, @Meta Context context) {
		Collection<ProcessVariable> vars = null;
		String engineName = businessProcessInstance.getEngineName();
		Engine engine = assertEngineIsKnown(engineName);
		ProcessInstance instance = engine.getInstanceService().startProcess(businessProcessInstance.getProcessName(), vars, null);
		ResourceURIBuilder builder = context.createURIBuilder();
		builder.pathElement("bpm").pathElement("instances").pathElement(instance.getId());
		return Response.created(builder.build().asString());
	}

	@GET
	@Path("{engineName}/{pid}")
	@AtomLink(target = BusinessProcessInstance.class, parameters = {
		@Parameter(name = "engineName", expression = "${this.engineName}"),
		@Parameter(name = "pid", expression = "${this.id}")
	})
	public Response readProcessState(@PathParam("engineName") String engineName, @PathParam("pid") String pid) {
		Engine engine = assertEngineIsKnown(engineName);
		ProcessInstance instance = engine.getInstanceService().getProcessInstance(pid);
		return Response.ok(map(instance, engineName));
	}

	private Engine assertEngineIsKnown(String engineName) throws IllegalArgumentException {
		Engine engine = knownEngines.get(engineName);
		if (engine == null) {
			throw new IllegalArgumentException("no process engine with name " + engineName + " found");
		}
		return engine;
	}

	@POST
	@Path("{engineName}/{pid}")
	@AtomLink(rel = "signal", target = BusinessProcessInstance.class, parameters = {
		@Parameter(name = "engineName", expression = "${this.engineName}"),
		@Parameter(name = "pid", expression = "${this.id}")
	})
	public Response signalEventToProcess(@PathParam("engineName") String engineName, @PathParam("pid") String pid) {
		Engine engine = assertEngineIsKnown(engineName);
		ProcessInstance instance = engine.getInstanceService().getProcessInstance(pid);
		String eventName = null;
		engine.getInstanceService().resumeProcess(instance, eventName, null);
		return Response.NO_CONTENT;
	}

	private BusinessProcessInstance map(ProcessInstance processInstance, String engineName) {
		BusinessProcessInstance instance = new BusinessProcessInstance();
		instance.setId(processInstance.getId());
		instance.setProcessName(processInstance.getProcessName());
		instance.setStartTime(processInstance.getStartTime());
		instance.setEndTime(processInstance.getEndTime());
		instance.setVariables(mapVariables(processInstance.getVariables()));
		instance.setEngineName(engineName);
		return instance;
	}

	private BusinessProcessVariable mapVariable(ProcessVariable variable) {
		BusinessProcessVariable result = new BusinessProcessVariable();
		result.setName(variable.getName());
		if (TypedProcessVariable.class.isInstance(variable)) {
			result.setType(((TypedProcessVariable) variable).getType());
		}
		return result;
	}

	private BusinessProcessVariables mapVariables(Collection<ProcessVariable> variables) {
		BusinessProcessVariables result = new BusinessProcessVariables();
		if (variables != null) {
			for (ProcessVariable variable : variables) {
				result.add(mapVariable(variable));
			}
		}
		return result;
	}

}
