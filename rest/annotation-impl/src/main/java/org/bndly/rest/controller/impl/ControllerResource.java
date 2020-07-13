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
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ControllerResource implements Resource {
	private final ResourceProvider provider;
	private final ResourceURI uri;
	private final ControllerBinding binding;
	private final Object result;

	public ControllerResource(ResourceProvider provider, ResourceURI uri, ControllerBinding binding, Object result) {
		this.provider = provider;
		this.uri = uri;
		this.binding = binding;
		this.result = result;
	}

	@Override
	public ResourceURI getURI() {
		return uri;
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}

	public Object getResult() {
		return result;
	}

	public ControllerBinding getBinding() {
		return binding;
	}
	
}
