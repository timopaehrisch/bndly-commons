package org.bndly.rest.swagger.model;

/*-
 * #%L
 * REST Swagger Integration
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
public final class SchemaReference implements Property {
	private final String referencedElementName;
	private ExternalDocumentation externalDocumentation;

	public SchemaReference(String referencedElementName) {
		if (referencedElementName == null) {
			throw new IllegalArgumentException("referencedElementName is not allowed to be null");
		}
		this.referencedElementName = referencedElementName;
	}

	public String getReferencedElementName() {
		return referencedElementName;
	}

	public ExternalDocumentation getExternalDocumentation() {
		return externalDocumentation;
	}

	public void setExternalDocumentation(ExternalDocumentation externalDocumentation) {
		this.externalDocumentation = externalDocumentation;
	}
	
	
}
