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

import org.bndly.common.json.model.JSString;

public class StringParsingState extends ParsingState {

	private final char borderChar;
	private boolean nextCharacterEscaped;
	private final StringBuffer valueBuffer = new StringBuffer();

	public StringParsingState(char borderChar) {
		this.borderChar = borderChar;
	}

	@Override
	public void handleChar(char c, Stack<ParsingState> stateStack) {
		if (nextCharacterEscaped) {
			nextCharacterEscaped = false;
			if (borderChar == c) {
				valueBuffer.append(c);
			} else if ('\\' == c) {
				valueBuffer.append(c);
			} else if ('/' == c) {
				valueBuffer.append(c);
			} else if ('b' == c) {
				valueBuffer.append('\b');
			} else if ('f' == c) {
				valueBuffer.append('\f');
			} else if ('r' == c) {
				valueBuffer.append('\r');
			} else if ('n' == c) {
				valueBuffer.append('\n');
			} else if ('t' == c) {
				valueBuffer.append('\t');
			} else if ('u' == c) {
				stateStack.add(new HexaCharacterParsingState() {

					@Override
					public void onCharacterParsed(char ch) {
						valueBuffer.append(ch);
					}

				});
			} else {
				throw new ParsingException("escaped character not supported");
			}
		} else {
			if ('\\' == c) {
				nextCharacterEscaped = true;
			} else if (borderChar == c) {
				stateStack.pop();
				onStringParsed(new JSString(valueBuffer.toString()));
			} else {
				valueBuffer.append(c);
			}
		}
	}

	public void onStringParsed(JSString string) {}

}
