package org.bndly.rest.swagger.resource;

/*-
 * #%L
 * REST Swagger Integration
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

import org.bndly.rest.controller.api.Documentation;
import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.VelocityTemplate;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.swagger.impl.MessageClassManager;
import org.bndly.rest.swagger.impl.SwaggerDocumentProvider;
import org.bndly.rest.swagger.model.Document;
import org.bndly.rest.swagger.model.Operation;
import org.bndly.rest.swagger.model.Paths;
import org.bndly.rest.swagger.model.SchemaModel;
import org.bndly.rest.swagger.model.Tag;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = DocumentationResource.class, immediate = true)
@Path("docs")
public class DocumentationResource {
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private SwaggerDocumentProvider swaggerDocumentProvider;
	@Reference
	private Renderer templateRenderer;
	private static final ContentType HTML = new ContentType() {

		@Override
		public String getName() {
			return "text/html";
		}

		@Override
		public String getExtension() {
			return "html";
		}
	};
	
	@Activate
	public void activate(ComponentContext componentContext) {
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		controllerResourceRegistry.undeploy(this);
	}
	
	@GET
	@Path(".html")
	@Documentation(authors = "bndly@bndly.org",produces = "text/html",responses = @DocumentationResponse(
			code = StatusWriter.Code.OK,
			description = "A HTML document that contains the entire documentation of the REST API."
		), 
		summary = "Read documentation about the REST API", 
		tags = "maintenance", 
		value = "This is a documentation resource, that renders HTML for developers in order to be able to connect clients to the application."
	)
	public Response getDocumentationRoot(@Meta final Context context) {
		try (Writer writer = new OutputStreamWriter(context.getOutputStream(), "UTF-8")) {
			return swaggerDocumentProvider.read(new SwaggerDocumentProvider.Consumer<Response>() {
				@Override
				public Response consume(Document doc) {
					context.setOutputContentType(HTML, "UTF-8");
					VelocityTemplate template = new VelocityTemplate();
					template.setTemplateName("rest-api/docs.vm");
					List<ContextData> contextData = new ArrayList<>();
					contextData.add(createContextData("urlPrefix", context.createURIBuilder().build().asString()));
					contextData.add(createContextData("swaggerDocument", doc));
					Map<String, Map<String, SchemaModel>> messagesByPackage = new LinkedHashMap<>();
					for (Map.Entry<String, SchemaModel> entrySet : doc.getDefinitions().entrySet()) {
						String className = entrySet.getKey();
						SchemaModel classDescription = entrySet.getValue();
						boolean isGlobal = className.startsWith(MessageClassManager.PREFIX_GLOBAL);
						int i = className.lastIndexOf(".");
						String packageName;
						if (!isGlobal && i >= 0) {
							packageName = className.substring(0, i);
						} else {
							packageName = "";
						}
						if (isGlobal) {
							className = className.substring(MessageClassManager.PREFIX_GLOBAL.length());
						}
						Map<String, SchemaModel> packageList = messagesByPackage.get(packageName);
						if (packageList == null) {
							packageList = new LinkedHashMap<>();
							messagesByPackage.put(packageName, packageList);
						}
						packageList.put(className, classDescription);
					}
					contextData.add(createContextData("messagesByPackage", messagesByPackage));
					Map<String, Paths> pathsByTag = new LinkedHashMap<>();
					contextData.add(createContextData("pathsByTag", pathsByTag));
					final Paths originalPaths = doc.getPaths();
					final Paths pathsOfUntagged = new Paths();
					for (Tag tag : doc.getTags()) {
						Paths pathsOfTag = new Paths();
						pathsByTag.put(tag.getName(), pathsOfTag);
						for (Map.Entry<String, org.bndly.rest.swagger.model.Path> entrySet : originalPaths.entrySet()) {
							String urlPattern = entrySet.getKey();
							org.bndly.rest.swagger.model.Path path = entrySet.getValue();
							filterOperationForCopy(path.getGet(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setGet(operation);
								}
							});
							filterOperationForCopy(path.getDelete(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setDelete(operation);
								}
							});
							filterOperationForCopy(path.getHead(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setHead(operation);
								}
							});
							filterOperationForCopy(path.getOptions(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setOptions(operation);
								}
							});
							filterOperationForCopy(path.getPatch(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setPatch(operation);
								}
							});
							filterOperationForCopy(path.getPost(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setPost(operation);
								}
							});
							filterOperationForCopy(path.getPut(), urlPattern, pathsOfTag, pathsOfUntagged, tag, new SetOperationCallback() {

								@Override
								public void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject) {
									pathObject.setPut(operation);
								}
							});
						}
					}
					if (!pathsOfUntagged.isEmpty()) {
						pathsByTag.put("untagged", pathsOfUntagged);
					}
					template.setContextData(contextData);
					templateRenderer.render(template, writer);
					try {
						writer.flush();
					} catch (IOException ex) {
						return Response.status(500).entity("failed to render template to writer");
					}
					return Response.ok();
				}
			});
		} catch (UnsupportedEncodingException e) {
			return Response.status(500).entity("UTF-8 encoding was not supported");
		} catch (IOException e) {
			return Response.status(500).entity("failed to render template to writer");
		}
	}
	
	private void filterOperationForCopy(Operation operation, String urlPattern, Paths pathsOfTag, Paths pathsOfUntagged, Tag tag, SetOperationCallback setOperationCallback) {
		if (operation == null) {
			return;
		}
		List<String> tagNamesOfOperation = operation.getTags();
		boolean appliesToTag = tagNamesOfOperation != null && tagNamesOfOperation.contains(tag.getName());
		if (appliesToTag) {
			org.bndly.rest.swagger.model.Path pathObject = pathsOfTag.get(urlPattern);
			if (pathObject == null) {
				pathObject = new org.bndly.rest.swagger.model.Path();
				pathsOfTag.put(urlPattern, pathObject);
			}
			setOperationCallback.set(operation, pathObject);
		} else {
			if (tagNamesOfOperation == null || tagNamesOfOperation.isEmpty()) {
				org.bndly.rest.swagger.model.Path pathObject = pathsOfUntagged.get(urlPattern);
				if (pathObject == null) {
					pathObject = new org.bndly.rest.swagger.model.Path();
					pathsOfUntagged.put(urlPattern, pathObject);
				}
				setOperationCallback.set(operation, pathObject);
			}
		}
	}
	
	private interface SetOperationCallback {
		void set(Operation operation, org.bndly.rest.swagger.model.Path pathObject);
	}
	
	private ContextData createContextData(String key, Object value) {
		ContextData contextData = new ContextData();
		contextData.setKey(key);
		contextData.setValue(value);
		return contextData;
	}
}
