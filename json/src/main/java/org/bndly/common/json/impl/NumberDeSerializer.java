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
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSValue;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NumberDeSerializer implements Serializer, Deserializer {
	private static final List<Class> supportedNumberTypes;
	
	static {
		supportedNumberTypes = new ArrayList<>();
		supportedNumberTypes.add(Long.class);
		supportedNumberTypes.add(Integer.class);
		supportedNumberTypes.add(Short.class);
		supportedNumberTypes.add(Byte.class);
		supportedNumberTypes.add(Double.class);
		supportedNumberTypes.add(Float.class);
		
		supportedNumberTypes.add(long.class);
		supportedNumberTypes.add(int.class);
		supportedNumberTypes.add(short.class);
		supportedNumberTypes.add(byte.class);
		supportedNumberTypes.add(double.class);
		supportedNumberTypes.add(float.class);
		
		supportedNumberTypes.add(BigDecimal.class);
	}
	
	@Override
	public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		if (!Class.class.isInstance(sourceType)) {
			return false;
		}
		Class classType = (Class) sourceType;
		if (classType.isPrimitive() && javaValue == null) {
			return false;
		}
		if (javaValue != null) {
			sourceType = javaValue.getClass();
		}
		return supportedNumberTypes.contains((Class) sourceType);
	}

	@Override
	public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
		if (javaValue != null) {
			sourceType = javaValue.getClass();
		}
		if (Long.class.equals(sourceType) || long.class.equals(sourceType)) {
			return new JSNumber((long) javaValue);
		} else if (Integer.class.equals(sourceType) || int.class.equals(sourceType)) {
			return new JSNumber((int) javaValue);
		} else if (Short.class.equals(sourceType) || short.class.equals(sourceType)) {
			return new JSNumber((short) javaValue);
		} else if (Byte.class.equals(sourceType) || byte.class.equals(sourceType)) {
			return new JSNumber((byte) javaValue);
		} else if (Double.class.equals(sourceType) || double.class.equals(sourceType)) {
			return new JSNumber((double) javaValue);
		} else if (Float.class.equals(sourceType) || float.class.equals(sourceType)) {
			return new JSNumber((float) javaValue);
		} else if (BigDecimal.class.equals(sourceType)) {
			return new JSNumber((BigDecimal) javaValue);
		} else {
			throw new IllegalArgumentException("java value was not supported for jsnumber conversion");
		}
	}

	@Override
	public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		if (!JSNumber.class.isInstance(value)) {
			return false;
		}
		if (!supportedNumberTypes.contains(targetType)) {
			return false;
		}
		if (((JSNumber) value).getValue() == null && ((Class) targetType).isPrimitive()) {
			return false;
		}
		return true;
	}

	@Override
	public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
		BigDecimal val = ((JSNumber) value).getValue();
		if (val == null) {
			return null;
		}
		if (Long.class.equals(targetType) || long.class.equals(targetType)) {
			return val.longValue();
		} else if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
			return val.intValue();
		} else if (Short.class.equals(targetType) || short.class.equals(targetType)) {
			return val.shortValue();
		} else if (Byte.class.equals(targetType) || byte.class.equals(targetType)) {
			return val.byteValue();
		} else if (Double.class.equals(targetType) || double.class.equals(targetType)) {
			return val.doubleValue();
		} else if (Float.class.equals(targetType) || float.class.equals(targetType)) {
			return val.floatValue();
		} else if (BigDecimal.class.equals(targetType)) {
			return val;
		} else {
			throw new IllegalArgumentException("java type was not supported for jsnumber conversion");
		}
	}

}
