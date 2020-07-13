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

import static org.bndly.common.reflection.AbstractBeanPropertyAccessorWriter.collectionPropertyDescriptor;
import static org.bndly.common.reflection.AbstractBeanPropertyAccessorWriter.increaseListSizeIfNeeded;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CompiledFieldBeanPropertyAccessorWriter implements BeanPropertyAccessor, BeanPropertyWriter {
	
	private final Class inspectedType;
	private final Map<String, CompiledField> fieldsByName;
	
	public static interface CompiledField extends CompiledMetaDataAccessor {
		Field getField();
		boolean isStatic();
		boolean isFinal();
		Class getCollectionParameterType();
		Object get(Object target, TypeHint... typeHints);
		boolean set(Object value, Object target, TypeHint... typeHints);
	}

	public CompiledFieldBeanPropertyAccessorWriter(Class inspectedType) {
		this.inspectedType = inspectedType;
		Map<String, CompiledField> tmp = new HashMap<>();
		inspectType(inspectedType, tmp);
		fieldsByName = Collections.unmodifiableMap(tmp);
	}
	
	public Set<String> getSupportedPropertyNames() {
		return fieldsByName.keySet();
	}
	
	public CompiledField getCompiledField(String propertyName) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyNameWithoutIndex);
		return compiledField;
	}
	
	public boolean isStatic(String propertyName) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyNameWithoutIndex);
		if (compiledField == null) {
			return false;
		}
		return compiledField.isStatic();
	}
	
	public boolean isFinal(String propertyName) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyNameWithoutIndex);
		if (compiledField == null) {
			return false;
		}
		return compiledField.isFinal();
	}
	
	private void inspectType(Class inspectedType, Map<String, CompiledField> fieldsByName) {
		if (inspectedType == null || Object.class.equals(inspectedType)) {
			return;
		}
		inspectType(inspectedType.getSuperclass(), fieldsByName);
		Field[] fields = inspectedType.getDeclaredFields();
		for (Field field : fields) {
			CompiledField compiledField = compileField(field);
			fieldsByName.put(field.getName(), compiledField);
		}
	}
	
	private CompiledField compileField(final Field field) {
		final Class<?> collectionParameterType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
		final boolean fieldIsFinal = ReflectionUtil.fieldIsFinal(field);
		final boolean fieldIsStatic = ReflectionUtil.fieldIsStatic(field);
		return new CompiledField() {
			@Override
			public Field getField() {
				return field;
			}

			@Override
			public boolean isFinal() {
				return fieldIsFinal;
			}

			@Override
			public boolean isStatic() {
				return fieldIsStatic;
			}
			
			@Override
			public Class getCollectionParameterType() {
				return collectionParameterType;
			}

			@Override
			public Object get(Object target, TypeHint... typeHints) {
				return ReflectionUtil.getFieldValue(field, target);
			}

			@Override
			public boolean set(Object value, Object target, TypeHint... typeHints) {
				return ReflectionUtil.setFieldValue(field, value, target);
			}

			@Override
			public <A extends Annotation> A getMetaData(Class<A> annotation) {
				return field.getAnnotation(annotation);
			}
			
		};
	}
	
	public <A extends Annotation> A getPropertyMetaData(String propertyName, Class<A> annotation) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyNameWithoutIndex);
		if (compiledField == null) {
			return null;
		}
		return compiledField.getMetaData(annotation);
	}
	
	@Override
	public Object get(String propertyName, Object target, TypeHint... typeHints) {
		String propertyNameWithoutIndex = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyNameWithoutIndex = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyNameWithoutIndex);
		if (compiledField == null) {
			return null;
		}
		Object value = compiledField.get(target, typeHints);
		if (value != null && collectionDescriptor != null) {
			int index = collectionDescriptor.getCollectionIndex();
			List list = (List) value;
			if (list.size() <= index) {
				return null;
			}
			return list.get(index);
		}
		return value;
	}
	
	@Override
	public boolean set(String propertyName, Object value, Object target, TypeHint... typeHints) {
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyName = collectionDescriptor.getCollectionPropertyName();
		}
		CompiledField compiledField = fieldsByName.get(propertyName);
		if (compiledField == null) {
			return false;
		}
		
		if (collectionDescriptor != null) {
			List collection = (List) compiledField.get(target, typeHints);
			int index = collectionDescriptor.getCollectionIndex();
			collection = increaseListSizeIfNeeded(collection, index);
			collection.set(index, value);

			return compiledField.set(collection, target, typeHints);
		} else {
			return compiledField.set(value, target, typeHints);
		}
	}

	@Override
	public Class<?> typeOf(String propertyName, Object target, TypeHint... typeHints) {
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyName = collectionDescriptor.getCollectionPropertyName();
		}

		CompiledField compiledField = fieldsByName.get(propertyName);
		if (compiledField == null) {
			throw new UnresolvablePropertyException(propertyName, inspectedType, "could not resolve property " + propertyName + " in type " + inspectedType.getSimpleName());
		}
		if (collectionDescriptor != null) {
			return compiledField.getCollectionParameterType();
		} else {
			Field field = compiledField.getField();
			return field.getType();
		}
	}

	
}
