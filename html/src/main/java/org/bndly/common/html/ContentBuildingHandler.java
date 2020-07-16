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
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ContentBuildingHandler extends DefaultHandler {

	protected final List<Content> content = new ArrayList<>();
	protected final Stack<Tag> tagStack = new Stack<>();

	private Tag buildDefensiveCopy(Tag input) {
		ContentContainer parent = null;
		if (!tagStack.isEmpty()) {
			parent = tagStack.peek();
		}
		Tag t = new Tag(parent);
		t.setName(input.getName());
		List<Attribute> atts = input.getAttributes();
		if (atts != null) {
			for (Attribute attribute : atts) {
				t.setAttribute(attribute.getName(), attribute.getValue());
			}
		}
		return t;
	}

	@Override
	public void openedTag(Tag tag) {
		Tag defensiveCopy = buildDefensiveCopy(tag);
		appendToContent(defensiveCopy);
		tagStack.push(defensiveCopy);
	}

	@Override
	public void closedTag(Tag tag) {
		tagStack.pop();
	}

	@Override
	public void onSelfClosingTag(SelfClosingTag tag) {
		appendToContent(tag);
	}

	@Override
	public void onEntity(Entity entity) {
		appendToContent(entity);
	}

	@Override
	public void onText(Text text) {
		appendToContent(text);
	}

	private void appendToContent(Content c) {
		if (tagStack.isEmpty()) {
			content.add(c);
		} else {
			Tag p = tagStack.peek();
			List<Content> con = p.getContent();
			if (con == null) {
				con = new ArrayList<>();
				p.setContent(con);
			}
			con.add(c);
		}
	}

	public List<Content> getContent() {
		return content;
	}

}
