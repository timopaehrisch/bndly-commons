package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.schema.api.services.Engine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SchemaResourceDeployer.class, immediate = true)
public class SchemaResourceDeployer {

	private ServiceTracker<Engine, Engine> tracker;
	
	private static final Logger LOG = LoggerFactory.getLogger(SchemaResourceDeployer.class);
	
	@org.osgi.service.component.annotations.Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	
	private final List<SchemaBoundResource> resources = new ArrayList<>();
	private final ReadWriteLock resourcesLock = new ReentrantReadWriteLock();
	private ConversionContext conversionContext;
	private SchemaResource schemaResource;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		conversionContext = new ConversionContextBuilder().initDefaults().build();
		schemaResource = new SchemaResource(conversionContext, resources, resourcesLock);
		controllerResourceRegistry.deploy(schemaResource);
		
		tracker = new ServiceTracker<Engine, Engine>(componentContext.getBundleContext(), Engine.class, null) {
			@Override
			public Engine addingService(ServiceReference<Engine> reference) {
				Engine engine = super.addingService(reference);
				resourcesLock.writeLock().lock();
				try {
					SchemaBoundResource resource = new SchemaBoundResource(engine, conversionContext);
					String prefix = "schema/" + engine.getDeployer().getDeployedSchema().getName();
					LOG.info("deploying schema bound resource to {}", prefix);
					controllerResourceRegistry.deploy(resource, prefix);
					resources.add(resource);
				} finally {
					resourcesLock.writeLock().unlock();
				}
				return engine;
			}

			@Override
			public void removedService(ServiceReference<Engine> reference, Engine service) {
				super.removedService(reference, service);
				resourcesLock.writeLock().lock();
				try {
					Iterator<SchemaBoundResource> iterator = resources.iterator();
					while (iterator.hasNext()) {
						SchemaBoundResource resource = iterator.next();
						if (resource.getEngine() == service) {
							iterator.remove();
							controllerResourceRegistry.undeploy(resource);
						}
					}
				} finally {
					resourcesLock.writeLock().unlock();
				}
			}

		};
		tracker.open();
	}

	@Deactivate
	public void deactivate() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		resourcesLock.writeLock().lock();
		try {
			for (SchemaBoundResource resource : resources) {
				controllerResourceRegistry.undeploy(resource);
			}
			resources.clear();
		} finally {
			resourcesLock.writeLock().unlock();
		}
		if (schemaResource != null) {
			controllerResourceRegistry.undeploy(schemaResource);
			schemaResource = null;
		}
	}
}
