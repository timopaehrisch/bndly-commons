package org.bndly.rest.cache.resources;

/*-
 * #%L
 * REST Cache Resource
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

import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.cache.beans.CacheFlushRestBean;
import org.bndly.rest.cache.beans.CacheStatusRestBean;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.Response;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = CacheResource.class, immediate = true)
@Path("cache")
public class CacheResource {
	
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
	@AtomLink(rel = "cache", target = Services.class)
	public Response getStatus() {
		CacheStatusRestBean status = new CacheStatusRestBean();
		return Response.ok(status);
	}
	
	@POST
	@Path("flush/everything")
	@AtomLink(rel = "flusheverything", target = CacheStatusRestBean.class)
	public Response flushEverything() {
		try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
			cacheTransaction.flush();
			return Response.NO_CONTENT;
		}
	}
	
	@POST
	@Path("flush")
	@AtomLink(rel = "flush", target = CacheStatusRestBean.class)
	public Response flushPath(CacheFlushRestBean bean) {
		try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
			if (bean.getPath() != null) {
				Boolean recursive = bean.getRecursive();
				if (recursive == null) {
					recursive = false;
				}
				if (recursive) {
					cacheTransaction.flushRecursive(bean.getPath());
				} else {
					cacheTransaction.flush(bean.getPath());
				}
			} else if (bean.getComplete() != null && bean.getComplete()) {
				return flushEverything();
			}
			cacheTransaction.flush();
			return Response.NO_CONTENT;
		}
	}
}
