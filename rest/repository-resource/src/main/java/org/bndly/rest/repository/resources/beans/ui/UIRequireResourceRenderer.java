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
@Designate(ocd = UIRequireResourceRenderer.Configuration.class)
public class UIRequireResourceRenderer implements ResourceRenderer {

	@ObjectClassDefinition(
			name = "UI Require Module Bean Resource Renderer"
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
		List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
		if (selectors == null || selectors.isEmpty()) {
			return false;
		}
		
		ResourceURI.Selector requireSelector = null;
		for (ResourceURI.Selector selector : selectors) {
			if ("require".equals(selector.getName())) {
				requireSelector = selector;
				break;
			}
		}
		if (requireSelector == null) {
			return false;
		}
		Bean bean = ((BeanResource) resource).getBean();
		if (!UIBean.class.isInstance(bean)) {
			return UIBean.BEAN_TYPE.equals(bean.getBeanType());
		}
		return true;
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		Bean bean = ((BeanResource)resource).getBean();
		if (!UIBean.class.isInstance(bean)) {
			bean = beanPojoFactory.getBean(bean, UIBean.class);
		}
		UIBean uiBean = (UIBean) bean;
		List<RequireModule> requireModules = uiBean.getRequireModuleBeans();
		try (Writer writer = new OutputStreamWriter(context.getOutputStream(), "UTF-8")) {
			JSONWriter jsonWriter = new JSONWriter(writer);
			writer.write("requirejs.config({\n");
			writer.write("baseUrl:");
			jsonWriter.writeString(context.createURIBuilder().build().asString()).writeComma();
			writer.write("\n");
			writer.write("paths:{\n");
			boolean first = true;
			ApplicationBean application = uiBean.getApplication();
			if (application != null) {
				for (RequireModule requireModuleBean : application.getRequireModuleBeans()) {
					String moduleId = requireModuleBean.getModuleId();
					if (moduleId != null) {
						if (!first) {
							jsonWriter.writeComma();
						}
						writer.write(requireModuleBean.getName());
						jsonWriter.writeColon().writeString(requireModuleBean.getModulePath());
						first = false;
					}
				}
			}
			// register paths to shared libraries such as jquery
			
			writer.write("},\nshim:{\n");
			if (application != null) {
				first = true;
				for (RequireModule requireModuleBean : application.getRequireModuleBeans()) {
					String moduleId = requireModuleBean.getModuleId();
					if (moduleId != null) {
						if (!first) {
							jsonWriter.writeComma();
						}
						writer.write(requireModuleBean.getName());
						jsonWriter.writeColon().writeObjectStart();
						boolean firstDependency = true;
						writer.write("deps");
						jsonWriter.writeColon().writeArrayStart();
						for (RequireModule dependency : requireModuleBean.getDependencies()) {
							if (!firstDependency) {
								jsonWriter.writeComma();
							}
							jsonWriter.writeString(dependency.getName());
							firstDependency = false;
						}
						jsonWriter.writeArrayEnd();
						if (requireModuleBean.isHavingExport()) {
							jsonWriter.writeComma();
							writer.write("exports");
							jsonWriter.writeColon().writeString(requireModuleBean.getExport());
						}
						jsonWriter.writeObjectEnd();
						first = false;
					}
				}
			}
			
			writer.write("}");
			writer.write("\n});\n");
			writer.write("requirejs(");
			jsonWriter.writeArrayStart();
			writer.write("\n");
			first = true;
			for (RequireModule requireModule : requireModules) {
				if (!first) {
					jsonWriter.writeComma();
					writer.write("\n");
				}
				if (requireModule.isProxyModule() && application != null && application.isApplicationModule(requireModule)) {
					jsonWriter.writeString(requireModule.getName());
				} else {
					jsonWriter.writeString(requireModule.getModulePath());
				}
				first = false;
			}
			writer.write("\n");
			jsonWriter.writeArrayEnd().writeComma();
			writer.write(" function (\n");
			first = true;
			for (RequireModule requireModule : requireModules) {
				if (!first) {
					jsonWriter.writeComma();
					writer.write("\n");
				}
				writer.write(requireModule.getModuleName());
				first = false;
			}
			writer.write("\n) {\n");
			for (RequireModule requireModule : requireModules) {
				String moduleInitMethod = requireModule.getModuleInitMethod();
				if (moduleInitMethod != null) {
					writer.append(requireModule.getModuleName()).append(".").append(moduleInitMethod).append("();\n");
				}
			}
			writer.write("});");
			writer.flush();
		}
	}
	
}
