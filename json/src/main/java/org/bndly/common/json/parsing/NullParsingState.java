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

import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSValue;

public class NullParsingState extends ConstantParsingState {
	private JSNull val;

	public NullParsingState() {
		super("null");
	}

	@Override
	public void onAcceptedValue(Stack<ParsingState> stateStack) {
		stateStack.pop();
		this.val = JSNull.INSTANCE;
		onNullParsed(val);
	}

	@Override
	public JSValue getValue() {
		if (val == null) {
			throw new ParsingException("expected string was not parsed yet");
		}
		return val;
	}
	
	public void onNullParsed(JSNull nullValue) {}

}
