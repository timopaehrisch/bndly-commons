package org.bndly.rest.query.resource;

/*-
 * #%L
 * REST Query Resource
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

import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.entity.resources.EntityResource;
import org.bndly.rest.entity.resources.EntityResourceDeploymentListener;
import org.bndly.rest.entity.resources.SchemaAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = EntityResourceDeploymentListener.class)
public class Activator implements EntityResourceDeploymentListener {

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private MapperFactoryProvider mapperFactoryProvider;
	private final Map<String, SchemaQueryResource> queryResourceBySchemaName = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Deactivate
	public void deactivate() {
		for (SchemaQueryResource value : queryResourceBySchemaName.values()) {
			controllerResourceRegistry.undeploy(value);
		}
		queryResourceBySchemaName.clear();
	}

	@Override
	public void deployed(SchemaAdapter sa, EntityResource er) {
		lock.writeLock().lock();
		try {
			SchemaQueryResource schemaQueryResource = queryResourceBySchemaName.get(sa.getSchema().getName());
			if (schemaQueryResource == null) {
				schemaQueryResource = new SchemaQueryResource(sa, mapperFactoryProvider);
				queryResourceBySchemaName.put(sa.getSchema().getName(), schemaQueryResource);
				controllerResourceRegistry.deploy(schemaQueryResource, "schema/" + sa.getSchema().getName());
			}
			schemaQueryResource.add(er);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void undeployed(SchemaAdapter sa, EntityResource er) {
		lock.writeLock().lock();
		try {
			SchemaQueryResource schemaQueryResource = queryResourceBySchemaName.get(sa.getSchema().getName());
			if (schemaQueryResource == null) {
				return;
			}
			schemaQueryResource.remove(er);
			if (schemaQueryResource.isHavingEntities()) {
				return;
			}
			queryResourceBySchemaName.remove(sa.getSchema().getName());
			controllerResourceRegistry.undeploy(schemaQueryResource);
		} finally {
			lock.writeLock().unlock();
		}
	}

}
