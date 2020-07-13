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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.controller.api.EntityParser;
import org.bndly.rest.controller.api.EntityRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.osgi.framework.ServiceRegistration;
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
@Component(service = XMLAndJSONSupport.class, immediate = true)
@Designate(ocd = XMLAndJSONSupport.Configuration.class)
public class XMLAndJSONSupport {
	private static final Logger LOG = LoggerFactory.getLogger(XMLAndJSONSupport.class);

	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Render with local context",
				description = "If set to true, the renderer renders a JAXB entity with a fresh JAXB Context instance created from the entity class"
		)
		boolean renderWithLocalContext() default false;

		@AttributeDefinition(
				name = "Parse with local context",
				description = "If set to true, the parser will parse the payload with a JAX Context created from the required type."
		)
		boolean parseWithLocalContext() default false;

		@AttributeDefinition(
				name = "XML to JSON namespaces",
				description = "This property defines the namespace mappings between XML and JSON. JSON attribtues will be prefixed for each namespace. This property contains mappings in the form of 'prefix@namespaceurl' in a comma separated string"
		)
		String[] xmlToJsonNamespaces() default {"xs@http://www.w3.org/2001/XMLSchema-instance"};
	}

	private final List<JAXBMessageClassProvider> messageClassProviders = new ArrayList<>();
	private final Set<Class> contextClasses = new HashSet<>();
	private JAXBContext context;
	private final ReadWriteLock contextLock = new ReentrantReadWriteLock();
	private final Map<String,String> xmlToJsonNamespaces = new HashMap<>();
	private Boolean renderWithLocalContext;
	private Boolean parseWithLocalContext;
	private XMLLocalContextEntityRenderer xmlLocal;
	private XMLGlobalContextEntityRenderer xmlGlobal;
	private JSONLocalEntityRenderer jsonLocal;
	private JSONGlobalEntityRenderer jsonGlobal;
	private ServiceRegistration<EntityRenderer> xmlRenderReg;
	private ServiceRegistration<EntityParser> xmlParseReg;
	private ServiceRegistration<EntityRenderer> jsonRenderReg;
	private ServiceRegistration<EntityParser> jsonParseReg;
	protected static final String[] DEFAULT_NAMESPACE_MAPPINGS = new String[]{
		"xs@http://www.w3.org/2001/XMLSchema-instance"
	};

	@Reference(
			bind = "addJAXBMessageClassProvider",
			unbind = "removeJAXBMessageClassProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = JAXBMessageClassProvider.class
	)
	public void addJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			contextLock.writeLock().lock();
			try {
				messageClassProviders.add(messageClassProvider);
				rebuildJaxbContext();
			} finally {
				contextLock.writeLock().unlock();
			}
		}
	}

	public void removeJAXBMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		if (messageClassProvider != null) {
			contextLock.writeLock().lock();
			try {
				Iterator<JAXBMessageClassProvider> iterator = messageClassProviders.iterator();
				while (iterator.hasNext()) {
					JAXBMessageClassProvider next = iterator.next();
					if (next == messageClassProvider) {
						iterator.remove();
					}
				}
				rebuildJaxbContext();
			} finally {
				contextLock.writeLock().unlock();
			}
		}
	}

	private void rebuildJaxbContext() {
		ArrayList<Class<?>> arrayList = new ArrayList<>();
		for (JAXBMessageClassProvider messageClassProvider : messageClassProviders) {
			arrayList.addAll(messageClassProvider.getJAXBMessageClasses());
		}
		Class[] asArray = arrayList.toArray(new Class[arrayList.size()]);
		try {
			this.context = JAXBContext.newInstance(asArray);
			this.contextClasses.clear();
			this.contextClasses.addAll(arrayList);
		} catch (JAXBException ex) {
			LOG.error("failed to create new jaxb context: " + ex.getMessage(), ex);
		}
	}

	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter da = new DictionaryAdapter(componentContext.getProperties());
		this.renderWithLocalContext = da.getBoolean("renderWithLocalContext", Boolean.FALSE);
		this.parseWithLocalContext = da.getBoolean("parseWithLocalContext", Boolean.FALSE);
		Collection<String> mappings = da.getStringCollection("xmlToJsonNamespaces", DEFAULT_NAMESPACE_MAPPINGS);
		if (mappings != null) {
			for (String mapping : mappings) {
				int i = mapping.indexOf("@");
				if (i > -1) {
					String prefix = mapping.substring(0, i);
					String namespace = mapping.substring(i + 1);
					xmlToJsonNamespaces.put(namespace, prefix);
				}
			}
		}
		this.xmlLocal = new XMLLocalContextEntityRenderer();
		this.xmlGlobal = new XMLGlobalContextEntityRenderer(contextLock) {
			@Override
			protected JAXBContext getJAXBContext() {
				return context;
			}
		};
		this.jsonLocal = new JSONLocalEntityRenderer(xmlToJsonNamespaces);
		this.jsonGlobal = new JSONGlobalEntityRenderer(contextLock, xmlToJsonNamespaces) {
			@Override
			protected JAXBContext getJAXBContext() {
				return context;
			}
		};

		registerXml(componentContext);
		registerJson(componentContext);
	}

	@Deactivate
	public void deactivate() {
		if (xmlRenderReg != null) {
			xmlRenderReg.unregister();
			xmlRenderReg = null;
		}
		if (xmlParseReg != null) {
			xmlParseReg.unregister();
			xmlParseReg = null;
		}
		if (jsonRenderReg != null) {
			jsonRenderReg.unregister();
			jsonRenderReg = null;
		}
		if (jsonParseReg != null) {
			jsonParseReg.unregister();
			jsonParseReg = null;
		}
	}

	private void registerXml(ComponentContext componentContext) {
		if (renderWithLocalContext) {
			xmlRenderReg = ServiceRegistrationBuilder
					.newInstance(EntityRenderer.class, xmlLocal)
					.pid(EntityRenderer.class.getName() + ".xml")
					.property("contentType", "xml")
					.register(componentContext.getBundleContext());
		} else {
			xmlRenderReg = ServiceRegistrationBuilder
					.newInstance(EntityRenderer.class, xmlGlobal)
					.pid(EntityRenderer.class.getName() + ".xml")
					.property("contentType", "xml")
					.register(componentContext.getBundleContext());
		}
		if (parseWithLocalContext) {
			xmlParseReg = ServiceRegistrationBuilder
					.newInstance(EntityParser.class, xmlLocal)
					.pid(EntityParser.class.getName() + ".xml")
					.property("contentType", "xml")
					.register(componentContext.getBundleContext());
		} else {
			xmlParseReg = ServiceRegistrationBuilder
					.newInstance(EntityParser.class, xmlGlobal)
					.pid(EntityParser.class.getName() + ".xml")
					.property("contentType", "xml")
					.register(componentContext.getBundleContext());
		}
	}

	private void registerJson(ComponentContext componentContext) {
		if (renderWithLocalContext) {
			jsonRenderReg = ServiceRegistrationBuilder
					.newInstance(EntityRenderer.class, jsonLocal)
					.pid(EntityRenderer.class.getName() + ".json")
					.property("contentType", "json")
					.register(componentContext.getBundleContext());
		} else {
			jsonRenderReg = ServiceRegistrationBuilder
					.newInstance(EntityRenderer.class, jsonGlobal)
					.pid(EntityRenderer.class.getName() + ".json")
					.property("contentType", "json")
					.register(componentContext.getBundleContext());
		}
		if (parseWithLocalContext) {
			jsonParseReg = ServiceRegistrationBuilder
					.newInstance(EntityParser.class, jsonLocal)
					.pid(EntityParser.class.getName() + ".json")
					.property("contentType", "json")
					.register(componentContext.getBundleContext());
		} else {
			jsonParseReg = ServiceRegistrationBuilder
					.newInstance(EntityParser.class, jsonGlobal)
					.pid(EntityParser.class.getName() + ".json")
					.property("contentType", "json")
					.register(componentContext.getBundleContext());
		}
	}
}
