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

import org.bndly.rest.controller.api.DocumentationDecorator;
import org.bndly.rest.controller.api.DocumentationInfo;
import org.bndly.rest.controller.api.DocumentationInfoWrapper;
import org.bndly.rest.controller.api.DocumentationProvider;
import org.bndly.rest.entity.resources.EntityResource;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BeanConsumingDocumentationDecorator extends EntityResourceDocumentationDecorator implements DocumentationDecorator {

	public BeanConsumingDocumentationDecorator(EntityResource entityResource) {
		super(entityResource);
	}

	@Override
	public DocumentationInfo decorateDocumentationInfo(DocumentationProvider.Context context, DocumentationInfo defaultDocumentationInfo) {
		final Class<?> rbt = entityResource.getRestBeanType();
		final DocumentationInfo.BodyParameterInfo bpi = new DocumentationInfo.BodyParameterInfo() {

			@Override
			public String getSchemaElementName() {
				return rbt.getName();
			}

			@Override
			public String getName() {
				return "body";
			}

			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public Boolean isRequired() {
				return true;
			}

			@Override
			public Type getJavaType() {
				return rbt;
			}
		};
		return new DocumentationInfoWrapper(defaultDocumentationInfo) {

			@Override
			public DocumentationInfo.BodyParameterInfo getBodyParameter() {
				return bpi;
			}
			
		};
	}

}
