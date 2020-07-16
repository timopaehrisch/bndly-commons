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

import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSValue;
import java.util.ArrayList;
import java.util.List;

public class ArrayParsingState extends ParsingState {

	private boolean expectingValue;
	private List<JSValue> elementsInternal;

	public ArrayParsingState() {
		expectingValue = true;
	}

	@Override
	public void handleChar(char c, final Stack<ParsingState> stateStack) {
		if (Character.isWhitespace(c)) {
			return;
		}
		if (expectingValue) {
			stateStack.add(new ValueParsingState() {

				@Override
				public void handleChar(char c, Stack<ParsingState> stateStack) {
					if (c == ']' && elementsInternal == null) {
						stateStack.pop(); // pop the value state
						stateStack.pop(); // pop the array state
						onArrayParsed(new JSArray());
					} else {
						super.handleChar(c, stateStack);
					}
				}

				@Override
				public void didParseValue(JSValue value, Character stopChar) {
					if (elementsInternal == null) {
						elementsInternal = new ArrayList<>();
					}
					elementsInternal.add(value);
					expectingValue = false;
					if (stopChar != null) {
						stateStack.top().handleChar(stopChar, stateStack);
					}
				}

			}).handleChar(c, stateStack);
		} else if (!expectingValue && ',' == c) {
			expectingValue = true;
		} else if (!expectingValue && ']' == c) {
			stateStack.pop();
			JSArray array = new JSArray();
			array.setItems(elementsInternal);
			onArrayParsed(array);
		} else {
			throw new ParsingException("did not expect anyting");
		}
	}

	public void onArrayParsed(JSArray array) {}
}
