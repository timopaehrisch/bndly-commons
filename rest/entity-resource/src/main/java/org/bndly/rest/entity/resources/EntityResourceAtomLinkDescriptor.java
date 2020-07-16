package org.bndly.rest.entity.resources;

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
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescriptor;
import org.bndly.rest.atomlink.api.Fragment;
import org.bndly.rest.descriptor.DefaultAtomLinkDescriptor;
import org.bndly.rest.descriptor.DelegatingAtomLinkDescription;
import org.bndly.schema.model.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public abstract class EntityResourceAtomLinkDescriptor extends DefaultAtomLinkDescriptor implements AtomLinkDescriptor {

	protected final EntityResource entityResource;

	public EntityResourceAtomLinkDescriptor(EntityResource entityResource) {
		this.entityResource = entityResource;
		if (entityResource == null) {
			throw new IllegalArgumentException("entityResource is not allowed to be null");
		}
	}

	public String getBaseUri() {
		final String baseUri = getType().getSchema().getName() + "/" + getType().getName();
		return baseUri;
	}
	
	@Override
	public AtomLinkDescription getAtomLinkDescription(Object controller, Method method, AtomLink atomLink) {
		AtomLinkDescription desc = super.getAtomLinkDescription(controller, method, atomLink);
		if (desc == null) {
			return null;
		}
		final String baseUri = getBaseUri();
		return new DelegatingAtomLinkDescription(desc) {
			private Field injectionFieldOverride;
			private boolean useInjectionFieldOverride;

			@Override
			public Class<?> getLinkedInClass() {
				Class<?> r = getLinkedInClassOverride();
				if (r != null) {
					return r;
				}
				return super.getLinkedInClass();
			}

			@Override
			public String getSegment() {
				return baseUri;
			}
		};
	}

	protected final Type getType() {
		return entityResource.getType();
	}

	protected Class<?> getLinkedInClassOverride() {
		return null;
	}
}
