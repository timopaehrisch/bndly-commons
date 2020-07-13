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

import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.jaxb.renderer.JAXBResourceRenderer.JAXBResource;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JAXBControllerResource extends ControllerResource implements JAXBResource {
	private final Object root;
	private final Class<? extends Object> rootType;

	public JAXBControllerResource(ResourceProvider provider, ResourceURI uri, ControllerBinding binding, Response response) {
		super(provider, uri, binding, response);
		this.root = response.getEntity();
		this.rootType = root.getClass();
		if (!rootType.isAnnotationPresent(XmlRootElement.class)) {
			throw new IllegalArgumentException("entity is not a jaxb annotated class");
		}
	}

	@Override
	public Class<?> getRootType() {
		return rootType;
	}

	@Override
	public Object getRootObject() {
		return root;
	}
	
}
