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

import org.bndly.common.bpm.api.BusinessProcessDataStore;
import org.bndly.common.bpm.api.EngineProviderListener;
import org.bndly.common.bpm.api.ProcessDeploymentService;
import org.bndly.common.bpm.api.ProcessInstanceService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractProcessEngineAwareResource implements EngineProviderListener {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractProcessEngineAwareResource.class);
	
	protected final class Engine {
		private final String name;
		private final ProcessInstanceService instanceService;
		private final ProcessDeploymentService deploymentService;
		private final BusinessProcessDataStore businessProcessDataStore;

		public Engine(String name, ProcessInstanceService instanceService, ProcessDeploymentService deploymentService, BusinessProcessDataStore businessProcessDataStore) {
			this.name = name;
			this.instanceService = instanceService;
			this.deploymentService = deploymentService;
			this.businessProcessDataStore = businessProcessDataStore;
		}

		public ProcessDeploymentService getDeploymentService() {
			return deploymentService;
		}

		public ProcessInstanceService getInstanceService() {
			return instanceService;
		}

		public BusinessProcessDataStore getBusinessProcessDataStore() {
			return businessProcessDataStore;
		}

		public String getName() {
			return name;
		}
		
	}
	
	protected final Map<String, Engine> knownEngines = new HashMap<>();
	
	@Override
	public final void createdEngine(String name, ProcessInstanceService instanceService, ProcessDeploymentService deploymentService, BusinessProcessDataStore businessProcessDataStore) {
		Engine e = knownEngines.get(name);
		if (e != null) {
			if (
				e.getInstanceService() == instanceService 
				&& e.getBusinessProcessDataStore() == businessProcessDataStore
				&& e.getDeploymentService() == deploymentService
			) {
				// do nothing. just a redundant event.
			} else {
				// log a warning, because something is wrong
				LOG.warn("received a new BPM engine, but an already existing engine instance has not been removed yet with the same name");
			}
		} else {
			knownEngines.put(name, new Engine(name, instanceService, deploymentService, businessProcessDataStore));
		}
	}

	@Override
	public final void destroyedEngine(String name, ProcessInstanceService instanceService, ProcessDeploymentService deploymentService, BusinessProcessDataStore businessProcessDataStore) {
		Engine e = knownEngines.get(name);
		if (e != null) {
			if (
				e.getInstanceService() == instanceService 
				&& e.getBusinessProcessDataStore() == businessProcessDataStore
				&& e.getDeploymentService() == deploymentService
			) {
				// remove it
				knownEngines.remove(name);
			} else {
				// log a warning, because something is wrong
				LOG.warn("received a destroyed BPM engine, but an already existing engine with the same name was registered.");
			}
		} else {
			LOG.warn("received a destroyed BPM engine, but it has not been registered before.");
		}
	}
	
}
