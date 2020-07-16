package org.bndly.rest.pdf.renderer;

/*-
 * #%L
 * REST PDF Renderer
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

import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PDFResource implements Resource {

	private final ResourceURI uri;
	private final ResourceProvider provider;
	private final Object entity;
	private final String cssName;
	private final String templateName;

	public PDFResource(ResourceURI uri, ResourceProvider provider, Object entity, String cssName, String templateName) {
		this.uri = uri;
		this.provider = provider;
		this.entity = entity;
		this.cssName = cssName == null ? "default.css" : cssName;
		this.templateName = templateName == null
				? entity == null 
					? null 
					: entity.getClass().getSimpleName() + ".vm"
				: templateName;
	}

	public PDFResource(ResourceURI uri, ResourceProvider provider, Object entity) {
		this.uri = uri;
		this.provider = provider;
		this.entity = entity;
		if (entity == null) {
			cssName = "default.css";
			templateName = null;
		} else {
			cssName = entity.getClass().getSimpleName() + ".css";
			templateName = entity.getClass().getSimpleName() + ".vm";
		}
	}

	
	@Override
	public ResourceURI getURI() {
		return uri;
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}

	public String getCssName() {
		return cssName;
	}

	public Object getEntity() {
		return entity;
	}

	public String getTemplateName() {
		return templateName;
	}
	
}
