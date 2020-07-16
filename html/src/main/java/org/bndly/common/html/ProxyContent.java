package org.bndly.common.html;

/*-
 * #%L
 * HTML
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
 * The proxy content serves as a wrapper for HTML content. For example when
 * parsing a HTML document it might be unclear what kind of content is being
 * parsed (regular tag or self closing tag). In order to be able to provide a
 * reference, this proxy object can be given to other instances.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ProxyContent implements Content {

	private Content content;
	private final ContentContainer parent;

	public ProxyContent(ContentContainer parent) {
		this.parent = parent;
	}

	@Override
	public final ContentContainer getParent() {
		return parent;
	}

	public final Content getContent() {
		return content;
	}

	public final void setContent(Content content) {
		this.content = content;
	}

}
