package org.bndly.common.osgi.config.impl;

/*-
 * #%L
 * OSGI Config
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

import org.bndly.common.osgi.config.TypeConverter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.osgi.service.metatype.AttributeDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class MetaTypeAttributeConverters {
	static final TypeConverter[] CONVERTERS = new TypeConverter[AttributeDefinition.PASSWORD + 1];
	
	static {
		CONVERTERS[AttributeDefinition.STRING] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return String.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				return rawValue;
			}

			@Override
			public String convertToString(Object value) {
				if (String.class.isInstance(value)) {
					return (String) value;
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new String[size];
			}
		};
		CONVERTERS[AttributeDefinition.LONG] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Long.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Long.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Long.class.isInstance(value)) {
					return ((Long) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Long[size];
			}
		};
		CONVERTERS[AttributeDefinition.INTEGER] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Integer.class;
			}
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Integer.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Integer.class.isInstance(value)) {
					return ((Integer) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Integer[size];
			}
		};
		CONVERTERS[AttributeDefinition.SHORT] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Short.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Short.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Short.class.isInstance(value)) {
					return ((Short) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Short[size];
			}
		};
		CONVERTERS[AttributeDefinition.CHARACTER] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Character.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				return rawValue.charAt(0);
			}

			@Override
			public String convertToString(Object value) {
				if (Character.class.isInstance(value)) {
					return ((Character) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Character[size];
			}
		};
		CONVERTERS[AttributeDefinition.BYTE] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Byte.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Byte.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Byte.class.isInstance(value)) {
					return ((Byte) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Byte[size];
			}
		};
		CONVERTERS[AttributeDefinition.DOUBLE] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Double.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Double.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Double.class.isInstance(value)) {
					return ((Double) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Double[size];
			}
		};
		CONVERTERS[AttributeDefinition.FLOAT] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Float.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return Float.valueOf(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (Float.class.isInstance(value)) {
					return ((Float) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Float[size];
			}
		};
		CONVERTERS[AttributeDefinition.BIGINTEGER] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return BigInteger.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return new BigInteger(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (BigInteger.class.isInstance(value)) {
					return ((BigInteger) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new BigInteger[size];
			}
		};
		CONVERTERS[AttributeDefinition.BIGDECIMAL] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return BigDecimal.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				try {
					return new BigDecimal(rawValue);
				} catch (NumberFormatException e) {
					return null;
				}
			}

			@Override
			public String convertToString(Object value) {
				if (BigDecimal.class.isInstance(value)) {
					return ((BigDecimal) value).toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new BigDecimal[size];
			}
		};
		CONVERTERS[AttributeDefinition.BOOLEAN] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return Boolean.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				if (rawValue == null || rawValue.isEmpty()) {
					return null;
				}
				return Boolean.valueOf(rawValue);
			}

			@Override
			public String convertToString(Object value) {
				if (Boolean.class.isInstance(value)) {
					return value.toString();
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new Boolean[size];
			}
			
		};
		CONVERTERS[AttributeDefinition.PASSWORD] = new TypeConverter() {
			@Override
			public Class<?> getTargetJavaType() {
				return String.class;
			}
			
			@Override
			public Object convertFromString(String rawValue) {
				return rawValue;
			}

			@Override
			public String convertToString(Object value) {
				if (String.class.isInstance(value)) {
					return (String) value;
				}
				return null;
			}

			@Override
			public Object[] createArray(int size) {
				return new String[size];
			}
			
		};
	}

	static TypeConverter get(AttributeDefinition attributeDef) {
		return CONVERTERS[attributeDef.getType()];
	}
	static TypeConverter get(int i) {
		return CONVERTERS[i];
	}
	

	private MetaTypeAttributeConverters() {
	}
	
}
