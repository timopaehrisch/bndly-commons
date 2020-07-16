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
import org.bndly.common.json.model.JSObject;
import java.util.LinkedHashSet;
import java.util.Set;

public class ObjectParsingState extends ParsingState {

	private final JSObject value = new JSObject();
	private boolean expectingMember = true;

	public JSObject getValue() {
		return value;
	}

	@Override
	public void handleChar(char c, final Stack<ParsingState> stateStack) {
		if (Character.isWhitespace(c)) {
			return;
		}
		if ('\"' == c && expectingMember) {
			// parse member name
			expectingMember = false;
			stateStack.add(new MemberParsingState() {

				@Override
				public void didParseMember(JSMember member) {
					Set<JSMember> members = value.getMembers();
					if (members == null) {
						members = new LinkedHashSet<>();
						value.setMembers(members);
					}
					members.add(member);
				}

			}).handleChar(c, stateStack);
		} else if ('}' == c) {
			// object complete
			stateStack.pop();
			onObjectParsed(value);
		} else if (',' == c && !expectingMember) {
			expectingMember = true;
		} else {
			throw new ParsingException("illegal character " + c + " while parsing object");
		}
	}

	public void onObjectParsed(JSObject object) {}
	
}
