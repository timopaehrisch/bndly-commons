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
import org.bndly.schema.model.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaAnnotationTagsDocumentationDecorator extends EntityResourceDocumentationDecorator {

	public SchemaAnnotationTagsDocumentationDecorator(EntityResource entityResource) {
		super(entityResource);
	}

	@Override
	public DocumentationInfo decorateDocumentationInfo(DocumentationProvider.Context context, DocumentationInfo defaultDocumentationInfo) {
		Type type = entityResource.getType();
		String tags = type.getAnnotations().get("tags");
		ArrayList<String> finalTags = new ArrayList<>();
		if (tags != null) {
			String[] tagsArray = tags.split(",");
			for (String tag : tagsArray) {
				String tagTrimmed = tag.trim();
				if (!tagTrimmed.isEmpty()) {
					finalTags.add(tagTrimmed);
				}
			}
		}
		finalTags.addAll(defaultDocumentationInfo.getTags());
		
		final List<String> extendedTags = Collections.unmodifiableList(finalTags);
		return new DocumentationInfoWrapper(defaultDocumentationInfo) {

			@Override
			public List<String> getTags() {
				return extendedTags;
			}
			
		};
	}
	
}
