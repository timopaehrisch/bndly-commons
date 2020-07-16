package org.bndly.rest.form.mapper.impl;

/*-
 * #%L
 * REST Form Mapper
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MultipartReader {

	private static final String SEPARATOR = ";";
	private static final String BOUNDARY = "boundary=";
	
	public MultipartConfiguration parseConfigurationFromContentType(String contentTypeString) {
		int rawContentTypeIndex = contentTypeString.indexOf(SEPARATOR);
		int boundaryStartIndex = contentTypeString.indexOf(BOUNDARY, rawContentTypeIndex);
		if (rawContentTypeIndex < 0 || boundaryStartIndex < 0) {
			return null;
		}
		final String rawContentType = contentTypeString.substring(0, rawContentTypeIndex);
		final String boundary = contentTypeString.substring(boundaryStartIndex + BOUNDARY.length());
		if (boundary.isEmpty() || rawContentType.isEmpty()) {
			return null;
		}
		MultipartConfiguration config = new MultipartConfiguration() {

			@Override
			public String getMimeType() {
				return rawContentType;
			}

			@Override
			public String getBoundary() {
				return boundary;
			}
		};
		return config;
	}
}
