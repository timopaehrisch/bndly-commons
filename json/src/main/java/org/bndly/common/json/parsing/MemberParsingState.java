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

import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;

public class MemberParsingState extends ParsingState {

	private JSMember member;
	private boolean expectingValue;
	
	@Override
	public void handleChar(char c, final Stack<ParsingState> stateStack) {
		if (Character.isWhitespace(c)) {
			return;
		}
		if (member == null && '\"' == c) {
			stateStack.add(new StringParsingState(c) {

				@Override
				public void onStringParsed(JSString string) {
					member = new JSMember();
					member.setName(string);
				}

			});

		} else if (member != null && ':' == c) {
			expectingValue = true;

		} else if (member != null && expectingValue) {
			stateStack.add(new ValueParsingState() {

				@Override
				public void didParseValue(JSValue value, Character stopChar) {
					stateStack.pop();
					member.setValue(value);
					didParseMember(member);
					if (stopChar != null) {
						stateStack.top().handleChar(stopChar, stateStack);
					}
				}

			}).handleChar(c, stateStack);
		} else {
			throw new ParsingException("failed to parse member of object");
		}
	}

	public void didParseMember(JSMember member) {}
	
}
