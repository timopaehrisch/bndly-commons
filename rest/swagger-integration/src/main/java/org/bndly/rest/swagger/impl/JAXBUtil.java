package org.bndly.rest.swagger.impl;

/*-
 * #%L
 * REST Swagger Integration
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

import org.bndly.rest.swagger.model.Parameter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class JAXBUtil {
	private static final Logger LOG = LoggerFactory.getLogger(JAXBUtil.class);

	private JAXBUtil() {
	}
	
	/**
	 * Fixes the {@code elementName} of an XML element by replacing instances of {@code "##default"} with the name of the provided field name.
	 * @param elementName the element name to fix
	 * @param field the field, that holds the fallback element name
	 * @return a fixed element name
	 */
	static String fixXMLName(String elementName, Field field) {
		if ("##default".equals(elementName)) {
			elementName = field.getName();
		}
		return elementName;
	}
	
	/**
	 * Checks if the provided type is a {@link Collection} type or subtype.
	 * @param fieldType the type to check
	 * @return true, if the provided type is a collection type
	 */
	static boolean isCollection(Class<?> fieldType) {
		return Collection.class.isAssignableFrom(fieldType);
	}

	/**
	 * Checks if the provided type is a complex JAXB object by looking for a {@link XmlAccessorType} annotation.
	 * @param fieldType the type to check
	 * @return true, if the provided type is a complex object type
	 */
	static boolean isObject(Class<?> fieldType) {
		return fieldType.getAnnotation(XmlAccessorType.class) != null;
	}
	
	/**
	 * Checks if the provided type is a {@link Date} type or subtype.
	 * @param fieldType the type to check
	 * @return true, if the provided type is a date
	 */
	static boolean isDate(Class<?> fieldType) {
		return Date.class.isAssignableFrom(fieldType);
	}
	
	/**
	 * Checks if the provided type is considered to be a simple type. Simple types are all Java primitives and their object wrapper types. 
	 * Furthermore {@link String}, {@link Number}, {@link BigDecimal} and {@link Date} are considered to be simple types.
	 * @param fieldType the type to check
	 * @return true, if the provided type is considered to be a simple type.
	 */
	static boolean isSimple(Class<?> fieldType) {
		return 
				byte.class.equals(fieldType) || Byte.class.equals(fieldType)
				|| short.class.equals(fieldType) || Short.class.equals(fieldType)
				|| int.class.equals(fieldType) || Integer.class.equals(fieldType)
				|| long.class.equals(fieldType) || Long.class.equals(fieldType)
				|| float.class.equals(fieldType) || Float.class.equals(fieldType)
				|| double.class.equals(fieldType) || Double.class.equals(fieldType)
				|| Number.class.equals(fieldType) || BigDecimal.class.equals(fieldType)
				|| boolean.class.equals(fieldType) || Boolean.class.equals(fieldType)
				|| String.class.equals(fieldType)
				|| Date.class.equals(fieldType)
				;
	}
	
	public static Parameter.Type mapJavaTypeToSwaggerType(Type javaType) {
		if (Class.class.isInstance(javaType)) {
			Class cls = (Class) javaType;
			if (
					byte.class.equals(cls) || Byte.class.equals(cls)
					|| short.class.equals(cls) || Short.class.equals(cls)
					|| int.class.equals(cls) || Integer.class.equals(cls)
					|| long.class.equals(cls) || Long.class.equals(cls)
			) {
				return Parameter.Type.INTEGER;
			} else if (
					float.class.equals(cls) || Float.class.equals(cls)
					|| double.class.equals(cls) || Double.class.equals(cls)
					|| BigDecimal.class.equals(cls)
					|| Number.class.equals(cls)
			) {
				return Parameter.Type.NUMBER;
			} else if (
					String.class.equals(cls)
					|| Date.class.equals(cls)
			) {
				return Parameter.Type.STRING;
			} else if (
					boolean.class.equals(cls) || Boolean.class.equals(cls)
			) {
				return Parameter.Type.BOOLEAN;
			}
		}

		LOG.warn("could not map java type to swagger type: " + javaType);
		return null;
	}
}
