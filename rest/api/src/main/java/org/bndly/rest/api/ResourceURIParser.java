package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Stack;

/**
 * The ResourceURIParser is a special parser that takes resource URL composition into account. This means that 
 * a ResourceURI does not only consist of the regular HTTP URL elements but also gives access to specific 
 * constructs such as selectors, extensions and suffixes.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ResourceURIParser {

	private final PathCoder pathCoder;
	private final String input;
	private final Stack<ParsingState> states = new Stack<>();
	private final ResourceURIBuilderImpl builder;
	private final URI uri;

	public ResourceURIParser(PathCoder pathCoder, String input) {
		URI u = null;
		try {
			if (input != null) {
				u = new URI(input);
				String s = u.getScheme();
				if (s != null) {
					String ssp = input.substring(s.length() + 1); // +1 for the ':' symbol
					input = ssp;
				}
				if (input.startsWith("//")) {
					int start = input.indexOf('/', 2);
					if (start > -1) {
						input = input.substring(start);
					} else {
						input = null;
					}
				}
			}
		} catch (URISyntaxException ex) {
			// ignore this exception
		}
		uri = u;
		this.input = input;
		builder = new ResourceURIBuilderImpl(pathCoder);
		if (uri != null) {
			builder.scheme(uri.getScheme());
			builder.host(uri.getHost());
			if (uri.getPort() > 0) {
				builder.port(uri.getPort());
			}
			builder.fragment(uri.getFragment());
		}
		this.pathCoder = pathCoder;
	}

	public ResourceURI getResourceURI() {
		return builder.build();
	}

	/**
	 * The ParsingException will be thrown if an input string does not conform to the sling URI design.
	 */
	public static class ParsingException extends IllegalStateException {

		public ParsingException(String s) {
			super(s);
		}

	}

	private interface ParsingState {

		void handleChar(char c) throws ParsingException;

		void complete() throws ParsingException;
	}

	private ParsingState push(ParsingState state) {
		states.push(state);
		return state;
	}

	private ParsingState pop() {
		return states.pop();
	}

	private ParsingState peek() {
		if (states.isEmpty()) {
			return null;
		}
		return states.peek();
	}

	public ResourceURIParser parse() throws ParsingException {
		if (input == null) {
			return this;
		}
		push(new PathParsingState());
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			peek().handleChar(c);
		}
		while (peek() != null) {
			peek().complete();
		}
		return this;
	}

	private class PathParsingState implements ParsingState {

		private boolean started = false;
		private StringBuffer sb;

		@Override
		public void handleChar(char c) throws ParsingException {
			if (c == '/') {
				if (!started) {
					started = true;
					append(c);
				} else {
					// new path element
					complete();
					push(new PathParsingState()).handleChar(c);
				}
			} else if ('?' == c || '#' == c) {
				complete();
				push(new SuffixParsingState()).handleChar(c);
			} else {
				if (started) {
					if (c == '.') {
						complete();
						push(new SelectorParsingState());
					} else {
						append(c);
					}
				} else {
					// parsing error
					throw new ParsingException("found non slash character but path has not started yet.");
				}
			}
		}

		private void append(char c) {
			if (sb == null) {
				sb = new StringBuffer();
			}
			sb.append(c);
		}

		@Override
		public void complete() throws ParsingException {
			createPathElement();
			pop();
		}

		private void createPathElement() {
			if (sb != null) {
				String element = sb.toString();
				String dec = pathCoder.decodeString(element);
				builder.pathElement(dec);
			}
		}

	}

	private class SelectorParsingState implements ParsingState {

		private boolean isExtension = true;
		private StringBuffer sb;

		@Override
		public void handleChar(char c) throws ParsingException {
			if (c == '.') {
				isExtension = false;
				complete();
				push(new SelectorParsingState());
			} else if ('?' == c) {
				complete();
				push(new SuffixParsingState()).handleChar(c);
			} else {
				if (c == '/') {
					complete();
					push(new SuffixParsingState()).handleChar(c);
				} else {
					if (sb == null) {
						sb = new StringBuffer();
					}
					sb.append(c);
				}
			}
		}

		@Override
		public void complete() throws ParsingException {
			if (isExtension) {
				if (sb != null) {
					String dec = pathCoder.decodeString(sb.toString());
					builder.extension(dec);
				}
			} else {
				if (sb != null) {
					String selectorName = sb.toString();
					String dec = pathCoder.decodeString(selectorName);
					builder.selector(dec);
				}
			}
			pop();
		}

	}

	private class SuffixParsingState implements ParsingState {

		private StringBuffer sb;

		@Override
		public void handleChar(char c) throws ParsingException {
			if (sb == null) {
				sb = new StringBuffer();
			}
			sb.append(c);
		}

		@Override
		public void complete() throws ParsingException {
			if (sb != null) {
				builder.suffix(sb.toString());
			}
			pop();
		}
	}
}
