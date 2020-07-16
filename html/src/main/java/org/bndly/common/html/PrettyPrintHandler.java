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
 * The pretty print handler is a simple HTML parsing handler that pretty prints
 * the HTML to a string buffer. Pretty printing means, that the indents are
 * added by the depth of the according content in the document tree.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PrettyPrintHandler extends DefaultHandler {

	int indent = 0;
	final StringBuffer sb = new StringBuffer();
	private boolean skipNewLines;
	private boolean skipIndent;

	public PrettyPrintHandler skipNewLines() {
		this.skipNewLines = true;
		return this;
	}

	public PrettyPrintHandler skipIndent() {
		this.skipIndent = true;
		return this;
	}

	@Override
	public void onText(Text text) {
		printIndent();
		sb.append(text.getValue());
		if (!skipNewLines) {
			sb.append('\n');
		}
	}

	@Override
	public void onEntity(Entity entity) {
		printIndent();
		sb.append('&');
		sb.append(entity.getName());
		sb.append(';');
		if (!skipNewLines) {
			sb.append('\n');
		}
	}

	@Override
	public void onSelfClosingTag(SelfClosingTag tag) {
		printIndent();
		sb.append('<');
		sb.append(tag.getName());
		sb.append("/>");
		if (!skipNewLines) {
			sb.append('\n');
		}
	}

	@Override
	public void openedTag(Tag tag) {
		printIndent();
		sb.append('<');
		sb.append(tag.getName());
		List<Attribute> attributes = tag.getAttributes();
		if (attributes != null) {
			for (Attribute attribute : attributes) {
				String n = attribute.getName();
				if (n != null) {
					sb.append(' ').append(n);
					String v = attribute.getValue();
					if (v != null) {
						sb.append("=\"").append(v).append('"');
					}
				}
			}
		}
		sb.append('>');
		if (!skipNewLines) {
			sb.append('\n');
		}
		indent++;
	}

	@Override
	public void closedTag(Tag tag) {
		indent--;
		printIndent();
		sb.append("</");
		sb.append(tag.getName());
		sb.append('>');
		if (!skipNewLines) {
			sb.append('\n');
		}
	}

	private void printIndent() {
		if (skipIndent) {
			return;
		}
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
	}

	public String getPrettyString() {
		return sb.toString();
	}

	public static void printContent(List<Content> parsedContent, PrettyPrintHandler pph) {
		if (parsedContent == null) {
			return;
		}
		for (Content content : parsedContent) {
			printContent(content, pph);
		}
	}

	public static void printContent(Content content, PrettyPrintHandler pph) {
		if (Text.class.isInstance(content)) {
			pph.onText((Text) content);
		} else if (Entity.class.isInstance(content)) {
			pph.onEntity((Entity) content);
		} else if (SelfClosingTag.class.isInstance(content)) {
			pph.onSelfClosingTag((SelfClosingTag) content);
		} else if (Tag.class.isInstance(content)) {
			Tag tag = (Tag) content;
			pph.openedTag(tag);
			printContent(tag.getContent(), pph);
			pph.closedTag(tag);
		} else if (ProxyContent.class.isInstance(content)) {
			printContent(((ProxyContent) content).getContent(), pph);
		}
	}
}
