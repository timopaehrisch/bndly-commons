package org.bndly.common.reflection;

/*-
 * #%L
 * Reflection
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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class StringTypeConversionUtil {

	private static final String DATE_FORMAT = "yyyyMMddHHmmss";
	private static final String DECIMAL_FORMAT = "0.#####";

	private StringTypeConversionUtil() {
	}

	public static <T> T stringToType(String stringValue, Class<T> type) {
		if (stringValue == null) {
			return null;
		}

		T t = null;
		if (Boolean.class.equals(type) || boolean.class.equals(type)) {
			if ("true".equals(stringValue)) {
				t = (T) Boolean.TRUE;
			} else if ("false".equals(stringValue)) {
				t = (T) Boolean.FALSE;
			} else {
				throw new IllegalArgumentException("can't convert '" + stringValue + "' to a boolean");
			}
		} else if (Character.class.equals(type) || char.class.equals(type)) {
			if (stringValue.length() == 1) {
				t = (T) new Character(stringValue.charAt(0));
			} else {
				throw new IllegalArgumentException("can't convert '" + stringValue + "' to a character because the input string is too long");
			}
		} else if (Byte.class.equals(type) || byte.class.equals(type)) {
			t = (T) new Byte(stringValue);
		} else if (Short.class.equals(type) || short.class.equals(type)) {
			t = (T) new Short(stringValue);
		} else if (Integer.class.equals(type) || int.class.equals(type)) {
			t = (T) new Integer(stringValue);
		} else if (Long.class.equals(type) || long.class.equals(type)) {
			t = (T) new Long(stringValue);
		} else if (Float.class.equals(type) || float.class.equals(type)) {
			t = (T) new Float(stringValue);
		} else if (Double.class.equals(type) || double.class.equals(type)) {
			t = (T) new Double(stringValue);
		} else if (BigDecimal.class.equals(type)) {
			t = (T) new BigDecimal(stringValue);
		} else if (String.class.equals(type)) {
			t = (T) stringValue;
		} else if (Date.class.equals(type)) {
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			try {
				t = (T) df.parse(stringValue);
			} catch (ParseException ex) {
				throw new IllegalArgumentException("cant't convert string " + stringValue + " to date. use the following date pattern: " + DATE_FORMAT);
			}
		} else {
			throw new IllegalArgumentException("cant't convert string to " + type.getSimpleName());
		}
		return t;
	}

	public static <T> String objectToString(T value) {
		DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);

		Class<? extends Object> type = value.getClass();

		if (Boolean.class.equals(type) || boolean.class.equals(type)) {
			Boolean v = (Boolean) value;
			return v.toString();
		} else if (Character.class.equals(type) || char.class.equals(type)) {
			Character v = (Character) value;
			return v.toString();
		} else if (Byte.class.equals(type) || byte.class.equals(type)) {
			Byte v = (Byte) value;
			return v.toString();
		} else if (Short.class.equals(type) || short.class.equals(type)) {
			Short v = (Short) value;
			return v.toString();
		} else if (Integer.class.equals(type) || int.class.equals(type)) {
			Integer v = (Integer) value;
			return Integer.toString(v);
		} else if (Long.class.equals(type) || long.class.equals(type)) {
			Long v = (Long) value;
			return Long.toString(v);
		} else if (Float.class.equals(type) || float.class.equals(type)) {
			Float v = (Float) value;
			return df.format(v);
		} else if (Double.class.equals(type) || double.class.equals(type)) {
			Double v = (Double) value;
			return df.format(v);
		} else if (BigDecimal.class.equals(type)) {
			return ((BigDecimal) value).stripTrailingZeros().toPlainString();
		} else if (String.class.equals(type)) {
			return (String) value;
		} else if (Date.class.equals(type)) {
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			return dateFormat.format((Date) value);
		} else {
			throw new IllegalArgumentException("cant't convert string to " + type.getSimpleName());
		}
	}
}
