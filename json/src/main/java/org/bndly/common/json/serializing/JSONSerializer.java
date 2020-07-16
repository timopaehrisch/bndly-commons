package org.bndly.common.json.serializing;

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
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

public class JSONSerializer {

	public void serialize(JSValue jsObject, OutputStream os, String encoding) throws IOException {
		serialize(jsObject, os, encoding, true);
	}

	public void serialize(JSValue jsObject, OutputStream os, String encoding, boolean closeAfterSerializing) throws IOException {
		JSONWriter writer = new JSONWriter(new OutputStreamWriter(os, encoding));
		serializeValue(jsObject, writer);
		writer.flush();
		if (closeAfterSerializing) {
			writer.close();
		}
	}

	public void serialize(JSValue jsObject, OutputStream os) throws IOException {
		serialize(jsObject, os, "UTF-8");
	}

	public void serialize(JSValue jsObject, final Writer writer) throws IOException {
		serializeValue(jsObject, new JSONWriter(writer));
	}

	private void serializeValue(JSValue jsValue, JSONWriter writer) throws IOException {
		if (JSObject.class.isAssignableFrom(jsValue.getClass())) {
			writer.writeObjectStart();
			JSObject jsObject = (JSObject) jsValue;
			Set<JSMember> members = jsObject.getMembers();
			if (members != null) {
				boolean insertComma = false;
				for (JSMember jsMember : members) {
					if (insertComma) {
						writer.writeComma();
					}

					JSString memberName = jsMember.getName();
					serializeValue(memberName, writer);

					writer.writeColon();

					JSValue memberValue = jsMember.getValue();
					serializeValue(memberValue, writer);

					insertComma = true;
				}
			}
			writer.writeObjectEnd();
		} else if (JSNull.class.isAssignableFrom(jsValue.getClass())) {
			writer.writeNull();
		} else if (JSBoolean.class.isAssignableFrom(jsValue.getClass())) {
			JSBoolean jsBoolean = (JSBoolean) jsValue;
			if (jsBoolean.isValue()) {
				writer.writeTrue();
			} else {
				writer.writeFalse();
			}
		} else if (JSNumber.class.isAssignableFrom(jsValue.getClass())) {
			JSNumber jsNumber = (JSNumber) jsValue;
			writer.writeDecimal(jsNumber.getValue());
		} else if (JSString.class.isAssignableFrom(jsValue.getClass())) {
			JSString jsString = (JSString) jsValue;
			writer.writeString(jsString.getValue());
		} else if (JSArray.class.isAssignableFrom(jsValue.getClass())) {
			writer.writeArrayStart();
			JSArray jsArray = (JSArray) jsValue;
			List<JSValue> items = jsArray.getItems();
			if (items != null) {
				boolean insertComma = false;
				for (JSValue item : items) {
					if (insertComma) {
						writer.writeComma();
					}
					serializeValue(item, writer);
					insertComma = true;
				}
			}
			writer.writeArrayEnd();
		}
	}

}
