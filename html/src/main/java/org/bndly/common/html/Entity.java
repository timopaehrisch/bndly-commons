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
 * All HTML entities <code>&amp;name;</code> will be parsed as instances of this
 * class. The length of an entity is considered to be 1 when using the HTML
 * shortener.
 *
 * @see HTMLShortener
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Entity implements Content {

	private final ContentContainer parent;
	private String name;

	public Entity(ContentContainer parent) {
		this.parent = parent;
	}

	public Entity(ContentContainer parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	@Override
	public final ContentContainer getParent() {
		return parent;
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

}
