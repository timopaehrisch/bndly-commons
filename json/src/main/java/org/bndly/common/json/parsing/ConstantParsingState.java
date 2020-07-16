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

import org.bndly.common.json.model.JSValue;

public abstract class ConstantParsingState extends ParsingState {

	private final String constant;
	private int pos;

	public ConstantParsingState(String constant) {
		if (constant == null || constant.isEmpty()) {
			throw new IllegalArgumentException("constant to parse has to be non null and not empty");
		}
		this.constant = constant;
		this.pos = 0;
	}

	@Override
	public void handleChar(char c, Stack<ParsingState> stateStack) {
		if (constant.charAt(pos) == c) {
			pos++;
			if (pos == constant.length()) {
				onAcceptedValue(stateStack);
			}
			return;
		}
		throw new ParsingException("expected constant " + constant + " was not found. error at position " + pos);
	}
	
	public final String getConstant() {
		return constant;
	}

	public abstract JSValue getValue();
	
	public abstract void onAcceptedValue(Stack<ParsingState> stateStack);
}
