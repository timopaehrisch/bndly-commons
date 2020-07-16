package org.bndly.code.output;

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

import org.bndly.code.model.XMLAttribute;
import org.bndly.code.model.XMLElement;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class XMLWriter {

	public void write(XMLElement el, OutputStream os) throws IOException {
		write(el, os, "UTF-8");
	}

	public void write(XMLElement el, OutputStream os, String encoding) throws IOException {
		write(el, new OutputStreamWriter(os, encoding));
	}

	public void write(XMLElement el, Writer writer) throws IOException {
		writeElement(el, writer);
		writer.flush();
	}

	private void writeElement(XMLElement el, Writer writer) throws IOException {
		if (el != null) {
			String name = el.getName();
			if (name != null) {
				writer.write('<');
				String ns = el.getNamespacePrefix();
				if (ns != null) {
					writer.write(ns);
					writer.write(":");
				}
				writeName(name, writer);

				List<XMLAttribute> attributes = el.getAttributes();
				if (attributes != null) {
					for (XMLAttribute xMLAttribute : attributes) {
						writer.write(' ');
						writeAttribute(xMLAttribute, writer);
					}
				}
				List<XMLElement> children = el.getElements();
				if (children != null && !children.isEmpty()) {
					writer.write(">");
					for (XMLElement xMLElement : children) {
						write(xMLElement, writer);
					}
					writer.write("</");
					if (ns != null) {
						writer.write(ns);
						writer.write(":");
					}
					writeName(name, writer);
					writer.write('>');
				} else {
					writer.write("/>");
				}
			}
		}
	}

	private void writeAttribute(XMLAttribute xMLAttribute, Writer writer) throws IOException {
		if (xMLAttribute != null) {
			String name = xMLAttribute.getName();
			if (name != null) {
				String ns = xMLAttribute.getNamespacePrefix();
				if (ns != null) {
					writer.write(ns);
					writer.write(":");
				}
				writeName(name, writer);
				String value = xMLAttribute.getValue();
				if (value != null) {
					writer.write("=\"");
					writeContentEscaped(value, writer);
					writer.write("\"");
				}
			}
		}
	}

	private void writeContentEscaped(String content, Writer writer) throws IOException {
		for (int i = 0; i < content.length(); i++) {
			char character = content.charAt(i);
			if (character == '"') {
				writer.append("&quot;");
			} else if (character == '&') {
				writer.append("&amp;");
			} else if (character == '<') {
				writer.append("&lt;");
			} else if (character == '>') {
				writer.append("&gt;");
			} else {
				writer.append(character);
			}
		}
	}

	private void writeName(String name, Writer writer) throws IOException {
		boolean isStart = true;
		for (int index = 0; index < name.length(); index++) {
			char character = name.charAt(index);
			if (isStart) {
				if (isNameStartChar(character)) {
					writer.append(character);
					isStart = false;
				} else {
					throw new XMLIllegalCharacterException("invalid start character for xml name.");
				}
			} else {
				if (isNameChar(character)) {
					writer.append(character);
				} else {
					throw new XMLIllegalCharacterException("invalid character for xml name.");
				}
			}
		}
	}
	
	private boolean isNameStartChar(char character) {
		if (character == ':') {
			return true;
		} else if (character >= 'A' && character <= 'Z') {
			return true;
		} else if (character == '_') {
			return true;
		} else if (character >= 'a' && character <= 'z') {
			return true;
		} else if (character >= 0xC0 && character <= 0xD6) {
			return true;
		} else if (character >= 0xD8 && character <= 0xF6) {
			return true;
		} else if (character >= 0xF8 && character <= 0x2FF) {
			return true;
		} else if (character >= 0x370 && character <= 0x37D) {
			return true;
		} else if (character >= 0x37F && character <= 0x1FFF) {
			return true;
		} else if (character >= 0x200C && character <= 0x200D) {
			return true;
		} else if (character >= 0x2070 && character <= 0x218F) {
			return true;
		} else if (character >= 0x2C00 && character <= 0x2FEF) {
			return true;
		} else if (character >= 0x3001 && character <= 0xD7FF) {
			return true;
		} else if (character >= 0xF900 && character <= 0xFDCF) {
			return true;
		} else if (character >= 0xFDF0 && character <= 0xFFFD) {
			return true;
		} else if (character >= 0x10000 && character <= 0xEFFFF) {
			return true;
		}
		return false;
	}
	
	private boolean isNameChar(char character) {
		if (isNameStartChar(character)) {
			return true;
		}
		if (character == '-') {
			return true;
		} else if (character == '.') {
			return true;
		} else if (character >= '0' && character <= '9') {
			return true;
		} else if (character == 0xB7) {
			return true;
		} else if (character >= 0x0300 && character <= 0x036F) {
			return true;
		} else if (character >= 0x203F && character <= 0x2040) {
			return true;
		}
		return false;
	}
}
