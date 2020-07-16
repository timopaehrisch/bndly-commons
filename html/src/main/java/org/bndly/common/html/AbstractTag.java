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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * A basic model class for representing HTML document trees. A tag can be self
 * closing or a regular tag with child content.
 *
 * @see SelfClosingTag
 * @see Tag
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractTag implements Content {

	private final ContentContainer parent;
	private String name;
	private List<Attribute> attributes;

	public AbstractTag(ContentContainer parent) {
		this.parent = parent;
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

	public final List<Attribute> getAttributes() {
		return attributes;
	}

	public final void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public final void setAttribute(String name, String value) {
		Attribute a = new Attribute();
		a.setName(name);
		a.setValue(value);
		List<Attribute> atts = getAttributes();
		if (atts == null) {
			atts = new ArrayList<>();
			setAttributes(atts);
		}
		atts.add(a);
	}

	public final Attribute getAttribute(String name) {
		List<Attribute> atts = getAttributes();
		if (atts == null) {
			return null;
		}
		for (Attribute attribute : atts) {
			if (name.equals(attribute.getName())) {
				return attribute;
			}
		}
		return null;
	}

}
