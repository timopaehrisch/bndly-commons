package org.bndly.schema.json.beans;

/*-
 * #%L
 * Schema JSON Beans
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
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JSONUtil {

	public static JSMember getMemberByName(String name, JSObject o) {
		if (name == null) {
			throw new IllegalArgumentException("member name was null.");
		}
		return o.getMember(name);
	}

	public static void createNullMember(JSObject jsObject, String memberName) {
		setJSMember(jsObject, memberName, new JSNull());
	}

	public static void createDateMember(JSObject jsObject, String memberName, Date dateValue) {
		if (dateValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		JSNumber jsValue = new JSNumber();
		jsValue.setValue(new BigDecimal(dateValue.getTime()));
		setJSMember(jsObject, memberName, jsValue);
	}

	public static void createBooleanMember(JSObject jsObject, String memberName, Boolean booleanValue) {
		if (booleanValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		JSBoolean jsValue = new JSBoolean();
		jsValue.setValue(booleanValue);
		setJSMember(jsObject, memberName, jsValue);
	}

	public static void createNumberMember(JSObject jsObject, String memberName, Double numberValue) {
		if (numberValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		createNumberMember(jsObject, memberName, new BigDecimal(numberValue));
	}

	public static void createNumberMember(JSObject jsObject, String memberName, Long numberValue) {
		if (numberValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		createNumberMember(jsObject, memberName, new BigDecimal(numberValue));
	}

	public static void createNumberMember(JSObject jsObject, String memberName, BigDecimal numberValue) {
		if (numberValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		JSNumber jsValue = new JSNumber();
		jsValue.setValue(numberValue);
		setJSMember(jsObject, memberName, jsValue);
	}

	public static void createStringMember(JSObject jsObject, String memberName, String stringValue) {
		if (stringValue == null) {
			createNullMember(jsObject, memberName);
			return;
		}
		setJSMember(jsObject, memberName, new JSString(stringValue));
	}

	public static void setJSMember(JSObject jsObject, String memberName, JSValue jsValue) {
		Set<JSMember> members = assertMembersNotNull(jsObject);
		JSMember member = new JSMember();
		member.setName(new JSString(memberName));
		member.setValue(jsValue);
		members.add(member);
	}

	public static void createJSObjectMember(JSObject jsObject, String memberName, JSObject nestedJsObject) {
		setJSMember(jsObject, memberName, nestedJsObject);
	}

	public static void createJSArrayMember(JSObject jsObject, String memberName, List<JSValue> nestedJsObject) {
		JSArray array = new JSArray();
		array.setItems(nestedJsObject);
		setJSMember(jsObject, memberName, array);
	}

	public static Set<JSMember> assertMembersNotNull(JSObject jsObject) {
		Set<JSMember> members = jsObject.getMembers();
		if (members == null) {
			members = new LinkedHashSet<>();
			jsObject.setMembers(members);
		}
		return members;
	}

}
