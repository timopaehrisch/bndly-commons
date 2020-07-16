package org.bndly.css;

/*-
 * #%L
 * PDF CSS Model
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class CSSReader {

	private List<CSSItem> items;
	private Stack<ParsingState> parsingStates;
	
	private abstract class ParsingState {
		abstract void handleChar(char c) throws CSSParsingException;
	}
	
	private abstract class ConsumeWhiteSpaceParsingState extends ParsingState {

		@Override
		void handleChar(char c) throws CSSParsingException {
			if (Character.isWhitespace(c)) {
				return;
			} else {
				onNonWhiteSpace(c);
			}
		}

		abstract void onNonWhiteSpace(char c) throws CSSParsingException;

	}
	private abstract class AcceptStringParsingState extends ParsingState {

		private final String toAccept;
		private int position;
		
		public AcceptStringParsingState(String toAccept) {
			this.toAccept = toAccept;
			this.position = 0;
		}
		
		@Override
		void handleChar(char c) throws CSSParsingException {
			if (toAccept.length() > position && toAccept.charAt(position) == c) {
				// go on
				position++;
				if (toAccept.length() == position) {
					onAcceptedString();
				}
				return;
			} else {
				onNotAcceptedString(c);
			}
		}

		abstract void onAcceptedString() throws CSSParsingException;
		abstract void onNotAcceptedString(char c) throws CSSParsingException;
		
	}
	
	private abstract class ReadUntilParsingState extends ParsingState {
		private final StringBuffer sb = new StringBuffer();
		protected final String endToken;
		private boolean didEnd;

		public ReadUntilParsingState(String endToken) {
			this.endToken = endToken;
		}

		@Override
		void handleChar(char c) throws CSSParsingException {
			if (didEnd) {
				throw new CSSParsingException("'read until' can only be used once.");
			}
			sb.append(c);
			int l = sb.length();
			if (l >= endToken.length()) {
				String end = sb.substring(l - endToken.length(), l);
				if (end.equals(endToken)) {
					didEnd = true;
					onEndToken(sb.toString());
				}
			}
		}

		abstract void onEndToken(String buffered) throws CSSParsingException;
		
	}
	
	private abstract class ReadEitherTokenParsingState extends ParsingState {
		private final String[] acceptedTokens;
		private final StringBuffer sb = new StringBuffer();

		public ReadEitherTokenParsingState(String... acceptedTokens) {
			this.acceptedTokens = acceptedTokens;
		}

		@Override
		void handleChar(char c) throws CSSParsingException {
			sb.append(c);
			String prefix = sb.toString();
			boolean allTokensNulled = true;
			for (int i = 0; i < acceptedTokens.length; i++) {
				String acceptedToken = acceptedTokens[i];
				if (acceptedToken != null) {
					if (!acceptedToken.startsWith(prefix)) {
						acceptedTokens[i] = null;
						acceptedToken = null;
					} else {
						if (acceptedToken.length() == prefix.length()) {
							tokenMatched(acceptedToken);
							return;
						}
					}
				}
				if (acceptedToken != null) {
					allTokensNulled = false;
				}
			}
			if (allTokensNulled) {
				noTokenMatched(prefix);
			}
		}

		abstract void tokenMatched(String acceptedToken) throws CSSParsingException;
		abstract void noTokenMatched(String readSoFar) throws CSSParsingException;
		
	}
	
	private abstract class BufferParsingState extends ParsingState {
		protected final StringBuffer buffer = new StringBuffer();
		@Override
		void handleChar(char c) throws CSSParsingException {
			if (isEndOfBuffer(c)) {
				onBufferEnd(c);
			} else {
				buffer.append(c);
			}
		}

		abstract boolean isEndOfBuffer(char c) throws CSSParsingException;

		abstract void onBufferEnd(char c) throws CSSParsingException;
		
	}
	
	private abstract class CSSAttributeParsingState extends ReadUntilParsingState {

		public CSSAttributeParsingState() {
			super(":");
		}

		@Override
		void handleChar(char c) throws CSSParsingException {
			if (c == '/') {
				parsingStates.push(new CommentPreflightParsingState() {
					@Override
					void onCommentText(String text) throws CSSParsingException {
						// skip the text
					}

					@Override
					void onNonCommentText(char c) throws CSSParsingException {
						superHandleChar('/');
						superHandleChar(c);
					}
					
				});
			} else {
				super.handleChar(c);
			}
		}
		
		void superHandleChar(char c) throws CSSParsingException {
			super.handleChar(c);
		}

		@Override
		void onEndToken(String buffered) throws CSSParsingException {
			final String attributeName = buffered.substring(0, buffered.length() - endToken.length()).trim();
			parsingStates.pop();
			parsingStates.push(new ReadUntilParsingState(";") {
				@Override
				void handleChar(char c) throws CSSParsingException {
					if (c == '/') {
						parsingStates.push(new CommentPreflightParsingState() {
							@Override
							void onCommentText(String text) throws CSSParsingException {
								// skip the text
							}

							@Override
							void onNonCommentText(char c) throws CSSParsingException {
								superHandleChar('/');
								superHandleChar(c);
							}
							
						});
					} else {
						super.handleChar(c);
					}
				}
				
				void superHandleChar(char c) throws CSSParsingException {
					super.handleChar(c);
				}
				
				@Override
				void onEndToken(String buffered) throws CSSParsingException {
					String attributeValue = buffered.substring(0, buffered.length() - endToken.length());
					attributeValue = attributeValue.trim();
					parsingStates.pop();
					onCSSAttribute(attributeName, attributeValue);
				}

			});
		}
		abstract void onCSSAttribute(String attributeName, String attributeValue) throws CSSParsingException;
		
	}
	
	private abstract class CSSAttributesParsingState extends ConsumeWhiteSpaceParsingState {
		
		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			if ('}' == c) {
				// the style is now closed

				parsingStates.pop();
				onStyleComplete();
			} else {
				// start a new attribute
				parsingStates.push(new CSSAttributeParsingState() {

					@Override
					void onCSSAttribute(String attributeName, String attributeValue) throws CSSParsingException {
						CSSAttribute attribute = new CSSAttribute();
						attribute.setName(attributeName);
						attribute.setValue(attributeValue);
						onStyleAttribute(attribute);
					}
				}).handleChar(c);
			}
		}
		
		abstract void onStyleAttribute(CSSAttribute attribute) throws CSSParsingException;
		abstract void onStyleComplete() throws CSSParsingException;
		
	}
	
	private abstract class CSSSelectorParsingState extends ReadUntilParsingState {

		public CSSSelectorParsingState() {
			super("{");
		}

		@Override
		void onEndToken(String buffered) throws CSSParsingException {
			final CSSStyle style = new CSSStyle();
			String selector = buffered.substring(0, buffered.length() - endToken.length());
			selector = selector.trim();
			style.setSelector(selector);
			parsingStates.pop();
			//push a state to handle the attributes
			parsingStates.push(new CSSAttributesParsingState() {

				@Override
				void onStyleAttribute(CSSAttribute attribute) throws CSSParsingException {
					style.addAttribute(attribute);
				}

				@Override
				void onStyleComplete() throws CSSParsingException {
					onStyle(style);
				}
			});
		}

		abstract void onStyle(CSSStyle style) throws CSSParsingException;
		
	}
	
	private class CSSDocumentParsingState extends ConsumeWhiteSpaceParsingState {

		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			if ('@' == c) {
				// media query
				parsingStates.push(new MediaQueryParsingState()).handleChar(c);
			} else if ('/' == c) {
				// comment
				parsingStates.push(new CommentParsingState()).handleChar(c);
			} else {
				// selector name
				parsingStates.push(new CSSSelectorParsingState() {

					@Override
					void onStyle(CSSStyle style) throws CSSParsingException {
						items.add(style);
					}

				}).handleChar(c);
			}
		}
	}
	
	private abstract class StylesParsingState extends ConsumeWhiteSpaceParsingState {
		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			if ('/' == c) {
				// comment
				parsingStates.push(new CommentParsingState()).handleChar(c);
			} else {
				if (isSelectorStartCharacter(c)) {
					// selector name
					final StylesParsingState _this = this;
					parsingStates.push(new CSSSelectorParsingState() {

						@Override
						void onStyle(CSSStyle style) throws CSSParsingException {
							_this.onStyle(style);
						}
					}).handleChar(c);
				} else {
					onNoMoreStyles(c);
				}
			}
		}

		abstract boolean isSelectorStartCharacter(char c);
		
		abstract void onStyle(CSSStyle style) throws CSSParsingException;
		abstract void onNoMoreStyles(char c) throws CSSParsingException;
	}
	
	private class CommentParsingState extends AcceptStringParsingState {

		private String comment;
		public CommentParsingState() {
			super("/*");
		}
		
		@Override
		void onAcceptedString() throws CSSParsingException {
			parsingStates.pop();
			parsingStates.push(new ReadUntilParsingState("*/") {
				
				@Override
				void onEndToken(String buffered) throws CSSParsingException {
					parsingStates.pop();
					comment = buffered.substring(0, buffered.length() - endToken.length());
					CSSComment c = new CSSComment(comment);
					items.add(c);
					
				}

			});
		}

		@Override
		void onNotAcceptedString(char c) throws CSSParsingException {
			throw new CSSParsingException("failed to parse CSS comment");
		}
		
	}
	
	private class MediaQueryFeatureParsingState extends ConsumeWhiteSpaceParsingState {
		private final CSSMediaQuery mediaQuery;

		public MediaQueryFeatureParsingState(CSSMediaQuery mediaQuery) {
			this.mediaQuery = mediaQuery;
		}
		
		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			if ('(' == c) {
				parsingStates.pop();
				parsingStates.push(new ConsumeWhiteSpaceParsingState() {

					@Override
					void onNonWhiteSpace(char c) throws CSSParsingException {
						parsingStates.pop();
						parsingStates.push(new BufferParsingState() {

							@Override
							boolean isEndOfBuffer(char c) throws CSSParsingException {
								return ':' == c || ')' == c;
							}

							@Override
							void onBufferEnd(char c) throws CSSParsingException {
								parsingStates.pop();
								if (':' == c) {
									final String featureName = buffer.toString();
									parsingStates.push(new ConsumeWhiteSpaceParsingState() {

										@Override
										void onNonWhiteSpace(char c) throws CSSParsingException {
											parsingStates.pop();
											parsingStates.push(new BufferParsingState() {

												@Override
												boolean isEndOfBuffer(char c) throws CSSParsingException {
													return ')' == c;
												}

												@Override
												void onBufferEnd(char c) throws CSSParsingException {
													parsingStates.pop();
													CSSMediaFeature feature = new CSSMediaFeature(featureName, buffer.toString());
													mediaQuery.addFeature(feature);
													parsingStates.push(new ConsumeWhiteSpaceParsingState() {

														@Override
														void onNonWhiteSpace(char c) throws CSSParsingException {
															parsingStates.pop();
															if ('a' == c) {
																parsingStates.push(new MediaQueryFeaturesParsingState(mediaQuery));
															}
															parsingStates.peek().handleChar(c);
														}
													});
												}
											}).handleChar(c);
										}
									});
								} else {
									// feature has no value
									CSSMediaFeature feature = new CSSMediaFeature(buffer.toString(), null);
									mediaQuery.addFeature(feature);
								}
							}
						}).handleChar(c);
					}
				});
				// parse white spaces
				// first non white space will start the feature name
				// parse white spaces
				// parse either : or )
				// if :
				//    parse whitespaces
				//    first non white space will start the feature value
				//....) will close the value and feature
				// if /
				//    close the feature immediately
			} else {
				throw new CSSParsingException("failed to parse feature of media query");
			}
		}
	};
	
	private class MediaQueryFeaturesParsingState extends ConsumeWhiteSpaceParsingState {
		private final CSSMediaQuery mediaQuery;

		public MediaQueryFeaturesParsingState(CSSMediaQuery mediaQuery) {
			this.mediaQuery = mediaQuery;
		}
		
		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			parsingStates.pop();
			parsingStates.push(new ReadEitherTokenParsingState("and") {
				
				@Override
				void tokenMatched(String acceptedToken) throws CSSParsingException {
					// read the pairs in the ()
					parsingStates.pop();
					parsingStates.push(new MediaQueryFeatureParsingState(mediaQuery));
				}
				
				@Override
				void noTokenMatched(String readSoFar) throws CSSParsingException {
					parseMediaQueryContentStart();
					replay(readSoFar);
				}
				
				void parseMediaQueryContentStart() {
					parsingStates.pop();
					parsingStates.push(new ConsumeWhiteSpaceParsingState() {

						@Override
						void onNonWhiteSpace(char c) throws CSSParsingException {
							parsingStates.pop();
							if ('{' == c) {
								throw new CSSParsingException("NOT IMPLEMENTED YET");
							} else {
								throw new CSSParsingException("failed to parse body of media query");
							}
						}
					});
				}
			}).handleChar(c);
		}
		
	}
	
	private class MediaQueryMediaTypeParsingState extends ConsumeWhiteSpaceParsingState {
		private final CSSMediaQueryList mediaQueryList;
		private final CSSMediaQuery mediaQuery;

		public MediaQueryMediaTypeParsingState(CSSMediaQueryList mediaQueryList, CSSMediaQuery mediaQuery) {
			this.mediaQueryList = mediaQueryList;
			this.mediaQuery = mediaQuery;
		}

		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			mediaQueryList.getQueries().add(mediaQuery);
			parsingStates.pop();
			parsingStates.push(new ReadEitherTokenParsingState("all", "aural", "braille", "embossed", "handheld", "projection", "tty", "tv", "print", "screen", "speech") {
				
				@Override
				void tokenMatched(String acceptedToken) throws CSSParsingException {
					if ("all".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.ALL);
					} else if ("aural".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.AURAL);
					} else if ("braille".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.BRAILLE);
					} else if ("embossed".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.EMBOSSED);
					} else if ("handheld".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.HANDHELD);
					} else if ("projection".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.PROJECTION);
					} else if ("tty".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.TTY);
					} else if ("tv".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.TV);
					} else if ("print".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.PRINT);
					} else if ("screen".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.SCREEN);
					} else if ("speech".equals(acceptedToken)) {
						mediaQuery.setType(CSSMediaQuery.Type.SPEECH);
					} else {
						throw new CSSParsingException("unsupported token for media query type: " + acceptedToken);
					}
					parsingStates.pop();
					parsingStates.push(new MediaQueryFeaturesParsingState(mediaQuery));
				}
				
				@Override
				void noTokenMatched(String readSoFar) throws CSSParsingException {
					parsingStates.pop();
					parsingStates.push(new MediaQueryFeatureParsingState(mediaQuery));
					replay(readSoFar);
				}
			}).handleChar(c);
		}
		
	}
	
	private class MediaQueryModifierParsingState extends ConsumeWhiteSpaceParsingState {
		private final CSSMediaQueryList mediaQueryList;
		private final CSSMediaQuery mediaQuery = new CSSMediaQuery();

		public MediaQueryModifierParsingState(CSSMediaQueryList mediaQueryList) {
			this.mediaQueryList = mediaQueryList;
		}
		
		@Override
		void onNonWhiteSpace(char c) throws CSSParsingException {
			parsingStates.pop();
			parsingStates.push(new ReadEitherTokenParsingState("not","only") {

				@Override
				void tokenMatched(String acceptedToken) throws CSSParsingException {
					if ("not".equals(acceptedToken)) {
						mediaQuery.setModifier(CSSMediaQuery.Modifier.NOT);
					} else {
						mediaQuery.setModifier(CSSMediaQuery.Modifier.ONLY);
					}
					parseMediaType();
				}

				@Override
				void noTokenMatched(String readSoFar) throws CSSParsingException {
					parseMediaType();
					replay(readSoFar);
				}

				private void parseMediaType() {
					parsingStates.pop();
					parsingStates.push(new MediaQueryMediaTypeParsingState(mediaQueryList, mediaQuery));
				}
			}).handleChar(c);
		}
		
	}
	
	private class MediaQueryParsingState extends AcceptStringParsingState {
		
		public MediaQueryParsingState() {
			super("@media");
		}
		
		@Override
		void onAcceptedString() {
			final CSSMediaQueryList mediaQueryList = new CSSMediaQueryList();
			items.add(mediaQueryList);
			parsingStates.pop();
			parsingStates.push(new ConsumeWhiteSpaceParsingState() {

				@Override
				void onNonWhiteSpace(char c) throws CSSParsingException {
					if (',' == c) {
						// new media query
						parsingStates.push(new MediaQueryModifierParsingState(mediaQueryList));
					} else if ('{' == c) {
						// content of media query list
						parsingStates.push(new StylesParsingState() {

							@Override
							boolean isSelectorStartCharacter(char c) {
								return '}' != c;
							}

							@Override
							void onStyle(CSSStyle style) throws CSSParsingException {
								mediaQueryList.addStyle(style);
//								parsingStates.pop();
							}

							@Override
							void onNoMoreStyles(char c) throws CSSParsingException {
								parsingStates.pop();
								parsingStates.peek().handleChar(c);
							}
							
						});
					} else if ('}' == c) {
						// end of content of media query list
						parsingStates.pop();
					} else {
						throw new CSSParsingException("failed to parse media query list");
					}
				}
			});
			parsingStates.push(new MediaQueryModifierParsingState(mediaQueryList));
			// 'only' 'not' or nothing
			// then comes the type all, print, screen, speech
			// then comes optionally 'and'
			// if there was 'and', then there come multiple features
		}

		@Override
		void onNotAcceptedString(char c) throws CSSParsingException {
			throw new CSSParsingException("failed to parse media query");
		}
		
	}

	private abstract class CommentPreflightParsingState extends ParsingState {

		@Override
		void handleChar(char c) throws CSSParsingException {
			parsingStates.pop();
			if (c == '*') {
				// comment has started
				parsingStates.push(new ReadUntilParsingState("*/") {
					@Override
					void onEndToken(String buffered) throws CSSParsingException {
						parsingStates.pop();
						onCommentText(buffered);
					}
				});
			} else {
				// not a comment
				onNonCommentText(c);
			}
		}
		abstract void onNonCommentText(char c) throws CSSParsingException;
		abstract void onCommentText(String text) throws CSSParsingException;
	}
	
	public List<CSSItem> read(String file) throws IOException, CSSParsingException {
		FileInputStream is = new FileInputStream(file);
		return read(is);
	}

	public List<CSSItem> read(InputStream is) throws IOException, CSSParsingException {
		if (is == null) {
			return Collections.EMPTY_LIST;
		}
		items = new ArrayList<>();
		parsingStates = new Stack<>();
		parsingStates.push(new CSSDocumentParsingState());
		
		InputStreamReader isr = new InputStreamReader(is);
		StringBuffer whatHasBeenRead = null;
		String debug = System.getProperty("bndly.css.parser.debug.enabled");
		boolean debugEnabled = "true".equals(debug);
		if (debugEnabled) {
			whatHasBeenRead = new StringBuffer();
		}
		int b = isr.read();
		while (b > -1) {
			char c = (char) b;
			if (debugEnabled) {
				whatHasBeenRead.append(c);
			}
			parsingStates.peek().handleChar(c);
			b = isr.read();
		}
		return items;
	}
	
	private void replay(String input) throws CSSParsingException {
		for (int i = 0; i < input.length(); i++) {
			parsingStates.peek().handleChar(input.charAt(i));
		}
	}

}
