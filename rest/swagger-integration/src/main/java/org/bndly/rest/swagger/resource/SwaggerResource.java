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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.common.beans.util.ExceptionMessageUtil;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.ControllerResourceRegistryListener;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationInfo;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.EntityParser;
import org.bndly.rest.controller.api.EntityRenderer;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.controller.api.DefaultDocumentationProvider;
import org.bndly.rest.controller.api.DocumentationExample;
import org.bndly.rest.controller.api.DocumentationExampleProvider;
import org.bndly.rest.controller.api.DocumentationProvider;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.swagger.impl.DocumentedController;
import org.bndly.rest.swagger.impl.EmptyPathUninstaller;
import org.bndly.rest.swagger.impl.Installer;
import static org.bndly.rest.swagger.impl.JAXBUtil.mapJavaTypeToSwaggerType;
import org.bndly.rest.swagger.impl.MessageClassManager;
import org.bndly.rest.swagger.impl.SwaggerDocumentProvider;
import org.bndly.rest.swagger.impl.Uninstaller;
import org.bndly.rest.swagger.model.Contact;
import org.bndly.rest.swagger.model.Definitions;
import org.bndly.rest.swagger.model.Document;
import org.bndly.rest.swagger.model.ExternalDocumentation;
import org.bndly.rest.swagger.model.Info;
import org.bndly.rest.swagger.model.License;
import org.bndly.rest.swagger.model.Operation;
import org.bndly.rest.swagger.model.Parameter;
import org.bndly.rest.swagger.model.Paths;
import org.bndly.rest.swagger.model.Responses;
import org.bndly.rest.swagger.model.SchemaModel;
import org.bndly.rest.swagger.model.SchemaReference;
import org.bndly.rest.swagger.model.Tag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = { SwaggerResource.class, ControllerResourceRegistryListener.class })
@Designate(ocd = SwaggerResource.Configuration.class)
public class SwaggerResource implements ControllerResourceRegistryListener {

	private static final Logger LOG = LoggerFactory.getLogger(SwaggerResource.class);
	private DictionaryAdapter dictionaryAdapter;
	
	@ObjectClassDefinition(
			name = "Swagger Resource", description = "The swagger resource is used to provide a documentation of the REST API."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "API Title",
				description = "API title"
		)
		String info_title();

		@AttributeDefinition(
				name = "API Version",
				description = "API version"
		)
		String info_version();

		@AttributeDefinition(
				name = "API Description",
				description = "Short textual API description"
		)
		String info_description();

		@AttributeDefinition(
				name = "License Name",
				description = "Name of the API license"
		)
		String license_name();

		@AttributeDefinition(
				name = "License URL",
				description = "URL to the API license description"
		)
		String license_url();

		@AttributeDefinition(
				name = "Contact Email",
				description = "The email address of the API provider"
		)
		String contact_email();

		@AttributeDefinition(
				name = "Contact Name",
				description = "The name of the API provider"
		)
		String contact_name();

		@AttributeDefinition(
				name = "Contact URL",
				description = "The URL to get in contact with the API provider"
		)
		String contact_url();

		@AttributeDefinition(
				name = "API Base Path",
				description = "The base context path of the API"
		)
		String basePath();

		@AttributeDefinition(
				name = "API URI Scheme",
				description = "The schema of the API ('http' or 'https')"
		)
		String scheme();

