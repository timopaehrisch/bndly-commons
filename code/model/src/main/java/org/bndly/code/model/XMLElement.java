package org.bndly.code.model;

/*-
 * #%L
 * Code Model
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

public class XMLElement extends XMLObject {
	private XMLElement parent;
	private List<XMLElement> elements;
	private List<XMLAttribute> attributes;

	public XMLElement() {
		super();
	}

	public XMLElement(String name) {
		super();
		setName(name);
	}

	public XMLElement(String name, String ns) {
		super();
		setName(name);
		setNamespacePrefix(ns);
	}

	public XMLElement getParent() {
		return parent;
	}

	public List<XMLElement> getElements() {
		return elements;
	}

	public void setElements(List<XMLElement> elements) {
		this.elements = elements;
	}

	public List<XMLAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<XMLAttribute> attributes) {
		this.attributes = attributes;
	}

	public void add(XMLAttribute att) {
		if (attributes == null) {
			attributes = new ArrayList<>();
		}
		attributes.add(att);
	}

	public void add(XMLElement el) {
		if (elements == null) {
			elements = new ArrayList<>();
		}
		elements.add(el);
		el.parent = this;
	}

	public XMLAttribute createAttribute() {
		XMLAttribute att = create(XMLAttribute.class);
		add(att);
		return att;
	}

	public XMLAttribute createAttribute(String name, String namespacePrefix, String value) {
		XMLAttribute att = createAttribute();
		att.setName(name);
		att.setNamespacePrefix(namespacePrefix);
		att.setValue(value);
		return att;
	}

	public XMLAttribute createAttribute(String name, String value) {
		XMLAttribute att = createAttribute();
		att.setName(name);
		att.setValue(value);
		return att;
	}

	public XMLElement createElement() {
		XMLElement el = create(XMLElement.class);
		add(el);
		return el;
	}

	public XMLElement createElement(String name) {
		XMLElement el = createElement();
		el.setName(name);
		return el;
	}

	public XMLElement createElement(String name, String namespacePrefix) {
		XMLElement el = createElement(name);
		el.setNamespacePrefix(namespacePrefix);
		return el;
	}

	public XMLAttribute getAttribute(String name) {
		if (attributes != null && name != null) {
			for (XMLAttribute att : attributes) {
				if (name.equals(att.getName())) {
					return att;
				}
			}
		}
		return null;
	}
}
