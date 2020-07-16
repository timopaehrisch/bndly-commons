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
import static org.bndly.rest.entity.resources.descriptor.DocumentationUtil.createQueryParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PaginationSupportDocumentationDecorator implements DocumentationDecorator {

	@Override
	public DocumentationInfo decorateDocumentationInfo(DocumentationProvider.Context context, DocumentationInfo defaultDocumentationInfo) {
		List<DocumentationInfo.GenericParameterInfo> queryParams = defaultDocumentationInfo.getQueryParameters();
		List<DocumentationInfo.GenericParameterInfo> newQueryParams = new ArrayList<>();
		DocumentationInfo.GenericParameterInfo pageSizeParam = null;
		DocumentationInfo.GenericParameterInfo pageStartParam = null;
		for (DocumentationInfo.GenericParameterInfo queryParam : queryParams) {
			if (EntityResource.PAGINATION_SIZE.equals(queryParam.getName())) {
				pageSizeParam = queryParam;
			} else if (EntityResource.PAGINATION_START.equals(queryParam.getName())) {
				pageStartParam = queryParam;
			}
			newQueryParams.add(queryParam);
		}
		if (pageSizeParam != null && pageStartParam != null) {
			return defaultDocumentationInfo;
		}
		if (pageSizeParam == null) {
			pageSizeParam = createQueryParameter(EntityResource.PAGINATION_SIZE, "the number of elements to load", false, Long.class);
			newQueryParams.add(pageSizeParam);
		}
		if (pageStartParam == null) {
			pageStartParam = createQueryParameter(EntityResource.PAGINATION_START, "the offset of elements to load", false, Long.class);
			newQueryParams.add(pageStartParam);
		}
		final List<DocumentationInfo.GenericParameterInfo> finalQueryParams = Collections.unmodifiableList(newQueryParams);
		return new DocumentationInfoWrapper(defaultDocumentationInfo) {

			@Override
			public List<DocumentationInfo.GenericParameterInfo> getQueryParameters() {
				return finalQueryParams;
			}
			
		};
	}
	
}
