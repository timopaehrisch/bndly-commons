package org.bndly.rest.swagger.impl;

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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.controller.api.DocumentationXSDFactory;
import org.bndly.rest.swagger.model.Definitions;
import org.bndly.rest.swagger.model.Document;
import org.bndly.rest.swagger.model.ExternalDocumentation;
import org.bndly.rest.swagger.model.Parameter;
import org.bndly.rest.swagger.model.Property;
import org.bndly.rest.swagger.model.SchemaModel;
import org.bndly.rest.swagger.model.SchemaReference;
import org.bndly.rest.swagger.model.SimpleProperty;
import org.bndly.rest.swagger.model.XMLHint;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.bndly.rest.swagger.impl.JAXBUtil.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = MessageClassManager.class)
public class MessageClassManager {
	private static final Logger LOG = LoggerFactory.getLogger(MessageClassManager.class);

	private ServiceTracker<JAXBMessageClassProvider, JAXBMessageClassProvider> messageClassProviderTracker;

	private final List<DocumentedMessageClassProvider> messageClassProviders = new ArrayList<>();
	private final ReadWriteLock messageClassesLock = new ReentrantReadWriteLock();
	
	private final Map<String, DocumentationXSDFactory> xsdFactoriesByRootElement = new HashMap<>();
	private final ReadWriteLock xsdFactoryLock = new ReentrantReadWriteLock();
	
	@Reference
	private ContextProvider contextProvider;
	@Reference
	private SwaggerDocumentProvider swaggerDocumentProvider;
	private ServiceTracker<DocumentationXSDFactory, DocumentationXSDFactory> documentationXSDFactoryTracker;
	
	public static final String PREFIX_GLOBAL = "GLOBAL|";
	
	public static interface XSDProvider {
		InputStream getXSDDataAsStream();
	}

	@Activate
	public void activate(ComponentContext componentContext) {
		documentationXSDFactoryTracker = new ServiceTracker<DocumentationXSDFactory, DocumentationXSDFactory>(componentContext.getBundleContext(), DocumentationXSDFactory.class, null) {
			
			private final Map<DocumentationXSDFactory, SchemaModel> schemaModelsByFactory = new HashMap<>();
			
			@Override
			public DocumentationXSDFactory addingService(ServiceReference<DocumentationXSDFactory> reference) {
				final DocumentationXSDFactory service = super.addingService(reference);
				final DictionaryAdapter da = new DictionaryAdapter(reference).emptyStringAsNull();
				final String rootElementName = da.getString(DocumentationXSDFactory.OSGI_PROPERTY_ROOT_ELEMENT);
				if (rootElementName != null) {
					final String normalizedRootElementName = rootElementName.startsWith(PREFIX_GLOBAL) ? rootElementName.substring(PREFIX_GLOBAL.length()) : rootElementName;
					xsdFactoryLock.writeLock().lock();
					try {
						xsdFactoriesByRootElement.put(normalizedRootElementName, service);
						swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
							@Override
							public Object consume(Document swaggerDocument) {
								Definitions defs = swaggerDocument.getDefinitions();
								if (defs == null) {
									defs = new Definitions();
									swaggerDocument.setDefinitions(defs);
								}
								SchemaModel model = new SchemaModel(SchemaModel.Type.object);
								ExternalDocumentation externalDocs = new ExternalDocumentation("UNKNOWN") {
									@Override
									public String getUrl() {
										if (contextProvider != null) {
											Context ctx = contextProvider.getCurrentContext();
											if (ctx != null) {
												return ctx.createURIBuilder().pathElement("messages").pathElement(normalizedRootElementName).extension("xsd").build().asString();
											}
										}
										return super.getUrl();
									}
									
								};
								externalDocs.setDescription(da.getString(DocumentationXSDFactory.OSGI_PROPERTY_DESCRIPTION));
								model.setExternalDocs(externalDocs);
								defs.put(rootElementName, model);
								schemaModelsByFactory.put(service, model);
								return null;
							}
						});
					} finally {
						xsdFactoryLock.writeLock().unlock();
					}
				}
				return service;
			}

