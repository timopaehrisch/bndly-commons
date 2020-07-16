package org.bndly.common.json.unmarshalling;

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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.reflection.ReflectivePojoValueProvider;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JSObjectReflectiveValueProvider implements ReflectivePojoValueProvider {
	private final JSObject object;
	private final ConversionContext conversionContext;

	public JSObjectReflectiveValueProvider(ConversionContext conversionContext, JSObject object) {
		if (conversionContext == null) {
			throw new IllegalArgumentException("conversionContext is not allowed to be null");
		}
		this.conversionContext = conversionContext;
		if (object == null) {
			throw new IllegalArgumentException("object is not allowed to be null");
		}
		this.object = object;
	}
	
	@Override
	public Object get(String propertyName, Type desiredType) {
		JSMember member = object.getMember(propertyName);
		if (member == null) {
			return null;
		}
		JSValue memberValue = member.getValue();
		return conversionContext.deserialize(desiredType, memberValue);
	}

	@Override
	public void set(String propertyName, Type requiredType, Object value) {
		JSValue boxed = conversionContext.serialize(requiredType, value);
		if (boxed != null) {
			JSMember member = object.getMember(propertyName);
			if (member == null) {
				member = object.createMember(propertyName);
			}
			member.setValue(boxed);
		}
	}
	
}
