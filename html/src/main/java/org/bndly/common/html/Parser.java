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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 *
 * The parser is used to read HTML input and notify a handler about found HTML
 * content. The notification interface is
 * {@link Handler}.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Parser {

	private final List<Content> parsedContent = new ArrayList<>();
	private final Stack<ParsingState> states = new Stack<>();
	private final Reader reader;
	private final StringBuffer readContent = new StringBuffer();
	private final ParserConfig config;
	private long row;
	private long column;
	private Handler handler;

	public Parser(String inputString) {
		this(inputString, null);
	}

	public Parser(String inputString, ParserConfig config) {
		this(new StringReader(inputString), config);
	}

	public Parser(InputStream is) {
		this(is, (ParserConfig) null);
	}

	public Parser(InputStream is, ParserConfig config) {
		this(new InputStreamReader(is), config);
	}

	public Parser(InputStream is, String encoding) throws UnsupportedEncodingException {
		this(is, encoding, null);
	}

	public Parser(InputStream is, String encoding, ParserConfig config) throws UnsupportedEncodingException {
		this(new InputStreamReader(is, encoding), config);
	}

	public Parser(Reader reader) {
		this(reader, null);
	}

	public Parser(Reader reader, ParserConfig config) {
		if (config == null) {
			config = new DefaultParserConfig();
		}
		this.reader = reader;
		this.config = config;
	}

	public Parser handler(Handler handler) {
		this.handler = handler;
		return this;
	}

	public Parser parse() throws HTMLParsingException, IOException {
		if (handler == null) {
			handler = DefaultHandler.NO_OP;
		}
		ContentContainer container = new ContentContainer() {

			@Override
			public List<Content> getContent() {
				return parsedContent;
			}

			@Override
			public ContentContainer getParent() {
				return null;
			}
		};
		states.push(new NewContentParsingState(container));
		int i;
		while ((i = reader.read()) != -1) {
			char c = (char) i;
			if (c == '\n') {
				row++;
				column = 0;
			}
			readContent.append(c);
			peek().handleChar(c);
			column++;
		}
		peek().eof();
		return this;
	}

	public List<Content> getContent() {
		return parsedContent;
	}
	
	public HTML getHTML() {
		return createHTMLFromContent(parsedContent);
	}
	
	private HTML createHTMLFromContent(final List<Content> content) {
		return new HTML() {

			@Override
			public HTML copy() {
				final List<Content> copied = _copy(content, null);
				return createHTMLFromContent(copied);
			}

			@Override
			public String toPlainString() {
				PrettyPrintHandler prettyPrintHandler = new PrettyPrintHandler().skipNewLines().skipIndent();
				for (Content content : getContent()) {
					print(content, prettyPrintHandler);
				}
				return prettyPrintHandler.getPrettyString();
			}

			@Override
			public List<Content> getContent() {
				return content;
			}

			@Override
			public ContentContainer getParent() {
				return null;
			}
		};
	}
	
	private void print(Content content, PrettyPrintHandler prettyPrintHandler) {
		if (Tag.class.isInstance(content)) {
			Tag sourceTag = (Tag) content;
			prettyPrintHandler.openedTag(sourceTag);
			if (sourceTag.getContent() != null) {
				for (Content nested : sourceTag.getContent()) {
					print(nested, prettyPrintHandler);
				}
			}
			prettyPrintHandler.closedTag(sourceTag);
		} else if (SelfClosingTag.class.isInstance(content)) {
			SelfClosingTag sourceTag = (SelfClosingTag) content;
			prettyPrintHandler.onSelfClosingTag(sourceTag);
		} else if (Text.class.isInstance(content)) {
			Text sourceText = (Text) content;
			prettyPrintHandler.onText(sourceText);
		} else if (Entity.class.isInstance(content)) {
			Entity sourceEntity = (Entity) content;
			prettyPrintHandler.onEntity(sourceEntity);
		} else if (ProxyContent.class.isInstance(content)) {
			ProxyContent proxy = (ProxyContent) content;
			Content proxied = proxy.getContent();
			if (proxied != null) {
				print(proxied, prettyPrintHandler);
			}
		} else {
			throw new IllegalStateException("unsupported content: " + content);
		}
	}
	
	private Content _copy(Content content, ContentContainer parent) {
		if (Tag.class.isInstance(content)) {
			Tag sourceTag = (Tag) content;
			Tag tag = new Tag(parent);
			tag.setName(sourceTag.getName());
			List<Attribute> attribtues = sourceTag.getAttributes();
			if (attribtues != null) {
				for (Attribute attribtue : attribtues) {
					tag.setAttribute(attribtue.getName(), attribtue.getValue());
				}
			}
			tag.setContent(_copy(sourceTag.getContent(), tag));
			return tag;
		} else if (SelfClosingTag.class.isInstance(content)) {
			SelfClosingTag sourceTag = (SelfClosingTag) content;
			SelfClosingTag selfClosingTag = new SelfClosingTag(parent);
			selfClosingTag.setName(sourceTag.getName());
			List<Attribute> attribtues = sourceTag.getAttributes();
			if (attribtues != null) {
				for (Attribute attribtue : attribtues) {
					selfClosingTag.setAttribute(attribtue.getName(), attribtue.getValue());
				}
			}
			return selfClosingTag;
		} else if (Text.class.isInstance(content)) {
			Text sourceText = (Text) content;
			Text text = new Text(parent);
			text.setValue(sourceText.getValue());
			return text;
		} else if (Entity.class.isInstance(content)) {
			Entity sourceEntity = (Entity) content;
			Entity entity = new Entity(parent);
			entity.setName(sourceEntity.getName());
			return entity;
		} else if (ProxyContent.class.isInstance(content)) {
			ProxyContent proxy = (ProxyContent) content;
			Content proxied = proxy.getContent();
			if (proxied != null) {
				return _copy(proxied, parent);
			}
			return null;
		} else {
			throw new IllegalStateException("unsupported content: " + content);
		}
	}

	private List<Content> _copy(List<Content> source, ContentContainer parent) {
		if (source == null) {
			return null;
		}
		final List<Content> copyContent = new ArrayList<>();
		for (Content sourceContent : source) {
			Content copy = _copy(sourceContent, parent);
			if (copy != null) {
				copyContent.add(copy);
			}
		}
		return copyContent;
	}
	
	private ParsingState replay(String acceptedSoFar) throws HTMLParsingException {
		for (int i = 0; i < acceptedSoFar.length(); i++) {
			peek().handleChar(acceptedSoFar.charAt(i));
		}
		return peek();
	}

	private <E extends ParsingState> E push(E state) {
		states.push(state);
		return state;
	}

	private ParsingState peek() {
		return states.peek();
	}

	private ParsingState pop() {
		return states.pop();
	}
	
	private HTMLParsingException createException(String message) {
		return createException(message, null);
	}
	
	private HTMLParsingException createException(Throwable cause) {
		return createException(null, cause);
	}
	
	private HTMLParsingException createException(String message, Throwable cause) {
		if (message != null && cause == null) {
			return new HTMLParsingException(row, column, message);
		} else if (message == null && cause != null) {
			return new HTMLParsingException(row, column, cause);
		} else if (message != null && cause != null) {
			return new HTMLParsingException(row, column, message, cause);
		}
		return new HTMLParsingException(row, column);
	}

	
	private TextParsingState createTextParsingState(final List<Content> content, ContentContainer parent) {
			return new TextParsingState(parent) {
				@Override
				protected void onText(Text text) {
					content.add(text);
					handler.onText(text);
				}
			};
		}
	private EntityParsingState createEntityParsingState(final List<Content> content, ContentContainer parent) {
		return new EntityParsingState(parent) {
			@Override
			protected void onEntity(Entity entity) throws HTMLParsingException {
				content.add(entity);
				handler.onEntity(entity);
			}
		};
	}

	private TagParsingState createTagParsingState(final List<Content> content, ContentContainer parent) {
		return new TagParsingState(parent) {

			@Override
			protected void onSelfClosingTag(SelfClosingTag tag) {
				content.add(tag);
				handler.onSelfClosingTag(tag);
			}

			@Override
			protected void onTagOpened(Tag tag) {
				content.add(tag);
				handler.openedTag(tag);
			}

			@Override
			protected void onTagClosed(Tag tag) {
				pop();
				handler.closedTag(tag);
			}

			@Override
			protected void onComment(Comment comment) {
				if (config.isCommentParsingEnabled()) {
					content.add(comment);
				}
			}

		};
	}
	
	private interface ParsingState {

		void handleChar(char c) throws HTMLParsingException;

		void eof() throws HTMLParsingException;
	}

	private class NewContentParsingState extends ContentParsingState {

		public NewContentParsingState(ContentContainer parent) {
			super(parent, parsedContent);
		}

		@Override
		public void eof() throws HTMLParsingException {
		}

	}

	private class ContentParsingState implements ParsingState {

		private final List<Content> content;
		private final ContentContainer parent;

		public ContentParsingState(ContentContainer parent) {
			this(parent, new ArrayList<Content>());
		}

		public ContentParsingState(ContentContainer parent, List<Content> content) {
			this.content = content;
			this.parent = parent;
		}

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if (c == '<') {
				// some unknown tag
				//push(new IntermediateTagParsingState(parent, content)).handleChar(c);
				push(createTagParsingState(content, parent)).handleChar(c);
			} else if (c == '&') {
				// entity
				push(createEntityParsingState(content, parent));
			} else {
				// text content
				push(createTextParsingState(content, parent)).handleChar(c);
			}
		}

		@Override
		public void eof() throws HTMLParsingException {
			if (Tag.class.isInstance(parent)) {
				if (config.isUnbalancedTagTolerated()) {
					handler.closedTag((Tag) parent);
				} else {
					throw createException("tag " + ((Tag) parent).getName() + " was not closed");
				}
			}
			pop();
			peek().eof();
		}

		public List<Content> getContent() {
			return content;
		}

	}

	private abstract class TextParsingState implements ParsingState {

		StringBuffer textBuf = new StringBuffer();
		private final Text text;

		public TextParsingState(ContentContainer parent) {
			text = new Text(parent);
		}

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if (c == '<' || c == '&') {
				// some unknown tag
				// or an entity
				text.setValue(textBuf.toString());
				onText(text);
				pop();
				peek().handleChar(c);
			} else {
				if (config.isCharacterWithRequiredHtmlEntityInText(c)) {
					String entity = HTML5EntityMap.INSTANCE.getEntity(c);
					if (entity != null && '>' == c && config.isIncompleteEntityTolerated()) {
						if (textBuf.length() > 0) {
							text.setValue(textBuf.toString());
							onText(text);
						}
						pop();
						replay(entity);
					} else {
						throw createException("character '" + c + "' was not escaped as an entity");
					}
				}
				// regular text
				textBuf.append(c);
			}
		}

		@Override
		public void eof() throws HTMLParsingException {
			text.setValue(textBuf.toString());
			onText(text);
			pop();
			peek().eof();
		}
		
		protected abstract void onText(Text text);

	}
	
	private abstract class CommentParsingState extends AcceptStringParsingState {
		private final Comment comment;

		public CommentParsingState(ContentContainer parent) {
			super("!--", false);
			this.comment = new Comment(parent);
		}

		@Override
		protected void onNotAccepted(char c) throws HTMLParsingException {
			throw createException("comment was not started properly");
		}

		@Override
		protected void onAccepted() throws HTMLParsingException {
			pop();
			push(new BufferUntilTokenParsingState("-->") {
				
				@Override
				protected void onEndTokenFound(String buffered) {
					comment.setValue(buffered);
					onComment(comment);
				}
				
				@Override
				public void eof() throws HTMLParsingException {
					if (config.isUnbalancedTagTolerated()) {
						pop();
						comment.setValue(getBuffer().toString());
						onComment(comment);
						peek().eof();
					} else {
						throw createException("comment was not closed properly");
					}
				}
			});
		}

		@Override
		public final void eof() throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				onComment(comment);
				peek().eof();
			} else {
				throw createException("comment was not closed properly");
			}
		}

		protected abstract void onComment(Comment comment);
	}

	private abstract class EntityParsingState implements ParsingState {

		StringBuffer entityNameBuf = new StringBuffer();
		private final Entity entity;

		public EntityParsingState(ContentContainer parent) {
			entity = new Entity(parent);
		}

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if (c == ';') {
				// close entity
				pop();
				entity.setName(entityNameBuf.toString());
				onEntity(entity);
			} else {
				if (config.isWhiteSpace(c)) {
					if (config.isIncompleteEntityTolerated()) {
						handleIncompleteEntity();
						peek().handleChar(c);
					} else {
						throw createException("HTML entities shall not contain white spaces");
					}
				} else {
					// entity name
					entityNameBuf.append(c);
				}
			}
		}

		private void handleIncompleteEntity() throws HTMLParsingException {
			if (entityNameBuf.length() == 0) {
				// should have been &amp;
				pop();
				entity.setName(HTML5EntityMap.INSTANCE.getEntityName('&'));
				onEntity(entity);
			} else {
				// close entity
				pop();
				entity.setName(entityNameBuf.toString());
				onEntity(entity);
			}
		}

		@Override
		public void eof() throws HTMLParsingException {
			if (config.isIncompleteEntityTolerated()) {
				handleIncompleteEntity();
				peek().eof();
			} else {
				throw createException("failed to parse HTML entity. reached EOF but missed ';' character.");
			}
		}
		
		protected abstract void onEntity(Entity entity) throws HTMLParsingException;

	}

	private abstract class TagDetectionParsingState implements ParsingState {

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if ('<' == c) {
				pop();
				push(new BufferStringParsingState(true, "!/>") {

					@Override
					protected void onBufferComplete(StringBuffer buffer, char c) throws HTMLParsingException {
						pop();
						String tagName = buffer.toString();
						if (tagName.length() == 0) {
							if ('/' == c) {
								onClosingTag();
							} else if ('!' == c) {
								onCommentStarted(c);
							} else {
								throw createException("tag name was empty");
							}
						} else {
							onOpeningTag(tagName, c);
						}
					}

					@Override
					public void eof() throws HTMLParsingException {
						if (config.isUnbalancedTagTolerated()) {
							pop();
							String tagName = getBuffer().toString();
							if (!tagName.isEmpty()) {
								onOpeningTag(tagName, null);
							}
							peek().eof();
						} else {
							throw createException("tag was not closed properly");
						}
					}

				});
			} else {
				if (config.isUnbalancedTagTolerated()) {
					pop();
					peek().eof();
				} else {
					throw createException("tags need to start with <");
				}
			}
		}
		
		protected abstract void onCommentStarted(char c) throws HTMLParsingException;
		protected abstract void onClosingTag() throws HTMLParsingException;
		protected abstract void onOpeningTag(String tagName, Character c) throws HTMLParsingException;
	}
	
	private abstract class TagParsingState extends TagDetectionParsingState {
		
		private final ContentContainer parent;
		private TagParsingState that;

		public TagParsingState(ContentContainer parent) {
			this.parent = parent;
		}
		
		
		@Override
		protected void onClosingTag() throws HTMLParsingException {
			that = this;
			if (!Tag.class.isInstance(parent)) {
				// it looks like we have a tag that shall be closed, but the parent is not a tag.
				if (config.isUnbalancedTagTolerated()) {
					push(new ParsingState() {

						@Override
						public void handleChar(char c) throws HTMLParsingException {
							if (c == '>') {
								pop();
							}
						}

						@Override
						public void eof() throws HTMLParsingException {
							pop();
							peek().eof();
						}
					});
				} else {
					throw createException("there is no tag to be closed");
				}
			} else {
				// it looks like we have a tag that shall be closed.
				// this means we have to accept the name of the parent tag now.
				push(new CloseTagParsingState((Tag) parent) {

					@Override
					protected void onTagClosed(Tag tag) {
						that.onTagClosed(tag);
					}
				});
			}
		}

		@Override
		protected void onOpeningTag(String tagName, Character c) throws HTMLParsingException {
			final TagParsingState that = this;
			push(new AttributeListParsingState(parent, tagName, new ArrayList<Attribute>()) {

				@Override
				protected void onSelfClosingTag(SelfClosingTag selfClosingTag) {
					that.onSelfClosingTag(selfClosingTag);
				}

				@Override
				protected void onTagOpened(Tag tag) {
					that.onTagOpened(tag);
				}
			});
			if (c != null) {
				peek().handleChar(c);
			}
		}

		@Override
		protected void onCommentStarted(char c) throws HTMLParsingException {
			final TagParsingState _this = this;
			push(new CommentParsingState(parent) {

				@Override
				protected void onComment(Comment comment) {
					_this.onComment(comment);
				}

			}).handleChar(c);
		}

		@Override
		public void eof() throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				peek().eof();
			} else {
				throw createException("tag was not balanced");
			}
		}
		
		protected abstract void onSelfClosingTag(SelfClosingTag tag);
		protected abstract void onTagOpened(Tag tag);
		protected abstract void onTagClosed(Tag tag);
		protected abstract void onComment(Comment comment);
	}
	
	private abstract class CloseTagParsingState extends AcceptStringParsingState {
		
		private final Tag parentTag;

		public CloseTagParsingState(Tag parentTag) {
			super(parentTag.getName(), config.isAutomaticLowerCaseEnabled(parentTag.getName()));
			this.parentTag = parentTag;
		}
		
		@Override
		protected final void onAccepted() {
			pop();
			push(new ConsumeWhiteSpaceParsingState() {

				@Override
				protected void onNonWhiteSpace(char c) throws HTMLParsingException {
					if (c == '>') {
						pop();
						onTagClosed(parentTag);
					} else {
						throw createException("tag " + parentTag.getName() + " was not closed properly");
					}
				}

				@Override
				public void eof() throws HTMLParsingException {
					if (config.isUnbalancedTagTolerated()) {
						pop();
						onTagClosed(parentTag);
						peek().eof();
					} else {
						throw createException("tag " + parentTag.getName() + " was not closed properly");
					}
				}
			});
		}

		@Override
		protected final void onNotAccepted(char c) throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				onTagClosed(parentTag);
				replay("</" + acceptedSoFar()).handleChar(c);
			} else {
				throw createException("tag " + parentTag.getName() + " was not closed");
			}
		}

		@Override
		public final void eof() throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				onTagClosed(parentTag);
				peek().eof();
			} else {
				throw createException("tag " + parentTag.getName() + " was not closed");
			}
		}
		
		protected abstract void onTagClosed(Tag tag);
	}
	
	private abstract class AttributeListParsingState extends ConsumeWhiteSpaceParsingState {
		private final ContentContainer parent;
		private final String tagName;
		private final List<Attribute> attributes;

		public AttributeListParsingState(ContentContainer parent, String tagName, List<Attribute> attributes) {
			this.parent = parent;
			if (tagName == null) {
				throw new IllegalArgumentException("tagName is not allowed to be null");
			}
			this.tagName = tagName;
			if (attributes == null) {
				throw new IllegalArgumentException("attributes is not allowed to be null");
			}
			this.attributes = attributes;
		}

		private SelfClosingTag createSelfClosingTag() {
			SelfClosingTag selfClosingTag = new SelfClosingTag(parent);
			if (config.isAutomaticLowerCaseEnabled(tagName)) {
				selfClosingTag.setName(tagName.toLowerCase());
			} else {
				selfClosingTag.setName(tagName);
			}
			selfClosingTag.setAttributes(attributes);
			return selfClosingTag;
		}

		private Tag createTag() {
			Tag tag = new Tag(parent);
			tag.setAttributes(attributes);
			if (config.isAutomaticLowerCaseEnabled(tagName)) {
				tag.setName(tagName.toLowerCase());
			} else {
				tag.setName(tagName);
			}
			tag.setContent(new ArrayList<Content>());
			return tag;
		}
		
		protected abstract void onSelfClosingTag(SelfClosingTag selfClosingTag);
		protected abstract void onTagOpened(Tag tag);
		
		@Override
		protected void onNonWhiteSpace(char c) throws HTMLParsingException {
			if ('/' == c) {
				pop();
				// self closing tag
				push(new AcceptCharacterParsingState('>') {

					@Override
					protected void onDifferingChar(char c) throws HTMLParsingException {
						throw createException("self closing tag was not closed properly. tagName: " + tagName);
					}

					@Override
					protected void onMatchedChar(char c) throws HTMLParsingException {
						pop();
						onSelfClosingTag(createSelfClosingTag());
					}

					@Override
					public void eof() throws HTMLParsingException {
						if (config.isUnbalancedTagTolerated()) {
							pop();
							onSelfClosingTag(createSelfClosingTag());
							peek().eof();
						} else {
							throw createException("self closing tag was not closed");
						}
					}
				});
			} else if ('>' == c) {
				pop();
				// maybe a self closing tag
				if (config.isSelfClosingTag(config.isAutomaticLowerCaseEnabled(tagName) ? tagName.toLowerCase() : tagName)) {
					onSelfClosingTag(createSelfClosingTag());
				} else {
					// parse content of tag
					Tag tag = createTag();
					onTagOpened(tag);
					push(new ContentParsingState(tag, tag.getContent()));
				}
			} else {
				//an attribute
				push(createAttributeParsingState()).handleChar(c);
			}
		}
		
		private ParsingState createAttributeParsingState() {
			return new AttributeParsingState() {

					@Override
					protected void onAttributeWithValue(final String attributeName) throws HTMLParsingException {
						push(new ConsumeWhiteSpaceParsingState() {

							@Override
							protected void onNonWhiteSpace(char c) throws HTMLParsingException {
								pop();
								if (c == '\'' || c == '"') {
									final String closeChar = Character.toString(c);
									push(new BufferStringParsingState(false, closeChar) {

										@Override
										protected void onBufferComplete(StringBuffer buffer, char c) throws HTMLParsingException {
											Attribute attribute = new Attribute();
											attribute.setName(attributeName);
											attribute.setValue(buffer.toString());
											attributes.add(attribute);
											pop();
										}

										@Override
										public void eof() throws HTMLParsingException {
											if (config.isUnbalancedTagTolerated()) {
												pop();
												Attribute attribute = new Attribute();
												attribute.setName(attributeName);
												attribute.setValue(getBuffer().toString());
												attributes.add(attribute);
												peek().eof();
											} else {
												throw createException("attribute values was not terminated by " + closeChar);
											}
										}
									});
								} else {
									if (config.isUnquotedAttributeValueTolerated()) {
										push(new BufferStringParsingState(true, ">") {

											@Override
											protected void onBufferComplete(StringBuffer buffer, char c) throws HTMLParsingException {
												Attribute attribute = new Attribute();
												attribute.setName(attributeName);
												if (buffer.charAt(buffer.length() - 1) == '/') {
													String value = buffer.substring(0, buffer.length() - 1);
													attribute.setValue(value);
												} else {
													attribute.setValue(buffer.toString());
												}
												attributes.add(attribute);
												pop();
												peek().handleChar(c);
											}

											@Override
											public void eof() throws HTMLParsingException {
												if (config.isUnbalancedTagTolerated()) {
													pop();
													Attribute attribute = new Attribute();
													attribute.setName(attributeName);
													attribute.setValue(getBuffer().toString());
													attributes.add(attribute);
													peek().eof();
												} else {
													throw createException("attribute value was incomplete");
												}
											}
										}).handleChar(c);
									} else {
										throw createException("attribute values need to be wrapped in ' or \" characters");
									}
								}
							}

							@Override
							public void eof() throws HTMLParsingException {
								if (config.isUnbalancedTagTolerated()) {
									pop();
									Attribute attribute = new Attribute();
									attribute.setName(attributeName);
									attributes.add(attribute);
									peek().eof();
								} else {
									throw createException("value of attribute could not be parsed");
								}
							}
						});
					}

					@Override
					protected void onAttributeWithoutValue(final String attributeName, Character c) throws HTMLParsingException {
						Attribute attribute = new Attribute();
						attribute.setName(attributeName);
						attributes.add(attribute);
						if (c != null) {
							peek().handleChar(c);
						}
					}
				};
		}

		@Override
		public void eof() throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				onSelfClosingTag(createSelfClosingTag());
				peek().eof();
			} else {
				throw createException("tag was not closed");
			}
		}
	}
	
	private abstract class AcceptCharacterParsingState implements ParsingState {
		private final char acceptedChar;

		public AcceptCharacterParsingState(char acceptedChar) {
			this.acceptedChar = acceptedChar;
		}

		@Override
		public final void handleChar(char c) throws HTMLParsingException {
			if (c != acceptedChar) {
				onDifferingChar(c);
			} else {
				onMatchedChar(c);
			}
		}

		protected abstract void onDifferingChar(char c) throws HTMLParsingException;

		protected abstract void onMatchedChar(char c) throws HTMLParsingException;
		
	}
	
	private abstract class AcceptStringParsingState implements ParsingState {
		private final String toAccept;
		private final boolean autoLowerCaseChars;
		private int pos;

		public AcceptStringParsingState(String toAccept, boolean autoLowerCaseChars) {
			this.toAccept = toAccept;
			this.autoLowerCaseChars = autoLowerCaseChars;
		}

		@Override
		public final void handleChar(char c) throws HTMLParsingException {
			if (autoLowerCaseChars) {
				c = Character.toLowerCase(c);
			}
			if (toAccept.charAt(pos) == c) {
				pos++;
				if (pos == toAccept.length()) {
					onAccepted();
				}
			} else {
				onNotAccepted(c);
			}
		}

		protected final String acceptedSoFar() {
			return toAccept.substring(0, pos);
		}
		
		protected abstract void onNotAccepted(char c) throws HTMLParsingException;

		protected abstract void onAccepted() throws HTMLParsingException;
		
	}
	
	private abstract class ConsumeWhiteSpaceParsingState implements ParsingState {

		@Override
		public final void handleChar(char c) throws HTMLParsingException {
			if (!config.isWhiteSpace(c)) {
				onNonWhiteSpace(c);
			}
		}

		protected abstract void onNonWhiteSpace(char c) throws HTMLParsingException;

	}
	
	private abstract class BufferStringParsingState implements ParsingState {

		private final boolean stopOnWhiteSpace;
		private final String excludedChars;
		private final StringBuffer buffer = new StringBuffer();

		public BufferStringParsingState(boolean stopOnWhiteSpace, String excludedChars) {
			this.excludedChars = excludedChars;
			this.stopOnWhiteSpace = stopOnWhiteSpace;
		}
		
		@Override
		public final void handleChar(char c) throws HTMLParsingException {
			if (excludedChars.indexOf(c) >= 0 || (stopOnWhiteSpace && config.isWhiteSpace(c))) {
				onBufferComplete(buffer, c);
			} else {
				buffer.append(c);
			}
		}

		protected StringBuffer getBuffer() {
			return buffer;
		}

		protected abstract void onBufferComplete(StringBuffer buffer, char c) throws HTMLParsingException;

	}
	
	private abstract class AttributeParsingState implements ParsingState {

		StringBuffer attributeNameBuf = new StringBuffer();
		boolean nameHasStarted = false;
		boolean nameHasCompleted = false;

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if (c == '=') {
				if (!nameHasStarted) {
					throw createException("attribute name of a tag is not allowed to start with =");
				}
				nameHasCompleted = true;
				pop();
				onAttributeWithValue(attributeNameBuf.toString());
				//attribute.setName(attributeNameBuf.toString());
				//pop();
				//push(new AttributeValueParsingState(attribute));
				// attribute value parsing
			} else if (config.isWhiteSpace(c)) {
				if (!nameHasStarted) {
					// do nothing
					throw createException("attribute of a tag had no name");
				} else {
					nameHasCompleted = true;
					pop();
					onAttributeWithoutValue(attributeNameBuf.toString(), c);
				}
				// handle white space
			} else if ('>' == c) {
				if (!nameHasStarted) {
					throw createException("closing an attribute without a name");
				}
				pop();
				onAttributeWithoutValue(attributeNameBuf.toString(), c);
			} else {
				if (!nameHasCompleted) {
					if ('/' == c) {
						if (!nameHasStarted) {
							throw createException("attribute without a name");
						} else {
							nameHasCompleted = true;
							pop();
							onAttributeWithoutValue(attributeNameBuf.toString(), c);
						}
					} else {
						if (!config.isAllowedAttributeNameCharacter(c)) {
							throw createException("unallowed character '" + c + "' for attribute name");
						}
						nameHasStarted = true;
						attributeNameBuf.append(c);
					}
				} else {
					throw createException("name of attribute '" + attributeNameBuf.toString() + "' has been parsed already.");
				}
			}
		}
		
		protected abstract void onAttributeWithValue(String attributeName) throws HTMLParsingException;
		protected abstract void onAttributeWithoutValue(String attributeName, Character c) throws HTMLParsingException;

		@Override
		public void eof() throws HTMLParsingException {
			if (config.isUnbalancedTagTolerated()) {
				pop();
				if (nameHasStarted) {
					onAttributeWithoutValue(attributeNameBuf.toString(), null);
				}
				peek().eof();
			} else {
				throw createException("attribute of tag was not parsed completely");
			}
		}

	}
	
	private static class Matcher {
		private final String expected;
		private boolean didMatch;
		private boolean didNotMatch;

		public Matcher(String expected) {
			this.expected = expected;
		}

		private int pos = 0;

		public final void match(char c) {
			if (pos >= expected.length()) {
				return;
			}
			if (expected.charAt(pos) == c) {
				pos++;
				if (pos == expected.length()) {
					didMatch = true;
				}
			} else {
				didNotMatch = true;
			}
		}
		
		protected boolean didMatch() {
			return didMatch;
		}
		protected boolean didNotMatch() {
			return didNotMatch;
		}
	}
	
	private abstract class BufferUntilTokenParsingState implements ParsingState {
		private final String endToken;
		private final StringBuffer buffer = new StringBuffer();
		final List<Matcher> matchers = new ArrayList<>();
		
		public BufferUntilTokenParsingState(String endToken) {
			if (endToken == null || endToken.isEmpty()) {
				throw new IllegalArgumentException("endtoken has to have at least one char");
			}
			this.endToken = endToken;
		}

		public final StringBuffer getBuffer() {
			return buffer;
		}

		@Override
		public void handleChar(char c) throws HTMLParsingException {
			if (endToken.charAt(0) == c) {
				matchers.add(new Matcher(endToken));
			}
			Iterator<Matcher> iterator = matchers.iterator();
			while (iterator.hasNext()) {
				Matcher next = iterator.next();
				next.match(c);
				if (next.didMatch()) {
					pop();
					String buffered = buffer.substring(0, buffer.length() - (endToken.length() - 1));
					onEndTokenFound(buffered);
					return;
				} else if (next.didNotMatch()) {
					iterator.remove();
				}
			}
			buffer.append(c);
		}

		protected abstract void onEndTokenFound(String buffered);
		
	}

}
