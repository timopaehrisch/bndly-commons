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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class SetterBeanPropertyWriter extends AbstractBeanPropertyAccessorWriter implements BeanPropertyWriter {

	@Override
	public boolean set(String propertyName, Object value, Object target, TypeHint... typeHints) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = null;
		if (propertyNameRefersToElementInCollection(propertyName)) {
			collectionDescriptor = collectionPropertyDescriptor(propertyName);
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}

		// i assume that there are no convenience setter methods
		Map<String, Method> methods = ReflectionUtil.collectMethodsImplementedBy(target);
		propertyNameWithoutIndex = propertyNameWithoutIndex.substring(0, 1).toUpperCase() + propertyNameWithoutIndex.substring(1);
		Method setterMethod = methods.get("set" + propertyNameWithoutIndex);
		if (setterMethod != null) {
			if (collectionDescriptor != null) {
				int index = collectionDescriptor.getCollectionIndex();
				List orig = (List) new GetterBeanPropertyAccessor().get(propertyNameWithoutIndex, target, typeHints);
				List l = increaseListSizeIfNeeded(orig, index);
				if (orig != l) {
					try {
						setterMethod.invoke(target, l);
					} catch (Exception ex) {
						return false;
					}
				}
				l.set(index, value);
				return true;
			} else {
				try {
					setterMethod.invoke(target, value);
					return true;
				} catch (Exception ex) {
					return false;
				}
			}
		}
		return false;
	}

}
