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
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ValueParsingState extends ParsingState {

	@Override
	public void handleChar(char c, Stack<ParsingState> stateStack) {
		// skip white spaces
		if (Character.isWhitespace(c)) {
			return;
		}
		// remove self
		stateStack.pop();
		if ('{' == c) {
			// new object
			stateStack.add(new ObjectParsingState() {

				@Override
				public void onObjectParsed(JSObject object) {
					didParseValue(object);
				}

			});
		} else if ('[' == c) {
			// new array
			stateStack.add(new ArrayParsingState() {

				@Override
				public void onArrayParsed(JSArray array) {
					didParseValue(array);
				}

			});
		} else if ('\"' == c) {
			// new string
			stateStack.add(new StringParsingState(c) {

				@Override
				public void onStringParsed(JSString string) {
					didParseValue(string);
				}

			});
		} else if ("-0123456789".indexOf(c) > -1) {
			// new number
			stateStack.add(new NumberParsingState() {

				@Override
				public void onNumberParsed(JSNumber number, Character stopChar) {
					didParseValue(number, stopChar);
				}

			}).handleChar(c, stateStack);
		} else if ('n' == c) {
			// null
			stateStack.add(new NullParsingState() {

				@Override
				public void onNullParsed(JSNull nullValue) {
					didParseValue(nullValue);
				}

			}).handleChar(c, stateStack);
		} else if ('t' == c) {
			// new boolean true
			stateStack.add(new BooleanParsingState(true) {

				@Override
				public void onBooleanParsed(JSBoolean booleanValue) {
					didParseValue(booleanValue);
				}

			}).handleChar(c, stateStack);
		} else if ('f' == c) {
			// new boolean false
			stateStack.add(new BooleanParsingState(false) {

				@Override
				public void onBooleanParsed(JSBoolean booleanValue) {
					didParseValue(booleanValue);
				}

			}).handleChar(c, stateStack);
		} else {
			throw new ParsingException("illegal character while parsing input data");
		}
	}
	
	private void didParseValue(JSValue value) {
		didParseValue(value, null);
	}
	
	public abstract void didParseValue(JSValue value, Character stopChar);
}
