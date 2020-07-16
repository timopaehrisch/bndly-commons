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

import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.rest.swagger.model.SchemaModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DocumentedMessageClassProvider {

	private final JAXBMessageClassProvider messageClassProvider;
	private final Map<String, Class> messageClassesByRootElementName = new HashMap<>();
	private final List<SchemaModel> createdSchemaModels = new ArrayList<>();

	public DocumentedMessageClassProvider(JAXBMessageClassProvider messageClassProvider) {
		this.messageClassProvider = messageClassProvider;
		Collection<Class<?>> classes = messageClassProvider.getJAXBMessageClasses();
		for (Class<?> messageClass : classes) {
			XmlRootElement rootElement = messageClass.getAnnotation(XmlRootElement.class);
			if (rootElement != null) {
				messageClassesByRootElementName.put(rootElement.name(), messageClass);
			}
		}
	}

	public JAXBMessageClassProvider getMessageClassProvider() {
		return messageClassProvider;
	}
	
	public Class getMessageClassByRootElementName(String rootElementName) {
		return messageClassesByRootElementName.get(rootElementName);
	}

	public List<SchemaModel> getCreatedSchemaModels() {
		return createdSchemaModels;
	}
	
}
