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

import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.SchemaUtil;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaResourceAttributeInstance extends SchemaResourceInstance  {
	private Attribute attribute;

		public SchemaResourceAttributeInstance(ResourceURI uri, ResourceProvider provider, Engine engine) {
			super(uri, provider, engine);
			String attributeName = uri.getPath().getElements().get(3);
			this.attribute = null;
			for (Attribute attribute : SchemaUtil.collectAttributes(getRecord().getType())) {
				if (attribute.getName().equals(attributeName)) {
					this.attribute = attribute;
					break;
				}
			}
		}

		public Attribute getAttribute() {
			return attribute;
		}
}
