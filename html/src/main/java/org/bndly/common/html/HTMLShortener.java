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
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class HTMLShortener {

	private static final class Holder {

		private Content content;
		private ContentContainer contentContainer;
		private Integer index;
		private boolean didAppendEndToken;

		public Content getContent() {
			return content;
		}

		public void setContent(Content content) {
			this.content = content;
		}

		public ContentContainer getContentContainer() {
			return contentContainer;
		}

		public void setContentContainer(ContentContainer contentContainer) {
			this.contentContainer = contentContainer;
		}

		public Integer getIndex() {
			return index;
		}

		public void setIndex(Integer index) {
			this.index = index;
		}

		public boolean didAppendEndToken() {
			return didAppendEndToken;
		}

		public void setDidAppendEndToken(boolean didAppendEndToken) {
			this.didAppendEndToken = didAppendEndToken;
		}

	}

	private HTMLShortener() {
	}

	/**
	 * Shortens the input string and strips all markup elements.
	 * <code>&lt;p&gt;bla&lt;/p&gt;</code> will be shortened to <code>bl</code>
	 * when a maxCharacters of 2 is used.
	 *
	 * @param inputHtml valid html string
	 * @param maxCharacters maximum number of visible text characters
	 * @return a string with a max length of maxCharacters and no markup
	 */
	public static String shorten(String inputHtml, final int maxCharacters) throws HTMLParsingException {
		return shorten(inputHtml, maxCharacters, false, null);
	}

	/**
	 * Shortens the input string and keeps all markup elements.
	 * <code>&lt;p&gt;bla&lt;/p&gt;</code> will be shortened to
	 * <code>&lt;p&gt;bl&lt;/p&gt;</code> when a maxCharacters of 2 is used.
	 *
	 * @param inputHtml valid html string
	 * @param maxCharacters maximum number of visible text characters
	 * @return a string with markup
	 */
	public static String shortenAndKeepMarkup(String inputHtml, final int maxCharacters) throws HTMLParsingException {
		return shorten(inputHtml, maxCharacters, true, null);
	}

	public static void shortenAndKeepMarkupOnHTML(HTML html, final int maxCharacters, String appendAtEnd) throws HTMLParsingException {
		List<Content> content = html.getContent();
		// 0 is the content, 1 is the container, 2 is the index in the container
		Holder holder = new Holder();
		shortenContentList(null, content, 0, maxCharacters, appendAtEnd, holder);
		if (holder.getContent() != null) {
			Content c = holder.getContent();
			if (Text.class.isInstance(c)) {
				Text tmpText = (Text) c;
				if (!tmpText.getValue().endsWith(appendAtEnd)) {
					tmpText.setValue(tmpText.getValue() + appendAtEnd);
				}
			} else if (Entity.class.isInstance(c)) {
				if (holder.getContentContainer() != null) {
					ContentContainer container = holder.getContentContainer();
					List<Content> contentItems = container.getContent();
					Text t = new Text(container);
					t.setValue(appendAtEnd);
					int index = holder.getIndex();
					contentItems.add(index, t);
				}
			}
		}
	}
	
	private static Content unwrap(Content content) {
		if (ProxyContent.class.isInstance(content)) {
			return unwrap(((ProxyContent) content).getContent());
		} else {
			return content;
		}
	}

	private static int shortenContentList(ContentContainer parent, List<Content> content, int length, int maxCharacters, final String appendAtEnd, final Holder holder) {
		if (content == null) {
			return length;
		}
		List<Content> toRemove = null;
		Text textToAppend = null;
		for (final Content _c : content) {
			if (length >= maxCharacters) {
				toRemove = appendToList(_c, toRemove);
			}
			final Content inspected = unwrap(_c);
			if (inspected == null) {
				continue;
			}
			if (Text.class.isInstance(inspected)) {
				Text text = (Text) inspected;
				int originalLength = length;
				int newLength = length + text.getValue().length();
				boolean textWillBeRemovedCompletely = false;
				if (newLength > maxCharacters) {
					String newText = cutTextAtLastSpace(text.getValue().substring(0, maxCharacters - length));
					text.setValue(newText);
					textWillBeRemovedCompletely = "".equals(newText);
					length = maxCharacters;
				} else {
					length = newLength;
				}
				if (length == maxCharacters) {
					if ("".equals(text.getValue().trim())) {
						// look for a previous text or entity
						toRemove = appendToList(_c, toRemove);
						if (!holder.didAppendEndToken()) {
							holder.setDidAppendEndToken(true);
							reverseTraverse(text, new TraverseListener() {
								@Override
								public void onContent(int index, Content content, ContentContainer container) {
									if (holder.getContent() != null) {
										return;
									}
									boolean isEntity = Entity.class.isInstance(content);
									boolean isText = Text.class.isInstance(content);
									if (isEntity) {
										holder.setContent(content);
										holder.setContentContainer(container);
										holder.setIndex(index);
									} else if (isText) {
										if (!"".equals(((Text) content).getValue().trim())) {
											holder.setContent(content);
											holder.setContentContainer(container);
											holder.setIndex(index);
										}
									}
								}
							});
						}
					} else {
						if (!holder.didAppendEndToken()) {
							holder.setDidAppendEndToken(true);
							text.setValue(text.getValue() + appendAtEnd);
						}
					}
				}
			} else if (Entity.class.isInstance(inspected)) {
				int newLength = length + 1;
				if (newLength > maxCharacters) {
					length = maxCharacters;
					toRemove = appendToList(_c, toRemove);
				} else {
					length = newLength;
					if (newLength == maxCharacters) {
						textToAppend = new Text(parent);
						textToAppend.setValue(appendAtEnd);
					}
				}
			} else if (Tag.class.isInstance(inspected)) {
				Tag tag = (Tag) inspected;
				List<Content> subContent = tag.getContent();
				length = shortenContentList(tag, subContent, length, maxCharacters, appendAtEnd, holder);
			} else if (SelfClosingTag.class.isInstance(inspected)) {
				// do nothing
			} else {
				throw new IllegalStateException("unsupported content");
			}
		}
		if (toRemove != null) {
			content.removeAll(toRemove);
		}
		if (textToAppend != null) {
			content.add(textToAppend);
		}
		return length;
	}

	private static interface TraverseListener {

		public void onContent(int index, Content content, ContentContainer container);
	}

	private static ContentContainer getRoot(ContentContainer contentContainer) {
		if (contentContainer == null) {
			return null;
		}
		if (contentContainer.getParent() != null) {
			return getRoot(contentContainer.getParent());
		}
		return contentContainer;
	}

	private static void reverseTraverse(Content content, TraverseListener listener) {
		ContentContainer contentContainer = content.getParent();
		if (contentContainer == null) {
			return;
		}
		List<Content> contentList = contentContainer.getContent();
		int index = -1;
		for (int i = contentList.size() - 1; i >= 0; i--) {
			Content c = contentList.get(i);
			if (ProxyContent.class.isInstance(c)) {
				c = ((ProxyContent) c).getContent();
			}
			if (c == content) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			throw new IllegalStateException("content is not in content list of container");
		}
		for (int i = index - 1; i >= 0; i--) {
			Content c = contentList.get(i);
			if (ProxyContent.class.isInstance(c)) {
				c = ((ProxyContent) c).getContent();
			}
			if (ContentContainer.class.isInstance(c)) {
				reverseTraverse((ContentContainer) c, listener);
			}
			listener.onContent(i, c, contentContainer);
		}
		reverseTraverse((Content) contentContainer, listener);

	}

	private static void reverseTraverse(ContentContainer contentContainer, TraverseListener listener) {
		List<Content> content = contentContainer.getContent();
		if (content != null) {
			for (int i = content.size() - 1; i >= 0; i--) {
				Content c = content.get(i);
				if (ProxyContent.class.isInstance(c)) {
					c = ((ProxyContent) c).getContent();
				}
				if (ContentContainer.class.isInstance(c)) {
					ContentContainer cc = (ContentContainer) c;
					reverseTraverse(cc, listener);
				}
				listener.onContent(i, c, contentContainer);
			}
		}
	}

	private static List appendToList(Object o, List list) {
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(o);
		return list;
	}

	/**
	 * Shortens the input string and keeps all markup elements. The value of
	 * appendAtEnd will be appended at the end inside the last element with
	 * textual content, if the result is shorter than the original input.
	 * <code>&lt;p&gt;bla&lt;/p&gt;</code> will be shortened to
	 * <code>&lt;p&gt;bl&lt;/p&gt;</code> when a maxCharacters of 2 is used.
	 *
	 * @param inputHtml valid html string
	 * @param maxCharacters maximum number of visible text characters
	 * @param appendAtEnd a String that will be appended after the last textual
	 * content, if the result is cut off.
	 * @return a string with markup
	 */
	public static String shortenAndKeepMarkup(String inputHtml, final int maxCharacters, String appendAtEnd) throws HTMLParsingException {
		return shorten(inputHtml, maxCharacters, true, appendAtEnd);
	}

	private static String shorten(String inputHtml, final int maxCharacters, final boolean keepMarkup, final String appendAtEnd)
			throws HTMLParsingException {
		try {
			final StringBuffer sb = new StringBuffer();
			final ParserConfig config = new DefaultParserConfig();
			new Parser(inputHtml, config).handler(new DefaultHandler() {
				int l = 0;
				boolean complete = false;
				Stack<Tag> tagStack = new Stack<>();
				private boolean didAppendAtEnd;

				@Override
				public void onText(Text text) {
					int length = text.getValue().length();
					if (!complete) {

						if (l + length >= maxCharacters) {
							length = maxCharacters - l;
							complete = true;
						}

						if (length == text.getValue().length()) {
							sb.append(text.getValue());
						} else {
							// get last text and cut on length
							String lastTxt = text.getValue().substring(0, length);
							lastTxt = cutTextAtLastSpace(lastTxt);
							sb.append(lastTxt);
						}
						l += length;
					} else {
						appendCompleteContent(appendAtEnd);
					}
				}

				@Override
				public void onEntity(Entity entity) {
					if (!complete) {
						sb.append('&').append(entity.getName()).append(';');
						l++;
						if (l == maxCharacters) {
							complete = true;
						}
					} else {
						appendCompleteContent(appendAtEnd);
					}
				}

				@Override
				public void onSelfClosingTag(SelfClosingTag tag) {
					if (keepMarkup) {
						if (complete) {
							return;
						}
						sb.append('<').append(tag.getName());
						if (!config.isSelfClosingTag(tag.getName())) {
							sb.append('/');
						}
						sb.append('>');
					}
				}

				@Override
				public void openedTag(Tag tag) {
					if (keepMarkup) {
						if (complete) {
							return;
						}
						tagStack.push(tag);
						sb.append('<').append(tag.getName());
						List<Attribute> attributes = tag.getAttributes();
						if (attributes != null) {
							for (Attribute attribute : attributes) {
								sb
										.append(' ')
										.append(attribute.getName());
								if (attribute.getValue() != null) {
									sb
											.append("=\"")
											.append(attribute.getValue())
											.append('\"');

								}
							}
						}
						sb.append('>');
					}
				}

				@Override
				public void closedTag(Tag tag) {
					if (keepMarkup) {
						if (!tagStack.isEmpty()) {
							Tag t = tagStack.pop();
							sb.append("</").append(t.getName()).append('>');
						}
					}
				}

				private void appendCompleteContent(String appendAtEnd) {
					if (appendAtEnd != null) {
						if (complete && !didAppendAtEnd) {
							sb.append(appendAtEnd);
							didAppendAtEnd = true;
						}
					}
				}

			}).parse();

			return sb.toString();

		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String cutTextAtLastSpace(String lastTxt) {
		// get index of last occurence of space
		int idx = lastTxt.lastIndexOf(" ");
		// if no space is found, cut on length
		if (idx > 0) {
			lastTxt = lastTxt.substring(0, idx);
		}
		return lastTxt;
	}
}
