package org.bndly.rest.repository.resources.beans.ui;

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

import org.bndly.common.json.serializing.JSONWriter;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.repository.resources.BeanResource;
import org.bndly.rest.repository.resources.beans.api.BeanPojoFactory;
import org.bndly.rest.repository.resources.html.JavaScriptNodeResourceRenderer;
import org.bndly.schema.api.repository.beans.Bean;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
@Designate(ocd = RequireModuleBeanResourceRenderer.Configuration.class)
public class RequireModuleBeanResourceRenderer implements ResourceRenderer {

	@ObjectClassDefinition(
			name = "Require Module Bean Resource Renderer"
	)
	public @interface Configuration {
		
	}
	
	@Reference
	private BeanPojoFactory beanPojoFactory;
	
	@Override
	public boolean supports(Resource resource, Context context) {
		if (!BeanResource.class.isInstance(resource)) {
			return false;
		}
		ResourceURI.Extension ext = context.getURI().getExtension();
		if (ext == null || !ext.getName().equals(JavaScriptNodeResourceRenderer.JAVASCRIPT.getExtension())) {
			return false;
		}
		Bean bean = ((BeanResource) resource).getBean();
		if (!RequireModuleBean.class.isInstance(bean) && !RequireModuleLoaderBean.class.isInstance(bean)) {
			return RequireModuleBean.BEAN_TYPE.equals(bean.getBeanType()) || RequireModuleLoaderBean.BEAN_TYPE.equals(bean.getBeanType());
		}
		return true;
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		Bean bean = ((BeanResource) resource).getBean();
		RequireModuleBean requireModuleBean;
		RequireModuleLoaderBean requireModuleLoaderBean;
		if (!RequireModuleBean.class.isInstance(bean) && !RequireModuleLoaderBean.class.isInstance(bean)) {
			Bean tmp = beanPojoFactory.getBean(bean);
			if (RequireModuleBean.class.isInstance(tmp)) {
				requireModuleBean = (RequireModuleBean) tmp;
				renderRequireModuleBean(requireModuleBean, context);
			} else if (RequireModuleLoaderBean.class.isInstance(tmp)) {
				requireModuleLoaderBean = (RequireModuleLoaderBean) tmp;
				renderRequireModuleLoaderBean(requireModuleLoaderBean, context);
			} else {
				throw new IllegalStateException("could not render require module, because bean was of unsupported type.");
			}
		} else if (RequireModuleBean.class.isInstance(bean)) {
			requireModuleBean = (RequireModuleBean) bean;
			renderRequireModuleBean(requireModuleBean, context);
		} else if (RequireModuleLoaderBean.class.isInstance(bean)) {
			requireModuleLoaderBean = (RequireModuleLoaderBean) bean;
			renderRequireModuleLoaderBean(requireModuleLoaderBean, context);
		}
	}
	
	private void renderRequireModuleBean(RequireModuleBean requireModuleBean, Context context) throws IOException {
		if (requireModuleBean.isProxyModule()) {
			RequireModule proxy = requireModuleBean.getProxyBean();
			if (RequireModuleBean.class.isInstance(proxy)) {
				renderRequireModuleBean((RequireModuleBean) proxy, context);
				return;
			}
		}
		context.setOutputContentType(JavaScriptNodeResourceRenderer.JAVASCRIPT, "UTF-8");
		try (Writer writer = new OutputStreamWriter(context.getOutputStream(), "UTF-8")) {
			JSONWriter jsonWriter = new JSONWriter(writer);
			writer.write("define([");
			renderDependenciesPaths(jsonWriter, writer, requireModuleBean.getDependencies());
			writer.write("], function(");
			renderDependenciesVariables(jsonWriter, writer, requireModuleBean.getDependencies());
			writer.write(") {\n");
			writer.write(requireModuleBean.getScript());
			writer.write("\n});");
			writer.flush();
		}
	}
	
	private void renderRequireModuleLoaderBean(RequireModuleLoaderBean requireModuleLoaderBean, Context context) throws IOException {
		context.setOutputContentType(JavaScriptNodeResourceRenderer.JAVASCRIPT, "UTF-8");
		try (Writer writer = new OutputStreamWriter(context.getOutputStream(), "UTF-8")) {
			JSONWriter jsonWriter = new JSONWriter(writer);
			writer.write("define([");
			renderDependenciesPaths(jsonWriter, writer, requireModuleLoaderBean.getDependencies());
			writer.write("], function(");
			renderDependenciesVariables(jsonWriter, writer, requireModuleLoaderBean.getDependencies());
			writer.write(") {\n");
			writer.write("return {\n");
			writer.write("init: function() {\n");
			renderDependencyInitCalls(jsonWriter, writer, requireModuleLoaderBean.getDependencies());
			writer.write("}");
			writer.write("};");
			writer.write("\n});");
			writer.flush();
		}
	}
	
	private void renderDependenciesVariables(JSONWriter jsonWriter, Writer writer, List<RequireModule> dependencies) throws IOException {
		boolean first = true;
		for (RequireModule dependency : dependencies) {
			dependency = unwrapProxy(dependency);
			if (!first) {
				jsonWriter.writeComma();
			}
			writer.write("\n");
			writer.write(dependency.getModuleName());
			first = false;
		}
	}
	
	private void renderDependencyInitCalls(JSONWriter jsonWriter, Writer writer, List<RequireModule> dependencies) throws IOException {
		for (RequireModule dependency : dependencies) {
			dependency = unwrapProxy(dependency);
			String initMethod = dependency.getModuleInitMethod();
			if (initMethod == null) {
				continue;
			}
			writer.write(dependency.getModuleName() + "." + initMethod + "();\n");
		}
	}
	
	private void renderDependenciesPaths(JSONWriter jsonWriter, Writer writer, List<RequireModule> dependencies) throws IOException {
		boolean first = true;
		for (RequireModule dependency : dependencies) {
			dependency = unwrapProxy(dependency);
			if (!first) {
				jsonWriter.writeComma();
			}
			String moduleId = dependency.getModuleId();
			if (moduleId == null) {
				moduleId = dependency.getModulePath();
			}
			writer.write("\n");
			jsonWriter.writeString(moduleId);
			first = false;
		}
	}

	private RequireModule unwrapProxy(RequireModule proxy) {
		if (proxy.isProxyModule()) {
			RequireModule proxyBean = proxy.getProxyBean();
			if (proxyBean == null) {
				return proxy;
			}
			return unwrapProxy(proxyBean);
		} else {
			return proxy;
		}
	}
}
