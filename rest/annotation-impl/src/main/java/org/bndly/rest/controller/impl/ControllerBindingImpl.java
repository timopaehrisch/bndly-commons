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

import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.PUT;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ControllerBindingImpl implements ControllerBinding {
	private final Documentation documentation;
	private final Class<?> controllerType;
	private final Object controller;
	private final String baseURI;
	private final ResourceURI uriPattern;
	private final Method javaMethod;
	private final HTTPMethod httpMethod;
	private final List<AtomLinkDescription> descriptions = new ArrayList<>();

	public ControllerBindingImpl(
			Class<?> controllerType, 
			Object controller, 
			String baseURI, 
			ResourceURI uriPattern, 
			Method javaMethod, 
			Annotation httpMethodAnnotation
	) {
		this.documentation = javaMethod.getAnnotation(Documentation.class);
		this.controllerType = controllerType;
		this.controller = controller;
		this.baseURI = baseURI;
		this.uriPattern = uriPattern;
		this.javaMethod = javaMethod;
		if (GET.class.isInstance(httpMethodAnnotation)) {
			httpMethod = HTTPMethod.GET;
		} else if (POST.class.isInstance(httpMethodAnnotation)) {
			httpMethod = HTTPMethod.POST;
		} else if (PUT.class.isInstance(httpMethodAnnotation)) {
			httpMethod = HTTPMethod.PUT;
		} else if (DELETE.class.isInstance(httpMethodAnnotation)) {
			httpMethod = HTTPMethod.DELETE;
		} else {
			throw new IllegalStateException("unsupported http method annotation " + httpMethodAnnotation.getClass().getName());
		}
	}

	@Override
	public HTTPMethod getHTTPMethod() {
		return httpMethod;
	}

	@Override
	public Method getMethod() {
		return javaMethod;
	}

	@Override
	public ResourceURI getResourceURIPattern() {
		return uriPattern;
	}

	@Override
	public Object getController() {
		return controller;
	}

	@Override
	public Class<?> getControllerType() {
		return controllerType;
	}

	@Override
	public Documentation getDocumentation() {
		return documentation;
	}

	@Override
	public List<AtomLinkDescription> getAtomLinkDescriptions() {
		return descriptions;
	}

	@Override
	public String getBaseURI() {
		return baseURI;
	}

}
