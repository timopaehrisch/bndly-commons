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

import java.lang.reflect.Field;
import java.util.List;

public class FieldBeanPropertyWriter extends AbstractBeanPropertyAccessorWriter implements BeanPropertyWriter {

	@Override
	public boolean set(String propertyName, Object value, Object target, TypeHint... typeHints) {
		CollectionProperty collectionDescriptor = null;
		if (propertyNameRefersToElementInCollection(propertyName)) {
			collectionDescriptor = collectionPropertyDescriptor(propertyName);
		}

		String fieldName = propertyName;
		if (collectionDescriptor != null) {
			fieldName = collectionDescriptor.getCollectionPropertyName();
		}
		Field field = ReflectionUtil.getFieldByName(fieldName, target.getClass());
		if (field == null) {
			return false;
		}
		if (collectionDescriptor != null) {
			List collection = (List) ReflectionUtil.getFieldValue(field, target);
			int index = collectionDescriptor.getCollectionIndex();
			collection = increaseListSizeIfNeeded(collection, index);
			collection.set(index, value);

			ReflectionUtil.setFieldValue(field, collection, target);
			return ((List) ReflectionUtil.getFieldValue(field, target)).get(index) == value;
		} else {
			ReflectionUtil.setFieldValue(field, value, target);
			// if the instances are the same...
			return ReflectionUtil.getFieldValue(field, target) == value;
		}

	}

}
