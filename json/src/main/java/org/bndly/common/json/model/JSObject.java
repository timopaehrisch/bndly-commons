package org.bndly.common.json.model;

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

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class JSObject extends JSValue {

	private Set<JSMember> members;

	public final Set<JSMember> getMembers() {
		return members;
	}

	public final void setMembers(Set<JSMember> members) {
		this.members = members;
	}

	public final JSMember createMember(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name is not allowed to be null or empty");
		}
		JSMember member = new JSMember();
		member.setName(new JSString(name));
		if (getMembers() == null) {
			setMembers(new LinkedHashSet<JSMember>());
		}
		getMembers().add(member);
		return member;
	}
	
	public final JSObject createMemberValue(String name, JSValue value) {
		createMember(name).setValue(value == null ? JSNull.INSTANCE : value);
		return this;
	}
	
	public final JSObject createMemberValue(String name, String value) {
		createMember(name).setValue(value == null ? JSNull.INSTANCE : new JSString(value));
		return this;
	}
	
	public final JSObject createMemberValue(String name, Boolean value) {
		createMember(name).setValue(value == null ? JSNull.INSTANCE : value ? JSBoolean.TRUE : JSBoolean.FALSE);
		return this;
	}
	
	public final JSObject createMemberValue(String name, BigDecimal value) {
		createMember(name).setValue(value == null ? JSNull.INSTANCE : new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, double value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, float value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, long value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, int value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, short value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final JSObject createMemberValue(String name, byte value) {
		createMember(name).setValue(new JSNumber(value));
		return this;
	}

	public final <E extends JSValue> E getMemberValue(String name, Class<E> valueType) {
		if (valueType == null) {
			throw new IllegalArgumentException("valueType is not allowed to be null");
		}
		JSValue value = getMemberValue(name);
		if (valueType.isInstance(value)) {
			return valueType.cast(value);
		}
		return null;
	}
	
	public final String getMemberStringValue(String name) {
		JSString value = getMemberValue(name, JSString.class);
		if (value != null) {
			return value.getValue();
		}
		return null;
	}
	
	public final JSObject getMemberObjectValue(String name) {
		JSObject value = getMemberValue(name, JSObject.class);
		return value;
	}
	
	public final JSArray getMemberArrayValue(String name) {
		JSArray value = getMemberValue(name, JSArray.class);
		return value;
	}

	public final Boolean getMemberBooleanValue(String name) {
		JSBoolean value = getMemberValue(name, JSBoolean.class);
		if (value != null) {
			return value.isValue();
		}
		return null;
	}

	public final BigDecimal getMemberNumberValue(String name) {
		JSNumber value = getMemberValue(name, JSNumber.class);
		if (value != null) {
			return value.getValue();
		}
		return null;
	}

	public final boolean isMemberNull(String name) {
		JSNull value = getMemberValue(name, JSNull.class);
		if (value != null) {
			return true;
		}
		return false;
	}
	
	public final JSValue getMemberValue(String name) {
		JSMember member = getMember(name);
		if (member == null) {
			return null;
		}
		JSValue value = member.getValue();
		return value;
	}
	
	public final JSMember getMember(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name is not allowed to be null or empty");
		}
		if (getMembers() == null) {
			return null;
		}
		for (JSMember member : getMembers()) {
			if (member.getName() != null && name.equals(member.getName().getValue())) {
				return member;
			}
		}
		return null;
	}

}
