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

import org.bndly.rest.controller.api.InputStreamResource;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.controller.api.ResponseResource;
import java.io.InputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class StreamWritingResource implements Resource, ResponseResource, InputStreamResource {

	private final Response response;
	private final InputStream is;
	private final ResourceProvider provider;
	private final ResourceURI uri;

	public StreamWritingResource(ResourceProvider provider, ResourceURI uri, Response response) {
		this.uri = uri;
		this.provider = provider;
		this.response = response;
		try {
			is = (InputStream) response.getEntity();
		} catch (ClassCastException e) {
			throw new IllegalStateException("stream writing response can only be created around responses with an inputstream as entity.");
		}
	}

	@Override
	public Response getResponse() {
		return response;
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public ResourceURI getURI() {
		return uri;
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}

}
