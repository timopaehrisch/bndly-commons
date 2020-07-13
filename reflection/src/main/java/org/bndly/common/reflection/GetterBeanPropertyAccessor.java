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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class GetterBeanPropertyAccessor extends AbstractBeanPropertyAccessorWriter implements BeanPropertyAccessor {

	@Override
	public Object get(String propertyName, Object target, TypeHint... typeHints) {
		if (target == null) {
			throw new IllegalArgumentException("target can not be null");
		}

		String uppercasePropertyName = propertyName;
		CollectionProperty collectionDescriptor = null;
		if (propertyNameRefersToElementInCollection(propertyName)) {
			collectionDescriptor = collectionPropertyDescriptor(propertyName);
			uppercasePropertyName = collectionDescriptor.getCollectionPropertyName();
		}
		uppercasePropertyName = uppercaseName(uppercasePropertyName);

		Map<String, Method> methods = ReflectionUtil.collectMethodsImplementedBy(target);
		Method method = methods.get("get" + uppercasePropertyName);
		if (method == null) {
			method = methods.get("is" + uppercasePropertyName);
		}

		Object propertyValue = null;
		if (method != null) {
			boolean ia = method.isAccessible();
			try {
				method.setAccessible(true);
				propertyValue = method.invoke(target);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				// this will be ignored. maybe some runtime expception might make sense here.
			} finally {
				method.setAccessible(ia);
			}
		}

		if (propertyValue != null && collectionDescriptor != null) {
			List list = (List) propertyValue;
			int index = collectionDescriptor.getCollectionIndex();
			if (list.size() <= index) {
				return null;
			}
			return list.get(index);
		}

		return propertyValue;
	}

	@Override
	public Class<?> typeOf(String propertyName, Object target, TypeHint... typeHints) {
		Class<?> targetType = target.getClass();

		String uppercasePropertyName = propertyName;
		CollectionProperty collectionDescriptor = null;
		boolean isCollection = false;
		if (propertyNameRefersToElementInCollection(propertyName)) {
			isCollection = true;
			collectionDescriptor = collectionPropertyDescriptor(propertyName);
			uppercasePropertyName = collectionDescriptor.getCollectionPropertyName();
		}
		for (TypeHint typeHint : typeHints) {
			if (typeHint.getPath().isEmpty() && ((isCollection && typeHint.isCollection()) || (!isCollection && !typeHint.isCollection()))) {
				return typeHint.getType();
			}
		}

		uppercasePropertyName = uppercaseName(uppercasePropertyName);

		Map<String, Method> methods = ReflectionUtil.collectMethodsImplementedBy(target);
		Method method = methods.get("get" + uppercasePropertyName);
		if (method == null) {
			method = methods.get("is" + uppercasePropertyName);
		}

		if (method != null) {
			if (collectionDescriptor != null) {
				Class<?> listItemType = ReflectionUtil.getCollectionParameterType(method.getGenericReturnType());
				if (listItemType == null) {
					throw new UnresolvablePropertyException(propertyName, targetType, "could not resolve list item type of " + propertyName + " in " + targetType.getSimpleName());
				}
				return listItemType;
			} else {
				return method.getReturnType();
			}
		} else {
			throw new UnresolvablePropertyException(
					propertyName, targetType, "could not resolve type of " + propertyName + " in " + targetType.getSimpleName() + " because no getter method was found."
			);
		}

	}

	private String uppercaseName(String uppercasePropertyName) {
		if (uppercasePropertyName.length() > 1) {
			uppercasePropertyName = uppercasePropertyName.substring(0, 1).toUpperCase() + uppercasePropertyName.substring(1);
		} else {
			uppercasePropertyName = uppercasePropertyName.toUpperCase();
		}
		return uppercasePropertyName;
	}
}
