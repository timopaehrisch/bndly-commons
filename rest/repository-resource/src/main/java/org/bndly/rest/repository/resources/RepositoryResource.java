package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.InputStreamResource;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.schema.api.repository.beans.Bean;
import org.bndly.schema.api.repository.beans.BeanFactory;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RepositoryResource.class, immediate = true)
@Path(RepositoryResource.URL_SEGEMENT)
public class RepositoryResource {

	public static final String URL_SEGEMENT = "repo";
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryResource.class);

	private static final PayloadToNodeConverter NO_OP = new PayloadToNodeConverter() {

		@Override
		public ContentType getSupportedContentType() {
			return null;
		}

		@Override
		public void convertPayload(Node node, Context context) throws RepositoryException {
		}
	};
	
	private final Map<String, PayloadToNodeConverter> convertersByContentTypeName = new HashMap<>();
	private final ReadWriteLock convertersLock = new ReentrantReadWriteLock();
	
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Reference
	private Repository repository;

	@Reference
	private BeanFactory beanFactory;

	@Reference
	private ConverterRegistry converterRegistry;

	@Activate
	public void activate(ComponentContext componentContext) {
		Dictionary<String, Object> props = componentContext.getProperties();
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}
	
	@Reference(
			bind = "addPayloadToNodeConverter",
			unbind = "removePayloadToNodeConverter",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = PayloadToNodeConverter.class
	)
	public void addPayloadToNodeConverter(PayloadToNodeConverter converter) {
		if (converter != null) {
			ContentType ct = converter.getSupportedContentType();
			if (ct != null && ct.getName() != null) {
				convertersLock.writeLock().lock();
				try {
					convertersByContentTypeName.put(ct.getName(), converter);
				} finally {
					convertersLock.writeLock().unlock();
				}
			}
		}
	}

	public void removePayloadToNodeConverter(PayloadToNodeConverter converter) {
		if (converter != null) {
			ContentType ct = converter.getSupportedContentType();
			if (ct != null && ct.getName() != null) {
				convertersLock.writeLock().lock();
				try {
					Iterator<PayloadToNodeConverter> iterator = convertersByContentTypeName.values().iterator();
					while (iterator.hasNext()) {
						PayloadToNodeConverter next = iterator.next();
						if (next == converter) {
							iterator.remove();
						}
					}
				} finally {
					convertersLock.writeLock().unlock();
				}
			}
		}
	}

	@GET
	@AtomLinks({
		@AtomLink(rel = "repository", target = Services.class)
	})
	public Resource getRootNode(@Meta Context context, @Meta ResourceProvider resourceProvider) throws RepositoryException {
		return getNodeByPath("/", context, resourceProvider);
	}
	
	@GET
	@Path("{path}")
	public Resource getNodeByPath(
			@PathParam("path") String path,
			@Meta final Context context,
			@Meta final ResourceProvider resourceProvider
	) throws RepositoryException {
		try (RepositorySession session = repository.createReadOnlySession()) {
			Node node = lookUpNode(session, context);
			// build reponse
			String suffix = context.getURI().getSuffix();
			if (suffix != null) {
				if (suffix.startsWith("/")) {
					suffix = suffix.substring(1);
				}
				Property prop = node.getProperty(suffix);
				if (prop.getType() == Property.Type.BINARY) {
					final InputStream binary = prop.getBinary();
					if (binary != null) {
						return new InputStreamResource() {
							@Override
							public InputStream getInputStream() {
								return binary;
							}

							@Override
							public ResourceURI getURI() {
								return context.getURI();
							}

							@Override
							public ResourceProvider getProvider() {
								return resourceProvider;
							}
						};
					}
				}
			}
			Bean bean = beanFactory.createBeanFromNode(node);
			if (bean != null) {
				return new BeanResourceImpl(bean, context.getURI(), resourceProvider);
			}
			return new NodeResourceImpl(node, context.getURI(), resourceProvider);
		}
	}

	private Node lookUpNode(final RepositorySession session, final Context context) throws NodeNotFoundException, RepositoryException {
		org.bndly.schema.api.repository.Path repoPath = ContextUriToPathStrategy.URI_PATH_ONLY.buildPath(context);
		final org.bndly.schema.api.repository.Path rawRepoPath = repoPath;
		Node node = null;
		try {
			node = session.getNode(repoPath);
		} catch (NodeNotFoundException e) {
			List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
			ResourceURI.Extension ext = context.getURI().getExtension();
			if ((selectors == null || selectors.isEmpty()) && ext == null) {
				throw e;
			}
			repoPath = ContextUriToPathStrategy.URI_WITH_SELECTORS_AND_EXTENSION.buildPath(context);
			try {
				node = session.getNode(repoPath);
			} catch (NodeNotFoundException e2) {
				// try with the selectors, while dropping the last selector with each iteration
				if (selectors == null || selectors.isEmpty()) {
					throw e2;
				}
				List<String> selectorsList = new ArrayList<>();
				for (ResourceURI.Selector selector : selectors) {
					selectorsList.add(selector.getName());
				}
				for (int i = 0; i < selectorsList.size(); i++) {
					try {
						repoPath = new SelectorsLookupStrategy(selectorsList.subList(0, selectorsList.size() - i)).buildPath(context);
						node = session.getNode(repoPath);
						return node;
					} catch (NodeNotFoundException e3) {
						if (i == selectorsList.size() - 1) {
							throw e3;
						}
					}
				}
			}
		}
		if (node == null) {
			throw new NodeNotFoundException("could not find node at " + rawRepoPath.toString());
		}
		return node;
	}

	@DELETE
	public Response deleteNodeByPath(@Meta Context context) throws RepositoryException {
		return deleteNodeByPath("/", context);
	}
	
	@DELETE
	@Path("{path}")
	public Response deleteNodeByPath(@PathParam("path") String path, @Meta Context context) throws RepositoryException {
		try (RepositorySession session = repository.createAdminSession()) {
			org.bndly.schema.api.repository.Path repoPath = ContextUriToPathStrategy.URI_PATH_ONLY.buildPath(context);
			try {
				Node node = lookUpNode(session, context);
				node.remove();
				session.flush();
				repoPath = node.getPath();
			} catch (NodeNotFoundException e) {
				// doesn't matter
			}
			return Response.seeOther(createLinkToParent(repoPath, context));
		}
	}
	
	private String createLinkToParent(org.bndly.schema.api.repository.Path repoPath, Context context) {
		ResourceURIBuilder builder = context.createURIBuilder();
		builder.pathElement(URL_SEGEMENT);
		if (!repoPath.getElementNames().isEmpty()) {
			int size = repoPath.getElementNames().size();
			for (int i = 0; i < (size - 1); i++) {
				String elementName = repoPath.getElementNames().get(i);
				builder.pathElement(elementName);
			}
		}
		List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
		if (selectors != null && !selectors.isEmpty()) {
			for (ResourceURI.Selector selector : selectors) {
				if (!selector.getName().isEmpty()) {
					builder.selector(selector.getName());
				}
			}
		}
		if (context.getURI().getExtension() != null) {
			builder.extension(context.getURI().getExtension().getName());
		}
		return builder.build().asString();
	}
	
	public Response moveNodeByPath(long targetIndex, String path, Context context) throws RepositoryException {
		try (RepositorySession session = repository.createAdminSession()) {
			Node node = lookUpNode(session, context);
			node.moveToIndex(targetIndex);
			session.flush();
		}
		return Response.NO_CONTENT;
	}
	
	@POST
	public Response handleUserData(@Meta Context context) throws RepositoryException, IOException {
		return handleUserData("/", context);
	}
	
	@POST
	@Path("{path}")
	public Response handleUserData(@PathParam("path") String path, @Meta Context context) throws RepositoryException, IOException {
		ResourceURI.QueryParameter removeParameter = context.getURI().getParameter("#remove");
		ResourceURI.QueryParameter moveParameter = context.getURI().getParameter("#move");
		if (removeParameter != null && "true".equals(removeParameter.getValue())) {
			return deleteNodeByPath(path, context);
		} else if (moveParameter != null && moveParameter.getValue() != null && !moveParameter.getValue().isEmpty()) {
			try {
				return moveNodeByPath((long) converterRegistry.getConverter(String.class, Long.class).convert(moveParameter.getValue()), path, context);
			} catch (ConversionException ex) {
				throw new RepositoryException("could not move node, because the provided index was not numeric", ex);
			}
		} else {
			try (RepositorySession session = repository.createAdminSession()) {
				boolean created = false;
				Node node;
				try {
					node = lookUpNode(session, context);
				} catch (NodeNotFoundException e) {
					org.bndly.schema.api.repository.Path repoPath = ContextUriToPathStrategy.URI_WITH_SELECTORS_AND_EXTENSION.buildPath(context);
					// ok, then the node does not exist and we have to create it
					node = session.getRoot();
					int size = repoPath.getElementNames().size();
					String desiredNodeType = NodeTypes.UNSTRUCTURED;
					ResourceURI.QueryParameter nodeType = context.getURI().getParameter("nodeType");
					if (nodeType != null && nodeType.getValue() != null) {
						desiredNodeType = nodeType.getValue();
					}
					for (int i = 0; i < size; i++) {
						String elementName = repoPath.getElementNames().get(i);
						boolean isLast = i == size - 1;
						try {
							node = node.getChild(elementName);
							created = false;
						} catch (NodeNotFoundException ex) {
							if (isLast) {
								node = node.createChild(elementName, desiredNodeType);
							} else {
								node = node.createChild(elementName, NodeTypes.UNSTRUCTURED);
							}
							created = true;
						}
					}
				}
				PayloadToNodeConverter converter = getPayloadToNodeConverter(context);
				converter.convertPayload(node, context);
				session.flush();
				if (created) {
					ResourceURIBuilder builder = context.createURIBuilder().pathElement("repo");
					for (String elementName : node.getPath().getElementNames()) {
						builder.pathElement(elementName);
					}
					return Response.created(builder.build().asString());
				} else {
					return Response.NO_CONTENT;
				}
			}
		}
	}

	private PayloadToNodeConverter getPayloadToNodeConverter(Context context) {
		ContentType ct = context.getInputContentType();
		if (ct == null) {
			LOG.info("could not find converter because no input content type is defined");
			return NO_OP;
		}
		convertersLock.readLock().lock();
		try {
			PayloadToNodeConverter converter = convertersByContentTypeName.get(ct.getName());
			if (converter == null) {
				LOG.info("could not find converter for content type: {}", ct.getName());
				return NO_OP;
			}
			return converter;
		} finally {
			convertersLock.readLock().unlock();
		}
	}

}
