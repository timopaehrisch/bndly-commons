package org.bndly.common.json.parsing;

/*-
 * #%L
 * JSON
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HexaCharacterParsingState extends ParsingState {
	private static final String HEXA_DIGITS = "0123456789ABCDEFabcdef";
	private final char[] chars = new char[4];
	private int pos = 0;
			
	@Override
	public void handleChar(char c, Stack<ParsingState> stateStack) {
		if (HEXA_DIGITS.indexOf(c) > -1) {
			chars[pos] = c;
			pos++;
			if (pos == chars.length) {
				int decoded = 0x0;
				for (char ch : chars) {
					int mask;
					if ('0' == ch) {
						mask = 0x0;
					} else if ('1' == ch) {
						mask = 0x1;
					} else if ('2' == ch) {
						mask = 0x2;
					} else if ('3' == ch) {
						mask = 0x3;
					} else if ('4' == ch) {
						mask = 0x4;
					} else if ('5' == ch) {
						mask = 0x5;
					} else if ('6' == ch) {
						mask = 0x6;
					} else if ('7' == ch) {
						mask = 0x7;
					} else if ('8' == ch) {
						mask = 0x8;
					} else if ('9' == ch) {
						mask = 0x9;
					} else if ('a' == ch || 'A' == ch) {
						mask = 0xA;
					} else if ('b' == ch || 'B' == ch) {
						mask = 0xB;
					} else if ('c' == ch || 'C' == ch) {
						mask = 0xC;
					} else if ('d' == ch || 'D' == ch) {
						mask = 0xD;
					} else if ('e' == ch || 'E' == ch) {
						mask = 0xE;
					} else if ('f' == ch || 'F' == ch) {
						mask = 0xF;
					} else {
						throw new ParsingException("illegal hexa expression in string: " + new String(chars));
					}
					decoded = (decoded << 4) | mask;
				}
				if (Character.isBmpCodePoint(decoded)) {
					stateStack.pop();
					onCharacterParsed((char) decoded);
				} else {
					throw new ParsingException("can not handle code points, that require more than one character");
				}
			}
		} else {
			throw new ParsingException("illegal character while parsing hexa character: " + c);
		}
	}
	
	public void onCharacterParsed(char ch) {}
	
}
