package org.bndly.common.service.validation.interpreter;

/*-
 * #%L
 * Validation Rules Interpreter
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XPathResolver {
	
	private static final Logger LOG = LoggerFactory.getLogger(XPathResolver.class);

	public <T, B> T resolve(String expression, B bean) {
		return resolve(expression, bean, true);
	}

	public <T, B> T resolve(String expression, B bean, boolean failOnNotFound) {
		String rawExpression = getRawExpression(expression);
		String nextExpression = getNextExpression(expression);
		String fieldName = getFieldName(rawExpression);
		String rawCondition = getRawCondition(rawExpression);
		String conditionAttributeName = getConditionAttributeName(rawCondition);
		Object conditionAttributeValue = getConditionAttributeValue(rawCondition);

		T result = null;
		if (conditionAttributeName == null && conditionAttributeValue == null) {
			result = getFieldValueByName(bean, fieldName, failOnNotFound);
		} else {
			throw new IllegalStateException("XPath Conditions are not yet implemented.");
		}

		if (nextExpression != null) {
			if (result != null) {
				return resolve(nextExpression, result, failOnNotFound);
			} else {
				return null;
			}
		} else {
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private <B, T> T getFieldValueByName(B bean, String fieldName, boolean failOnNotFound) {
		Class<B> clazz = (Class<B>) bean.getClass();
		List<FieldBinding> fields = getAllFieldsFromClass(clazz);
		Field field = getFieldByName(fields, fieldName);
		if (field == null) {
			if (failOnNotFound) {
				throw new IllegalArgumentException("could not find field " + fieldName + " in " + bean.getClass().getSimpleName());
			} else {
				return null;
			}
		}
		try {
			boolean accessible = field.isAccessible();
			field.setAccessible(true);
			T result = (T) field.get(bean);
			field.setAccessible(accessible);
			return result;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOG.error("failed to get value of field " + fieldName + ": " + e.getMessage(), e);
		}
		return null;
	}

	private Field getFieldByName(List<FieldBinding> fields, String fieldName) {
		for (FieldBinding fieldBinding : fields) {
			if (fieldName.equals(fieldBinding.getField().getName())) {
				return fieldBinding.getField();
			}
		}
		return null;
	}

	public static <B> List<FieldBinding> getAllFieldsFromClass(Class<B> clazz) {
		List<FieldBinding> list = new ArrayList<>();
		collectAllFieldsFromClass(clazz, list);
		return list;
	}

	private static <B> void collectAllFieldsFromClass(Class<B> clazz, List<FieldBinding> list) {
		Field[] fields = clazz.getDeclaredFields();
		boolean isUsingManualAccessor = false;
		XmlAccessorType accessorAnnotation = clazz.getAnnotation(XmlAccessorType.class);
		if (accessorAnnotation != null) {
			XmlAccessType accessorType = accessorAnnotation.value();
			if (XmlAccessType.NONE.equals(accessorType)) {
				isUsingManualAccessor = true;
			}
		}

		if (isUsingManualAccessor) {
			for (Field field : fields) {
				XmlElementWrapper xmlElementWrapperAnnotation = field.getAnnotation(XmlElementWrapper.class);
				if (xmlElementWrapperAnnotation != null) {
					handleXmlElementWrapper(xmlElementWrapperAnnotation, field, list);
				} else {
					XmlElements xmlElementsAnnotation = field.getAnnotation(XmlElements.class);
					if (xmlElementsAnnotation != null) {
						XmlElement[] xmlElements = xmlElementsAnnotation.value();
						for (XmlElement xmlElement : xmlElements) {
							handleXmlElement(xmlElement, field, list);
						}
					} else {
						XmlElement xmlElement = field.getAnnotation(XmlElement.class);
						if (xmlElement != null) {
							handleXmlElement(xmlElement, field, list);
						}
					}
				}
			}
		}
		Class<? super B> superClazz = clazz.getSuperclass();
		if (superClazz != null) {
			collectAllFieldsFromClass(superClazz, list);
		}
	}

	private static void handleXmlElementWrapper(XmlElementWrapper element, Field field, List<FieldBinding> list) {
		String elementName = element.name();
		if (elementName.equals("##default")) {
			elementName = field.getName();
		}
		list.add(new FieldBinding(field, elementName));
	}

	private static void handleXmlElement(XmlElement element, Field field, List<FieldBinding> list) {
		String elementName = element.name();
		if (elementName.equals("##default")) {
			elementName = field.getName();
		}
		list.add(new FieldBinding(field, elementName));
	}

	private String getRawExpression(String expression) {
		int i = expression.indexOf(".");
		if (i > 0) {
			return expression.substring(0, i);
		} else {
			return expression;
		}
	}

	private String getNextExpression(String expression) {
		int i = expression.indexOf(".");
		if (i > 0) {
			return expression.substring(i + 1);
		} else {
			return null;
		}
	}

	private String getFieldName(String expression) {
		int i = expression.indexOf("[");
		if (i > 0) {
			return expression.substring(0, i);
		} else {
			return expression;
		}
	}

	private String getRawCondition(String expression) {
		int si = expression.indexOf("[");
		int ei = expression.indexOf("]");
		if (si > 0 && ei > si) {
			return expression.substring(si + 1, ei - si - 1);
		} else {
			return null;
		}
	}

	private String getConditionAttributeName(String rawCondition) {
		if (rawCondition == null) {
			return null;
		}
		int i = rawCondition.indexOf("=");
		if (i > 0) {
			return rawCondition.substring(0, i);
		} else {
			return null;
		}
	}

	private Object getConditionAttributeValue(String rawCondition) {
		if (rawCondition == null) {
			return null;
		}
		int i = rawCondition.indexOf("=");
		if (i > 0) {
			String rawValue = rawCondition.substring(i + 1);
			int si = rawValue.indexOf("'");
			int ei = rawValue.lastIndexOf("'");
			if (si > 0 && si < ei) {
				return rawValue.substring(si + 1, ei - si - 1);
			} else {
				return rawValue;
			}
		} else {
			return null;
		}

	}
}
