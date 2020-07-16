package org.bndly.common.json.impl;

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
import org.bndly.common.json.api.Deserializer;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.unmarshalling.JSObjectReflectiveValueProvider;
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class InterfaceDeserializer implements Deserializer {

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (value == null || !JSObject.class.isInstance(value)) {
			return false;
		}
		Class cls = ReflectionUtil.getSimpleClassType(targetType);
		if (cls == null) {
			return false;
		}
		if (!cls.isInterface()) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (conversionContext.canInstantiate(targetType, value) && JSObject.class.isInstance(value)) {
			Object instance = conversionContext.newInstance(targetType, value);
			ObjectDeSerializer.deserializePublicSetters(targetType, conversionContext, (JSObject) value, instance.getClass(), instance);
			return instance;
		} else {
			Class cls = ReflectionUtil.getSimpleClassType(targetType);
			JSObject obj = (JSObject) value;
			JSObjectReflectiveValueProvider valueProdiver = new JSObjectReflectiveValueProvider(conversionContext, obj);
			Object proxy = InstantiationUtil.instantiateDomainModelInterface(cls, valueProdiver);
			return proxy;
		}
	}

}
