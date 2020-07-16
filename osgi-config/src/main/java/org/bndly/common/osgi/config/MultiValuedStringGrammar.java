package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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

import org.bndly.common.lang.StringUtil;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * A multi valued string separates the values by <code>,</code> symbols. 
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class MultiValuedStringGrammar {
	
	private static final int STATE_START = 0;
	private static final int STATE_NEW_VALUE = 1;
	private static final int STATE_CONSUME_PLAIN = 2;
	private static final int STATE_CONSUME_ESCAPED = 3;
	private static final int STATE_COULD_BE_ESCAPED = 4;
	private static final char SEPARATOR = ',';
	private static final char ESCAPE = '"';

	private MultiValuedStringGrammar() {
	}
	
	public static Iterable<String> split(String raw) {
		List<String> result = new ArrayList<>();
		int currentState = STATE_START;
		StringBuilder currentElement = new StringBuilder();
		for (int codePoint : StringUtil.codePoints(raw)) {
			if (currentState == STATE_START) {
				if (codePoint == SEPARATOR) {
					result.add("");
					currentState = STATE_NEW_VALUE;
				} else if (codePoint == ESCAPE) {
					currentState = STATE_CONSUME_ESCAPED;
				} else {
					currentElement.appendCodePoint(codePoint);
					currentState = STATE_CONSUME_PLAIN;
				}
			} else if (currentState == STATE_NEW_VALUE) {
				if (codePoint == SEPARATOR) {
					result.add("");
				} else if (codePoint == ESCAPE) {
					currentElement = new StringBuilder();
					currentState = STATE_CONSUME_ESCAPED;
				} else {
					currentElement = new StringBuilder();
					currentElement.appendCodePoint(codePoint);
					currentState = STATE_CONSUME_PLAIN;
				}
			} else if (currentState == STATE_CONSUME_PLAIN) {
				if (codePoint == SEPARATOR) {
					result.add(currentElement.toString());
					currentState = STATE_NEW_VALUE;
				} else {
					currentElement.appendCodePoint(codePoint);
					currentState = STATE_CONSUME_PLAIN;
				}
			} else if (currentState == STATE_CONSUME_ESCAPED) {
				if (codePoint == ESCAPE) {
					currentState = STATE_COULD_BE_ESCAPED;
				} else {
					currentElement.appendCodePoint(codePoint);
					currentState = STATE_CONSUME_ESCAPED;
				}
			} else if (currentState == STATE_COULD_BE_ESCAPED) {
				if (codePoint == ESCAPE) {
					currentElement.append(ESCAPE);
					currentState = STATE_CONSUME_ESCAPED;
				} else {
					result.add(currentElement.toString());
					
					// behave like STATE_NEW_VALUE
					if (codePoint == SEPARATOR) {
						currentState = STATE_NEW_VALUE;
					} else {
						// illegal state
						throw new IllegalStateException("could not split " + raw);
					}
				}
			}
		}
		// clean up state
		if (currentState == STATE_START) {
			result.add("");
		} else if (currentState == STATE_NEW_VALUE) {
			result.add("");
		} else if (currentState == STATE_CONSUME_PLAIN) {
			result.add(currentElement.toString());
		} else if (currentState == STATE_CONSUME_ESCAPED) {
			result.add(currentElement.toString());
		} else if (currentState == STATE_COULD_BE_ESCAPED) {
			result.add(currentElement.toString());
		}
		return result;
	}
	
	public static String concat(Iterable<String> input) {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (String string : input) {
			if (!isFirst) {
				sb.append(SEPARATOR);
			}
			if (!string.isEmpty()) {
				// iterate over the code points and escape them
				if (containsSpecialCharacter(string)) {
					sb.append(ESCAPE);
					for (int codePoint : StringUtil.codePoints(string)) {
						if (codePoint == ESCAPE) {
							sb.append(ESCAPE).append(ESCAPE);
						} else {
							sb.appendCodePoint(codePoint);
						}
					}
					sb.append(ESCAPE);
				} else {
					sb.append(string);
				}
			}
			isFirst = false;
		}
		return sb.toString();
	}
	
	private static boolean containsSpecialCharacter(String input) {
		for (int codePoint : StringUtil.codePoints(input)) {
			if (codePoint == ESCAPE || codePoint == SEPARATOR) {
				return true;
			}
		}
		return false;
	}
}
