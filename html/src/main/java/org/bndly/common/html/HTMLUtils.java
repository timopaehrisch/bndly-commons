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
import java.util.ArrayList;
import java.util.List;

public final class HTMLUtils {

	private HTMLUtils() {
		// static methods only
	}

	public static void applyCSSOnBulletpoints(List<Content> content, boolean isInList, String bulletPointIcon) {
		if (content != null) {
			for (Content c : content) {
				if (Tag.class.isInstance(c)) {
					Tag tag = (Tag) c;
					if ("ul".equals(tag.getName())) {
						Attribute att = tag.getAttribute("class");
						String cls = "list-" + bulletPointIcon;
						if (att == null) {
							tag.setAttribute("class", cls);
						} else {
							String v = att.getValue();
							if (v == null) {
								att.setValue(cls);
							} else {
								att.setValue(att.getValue() + " " + cls);
							}
						}
						applyCSSOnBulletpoints(tag.getContent(), true, bulletPointIcon);
					} else {
						if ("li".equals(tag.getName()) && isInList) {
							Tag spanTag = new Tag(tag);
							spanTag.setName("span");
							spanTag.setAttribute("class", "icon iconfont-" + bulletPointIcon);
							spanTag.setContent(new ArrayList<Content>());
							spanTag.getContent().add(new Entity(spanTag, "#8203"));
							List<Content> liContent = tag.getContent();
							if (liContent == null) {
								liContent = new ArrayList<>();
								tag.setContent(liContent);
							}
							liContent.add(0, spanTag);
						}
						applyCSSOnBulletpoints(tag.getContent(), false, bulletPointIcon);
					}
				}
			}
		}
	}

	public static void applyCSSOnUl(List<Content> content, String listType) {
		if (content != null) {
			for (Content c : content) {
				if (Tag.class.isInstance(c)) {
					Tag tag = (Tag) c;
					if ("ul".equals(tag.getName())) {
						Attribute att = tag.getAttribute("class");
						String cls = "list-" + listType;
						if (att == null) {
							tag.setAttribute("class", cls);
						} else {
							String v = att.getValue();
							if (v == null) {
								att.setValue(cls);
							} else {
								att.setValue(att.getValue() + " " + cls);
							}
						}
						applyCSSOnUl(tag.getContent(), listType);
					} else {
						applyCSSOnUl(tag.getContent(), listType);
					}
				}
			}
		}
	}

	public static HTML applyCSSOnHTML(HTML html, final String cssClass) {
		List<Content> content = new ArrayList<>();
		if (html != null) {
			content = html.getContent();
		}
		for (Content c : content) {
			if (Tag.class.isInstance(c)) {
				Tag tag = (Tag) c;
				if ("p".equals(tag.getName())) {
					appendCSSClassToTag(tag, cssClass);
				}
			}
		}
		return html;
	}

	public static String applyCSSOnHTMLString(String html, final String cssClass) {
		if (html == null || "".equals(html)) {
			return null;
		}
		PrettyPrintHandler lightFontHandler = new PrettyPrintHandler() {
			private int level = 0;

			@Override
			public void openedTag(Tag tag) {
				if (isOnRootLevel() && "p".equals(tag.getName())) {
					appendCSSClassToTag(tag, cssClass);
				}
				super.openedTag(tag);
				level++;
			}

			@Override
			public void closedTag(Tag tag) {
				super.closedTag(tag);
				level--;
			}

			private boolean isOnRootLevel() {
				return level == 0;
			}

		};
		try {
			new Parser(html).handler(lightFontHandler).parse();
			return lightFontHandler.getPrettyString();
		} catch (HTMLParsingException | IOException ex) {
			throw new IllegalStateException("could not reformat html", ex);
		}
	}

	private static void appendCSSClassToTag(Tag tag, String cssClass) {
		Attribute classAtt = tag.getAttribute("class");
		if (classAtt == null) {
			tag.setAttribute("class", cssClass);
		} else {
			String v = classAtt.getValue();
			if (v == null) {
				classAtt.setValue(cssClass);
			} else {
				classAtt.setValue(v + " " + cssClass);
			}
		}
	}
}
