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

import org.bndly.rest.controller.api.DocumentationInfo;
import org.bndly.rest.controller.api.DocumentationInfoWrapper;
import org.bndly.rest.controller.api.DocumentationProvider;
import org.bndly.rest.entity.resources.EntityResource;
import static org.bndly.rest.entity.resources.descriptor.DocumentationUtil.createQueryParameter;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.SimpleAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SortingSupportDocumentationDecorator extends EntityResourceDocumentationDecorator {

	public SortingSupportDocumentationDecorator(EntityResource entityResource) {
		super(entityResource);
	}

	@Override
	public DocumentationInfo decorateDocumentationInfo(DocumentationProvider.Context context, DocumentationInfo defaultDocumentationInfo) {
		List<DocumentationInfo.GenericParameterInfo> queryParams = defaultDocumentationInfo.getQueryParameters();
		List<DocumentationInfo.GenericParameterInfo> newQueryParams = new ArrayList<>();
		DocumentationInfo.GenericParameterInfo sortingDirectionParam = null;
		DocumentationInfo.GenericParameterInfo sortingFieldParam = null;
		for (DocumentationInfo.GenericParameterInfo queryParam : queryParams) {
			boolean addToList = true;
			if (EntityResource.SORTING_DIRECTION.equals(queryParam.getName())) {
				sortingDirectionParam = queryParam;
				addToList = isHavingDescription(queryParam);
			} else if (EntityResource.SORTING_FIELD.equals(queryParam.getName())) {
				sortingFieldParam = queryParam;
				addToList = isHavingDescription(queryParam);
			}
			if (addToList) {
				newQueryParams.add(queryParam);
			}
		}
		if (isHavingDescription(sortingFieldParam) && isHavingDescription(sortingDirectionParam)) {
			return defaultDocumentationInfo;
		}
		boolean hasSortingFields = false;
		if (!isHavingDescription(sortingFieldParam)) {
			StringBuffer sb = null;
			List<Attribute> atts = SchemaUtil.collectAttributes(entityResource.getType());
			if (atts != null) {
				for (Attribute att : atts) {
					if (SimpleAttribute.class.isInstance(att) && !att.isVirtual()) {
						String name = att.getName();
						if (sb == null) {
							sb = new StringBuffer(name);
						} else {
							sb.append(", ").append(name);
						}
					}
				}
				if (sb != null) {
					sortingFieldParam = createQueryParameter(
						EntityResource.SORTING_FIELD,
						"This parameter defines the attribute used to sort the elements in the list. The allowed values are: " + sb.toString(),
						false,
						Long.class
					);
					newQueryParams.add(sortingFieldParam);
					hasSortingFields = true;
				}
			}
		} else {
			hasSortingFields = true;
		}
		if (!isHavingDescription(sortingDirectionParam) && hasSortingFields) {
			sortingDirectionParam = createQueryParameter(
				EntityResource.SORTING_DIRECTION, "This paramter defines the sorting direction. The allowed values are: ASC, DESC", false, Long.class
			);
			newQueryParams.add(sortingDirectionParam);
		}
		final List<DocumentationInfo.GenericParameterInfo> finalQueryParams = Collections.unmodifiableList(newQueryParams);
		return new DocumentationInfoWrapper(defaultDocumentationInfo) {

			@Override
			public List<DocumentationInfo.GenericParameterInfo> getQueryParameters() {
				return finalQueryParams;
			}
			
		};
	}
	
	private boolean isHavingDescription(DocumentationInfo.GenericParameterInfo parameterInfo) {
		if (parameterInfo == null) {
			return false;
		}
		String desc = parameterInfo.getDescription();
		return desc != null && !desc.isEmpty();
	}
}