			@Override
			public void removedService(ServiceReference<DocumentationXSDFactory> reference, DocumentationXSDFactory service) {
				super.removedService(reference, service);
				final String rootElement = new DictionaryAdapter(reference).emptyStringAsNull().getString(DocumentationXSDFactory.OSGI_PROPERTY_ROOT_ELEMENT);
				if (rootElement != null) {
					xsdFactoryLock.writeLock().lock();
					final SchemaModel modelToRemove = schemaModelsByFactory.get(service);
					if (modelToRemove != null) {
						swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
							@Override
							public Object consume(Document swaggerDocument) {
								Definitions definitions = swaggerDocument.getDefinitions();
								if (definitions == null) {
									return null;
								}
								SchemaModel get = definitions.get(rootElement);
								if (get == modelToRemove) {
									definitions.remove(rootElement);
								}
								return null;
							}
						});
					}
					try {
						DocumentationXSDFactory existing = xsdFactoriesByRootElement.get(rootElement);
						if (existing == service) {
							xsdFactoriesByRootElement.remove(rootElement);
						}
					} finally {
						xsdFactoryLock.writeLock().unlock();
					}
				}
			}
			
		};
		documentationXSDFactoryTracker.open();
		messageClassProviderTracker = new ServiceTracker<JAXBMessageClassProvider, JAXBMessageClassProvider>(componentContext.getBundleContext(), JAXBMessageClassProvider.class, null) {
			@Override
			public JAXBMessageClassProvider addingService(ServiceReference<JAXBMessageClassProvider> reference) {
				JAXBMessageClassProvider messageClassProvider = super.addingService(reference);
				messageClassesLock.writeLock().lock();
				try {
					DocumentedMessageClassProvider documentedMessageClassProvider = new DocumentedMessageClassProvider(messageClassProvider);
					initMessageClassProvider(documentedMessageClassProvider);
					messageClassProviders.add(documentedMessageClassProvider);
				} finally {
					messageClassesLock.writeLock().unlock();
				}
				return messageClassProvider;
			}

			@Override
			public void removedService(ServiceReference<JAXBMessageClassProvider> reference, JAXBMessageClassProvider messageClassProvider) {
				messageClassesLock.writeLock().lock();
				try {
					Iterator<DocumentedMessageClassProvider> iter = messageClassProviders.iterator();
					while (iter.hasNext()) {
						final DocumentedMessageClassProvider provider = iter.next();
						if (provider.getMessageClassProvider() == messageClassProvider) {
							swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
								@Override
								public Object consume(Document swaggerDocument) {
									Definitions defs = swaggerDocument.getDefinitions();
									if (defs != null) {
										for (SchemaModel model : provider.getCreatedSchemaModels()) {
											Iterator<SchemaModel> iterator = defs.values().iterator();
											while (iterator.hasNext()) {
												if (iterator.next() == model) {
													iterator.remove();
												}
											}
										}
									}
									return null;
								}
							});
							iter.remove();
						}
					}
				} finally {
					messageClassesLock.writeLock().unlock();
				}
			}

		};
		messageClassProviderTracker.open();
	}

	@Deactivate
	public void deactivate() {
		messageClassProviderTracker.close();
		messageClassProviderTracker = null;
		documentationXSDFactoryTracker.close();
		documentationXSDFactoryTracker = null;
	}
	
	private void initMessageClassProvider(final DocumentedMessageClassProvider documentedMessageClassProvider) {
		Collection<Class<?>> classes = documentedMessageClassProvider.getMessageClassProvider().getJAXBMessageClasses();
		if (classes != null) {
			for (final Class<?> messageClass : classes) {
				final SchemaModel model = new SchemaModel(SchemaModel.Type.object);
				model.setExternalDocs(createExternalDocsOfMessageClass(messageClass));
				final String modelKey = messageClass.getName();
				Class<?> superCls = messageClass.getSuperclass();
				if (superCls.isAnnotationPresent(XmlAccessorType.class)) {
					model.setSuperModel(superCls.getName());
				}

				XmlAccessorType accessorType = messageClass.getAnnotation(XmlAccessorType.class);
				if (accessorType == null) {
					LOG.warn("found message class without XmlAccessorType: " + modelKey);
					continue;
				}
				if (accessorType.value() != XmlAccessType.NONE) {
					LOG.warn("found message class without XmlAccessorType=XmlAccessType.NONE: " + modelKey);
				}
				Field[] fields = messageClass.getDeclaredFields();
				for (Field field : fields) {
					XmlElements xmlElements = field.getAnnotation(XmlElements.class);
					if (xmlElements != null) {
						XmlElement[] elements = xmlElements.value();
						for (XmlElement element : elements) {
							Property p = createPropertyFromXmlElement(messageClass, field, element, model);
						}
					} else {
						XmlElement xmlElement = field.getAnnotation(XmlElement.class);
						if (xmlElement != null) {
							Property p = createPropertyFromXmlElement(messageClass, field, xmlElement, model);
						} else {
							XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
							if (xmlAttribute != null) {
								Property p = createPropertyFromXmlAttribute(messageClass, field, xmlAttribute, model);
							}
						}
					}
				}
				
				swaggerDocumentProvider.write(new SwaggerDocumentProvider.Consumer<Object>() {
					@Override
					public Object consume(Document swaggerDocument) {
						installMessageClassFromMessageClassProvider(swaggerDocument, modelKey, model, documentedMessageClassProvider);
						return null;
					}
				});
			}
		}
	}
	protected void installMessageClassFromMessageClassProvider(final Document swaggerDocument, final String modelKey, final SchemaModel model, DocumentedMessageClassProvider documentedMessageClassProvider) {
		Definitions defs = swaggerDocument.getDefinitions();
		if (defs == null) {
			defs = new Definitions();
			swaggerDocument.setDefinitions(defs);
		}
		defs.put(modelKey, model);
		documentedMessageClassProvider.getCreatedSchemaModels().add(model);
	}
	
	public ExternalDocumentation createExternalDocsOfMessageClass(Class<?> messageClass) {
		XmlRootElement rootElement = (XmlRootElement) messageClass.getAnnotation(XmlRootElement.class);
		final String rootElementName = rootElement == null ? null : rootElement.name();
		if (rootElementName != null) {
			ExternalDocumentation externalDoc = new ExternalDocumentation("UNKNOWN") {
				
				@Override
				public String getUrl() {
					if (contextProvider != null && rootElementName != null) {
						Context ctx = contextProvider.getCurrentContext();
						if (ctx != null) {
							return ctx.createURIBuilder().pathElement("messages").pathElement(rootElementName).extension("xsd").build().asString();
						}
					}
					return super.getUrl();
				}

			};
			externalDoc.setDescription("The XSD for the message class " + messageClass.getName());
			return externalDoc;
		}
		return null;
	}
	
	private Property createPropertyFromXmlElement(Class<?> messageClass, Field field, XmlElement element, SchemaModel model) {
		Class<?> fieldType = field.getType();
		
		XMLHint xmlHint = new XMLHint();
		String propertyName = fixXMLName(element.name(), field);
		xmlHint.setName(propertyName);
		
		Class elementType = element.type();
		if (XmlElement.DEFAULT.class.equals(elementType)) {
			elementType = fieldType;
		}

		if (isCollection(fieldType)) {
			SimpleProperty sp = new SimpleProperty(Parameter.Type.ARRAY);
			Class<?> collectionParameter = ReflectionUtil.getCollectionParameterType(field.getGenericType());
			if (collectionParameter != null && collectionParameter.getAnnotation(XmlAccessorType.class) != null) {
				Property itemProperty = new SchemaReference("#/definitions/" + collectionParameter.getName());
				sp.setItems(itemProperty);
				sp.setXml(xmlHint);
				addPropertyToSchemaModel(model, propertyName, sp);
				return sp;
			} else if (collectionParameter != null && isSimple(collectionParameter)) {
				SimpleProperty itemProperty = new SimpleProperty(mapJavaTypeToSwaggerType(collectionParameter));
				sp.setItems(itemProperty);
				sp.setXml(xmlHint);
				addPropertyToSchemaModel(model, propertyName, sp);
				return sp;
			} else {
				LOG.warn("could not map collection type to swagger type: " + fieldType.getName() + " " + field.getDeclaringClass() + "." + field.getName());
				return null;
			}
		} else if (isObject(fieldType) || Object.class.equals(fieldType)) {
			SchemaReference ref = new SchemaReference("#/definitions/" + elementType.getName());
			addPropertyToSchemaModel(model, propertyName, ref);
			return ref;
		} else if (isSimple(fieldType)) {
			Parameter.Type type = mapJavaTypeToSwaggerType(fieldType);
			if (type == null) {
				LOG.warn("could not map simple type to swagger type: " + fieldType.getName() + " " + field.getDeclaringClass() + "." + field.getName());
				return null;
			}
			SimpleProperty sp = new SimpleProperty(type);
			if (!field.getName().equals(xmlHint.getName())) {
				sp.setXml(xmlHint);
			}
			if (isDate(fieldType)) {
				sp.setFormat("date-time");
			}
			addPropertyToSchemaModel(model, xmlHint.getName(), sp);
			return sp;
		} else {
			LOG.warn("unsupported java type while generating json schema: " + fieldType.getName() + " " + field.getDeclaringClass() + "." + field.getName());
		}
		
		return null;
	}

	private void addPropertyToSchemaModel(SchemaModel model, String propertyName, Property property) {
		Map<String, Property> props = model.getProperties();
		if (props == null) {
			props = new HashMap<>();
			model.setProperties(props);
		}
		props.put(propertyName, property);
	}
	
	private Property createPropertyFromXmlAttribute(Class<?> messageClass, Field field, XmlAttribute xmlAttribute, SchemaModel model) {
		Class<?> fieldType = field.getType();
		
		XMLHint xmlHint = new XMLHint();
		String propertyName = fixXMLName(xmlAttribute.name(), field);
		xmlHint.setName(propertyName);
		xmlHint.setAttribute(Boolean.TRUE);
		
		if (isSimple(fieldType)) {
			Parameter.Type type = mapJavaTypeToSwaggerType(fieldType);
			if (type == null) {
				LOG.warn("could not map simple type to swagger type: " + fieldType.getName());
				return null;
			}
			SimpleProperty sp = new SimpleProperty(type);
			sp.setXml(xmlHint);
			if (field.getName().equals(xmlHint.getName())) {
				xmlHint.setName(null);
			}
			if (isDate(fieldType)) {
				sp.setFormat("date-time");
			}
			addPropertyToSchemaModel(model, propertyName, sp);
			return sp;
		} else {
			LOG.warn("unsupported java type while generating json schema for xml attribute: " + fieldType.getName());
		}
		
		return null;
	}
	
	public XSDProvider getXSDProviderForRootElement(String rootElement) {
		// see if there is a pre-registered XSDProvider
		xsdFactoryLock.readLock().lock();
		try {
			final DocumentationXSDFactory documentationXSDFactory = xsdFactoriesByRootElement.get(rootElement);
			if (documentationXSDFactory != null) {
				return new XSDProvider() {
					@Override
					public InputStream getXSDDataAsStream() {
						return documentationXSDFactory.getXSDDataAsStream();
					}
				};
			}
		} finally {
			xsdFactoryLock.readLock().unlock();
		}
		// if there is no predefined XSDProvider, then we will create it on the fly
		Class<?> messageClassByRootElementName = getMessageClassByRootElementName(rootElement);
		if (messageClassByRootElementName == null) {
			return null;
		}
		return createXSDProvider(messageClassByRootElementName);
	}
	
	private Class<?> getMessageClassByRootElementName(String rootElement) {
		messageClassesLock.readLock().lock();
		try {
			for (DocumentedMessageClassProvider messageClassProvider : messageClassProviders) {
				Class messageType = messageClassProvider.getMessageClassByRootElementName(rootElement);
				if (messageType != null) {
					return messageType;
				}
			}
		} finally {
			messageClassesLock.readLock().unlock();
		}
		return null;
	}
	
	private XSDProvider createXSDProvider(final Class messageType) {
		return new XSDProvider() {
			@Override
			public InputStream getXSDDataAsStream() {
				try {
					JAXBContext jaxbContext = JAXBContext.newInstance(messageType);
					final StringWriter sw = new StringWriter();
					SchemaOutputResolver or = new SchemaOutputResolver() {

							@Override
							public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
								StreamResult res = new StreamResult(sw);
								res.setSystemId("no-id");
								return res;
							}
						};
					jaxbContext.generateSchema(or);
					sw.flush();
					ByteArrayInputStream xsdInputStream = new ByteArrayInputStream(sw.toString().getBytes("UTF-8"));
					return xsdInputStream;
				} catch (IOException | JAXBException e) {
					LOG.error("failed to generate xsd for message type: " + messageType.getName(), e);
					return null;
				}
			}
		};
	}
}
