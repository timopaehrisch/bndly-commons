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
 * Text is a model class for text content in a HTML document. HTML entities are
 * not part of the text model. The length of a text is being determined by the
 * number of characters in the string that makes up the text.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Text implements Content {

	private String value;

	private final ContentContainer parent;

	public Text(ContentContainer parent) {
		this.parent = parent;
	}

	@Override
	public final ContentContainer getParent() {
		return parent;
	}

	public final String getValue() {
		return value;
	}

	public final void setValue(String value) {
		this.value = value;
	}

}
