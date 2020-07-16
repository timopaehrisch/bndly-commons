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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeNameReplacingDocumentationDecorator extends EntityResourceDocumentationDecorator {

	public static interface VariableResolver {
		String resolve(String variableName);
	}

	private final VariableResolver variableResolver;
	
	public TypeNameReplacingDocumentationDecorator(EntityResource finalEntityResource) {
		super(finalEntityResource);
		variableResolver = new VariableResolver() {

			@Override
			public String resolve(String variableName) {
				if ("TYPE.NAME".equals(variableName)) {
					return entityResource.getType().getName();
				}
				return resolveInternal(variableName);
			}
		};
	}

	protected String resolveInternal(String variableName) {
		return null;
	}
	
	@Override
	public DocumentationInfo decorateDocumentationInfo(DocumentationProvider.Context context, DocumentationInfo defaultDocumentationInfo) {
		final String summary = filterStringForVariables(defaultDocumentationInfo.getSummary(), variableResolver);
		final String text = filterStringForVariables(defaultDocumentationInfo.getText(), variableResolver);
		return new DocumentationInfoWrapper(defaultDocumentationInfo) {

			@Override
			public String getSummary() {
				return summary;
			}

			@Override
			public String getText() {
				return text;
			}
			
		};
	}
	
	public static String filterStringForVariables(String input, VariableResolver variableResolver) {
		StringBuffer sb = new StringBuffer();
		boolean willOpen = false;
		boolean willClose = false;
		StringBuffer currentVariableName = null;
		
		
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '{') {
				willClose = false;
				if (!willOpen) {
					willOpen = true;
				} else {
					currentVariableName = new StringBuffer();
				}
			} else if (c == '}') {
				if (currentVariableName != null) {
					if (!willClose) {
						willClose = true;
					} else {
						String resolved = variableResolver.resolve(currentVariableName.toString());
						if (resolved != null) {
							sb.append(resolved);
						}
						currentVariableName = null;
						willClose = false;
					}
				} else {
					if (willOpen) {
						sb.append('{');
					}
					sb.append(c);
				}
				willOpen = false;
			} else {
				willOpen = false;
				willClose = false;
				if (currentVariableName != null) {
					currentVariableName.append(c);
				} else {
					sb.append(c);
				}
			}
		}
		if (currentVariableName != null) {
			sb.append("{{").append(currentVariableName);
		} else if (willOpen) {
			sb.append('{');
		}
		if (willClose) {
			sb.append('}');
		}
		
		return sb.toString();
	}
	
}
