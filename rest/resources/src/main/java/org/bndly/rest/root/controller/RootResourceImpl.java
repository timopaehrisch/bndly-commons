package org.bndly.rest.root.controller;

/*-
 * #%L
 * REST Root Controller
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

import org.bndly.common.json.marshalling.Marshaller;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.descriptor.CommunicationDescriptionProviderImpl;
import org.bndly.rest.descriptor.model.LinkDescriptor;
import org.bndly.rest.descriptor.model.TypeBinding;
import org.bndly.rest.descriptor.model.TypeDescriptor;
import org.bndly.rest.descriptor.model.TypeMember;
import org.bndly.rest.descriptor.util.TypeUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Path("")
@Component(service = RootResourceImpl.class, immediate = true)
public class RootResourceImpl {

	@Reference
	private CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl;

	@Reference
	private AtomLinkInjector atomLinkInjector;

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Activate
	public void activate() {
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}

	@GET
	@AtomLink(target = Services.class)
	@Documentation(
		authors = "bndly@bndly.org",
		value = "This is the entry point into the Bndly eBusiness Core. From here on, you will be able to digg into the existing resources."
	)
	public Response getServices() {
		Services services = new Services();
		return Response.ok(services);
	}

	private JSObject buildCommunitcationDescriptionPayload() {
		Map<String, Object> result = generateCommunicationDescription();
		JSValue json = new Marshaller().marshall(result);
		final JSObject wrapper = new JSObject();
		wrapper.setMembers(new LinkedHashSet<JSMember>());
		JSMember types = new JSMember();
		types.setName(new JSString("types"));
		types.setValue(json);
		wrapper.getMembers().add(types);
		JSMember entryPoint = new JSMember();
		entryPoint.setName(new JSString("entry"));
		Services services = getServicesPayload();
		org.bndly.rest.common.beans.AtomLink lnk = services.follow("self");
		entryPoint.setValue(new JSString(lnk.getHref()));
		wrapper.getMembers().add(entryPoint);
		return wrapper;
	}

	private Services getServicesPayload() {
		Services services = new Services();
		atomLinkInjector.addDiscovery(services);
		return services;
	}

	@GET
	@Path("communicationDescription.jsonp")
	@AtomLink(rel = "communicationDescription", target = Services.class)
	@Documentation(
		authors = "bndly@bndly.org",
		value = "This resource is used by the Bndly eBusiness Admin in order to load a machine readable description of the exchanged messages and the links between the different resources."
	)
	public Response getCommunicationDescription(@Meta Context context) {
		final JSObject wrapper = buildCommunitcationDescriptionPayload();

		ResourceURI uri = context.getURI();
		ResourceURI.QueryParameter p = uri.getParameter("callback");
		String tmpCallbackName = "define";
		if (p != null) {
			if (p.getValue() != null) {
				tmpCallbackName = p.getValue();
			}
		}
		final String callbackName = tmpCallbackName;

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(bos, "UTF-8");
			writer.append(callbackName);
			writer.append("(");
			writer.flush();
			new JSONSerializer().serialize(wrapper, writer);
			writer.flush();
			writer.append(");");
			writer.flush();
			writer.close();
			byte[] communicationDescription = bos.toByteArray();
			return Response.ok(new ByteArrayInputStream(communicationDescription)).contentType(new ContentType() {

				@Override
				public String getName() {
					return "application/javascript";
				}

				@Override
				public String getExtension() {
					return "js";
				}

			}, "UTF-8");
		} catch (IOException e) {
			throw new IllegalStateException("could not serialize description", e);
		}
	}

	@GET
	@Path("communicationschema.xsd")
	@AtomLink(rel = "communicationschema", target = Services.class)
	@Documentation(
		authors = "bndly@bndly.org",
		value = "This XSD is generated from all available JAXB classes in the eBX Core."
	)
	public Response getCommunicationSchema(@Meta Context context) {
		String xsdAsString = communicationDescriptionProviderImpl.getXsdString();
		if (xsdAsString == null) {
			return Response.status(404);
		}
		try {
			byte[] bytes = xsdAsString.getBytes("UTF-8");
			context.setOutputContentType(ContentType.XML, "UTF-8");
			return Response.ok(new ByteArrayInputStream(bytes));
		} catch (UnsupportedEncodingException ex) {
			return Response.status(500);
		}
	}

	private String convertTypeMemberToJSType(Class<?> javaType) {
		String typeString;
		if (TypeUtil.isSimpleType(javaType)) {
			if (TypeUtil.isNumericType(javaType)) {
				typeString = "Number";
			} else if (TypeUtil.isStringType(javaType)) {
				typeString = "String";
			} else if (TypeUtil.isDateType(javaType)) {
				typeString = "Date";
			} else if (TypeUtil.isBooleanType(javaType)) {
				typeString = "Boolean";
			} else {
				throw new IllegalStateException("unmapped java type: " + javaType);
			}
		} else {
			if (TypeUtil.isCollection(javaType)) {
				typeString = "Collection";
			} else {
				typeString = javaType.getSimpleName();
			}
		}
		return typeString;
	}

	private Map<String, Object> generateCommunicationDescription() {
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();

		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<Class<?>, TypeDescriptor> entry : desc.entrySet()) {
			final TypeDescriptor typeDescriptor = entry.getValue();
			result.put(typeDescriptor.getName(), new Object() {
				public String getParent() {
					if (typeDescriptor.getParent() == null) {
						return null;
					}
					return typeDescriptor.getParent().getName();
				}

				public String getRootElement() {
					return typeDescriptor.getRootElement();
				}


				public Boolean getReferenceType() {
					return typeDescriptor.isReferenceType();
				}

				public List<String> getSub() {
					List<String> l = new ArrayList<>();
					for (TypeDescriptor subTypeDescriptor : typeDescriptor.getSubs()) {
						l.add(subTypeDescriptor.getName());
					}
					return l;
				}

				public Map<String, Object> getLinks() {
					Map<String, Object> links = new HashMap<>();
					for (final LinkDescriptor linkDescriptor : typeDescriptor.getLinks()) {
						links.put(linkDescriptor.getRel(), new Object() {
							public String getReturns() {
								if (linkDescriptor.getReturns() == null) {
									return null;
								}
								return linkDescriptor.getReturns().getName();
							}

							public String getConsumes() {
								if (linkDescriptor.getConsumes() == null) {
									return null;
								}
								return linkDescriptor.getConsumes().getName();
							}

							public String getDescription() {
								return linkDescriptor.getDescription();
							}
						});
					}
					return links;
				}

				public Map<String, Object> getMembers() {
					Map<String, Object> members = new HashMap<>();
					for (final TypeMember typeMember : typeDescriptor.getMembers()) {
						members.put(typeMember.getName(), new Object() {
							public Boolean getBeanId() {
								return typeMember.isBeanId();
							}

							public Boolean getCollection() {
								return typeMember.isCollection();
							}

							public String getType() {
								return convertTypeMemberToJSType(typeMember.getJavaType());
							}

							public Map<String, String> getBindings() {
								Map<String, String> bindings = new HashMap<>();
								for (final TypeBinding typeBinding : typeMember.getBindings()) {
									bindings.put(typeBinding.getName(), convertTypeMemberToJSType(typeBinding.getType()));
								}
								return bindings;
							}
						});
					}
					return members;
				}
			});
		}

		return result;
	}

	public void setCommunicationDescriptionProviderImpl(CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl) {
		this.communicationDescriptionProviderImpl = communicationDescriptionProviderImpl;
	}

	public void setAtomLinkInjector(AtomLinkInjector atomLinkInjector) {
		this.atomLinkInjector = atomLinkInjector;
	}

}
