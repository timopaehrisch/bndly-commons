package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.repository.Node;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class NodeResourceImpl implements NodeResource {
	private final Node node;
	private final ResourceURI resourceURI;
	private final ResourceProvider resourceProvider;

	public NodeResourceImpl(Node node, ResourceURI resourceURI, ResourceProvider resourceProvider) {
		if (node == null) {
			throw new IllegalArgumentException("node is not allowed to be null");
		}
		this.node = node;
		if (resourceURI == null) {
			throw new IllegalArgumentException("resourceURI is not allowed to be null");
		}
		this.resourceURI = resourceURI;
		if (resourceProvider == null) {
			throw new IllegalArgumentException("resourceProvider is not allowed to be null");
		}
		this.resourceProvider = resourceProvider;
	}
	
	@Override
	public final ResourceURI getURI() {
		return resourceURI;
	}

	@Override
	public final ResourceProvider getProvider() {
		return resourceProvider;
	}

	@Override
	public final Node getNode() {
		return node;
	}
	
}
