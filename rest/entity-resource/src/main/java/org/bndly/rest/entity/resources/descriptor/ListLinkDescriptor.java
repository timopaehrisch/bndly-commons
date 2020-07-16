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
import org.bndly.rest.entity.resources.EntityResource;
import org.bndly.rest.entity.resources.EntityResourceAtomLinkDescriptor;
import java.lang.reflect.Method;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ListLinkDescriptor extends EntityResourceAtomLinkDescriptor {

	public ListLinkDescriptor(EntityResource entityResource) {
		super(entityResource);
	}

	@Override
	public AtomLinkDescription getAtomLinkDescription(Object controller, final Method method, final AtomLink atomLink) {
		DelegatingAtomLinkDescription desc = new DelegatingAtomLinkDescription(super.getAtomLinkDescription(controller, method, atomLink)) {
			@Override
			public Class<?> getLinkedInClass() {
				return entityResource.getListRestBean();
			}

			@Override
			public Class<?> getReturnType() {
				if ("self".equals(getRel()) || "next".equals(getRel()) || "previous".equals(getRel())) {
					return entityResource.getListRestBean();
				}
				return super.getReturnType();
			}

			@Override
			public Class<?> getConsumesType() {
				if ("find".equals(getRel())) {
					return entityResource.getRestBeanType();
				} else if ("findAll".equals(getRel())) {
					return entityResource.getRestBeanType();
				} else if ("add".equals(getRel()) || "addUnCascaded".equals(getRel())) {
					return entityResource.getRestBeanType();
				}
				return super.getConsumesType();
			}

		};
		return desc;
	}

	@Override
	protected Class<?> getLinkedInClassOverride() {
		return entityResource.getListRestBean();
	}

}
