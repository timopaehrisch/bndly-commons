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

import org.bndly.common.bpm.api.BusinessProcess;
import org.bndly.common.bpm.api.BusinessProcessData;
import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.bpm.api.BusinessProcessDataStoreFactory;
import org.bndly.common.bpm.api.EngineProviderListener;
import org.bndly.common.bpm.api.ProcessDeploymentListener;
import org.bndly.common.bpm.exception.ProcessDeploymentException;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.bpm.beans.BusinessProcessDefinition;
import org.bndly.rest.bpm.beans.BusinessProcessDefinitions;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { BusinessProcessDefinitionResource.class, EngineProviderListener.class, ProcessDeploymentListener.class })
@Path("bpm/definitions")
public class BusinessProcessDefinitionResource extends AbstractProcessEngineAwareResource implements ProcessDeploymentListener {

	@Reference
	private BusinessProcessDataStoreFactory businessProcessDataStoreFactory;

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	
	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	
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
		@AtomLink(rel = "businessProcessDefinitions", target = Services.class),
		@AtomLink(rel = "list", target = BusinessProcessDefinition.class),
		@AtomLink(rel = "self", target = BusinessProcessDefinitions.class)
	})
	public Response listDeployedProcesses() {
		BusinessProcessDefinitions defs = new BusinessProcessDefinitions();
		for (Map.Entry<String, Engine> entry : knownEngines.entrySet()) {
			Engine engine = entry.getValue();
			List<BusinessProcess> processes = engine.getDeploymentService().getDeployedProcesses();
			if (processes != null) {
				for (BusinessProcess businessProcess : processes) {
					BusinessProcessDefinition bpd = businessProcessToBusinessProcessDefinition(businessProcess, engine);
					defs.add(bpd);
				}
			}
		}
		return Response.ok(defs);
	}
	
	@POST
	@Path("findAll")
	@AtomLink(rel = "findAll", target = BusinessProcessDefinitions.class)
	public Response findAll(BusinessProcessDefinition processDefinition, @Meta Context context) {
		return Response.seeOther(context.createURIBuilder().pathElement("bpm").pathElement("definitions").build().asString());
	}

	@POST
	@AtomLink(target = BusinessProcessDefinitions.class)
	public Response deployProcess(BusinessProcessDefinition businessProcessDefinition, @Meta Context context) throws ProcessDeploymentException {
		String engineName = businessProcessDefinition.getEngineName();
		Engine engine = assertEngineIsKnown(engineName);
		BusinessProcess p = businessProcessDefinitionToBusinessProcess(businessProcessDefinition, engine.getBusinessProcessDataStore());
		p = engine.getDeploymentService().deploy(p);
		ResourceURIBuilder builder = context.createURIBuilder();
		builder.pathElement("bpm").pathElement("definitions").pathElement(engineName).pathElement(p.getId());
		return Response.created(builder.build().asString());
	}

	private Engine assertEngineIsKnown(String engineName) {
		if (engineName == null) {
			throw new IllegalArgumentException("engineName is required, when deploying a process");
		}
		Engine engine = knownEngines.get(engineName);
		if (engine == null) {
			throw new IllegalStateException("engine with name " + engineName + " could not be found.");
		}
		return engine;
	}

	@GET
	@Path("{engineName}/{id}")
	@AtomLink(target = BusinessProcessDefinition.class, parameters = {
		@Parameter(name = "engineName", expression = "${this.engineName}"),
		@Parameter(name = "id", expression = "${this.id}")
	})
	public Response readDeployedProcess(@PathParam("engineName") String engineName, @PathParam("id") String id) {
		Engine engine = assertEngineIsKnown(engineName);
		BusinessProcess b = engine.getDeploymentService().getDeployedProcess(id);
		if (b == null) {
			return Response.status(404);
		}
		BusinessProcessDefinition bpd = businessProcessToBusinessProcessDefinition(b, engine);
		return Response.ok(bpd);
	}

	@DELETE
	@Path("{engineName}/{id}")
	@AtomLink(target = BusinessProcessDefinition.class, parameters = {
		@Parameter(name = "engineName", expression = "${this.engineName}"),
		@Parameter(name = "id", expression = "${this.id}")
	})
	public Response undeployProcess(@PathParam("engineName") String engineName, @PathParam("id") String id) {
		Engine engine = assertEngineIsKnown(engineName);
		BusinessProcess p = engine.getDeploymentService().getDeployedProcess(id);
		if (p != null) {
			engine.getDeploymentService().undeploy(p);
		}
		return Response.NO_CONTENT;
	}

	private BusinessProcessDefinition businessProcessToBusinessProcessDefinition(BusinessProcess businessProcess, Engine engine) {
		BusinessProcessDefinition bpd = new BusinessProcessDefinition();
		bpd.setEngineName(engine.getName());
		bpd.setCategory(businessProcess.getCategory());
		bpd.setDeploymentId(businessProcess.getDeploymentId());
		bpd.setDescription(businessProcess.getDescription());
		bpd.setDiagramResourceName(businessProcess.getDiagramResourceName());
		bpd.setId(businessProcess.getId());
		bpd.setKey(businessProcess.getKey());
		bpd.setName(businessProcess.getName());
		bpd.setResourceName(businessProcess.getResourceName());
		bpd.setVersion(businessProcess.getVersion());
		return bpd;
	}

	private BusinessProcess businessProcessDefinitionToBusinessProcess(final BusinessProcessDefinition businessProcessDefinition, final BusinessProcessDataStore businessProcessDataStore) {
		return new BusinessProcess() {
			@Override
			public String getName() {
				return businessProcessDefinition.getName();
			}

			@Override
			public String getKey() {
				return businessProcessDefinition.getKey();
			}

			@Override
			public String getId() {
				return businessProcessDefinition.getId();
			}

			@Override
			public Integer getVersion() {
				return businessProcessDefinition.getVersion();
			}

			@Override
			public String getDeploymentId() {
				return businessProcessDefinition.getDeploymentId();
			}

			@Override
			public String getResourceName() {
				return businessProcessDefinition.getResourceName();
			}

			@Override
			public BusinessProcessData getData() {
				String resourceName = getResourceName();
				if (resourceName != null) {
					BusinessProcessData data = businessProcessDataStore.findByName(resourceName);
					return data;
				}
				return null;
			}

			@Override
			public BusinessProcessData getImageData() {
				String diagramResourceName = getDiagramResourceName();
				if (diagramResourceName != null) {
					BusinessProcessData data = businessProcessDataStore.findByName(diagramResourceName);
					return data;
				}
				return null;
			}

			@Override
			public String getCategory() {
				return businessProcessDefinition.getCategory();
			}

			@Override
			public String getDiagramResourceName() {
				return businessProcessDefinition.getDiagramResourceName();
			}

			@Override
			public String getDescription() {
				return businessProcessDefinition.getDescription();
			}
		};
	}

	public void setBusinessProcessDataStoreFactory(BusinessProcessDataStoreFactory businessProcessDataStoreFactory) {
		this.businessProcessDataStoreFactory = businessProcessDataStoreFactory;
	}

	@Override
	public void onProcessDefinitionDeployment(String engineName, BusinessProcess businessProcess) {
		flushAfterBusinessProcessEvent(engineName, businessProcess);
	}

	@Override
	public void onProcessDefinitionUndeployment(String engineName, BusinessProcess businessProcess) {
		flushAfterBusinessProcessEvent(engineName, businessProcess);
	}
	
	private void flushAfterBusinessProcessEvent(String engineName, BusinessProcess businessProcess) {
		try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
			cacheTransaction.flush("/bpm/definitions");
			String id = businessProcess.getId();
			if (id != null) {
				cacheTransaction.flush("/bpm/definitions/" + engineName + "/" + id);
			}
		}
	}

}
