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

import org.bndly.rest.api.StatusWriter;
import java.lang.reflect.Type;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface DocumentationInfo {
	public static interface ResponseInfo {
		StatusWriter.Code getCode();
		String getDescription();
		Type getJavaType();
	}
	public static interface ParameterInfo {
		String getName();
		String getDescription();
		Boolean isRequired();
		Type getJavaType();
	}
	public static interface BodyParameterInfo extends ParameterInfo {
		String getSchemaElementName();
	}
	public static interface HasExternalDocumentation {
		String getExternalDocumentationUrl();
		String getExternalDocumentationDescription();
	}
	public static interface GenericParameterInfo extends ParameterInfo {
	}
	
	String getSummary();
	String getText();
	List<String> getAuthors();
	List<String> getProduces();
	List<String> getConsumes();
	List<String> getTags();
	List<ResponseInfo> getResponses();
	List<GenericParameterInfo> getQueryParameters();
	List<GenericParameterInfo> getPathParameters();
	BodyParameterInfo getBodyParameter();
	List<DocumentationExample> getExamples();
}
