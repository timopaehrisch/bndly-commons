package org.bndly.rest.repository.resources.html;

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

import org.bndly.common.lang.StringUtil;
import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.RenderingListener;
import org.bndly.common.velocity.api.VelocityTemplate;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.repository.resources.BeanResource;
import org.bndly.rest.repository.resources.NodeResource;
import org.bndly.schema.api.repository.beans.Bean;
import org.bndly.schema.api.repository.beans.BeanDefinition;
import org.bndly.schema.api.repository.beans.BeanDefinitionRegistry;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.Property;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class HTMLNodeResourceRenderer implements ResourceRenderer, ViewResolver, RenderingListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(HTMLNodeResourceRenderer.class);
	
	@Reference
	private Renderer renderer;
	
	@Reference
	private BeanDefinitionRegistry beanDefinitionRegistry;
	
	@Activate
	public void activate() {
		renderer.addRenderingListener(this);
	}
	
	@Deactivate
	public void deactivate() {
		renderer.removeRenderingListener(this);
	}
	
	@Override
	public boolean supports(Resource resource, Context context) {
		if (!NodeResource.class.isInstance(resource) && !BeanResource.class.isInstance(resource)) {
			return false;
		}
		ContentType dc = context.getDesiredContentType();
		if (dc == null) {
			ResourceURI.Extension ext = context.getURI().getExtension();
			if (ext == null) {
				return false;
			} else {
				return ContentType.HTML.getExtension().equals(ext.getName());
			}
		}
		return ContentType.HTML.getName().equals(dc.getName());
	}
	
	@Override
	public void render(Resource resource, Context context) throws IOException {
		Object entity;
		String templateName = null;
		String currentPath;
		if (NodeResource.class.isInstance(resource)) {
			NodeResource nodeResource = (NodeResource) resource;
			Node node = nodeResource.getNode();
			entity = node;
			templateName = nodeTypeToTemplateName(node, context, false);
			currentPath = node.getPath().toString();
		} else {
			BeanResource beanResource = (BeanResource) resource;
			Bean bean = beanResource.getBean();
			ResourceURI.QueryParameter nodeParameter = context.getURI().getParameter("node");
			String view;
			List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
			if (selectors == null || selectors.isEmpty()) {
				view = null;
			} else {
				StringBuilder sb = null;
				for (ResourceURI.Selector selector : selectors) {
					if (sb == null) {
						sb = new StringBuilder(escapeStringForTemplateName(selector.getName()));
					} else {
						sb.append(".").append(escapeStringForTemplateName(selector.getName()));
					}
				}
				if (sb == null) {
					view = null;
				} else {
					view = sb.toString();
				}
			}
			entity = bean;
			if (nodeParameter == null) {
				templateName = resolveTemplateNameForView(bean, view);
			}
			if (nodeParameter != null || templateName == null) {
				Node node = bean.morphTo(Node.class);
				if (node != null) {
					entity = node;
					templateName = nodeTypeToTemplateName(node, context, false);
				} else {
					LOG.error("could not render {} bean resource, because there was not template available for the view {}", bean.getBeanType(), view);
					return;
				}
			}
			currentPath = bean.getPath();
		}
		// render it
		context.setOutputContentType(ContentType.HTML, "UTF-8");
		try (Writer writer = new OutputStreamWriter(context.getOutputStream(), "UTF-8")) {
			VelocityTemplate template = new VelocityTemplate()
					.addContextData(ContextData.newInstance("currentPath", currentPath))
					.addContextData(ContextData.newInstance("resource", resource))
					.addContextData(ContextData.newInstance("restContext", context))
					.addContextData(ContextData.newInstance("baseUrl", context.createURIBuilder().build().asString()))
					.setEntity(entity)
					.setLocale(context.getLocale())
					.setTemplateName(templateName)
			;
			renderer.render(template, writer);
			writer.flush();
		}
	}
	
	private String escapeStringForTemplateName(String input) {
		StringBuilder sb = new StringBuilder();
		for (int codePoint : StringUtil.codePoints(input)) {
			if (':' == codePoint || '/' == codePoint || '\\' == codePoint) {
				sb.append('_');
			} else {
				sb.appendCodePoint(codePoint);
			}
		}
		return sb.toString();
	}
	
	private String nodeTypeToTemplateName(Node node, Context context, boolean useNodeType) {
		String nodeTypeName = node.getType();
		String lastElement = node.getPath().getLastElement();
		int countOfSelectors = 0;
		for (int codePoint : StringUtil.codePoints(lastElement)) {
			if (codePoint == '.') {
				countOfSelectors++;
			}
		}
		String templateName = nodeTypeToTemplateName(nodeTypeName, context, useNodeType, true, countOfSelectors);
		if (renderer.isTemplateAvailable(templateName)) {
			return templateName;
		} else {
			return nodeTypeToTemplateName(nodeTypeName, context, useNodeType, false, countOfSelectors);
		}
	}
	
	private String nodeTypeToTemplateName(String nodeTypeName, Context context, boolean useNodeType, boolean appendSelectors, int selectorOffset) {
		StringBuilder sb = new StringBuilder();
		if (useNodeType) {
			sb.append(escapeStringForTemplateName(nodeTypeName));
		} else {
			sb.append("node");
		}
		if (appendSelectors) {
			List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
			if (selectors != null && !selectors.isEmpty()) {
				for (int i = selectorOffset; i < selectors.size(); i++) {
					ResourceURI.Selector selector = selectors.get(i);
					String name = selector.getName();
					if (!name.isEmpty()) {
						sb.append('.').append(name);
					}
				}
			}
		}
		sb.append(".vm");
		return sb.toString();
	}

	@Override
	public String resolveTemplateNameForView(Object model, String viewName) {
		if (Node.class.isInstance(model)) {
			Node node = (Node) model;
			StringBuilder templateName = new StringBuilder("node");
			if (viewName != null && !viewName.isEmpty()) {
				templateName.append(".").append(viewName);
			}
			templateName.append(".vm");
			String templateNameString = templateName.toString();
			if (renderer.isTemplateAvailable(templateNameString)) {
				return templateNameString;
			} else if (renderer.isTemplateAvailable("node.vm")) {
				return "node.vm";
			} else {
				return null;
			}
		} else if (Property.class.isInstance(model)) {
			Property property = (Property) model;
			StringBuilder templateName = new StringBuilder("property");
			if (viewName != null && !viewName.isEmpty()) {
				templateName.append(".").append(viewName);
			}
			templateName.append(".vm");
			String templateNameString = templateName.toString();
			if (renderer.isTemplateAvailable(templateNameString)) {
				return templateNameString;
			} else if (renderer.isTemplateAvailable("property.vm")) {
				return "property.vm";
			} else {
				return null;
			}
		} else if (Bean.class.isInstance(model)) {
			Bean bean = (Bean) model;
			return resolveTemplateNameForView(beanDefinitionRegistry.getBeanDefinition(bean.getBeanType()), viewName);
		} else {
			return viewName + ".vm";
		}
	}
	
	public String resolveTemplateNameForView(Bean bean, String viewName) {
		if (bean == null) {
			return null;
		}
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(bean.getBeanType());
		return resolveTemplateNameForView(beanDefinition, viewName);
	}
	
	public String resolveTemplateNameForView(BeanDefinition beanDefinition, String viewName) {
		if (beanDefinition == null) {
			return null;
		}
		String name = escapeStringForTemplateName(beanDefinition.getName());
		StringBuilder templateName = new StringBuilder(name);
		if (viewName != null && !viewName.isEmpty()) {
			templateName.append(".").append(viewName);
		}
		templateName.append(".vm");
		String templateNameString = templateName.toString();
		if (renderer.isTemplateAvailable(templateNameString)) {
			return templateNameString;
		} else {
			String tmp = resolveTemplateNameForView(beanDefinition.getParent(), viewName);
			if (tmp != null) {
				return tmp;
			}
		}
		if (viewName == null) {
			return null;
		} else {
			return resolveTemplateNameForView(beanDefinition, null);
		}
	}

	@Override
	public void beforeRendering(VelocityTemplate velocityTemplate, Writer writer) {
		velocityTemplate
				.addContextData(ContextData.newInstance("viewResolver", this))
				.addContextData(ContextData.newInstance("nodeEditorFactory", new NodeEditorFactory(beanDefinitionRegistry)))
				.addContextData(ContextData.newInstance("beanDefinitions", beanDefinitionRegistry.getBeanDefinitions()));
	}

	@Override
	public void afterRendering(VelocityTemplate velocityTemplate, Writer writer) {
	}

	
}
