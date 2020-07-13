package org.bndly.common.lang;

/*-
 * #%L
 * Lang
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
import java.io.StringWriter;
import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class StringUtil {

	private StringUtil() {
		throw new IllegalStateException("do not instantiate this class");
	}

	private static final char[] HEX_ARRAY = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private static final int[] INT_HEX_MASKS;
	private static final int[] INT_HEX_SHIFTS;
	static {
		final int bytes = Integer.SIZE >> 3;
		INT_HEX_MASKS = new int[bytes * 2];
		INT_HEX_SHIFTS = new int[bytes * 2];
		for (int i = 0; i < INT_HEX_MASKS.length; i++) {
			if (i == 0) {
				INT_HEX_MASKS[i] = 0xF;
				INT_HEX_SHIFTS[i] = 0;
			} else {
				INT_HEX_MASKS[i] = INT_HEX_MASKS[i - 1] << 4;
				INT_HEX_SHIFTS[i] = i * 4;
			}
		}
	}
	
	public static Iterable<Integer> codePoints(final String input) {
		if (input == null) {
			return new Iterable<Integer>() {
				@Override
				public Iterator<Integer> iterator() {
					return new Iterator<Integer>() {
						@Override
						public boolean hasNext() {
							return false;
						}

						@Override
						public Integer next() {
							return null;
						}

						@Override
						public void remove() {
						}
						
					};
				}
			};
		}
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {
					private int nextIndex = 0;

					@Override
					public boolean hasNext() {
						return nextIndex < input.length();
					}

					@Override
					public Integer next() {
						int codePoint = input.codePointAt(nextIndex);
						nextIndex += Character.charCount(codePoint);
						return codePoint;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static void appendIntAsHex(int integer, Appendable appendable) throws IOException {
		appendIntAsHex(integer, appendable, true);
	}
	
	public static void appendIntAsHex(int integer, Appendable appendable, boolean writeLeadingZeros) throws IOException {
		// total hex elements = Integer.BYTES*2
		// if the int has 4 bytes length -> 32bit -> 4bit can be stored in a single hex digit -> 8 hex digits required
		boolean hasNonZero = writeLeadingZeros;
		for (int i = INT_HEX_MASKS.length - 1; i >= 0; i--) {
			int mask = INT_HEX_MASKS[i];
			int value = (integer & mask) >> INT_HEX_SHIFTS[i];
			hasNonZero = hasNonZero || (value != 0);
			if (hasNonZero) {
				appendable.append(HEX_ARRAY[value]);
			}
		}
		if (!hasNonZero) {
			appendable.append(HEX_ARRAY[0]);
		}
	}
	
	public static String lowerCaseFirstLetter(String value) {
		StringWriter stringWriter = new StringWriter();
		boolean first = true;
		for (int codePoint : codePoints(value)) {
			if (first) {
				codePoint = Character.toLowerCase(codePoint);
				first = false;
			}
			stringWriter.write(codePoint);
		}
		return stringWriter.toString();
	}

	public static String upperCaseFirstLetter(String value) {
		StringWriter stringWriter = new StringWriter();
		boolean first = true;
		for (int codePoint : codePoints(value)) {
			if (first) {
				codePoint = Character.toUpperCase(codePoint);
				first = false;
			}
			stringWriter.write(codePoint);
		}
		return stringWriter.toString();
	}
}
