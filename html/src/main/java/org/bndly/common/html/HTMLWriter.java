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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A HTMLWriter is a convenience utility to render HTML/XML Trees while allowing
 * a partial flush of already defined elements and attributes.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class HTMLWriter {

	private final Writer writer;
	private final List<Element> rootElements = new ArrayList<>();
	private Element currentElement;

	public HTMLWriter(Writer writer) {
		this.writer = writer;
	}

	public void flush() throws IOException {
		for (Element element : rootElements) {
			if (!element.isRendered()) {
				element.render();
			}
		}
	}

	/**
	 * A shared interface to separate render logic into attribute and element
	 * class.
	 *
	 * @author cybercon &lt;bndly@cybercon.de&gt;
	 */
	private interface Renderable {

		boolean isRendered();

		void render() throws IOException;
	}

	/**
	 * A model class for attributes in HTML or XML.
	 *
	 * @author cybercon &lt;bndly@cybercon.de&gt;
	 */
	private final class Attribute implements Renderable {

		private boolean rendered;
		private final String name;
		private final String value;

		public Attribute(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean isRendered() {
			return rendered;
		}

		@Override
		public void render() throws IOException {
			writer.append(getName());
			if (getValue() != null) {
				writer.append("=\"").append(getValue()).append('\"');
			}
			rendered = true;
		}

	}

	/**
	 * A model class for HTML or XML elements.
	 *
	 * @author cybercon &lt;bndly@cybercon.de&gt;
	 */
	private final class Element implements Renderable {

		private boolean renderedStart;
		private boolean renderingComplete;
		private boolean closed = false;

		private final List<Attribute> attributes = new ArrayList<>();
		private final Element parent;
		private final List<Renderable> childElements = new ArrayList<>();
		private final String type;

		public Element(String type, Element parent) {
			this.type = type;
			this.parent = parent;
		}

		public List<Attribute> getAttributes() {
			return attributes;
		}

		public List<Renderable> getChildElements() {
			return childElements;
		}

		public Element getParent() {
			return parent;
		}

		@Override
		public boolean isRendered() {
			return renderingComplete;
		}

		@Override
		public void render() throws IOException {
			if (!renderedStart) {
				writer.append('<').append(type);
				if (!attributes.isEmpty()) {
					for (Attribute attribute : attributes) {
						writer.append(' ');
						attribute.render();
					}
				}
				writer.append('>');
				renderedStart = true;
			}
			if (!childElements.isEmpty()) {
				for (Renderable element : childElements) {
					if (!renderingComplete && !element.isRendered()) {
						element.render();
					}
				}
			}
			if (closed) {
				if (!renderingComplete) {
					writer.append("</").append(type).append('>');
					renderingComplete = true;
				}
			}
		}

	}

	private final class Content implements Renderable {

		private boolean rendered;
		private final String value;

		public Content(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean isRendered() {
			return rendered;
		}

		@Override
		public void render() throws IOException {
			if (!rendered && value != null) {
				writer.append(value);
			}
			rendered = true;
		}

	}

	public final HTMLWriter createElement(String type) {
		currentElement = new Element(type, currentElement);
		Element p = currentElement.getParent();
		if (p != null) {
			p.getChildElements().add(currentElement);
		} else {
			rootElements.add(currentElement);
		}
		return this;
	}

	public final HTMLWriter closeElement() {
		currentElement.closed = true;
		currentElement = currentElement.getParent();
		return this;
	}

	public final HTMLWriter setAttribute(String name, String value) {
		Attribute att = new Attribute(name, value);
		currentElement.getAttributes().add(att);
		return this;
	}

	public final HTMLWriter content(String value) {
		currentElement.getChildElements().add(new Content(value));
		return this;
	}
}
