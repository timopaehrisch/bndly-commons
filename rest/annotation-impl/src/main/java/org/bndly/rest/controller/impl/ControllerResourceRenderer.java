package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.StatusWriter.Code;
import org.bndly.rest.controller.api.EntityRenderer;
import org.bndly.rest.controller.api.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class ControllerResourceRenderer implements ResourceRenderer {

	private final List<EntityRenderer> listOfAllEntityRenderers = new ArrayList<>();
	private final ReadWriteLock entityRenderersLock = new ReentrantReadWriteLock();
	
	private EntityRenderer xmlEntityRenderer;
	private EntityRenderer jsonEntityRenderer;
	
	@Activate
	public void activate() {
	}
	@Deactivate
	public void deactivate() {
		entityRenderersLock.writeLock().lock();
		try {
			listOfAllEntityRenderers.clear();
			xmlEntityRenderer = null;
			jsonEntityRenderer = null;
		} finally {
			entityRenderersLock.writeLock().unlock();
		}
	}
	
	@Reference(
			bind = "addEntityRenderer",
			unbind = "removeEntityRenderer",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = EntityRenderer.class
	)
	public void addEntityRenderer(EntityRenderer entityRenderer) {
		if (entityRenderer != null) {
			entityRenderersLock.writeLock().lock();
			try {
				listOfAllEntityRenderers.add(entityRenderer);
				if (entityRenderer.getSupportedContentType().getName().equals(ContentType.XML.getName())) {
					xmlEntityRenderer = entityRenderer;
				}
				if (entityRenderer.getSupportedContentType().getName().equals(ContentType.JSON.getName())) {
					jsonEntityRenderer = entityRenderer;
				}
			} finally {
				entityRenderersLock.writeLock().unlock();
			}
		}
	}

	public void removeEntityRenderer(EntityRenderer entityRenderer) {
		if (entityRenderer != null) {
			// this is not thread safe, but this should only be critical when there is some deployment going on.
			// hence i leave it unsafe regarding multithreading.
			entityRenderersLock.writeLock().lock();
			try {
				if (entityRenderer == xmlEntityRenderer) {
					xmlEntityRenderer = null;
				}
				if (entityRenderer == jsonEntityRenderer) {
					jsonEntityRenderer = null;
				}
				Iterator<EntityRenderer> iterator = listOfAllEntityRenderers.iterator();
				while (iterator.hasNext()) {
					EntityRenderer next = iterator.next();
					if (next == entityRenderer) {
						iterator.remove();
					}
				}
			} finally {
				entityRenderersLock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public boolean supports(Resource resource, Context context) {
		return ControllerResource.class.isInstance(resource);
	}
	
	@Override
	public void render(Resource resource, Context context) throws IOException {
		ControllerResource cr = (ControllerResource) resource;
		Object result = cr.getResult();
		if (result != null) {
			if (Response.class.isInstance(result)) {
				Response r = (Response) result;
				renderResponse(r, context);
			} else {
				throw new IllegalStateException("unsupported result for controller resource");
			}
		}
	}

	private void renderResponse(final Response response, Context context) throws IOException {
		Code code = Code.fromHttpCode(response.getStatus());
		if (code != null) {
			context.getStatusWriter().write(code);
		}
		String l = response.getLocation();
		if (l != null) {
			context.setLocation(context.createURIBuilder().replace(l).build());
		}
		Map<String, String> headers = response.getHeaders();
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				String name = entry.getKey();
				final String value = entry.getValue();
				if ("Content-Type".equalsIgnoreCase(name)) {
					// ignore
				} else {
					context.setOutputHeader(name, value);
				}
			}
		}

		Object entity = response.getEntity();
		if (entity != null) {
			// render the entity to the output stream
			OutputStream os = context.getOutputStream();
			// render the entity to the output stream
			if (InputStream.class.isInstance(entity)) {
				IOUtils.copy((InputStream) entity, os);
			} else if (String.class.isInstance(entity)) {
				IOUtils.copy(((String) entity).getBytes("UTF-8"), os);
			} else {
				// dependending on the requested content type, we will render
				// use a custom renderer for xml, json or anything else
				final EntityRenderer usedRenderer = findMatchingEntityRenderer(context);
				context.setOutputContentType(usedRenderer.getSupportedContentType(), usedRenderer.getSupportedEncoding());
				usedRenderer.render(entity, os);
			}
		}
	}

	private EntityRenderer findMatchingEntityRenderer(Context context) {
		ContentType dct = context.getDesiredContentType();
		entityRenderersLock.readLock().lock();
		try {
			Iterator<EntityRenderer> iter = listOfAllEntityRenderers.iterator();
			ResourceURI.Extension ext = context.getURI().getExtension();
			String contextExtension = ext == null ? null : ext.getName();
			while (iter.hasNext()) {
				EntityRenderer next = iter.next();
				if (next.getSupportedContentType() == dct
						|| next.getSupportedContentType().getExtension().equals(contextExtension)
						|| dct != null
						&& (next.getSupportedContentType().getExtension().equals(dct.getExtension())
						|| next.getSupportedContentType().getName().equals(dct.getName()))) {
					return next;
				}
			}
			if (xmlEntityRenderer != null) {
				return xmlEntityRenderer;
			}
			if (jsonEntityRenderer != null) {
				return jsonEntityRenderer;
			}
			return null;
		} finally {
		entityRenderersLock.readLock().unlock();
		}
	}

	public void setXmlEntityRenderer(EntityRenderer xmlEntityRenderer) {
		entityRenderersLock.writeLock().lock();
		try {
			this.xmlEntityRenderer = xmlEntityRenderer;
		} finally {
			entityRenderersLock.writeLock().unlock();
		}
	}

	public void setJsonEntityRenderer(EntityRenderer jsonEntityRenderer) {
		entityRenderersLock.writeLock().lock();
		try {
			this.jsonEntityRenderer = jsonEntityRenderer;
		} finally {
			entityRenderersLock.writeLock().unlock();
		}
	}
	
}
