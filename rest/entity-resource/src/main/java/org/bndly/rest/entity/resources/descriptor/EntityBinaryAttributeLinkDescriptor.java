package org.bndly.rest.entity.resources.descriptor;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.descriptor.DelegatingAtomLinkDescription;
import org.bndly.rest.entity.resources.EntityBinaryAttributeResource;
import org.bndly.rest.entity.resources.EntityResourceAtomLinkDescriptor;
import java.lang.reflect.Method;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class EntityBinaryAttributeLinkDescriptor extends EntityResourceAtomLinkDescriptor {

	private final EntityBinaryAttributeResource entityBinaryAttributeResource;

	public EntityBinaryAttributeLinkDescriptor(EntityBinaryAttributeResource entityBinaryAttributeResource) {
		super(entityBinaryAttributeResource.getEntityResource());
		this.entityBinaryAttributeResource = entityBinaryAttributeResource;
	}

	@Override
	public String getBaseUri() {
		String baseUri = super.getBaseUri();
		baseUri += "/" + entityBinaryAttributeResource.getBinaryAttribute().getName();
		return baseUri;
	}

	@Override
	public AtomLinkDescription getAtomLinkDescription(Object controller, Method method, AtomLink atomLink) {
		final AtomLinkDescription desc = super.getAtomLinkDescription(controller, method, atomLink);
		return new DelegatingAtomLinkDescription(desc) {

			@Override
			public String getRel() {
				String r = super.getRel();
				return r + "_" + entityBinaryAttributeResource.getBinaryAttribute().getName();
			}
			
		};
	}

	@Override
	protected Class<?> getLinkedInClassOverride() {
		return entityResource.getRestBeanType();
	}
	
	
}
