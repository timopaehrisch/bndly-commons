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

import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSValue;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BooleanParsingState extends ConstantParsingState {

	public final boolean expected;
	private JSBoolean val;

	public BooleanParsingState(boolean expected) {
		super(expected ? "true" : "false");
		this.expected = expected;
	}

	@Override
	public void onAcceptedValue(Stack<ParsingState> stateStack) {
		stateStack.pop();
		if (expected) {
			val = JSBoolean.TRUE;
		} else {
			val = JSBoolean.FALSE;
		}
		onBooleanParsed(val);
	}
	
	@Override
	public JSValue getValue() {
		if (val == null) {
			throw new ParsingException("expected string was not accepted yet");
		}
		return val;
	}

	public void onBooleanParsed(JSBoolean booleanValue) {}
	
}
