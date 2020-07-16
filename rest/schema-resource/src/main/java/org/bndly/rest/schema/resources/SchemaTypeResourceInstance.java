/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaTypeResourceInstance implements Resource {

	private final ResourceURI uri;
	private final ResourceProvider provider;
	private final Type type;
	private final Engine engine;

	public SchemaTypeResourceInstance(ResourceURI uri, ResourceProvider provider, Engine engine) {
		this.uri = uri;
		this.engine = engine;
		this.provider = provider;
		String typeName = uri.getPath().getElements().get(1);
		this.type = engine.getTableRegistry().getTypeTableByType(typeName).getType();
	}

	public Type getType() {
		return type;
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}

	@Override
	public ResourceURI getURI() {
		return uri;
	}
}