		@AttributeDefinition(
				name = "API Host",
				description = "The host name of the API"
		)
		String host();
	}
	
	@Reference
	private MessageClassManager messageClassManager;
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private SwaggerDocumentProvider swaggerDocumentProvider;
	private Configuration configuration;
	private final Map<String, Tag> swaggerTagsByName = new HashMap<>();
	private final DefaultDocumentationProvider defaultDocumentationProvider = new DefaultDocumentationProvider();
	private final Map<Class, List<DocumentedController>> documentedControllersByControllerType = new HashMap<>();
	private final ReadWriteLock documentedControllersByControllerTypeLock = new ReentrantReadWriteLock();
	
	private final List<DocumentationExampleProvider> documentationExampleProviders = new ArrayList<>();
	private final ReadWriteLock examplesLock = new ReentrantReadWriteLock();
	
	private final List<String> listOfAllEntityRendererContentTypes = new ArrayList<>();
	private final ReadWriteLock entityRenderersLock = new ReentrantReadWriteLock();
	
	private final List<String> listOfAllEntityParserContentTypes = new ArrayList<>();
	private final ReadWriteLock entityParsersLock = new ReentrantReadWriteLock();
	
	private final List<Installer> installers = new ArrayList<>();

	@Activate
	public void activate(Configuration configuration, ComponentContext componentContext) {
		dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());
		this.configuration = configuration;
		initSwaggerDoc();
		for (Installer installer : installers) {
			installer.install();
		}
		installers.clear();
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		controllerResourceRegistry.undeploy(this);
		configuration = null;
		documentedControllersByControllerTypeLock.writeLock().lock();
		try {
			documentedControllersByControllerType.clear();
		} finally {
			documentedControllersByControllerTypeLock.writeLock().unlock();
		}
		swaggerTagsByName.clear();
	}
	
	@Reference(
			bind = "addDocumentationExampleProvider",
			unbind = "removeDocumentationExampleProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = DocumentationExampleProvider.class
	)
	public void addDocumentationExampleProvider(DocumentationExampleProvider documentationExampleProvider) {
		if (documentationExampleProvider != null) {
			examplesLock.writeLock().lock();
			try {
				documentationExampleProviders.add(documentationExampleProvider);
			} finally {
				examplesLock.writeLock().unlock();
			}
		}
	}
	
	public void removeDocumentationExampleProvider(DocumentationExampleProvider documentationExampleProvider) {
		if (documentationExampleProvider != null) {
			examplesLock.writeLock().lock();
			try {
				Iterator<DocumentationExampleProvider> iterator = documentationExampleProviders.iterator();
				while (iterator.hasNext()) {
					DocumentationExampleProvider next = iterator.next();
					if (next == documentationExampleProvider) {
						iterator.remove();
					}
				}
			} finally {
				examplesLock.writeLock().unlock();
			}
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
			ContentType ct = entityRenderer.getSupportedContentType();
			if (ct != null) {
				String name = ct.getName();
				if (name != null) {
					entityRenderersLock.writeLock().lock();
					try {
						if (!listOfAllEntityRendererContentTypes.contains(name)) {
							listOfAllEntityRendererContentTypes.add(name);
						}
					} finally {
						entityRenderersLock.writeLock().unlock();
					}
				}
			}
		}
	}

	public void removeEntityRenderer(EntityRenderer entityRenderer) {
		if (entityRenderer != null) {
			ContentType ct = entityRenderer.getSupportedContentType();
			if (ct != null) {
				String name = ct.getName();
				if (name != null) {
					entityRenderersLock.writeLock().lock();
					try {
						Iterator<String> iterator = listOfAllEntityRendererContentTypes.iterator();
						while (iterator.hasNext()) {
							String next = iterator.next();
							if (next.equals(name)) {
								iterator.remove();
							}
						}
					} finally {
						entityRenderersLock.writeLock().unlock();
					}
				}
			}
		}
	}
	
	@Reference(
			bind = "addEntityParser",
			unbind = "removeEntityParser",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = EntityParser.class
	)
	public void addEntityParser(EntityParser entityParser) {
		if (entityParser != null) {
			ContentType ct = entityParser.getSupportedContentType();
			if (ct != null) {
				String name = ct.getName();
				if (name != null) {
					entityParsersLock.writeLock().lock();
					try {
						if (!listOfAllEntityParserContentTypes.contains(name)) {
							listOfAllEntityParserContentTypes.add(name);
						}
					} finally {
						entityParsersLock.writeLock().unlock();
					}
				}
			}
		}
	}

	public void removeEntityParser(EntityParser entityParser) {
		if (entityParser != null) {
			ContentType ct = entityParser.getSupportedContentType();
			if (ct != null) {
				String name = ct.getName();
				if (name != null) {
					entityParsersLock.writeLock().lock();
					try {
						Iterator<String> iterator = listOfAllEntityParserContentTypes.iterator();
						while (iterator.hasNext()) {
							String next = iterator.next();
							if (next.equals(name)) {
								iterator.remove();
							}
						}
					} finally {
						entityParsersLock.writeLock().unlock();
					}
				}
			}
		}
	}

	private void initSwaggerDoc() {
		swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
			@Override
			public Object consume(final Document swaggerDocument) {
				Info info = new Info(configuration.info_title(), configuration.info_version());
				info.setDescription(configuration.info_description());
				License license = new License(configuration.license_name());
				license.setUrl(configuration.license_url());
				info.setLicense(license);
				Contact contact = new Contact();
				contact.setEmail(configuration.contact_email());
				contact.setName(configuration.contact_name());
				contact.setUrl(configuration.contact_url());
				info.setContact(contact);
				swaggerDocument.setInfo(info);
				swaggerDocument.setBasePath(configuration.basePath());
				swaggerDocument.setSchemes(Arrays.asList(configuration.scheme()));
				swaggerDocument.setHost(configuration.host());

				List<Tag> tags = new ArrayList<>();
				swaggerDocument.setTags(tags);

				Paths paths = new Paths();
				swaggerDocument.setPaths(paths);
				return null;
			}
		});
	}

	@GET
	@Path("messages/{rootElement}.xsd")
	public Response getMessageTypeXSD(@PathParam("rootElement") String rootElement, @Meta Context context) {
		ResourceURI uri = context.getURI();
		StringBuilder sb = new StringBuilder(rootElement);
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (selectors != null && !selectors.isEmpty()) {
			for (ResourceURI.Selector selector : selectors) {
				sb.append(".").append(selector.getName());
			}
		}
		rootElement = sb.toString();
		MessageClassManager.XSDProvider xsdProviderForRootElement = messageClassManager.getXSDProviderForRootElement(rootElement);
		if (xsdProviderForRootElement == null) {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setMessage("root element not found");
			errorBean.setName("unknownRootElement");
			ExceptionMessageUtil.createKeyValue(errorBean, "rootElement", rootElement);
			return Response.status(StatusWriter.Code.NOT_FOUND.getHttpCode()).entity(errorBean);
		}
		InputStream xsdInputStream = xsdProviderForRootElement.getXSDDataAsStream();
		if (xsdInputStream == null) {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setMessage("failed to generate xsd");
			errorBean.setName("xsdGenerationFailed");
			ExceptionMessageUtil.createKeyValue(errorBean, "rootElement", rootElement);
			return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode()).entity(errorBean);
		}
		return Response.ok(xsdInputStream);
	}
	
	@GET
	@Path("swagger.json")
	@AtomLink(rel = "swaggerDocumentation", target = Services.class)
	@Documentation(
			summary = "Swagger documentation data",
			authors = "bndly@bndly.org",
			value = "returns a swagger documentation.",
			responses = {
				@DocumentationResponse(description = "a swagger 2.0 document, that describes all available url patterns")
			},
			produces = "application/json"
	)
	public Response getSwaggerDocumentation(@Meta Context context) throws IOException {
		context.setOutputContentType(ContentType.JSON, "UTF-8");

		final ConversionContext conversionContext = new ConversionContextBuilder()
				.initDefaults()
				.skipNullValues()
				.serializer(new Serializer() {
					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return Definitions.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						Map<String, SchemaModel> copy = new HashMap<String, SchemaModel>();
						for (Map.Entry<String, SchemaModel> entry : ((Definitions)javaValue).entrySet()) {
							String key = entry.getKey();
							if (key.startsWith(MessageClassManager.PREFIX_GLOBAL)) {
								copy.put(key.substring(MessageClassManager.PREFIX_GLOBAL.length()), entry.getValue());
							} else {
								copy.put(key, entry.getValue());
							}
						}
						return conversionContext.serialize(Map.class, copy);
					}

				})
				.serializer(new Serializer() {

					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return Parameter.Type.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						return new JSString(((Parameter.Type)javaValue).getAsSwaggerString());
					}
				})
				.serializer(new Serializer() {

					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return Parameter.Location.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						return new JSString(((Parameter.Location)javaValue).toString());
					}
				})
				.serializer(new Serializer() {

					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return SchemaReference.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						SchemaReference ref = (SchemaReference)javaValue;
						JSObject obj = new JSObject();
						obj.createMember("$ref").setValue(new JSString(ref.getReferencedElementName()));
						if (ref.getExternalDocumentation() != null) {
							obj.createMember("externalDocs").setValue(conversionContext.serialize(ExternalDocumentation.class, ref.getExternalDocumentation()));
						}
						return obj;
					}
				})
				.serializer(new Serializer() {

					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return SchemaModel.Type.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						SchemaModel.Type ref = (SchemaModel.Type)javaValue;
						return new JSString(ref.toString());
					}
				})
				.serializer(new Serializer() {

					@Override
					public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						return SchemaModel.class.equals(sourceType);
					}

					@Override
					public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
						if (javaValue == null) {
							return null;
						}
						SchemaModel sm = (SchemaModel) javaValue;
						JSObject obj = new JSObject();
						if (sm.getSuperModel() == null) {
							if (sm.getProperties() != null) {
								obj.createMember("properties").setValue(conversionContext.serialize(Map.class, sm.getProperties()));
							}
							if (sm.getExternalDocs() != null) {
								obj.createMember("externalDocs").setValue(conversionContext.serialize(ExternalDocumentation.class, sm.getExternalDocs()));
							}
						} else {
							JSArray array = new JSArray();
							obj.createMember("allOf").setValue(array);
							JSObject superModelRef = new JSObject();
							superModelRef.createMember("$ref").setValue(new JSString(sm.getSuperModel()));
							array.add(superModelRef);
							JSObject propertiesObj = new JSObject();
							if (sm.getProperties() != null) {
								propertiesObj.createMember("properties").setValue(conversionContext.serialize(Map.class, sm.getProperties()));
							}
							if (sm.getExternalDocs() != null) {
								propertiesObj.createMember("externalDocs").setValue(conversionContext.serialize(ExternalDocumentation.class, sm.getExternalDocs()));
							}
							if (propertiesObj.getMembers() != null && !propertiesObj.getMembers().isEmpty()) {
								array.add(propertiesObj);
							}
						}
						return obj;
					}
				})
				.build();
		
		JSValue swaggerDoc = swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<JSValue>() {
			@Override
			public JSValue consume(Document swaggerDocument) {
				swaggerDocument.getPaths().sortByKey();
				List<Tag> tags = swaggerDocument.getTags();
				if (tags != null) {
					Collections.sort(tags, new Comparator<Tag>() {

						@Override
						public int compare(Tag o1, Tag o2) {
							return o1.getName().compareTo(o2.getName());
						}
					});
				}
				JSValue swaggerDoc = conversionContext.serialize(Document.class, swaggerDocument);
				return swaggerDoc;
			}
		});

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new JSONSerializer().serialize(swaggerDoc, bos, "UTF-8");
		bos.flush();

		byte[] buf = bos.toByteArray();
		InputStream inputStream = new ByteArrayInputStream(buf);
		return Response.ok(inputStream);
	}

	private void addControllerBindingsToSwaggerDoc(Document swaggerDocument, List<ControllerBinding> controllerBindings, DocumentedController documentedController) {
		if (controllerBindings == null) {
			return;
		}
		addControllerBindingsToSwaggerDoc(swaggerDocument, controllerBindings.iterator(), documentedController);
	}

	private void addControllerBindingsToSwaggerDoc(Document swaggerDocument, Iterator<ControllerBinding> controllerBindings, final DocumentedController documentedController) {
		if (controllerBindings == null) {
			return;
		}
		Paths paths = swaggerDocument.getPaths();
		while (controllerBindings.hasNext()) {
			ControllerBinding binding = controllerBindings.next();
			

			String uriPattern = binding.getResourceURIPattern().asString();
			if (uriPattern.isEmpty()) {
				uriPattern = "/";
			}

			org.bndly.rest.swagger.model.Path path = paths.get(uriPattern);
			if (path == null) {
				path = new org.bndly.rest.swagger.model.Path();
				paths.put(uriPattern, path);
			}

			Documentation docTmp = binding.getDocumentation();
			if (docTmp != null) {
				DocumentedController dr = documentedController;
				if (dr == null) {
					dr = assertDocumentedControllerExists(binding.getController(), binding.getControllerType());
				}
				DocumentationProvider.Context documentationContext = createDocumentationContext(binding, docTmp);
				DocumentationInfo info = defaultDocumentationProvider.getDocumentationInfo(documentationContext);
				if (info != null) {
					final Operation operation = new Operation();
					
					operation.setSummary(info.getSummary());
					operation.setDescription(info.getText());
					if (!info.getProduces().isEmpty()) {
						if (info.getProduces().size() == 1 && Documentation.ANY_CONTENT_TYPE.equals(info.getProduces().get(0))) {
							// append all the available entity renderers
							entityRenderersLock.readLock().lock();
							try {
								operation.setProduces(new ArrayList<String>(listOfAllEntityRendererContentTypes));
							} finally {
								entityRenderersLock.readLock().unlock();
							}
						} else {
							operation.setProduces(info.getProduces());
						}
					}

					if (!info.getConsumes().isEmpty()) {
						if (info.getConsumes().size() == 1 && Documentation.ANY_CONTENT_TYPE.equals(info.getConsumes().get(0))) {
							// append all the available entity renderers
							entityParsersLock.readLock().lock();
							try {
								operation.setConsumes(new ArrayList<String>(listOfAllEntityParserContentTypes));
							} finally {
								entityParsersLock.readLock().unlock();
							}
						} else {
							operation.setConsumes(info.getConsumes());
						}
					}
					List<Parameter> parameters = createParameters(info);
					operation.setParameters(parameters);

					Responses responses = new Responses();
					for (DocumentationInfo.ResponseInfo r : info.getResponses()) {
						org.bndly.rest.swagger.model.Response response = new org.bndly.rest.swagger.model.Response(r.getDescription());
						Type javaType = r.getJavaType();
						if (Class.class.isInstance(javaType)) {
							Class cls = (Class) javaType;
							String ref = "#/definitions/" + cls.getName();
							SchemaReference schema = new SchemaReference(ref);
							schema.setExternalDocumentation(messageClassManager.createExternalDocsOfMessageClass(cls));
							response.setSchema(schema);
						}
						responses.put(r.getCode().getHttpCode(), response);
					}
					operation.setResponses(responses);
					if (!info.getTags().isEmpty()) {
						for (String tagName : info.getTags()) {
							assertSwaggerTagExists(swaggerDocument, tagName);
						}
						operation.setTags(info.getTags());
					}

					final Uninstaller removeEmptyPath = new EmptyPathUninstaller(uriPattern, path, swaggerDocument);
					
					final org.bndly.rest.swagger.model.Path finalPath = path;
					if (binding.getHTTPMethod() == HTTPMethod.GET) {
						path.setGet(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getGet() == operation) {
									finalPath.setGet(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					} else if (binding.getHTTPMethod() == HTTPMethod.POST) {
						path.setPost(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getPost() == operation) {
									finalPath.setPost(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					} else if (binding.getHTTPMethod() == HTTPMethod.PUT) {
						path.setPut(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getPut() == operation) {
									finalPath.setPut(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					} else if (binding.getHTTPMethod() == HTTPMethod.DELETE) {
						path.setDelete(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getDelete() == operation) {
									finalPath.setDelete(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					} else if (binding.getHTTPMethod() == HTTPMethod.HEAD) {
						path.setHead(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getHead() == operation) {
									finalPath.setHead(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					} else if (binding.getHTTPMethod() == HTTPMethod.OPTIONS) {
						path.setOptions(operation);
						dr.add(info, new Uninstaller() {

							@Override
							public void uninstall() {
								if (finalPath.getOptions() == operation) {
									finalPath.setOptions(null);
									removeEmptyPath.uninstall();
								}
							}
						});
					}
				}
			}
		}
	}
	
	private void assertSwaggerTagExists(Document swaggerDocument, String tagName) {
		if (tagName == null || tagName.isEmpty()) {
			throw new IllegalArgumentException("tag names for swagger should not be empty");
		}
		Tag tag = swaggerTagsByName.get(tagName);
		if (tag == null) {
			tag = new Tag(tagName);
			String desc = dictionaryAdapter.getString("tag.description." + tagName);
			tag.setDescription(desc);
			swaggerTagsByName.put(tagName, tag);
			swaggerDocument.getTags().add(tag);
		}
	}

	@Override
	public void deployedController(final Object controller, final Class<?> controllerInterface, final List<ControllerBinding> bindings, String baseURI, ControllerResourceRegistry registry) {
		swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
			@Override
			public Object consume(Document swaggerDocument) {
				addControllerBindingsToSwaggerDoc(swaggerDocument, bindings, assertDocumentedControllerExists(controller, controllerInterface));
				return null;
			}
		});
	}

	@Override
	public void undeployedController(Object controller, Class<?> controllerInterface, List<ControllerBinding> bindings, String baseURI, ControllerResourceRegistry registry) {
		dropDocumentationOfController(controller, controllerInterface);
	}

	@Override
	public void boundTo(final ControllerResourceRegistry registry) {
		swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
			@Override
			public Object consume(Document swaggerDocument) {
				addControllerBindingsToSwaggerDoc(swaggerDocument, registry.listDeployedControllerBindings(), null);
				return null;
			}
		});
	}

	@Override
	public void unboundFrom(ControllerResourceRegistry registry) {
		Iterator<ControllerBinding> bindings = registry.listDeployedControllerBindings();
		while (bindings.hasNext()) {
			ControllerBinding binding = bindings.next();
			dropDocumentationOfController(binding.getController(), binding.getControllerType());
		}
	}
	
	private DocumentationProvider.Context createDocumentationContext(final ControllerBinding binding, final Documentation documentation) {
		final String _uriPattern = binding.getResourceURIPattern().asString();
		return new DocumentationProvider.Context() {

			@Override
			public Method getMethod() {
				return binding.getMethod();
			}

			@Override
			public Documentation getDocumentation() {
				return documentation;
			}

			@Override
			public Object getController() {
				return binding.getController();
			}

			@Override
			public String getUriPattern() {
				return _uriPattern;
			}

			@Override
			public HTTPMethod getHTTPMethod() {
				return binding.getHTTPMethod();
			}

			@Override
			public List<DocumentationExample> getExamplesFromProviders() {
				List<DocumentationExample> tmp = new ArrayList<>();
				examplesLock.readLock().lock();
				try {
					for (DocumentationExampleProvider documentationExampleProvider : documentationExampleProviders) {
						List<DocumentationExample> e = documentationExampleProvider.createExamples(this);
						if (e != null) {
							tmp.addAll(e);
						}
					}
				} finally {
					examplesLock.readLock().unlock();
				}
				
				return tmp;
			}
	
		};
	}

	private void dropDocumentationOfController(Object controller, Class<?> controllerInterface) {
		documentedControllersByControllerTypeLock.writeLock().lock();
		try {
			List<DocumentedController> list = documentedControllersByControllerType.get(controllerInterface);
			if (list != null) {
				Iterator<DocumentedController> iter = list.iterator();
				while (iter.hasNext()) {
					DocumentedController documentedController = iter.next();
					if (documentedController.getController() == controller) {
						documentedController.uninstall();
						iter.remove();
					}
				}
			}
		} finally {
			documentedControllersByControllerTypeLock.writeLock().unlock();
		}
	}

	private DocumentedController assertDocumentedControllerExists(Object controller, Class<?> controllerType) {
		documentedControllersByControllerTypeLock.writeLock().lock();
		try {
			List<DocumentedController> list = documentedControllersByControllerType.get(controllerType);
			if (list == null) {
				list = new ArrayList<>();
				DocumentedController dc = new DocumentedController(controller);
				list.add(dc);
				return dc;
			} else {
				for (DocumentedController dc : list) {
					if (dc == controller) {
						return dc;
					}
				}
				DocumentedController dc = new DocumentedController(controller);
				list.add(dc);
				return dc;
			}
		} finally {
			documentedControllersByControllerTypeLock.writeLock().unlock();
		}
	}

	private List<Parameter> createParameters(DocumentationInfo info) {
		List<Parameter> parameters = null;
		List<DocumentationInfo.GenericParameterInfo> qp = info.getQueryParameters();
		for (DocumentationInfo.GenericParameterInfo queryParameter : qp) {
			if (parameters == null) {
				parameters = new ArrayList<>();
			}
			Parameter p = new Parameter(queryParameter.getName(), Parameter.Location.query);
			initAndAddParameter(queryParameter, p, parameters);
		}

		List<DocumentationInfo.GenericParameterInfo> pp = info.getPathParameters();
		for (DocumentationInfo.GenericParameterInfo pathParameter : pp) {
			if (parameters == null) {
				parameters = new ArrayList<>();
			}
			Parameter p = new Parameter(pathParameter.getName(), Parameter.Location.path);
			initAndAddParameter(pathParameter, p, parameters);
		}

		DocumentationInfo.BodyParameterInfo bp = info.getBodyParameter();
		if (bp != null) {
			if (parameters == null) {
				parameters = new ArrayList<>();
			}
			Parameter p = new Parameter("body", Parameter.Location.body);
			String schemaElementName = bp.getSchemaElementName();
			if (schemaElementName != null) {
				SchemaReference schema = new SchemaReference("#/definitions/" + schemaElementName);
				if (bp instanceof DocumentationInfo.HasExternalDocumentation) {
					DocumentationInfo.HasExternalDocumentation ed = (DocumentationInfo.HasExternalDocumentation) bp;
					String url = ed.getExternalDocumentationUrl();
					if (url != null) {
						ExternalDocumentation externalDocumentation = new ExternalDocumentation(url);
						externalDocumentation.setDescription(ed.getExternalDocumentationDescription());
						schema.setExternalDocumentation(externalDocumentation);
					}
				}
				p.setSchema(schema);
				p.setDescription(bp.getDescription());
				p.setRequired(bp.isRequired());
				parameters.add(p);
			}
		}
		return parameters;
	}

	protected void initAndAddParameter(DocumentationInfo.GenericParameterInfo parameter, Parameter p, List<Parameter> parameters) {
		Parameter.Type type = mapJavaTypeToSwaggerType(parameter.getJavaType());
		if (type != null) {
			p.setType(type);
			p.setRequired(parameter.isRequired());
			p.setDescription(parameter.getDescription());
			parameters.add(p);
		}
	}

}
