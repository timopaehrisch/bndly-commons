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

import java.util.List;

/**
 *
 * A HTML tag may contain further content. It is closed by a separate closing
 * tag. For example &lt;foo&gt;&lt;/foo&gt;.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Tag extends AbstractTag implements ContentContainer {

	private List<Content> content;

	public Tag(ContentContainer parent) {
		super(parent);
	}

	@Override
	public final List<Content> getContent() {
		return content;
	}

	public final void setContent(List<Content> content) {
		this.content = content;
	}
}
