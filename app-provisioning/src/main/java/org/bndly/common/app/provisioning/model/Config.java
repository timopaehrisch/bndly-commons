package org.bndly.common.app.provisioning.model;

/*-
 * #%L
 * App Provisioning
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Config {
	private final String name;
	private final Map<String, Object> properties;

	Config(String configName, JSObject properties) {
		Set<JSMember> members = properties.getMembers();
		if (members != null && !members.isEmpty()) {
			Map<String, Object> tmp = new LinkedHashMap<>();
			for (JSMember member : members) {
				String propertyName = member.getName().getValue();
				JSValue val = member.getValue();
				Object valJava = convert(val);
				tmp.put(propertyName, valJava);
			}
			this.properties = Collections.unmodifiableMap(tmp);
		} else {
			this.properties = Collections.EMPTY_MAP;
		}
		this.name = configName;
	}

	public JSValue toJsValue() {
		ConversionContext context = new ConversionContextBuilder().initDefaults().build();
		final JSObject jsObject = new JSObject();
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			Object val = entry.getValue();
			if (val == null) {
				jsObject.createMember(entry.getKey()).setValue(JSNull.INSTANCE);
			} else {
				jsObject.createMember(entry.getKey()).setValue(context.serialize(val.getClass(), val));
			}
		}
		return jsObject;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	private Object convert(JSValue val) {
		if (JSArray.class.isInstance(val)) {
			List<Object> converted = new ArrayList<>();
			JSArray arr = (JSArray) val;
			for (JSValue arrayItem : arr) {
				converted.add(convert(arrayItem));
			}
			return converted.toArray();
		} else if (JSString.class.isInstance(val)) {
			return ((JSString) val).getValue();
		} else if (JSNull.class.isInstance(val)) {
			return null;
		} else if (JSNumber.class.isInstance(val)) {
			return ((JSNumber) val).getValue();
		} else if (JSBoolean.class.isInstance(val)) {
			return ((JSBoolean) val).isValue();
		} else {
			throw new IllegalArgumentException("unsupported value: " + val);
		}
	}
	
}
