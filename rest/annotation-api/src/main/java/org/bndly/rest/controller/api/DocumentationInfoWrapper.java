package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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

import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DocumentationInfoWrapper implements DocumentationInfo {
	private final DocumentationInfo documentationInfo;

	public DocumentationInfoWrapper(DocumentationInfo documentationInfo) {
		this.documentationInfo = documentationInfo;
		if (documentationInfo == null) {
			throw new IllegalArgumentException("documentationInfo is not allowed to be null");
		}
	}

	@Override
	public String getText() {
		return documentationInfo.getText();
	}

	@Override
	public String getSummary() {
		return documentationInfo.getSummary();
	}

	@Override
	public List<String> getAuthors() {
		return documentationInfo.getAuthors();
	}

	@Override
	public List<String> getProduces() {
		return documentationInfo.getProduces();
	}

	@Override
	public List<String> getConsumes() {
		return documentationInfo.getConsumes();
	}

	@Override
	public List<String> getTags() {
		return documentationInfo.getTags();
	}

	@Override
	public List<ResponseInfo> getResponses() {
		return documentationInfo.getResponses();
	}

	@Override
	public List<GenericParameterInfo> getQueryParameters() {
		return documentationInfo.getQueryParameters();
	}

	@Override
	public List<GenericParameterInfo> getPathParameters() {
		return documentationInfo.getPathParameters();
	}

	@Override
	public BodyParameterInfo getBodyParameter() {
		return documentationInfo.getBodyParameter();
	}

	@Override
	public List<DocumentationExample> getExamples() {
		return documentationInfo.getExamples();
	}
	
}
