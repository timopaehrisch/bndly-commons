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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CompiledMethodBeanPropertyAccessorWriter implements BeanPropertyAccessor, BeanPropertyWriter {
	
	private final Class inspectedType;
	private final Map<String, CompiledGetter> gettersByName;
	private final Map<String, CompiledSetter> settersByName;
	
	private static final String SETTER_PREFIX = "set";
	private static final String GETTER_PREFIX = "get";
	private static final String BOOLEAN_GETTER_PREFIX = "is";

	private static interface CompiledMethod {
		Method getMethod();
		Class getCollectionParameterType();
	}
	private static interface CompiledGetter extends CompiledMethod {
		Object get(Object target, TypeHint... typeHints);
	}
	private static interface CompiledSetter extends CompiledMethod {
		boolean set(Object value, Object target, TypeHint... typeHints);
	}
	
	public CompiledMethodBeanPropertyAccessorWriter(Class inspectedType) {
		this.inspectedType = inspectedType;
		Map<String, CompiledGetter> getters = new HashMap<>();
		Map<String, CompiledSetter> setters = new HashMap<>();
		inspectType(inspectedType, getters, setters, new HashSet<Class>());
		gettersByName = Collections.unmodifiableMap(getters);
		settersByName = Collections.unmodifiableMap(setters);
	}
	
	public Set<String> getSupportedReadablePropertyNames() {
		return gettersByName.keySet();
	}
	
	public Set<String> getSupportedWritablePropertyNames() {
		return settersByName.keySet();
	}

	private void inspectType(Class inspectedType, Map<String, CompiledGetter> getters, Map<String, CompiledSetter> setters, Set<Class> inspected) {
		if (inspectedType == null || Object.class.equals(inspectedType)) {
			return;
		}
		if (inspected.contains(inspectedType)) {
			// break the recursion
			return;
		}
		inspected.add(inspectedType);
		if (inspectedType.isInterface()) {
			Class[] interfaces = inspectedType.getInterfaces();
			for (Class aInterface : interfaces) {
				inspectType(aInterface, getters, setters, inspected);
			}
		}
		inspectType(inspectedType.getSuperclass(), getters, setters, inspected);
		Method[] methods = inspectedType.getDeclaredMethods();
		for (Method method : methods) {
			String name = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			Class<?> returnType = method.getReturnType();
			if (parameterTypes.length == 0) {
				if (name.startsWith(GETTER_PREFIX)) {
					String propertyName = lowercaseName(name.substring(GETTER_PREFIX.length()));
					CompiledGetter compiledGetter = compileGetter(method);
					getters.put(propertyName, compiledGetter);
				} else if (name.startsWith(BOOLEAN_GETTER_PREFIX) && (Boolean.class.equals(returnType) || boolean.class.equals(returnType))) {
					String propertyName = lowercaseName(name.substring(BOOLEAN_GETTER_PREFIX.length()));
					CompiledGetter compiledGetter = compileGetter(method);
					getters.put(propertyName, compiledGetter);
				}
			} else if (parameterTypes.length == 1) {
				if (name.startsWith(SETTER_PREFIX)) {
					String propertyName = lowercaseName(name.substring(SETTER_PREFIX.length()));
					CompiledSetter compiledSetter = compileSetter(method);
					setters.put(propertyName, compiledSetter);
				}
			}
		}
	}
	
	private CompiledSetter compileSetter(final Method method) {
		final Class<?> collectionParameterType = ReflectionUtil.getCollectionParameterType(method.getGenericParameterTypes()[0]);
		return new CompiledSetter() {
			@Override
			public boolean set(Object value, Object target, TypeHint... typeHints) {
				boolean ia = method.isAccessible();
				try {
					method.setAccessible(true);
					method.invoke(target, value);
					return true;
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					// this will be ignored. maybe some runtime expception might make sense here.
					return false;
				} finally {
					method.setAccessible(ia);
				}
			}

			@Override
			public Method getMethod() {
				return method;
			}

			@Override
			public Class getCollectionParameterType() {
				return collectionParameterType;
			}
		};
	}
	
	private CompiledGetter compileGetter(final Method method) {
		final Class<?> collectionParameterType = ReflectionUtil.getCollectionParameterType(method.getGenericReturnType());
		return new CompiledGetter() {
			@Override
			public Object get(Object target, TypeHint... typeHints) {
				boolean ia = method.isAccessible();
				try {
					method.setAccessible(true);
					return method.invoke(target);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					// this will be ignored. maybe some runtime expception might make sense here.
					return null;
				} finally {
					method.setAccessible(ia);
				}
			}

			@Override
			public Method getMethod() {
				return method;
			}

			@Override
			public Class getCollectionParameterType() {
				return collectionParameterType;
			}
		};
	}
	
	@Override
	public Object get(String propertyName, Object target, TypeHint... typeHints) {
		if (target == null) {
			throw new IllegalArgumentException("target can not be null");
		}
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyName = collectionDescriptor.getCollectionPropertyName();
		}

		CompiledGetter getter = gettersByName.get(propertyName);
		if (getter == null) {
			return null;
		}
		Object propertyValue = getter.get(target, typeHints);

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
	public boolean set(String propertyName, Object value, Object target, TypeHint... typeHints) {
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			propertyName = collectionDescriptor.getCollectionPropertyName();
		}

		// i assume that there are no convenience setter methods
		CompiledSetter setter = settersByName.get(propertyName);
		if (setter == null) {
			return false;
		}
		if (collectionDescriptor != null) {
			int index = collectionDescriptor.getCollectionIndex();
			CompiledGetter getter = gettersByName.get(propertyName);
			if (getter == null) {
				// to set a nested value in list, we have to be able to get the list first
				return false;
			}
			List orig = (List) getter.get(target, typeHints);
			List l = increaseListSizeIfNeeded(orig, index);
			if (orig != l) {
				return setter.set(l, target, typeHints);
			}
			l.set(index, value);
			return true;
		} else {
			return setter.set(value, target, typeHints);
		}
	}

	@Override
	public Class<?> typeOf(String propertyName, Object target, TypeHint... typeHints) {
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		boolean isCollection = collectionDescriptor != null;
		if (isCollection) {
			collectionDescriptor = collectionPropertyDescriptor(propertyName);
			propertyName = collectionDescriptor.getCollectionPropertyName();
		}
		for (TypeHint typeHint : typeHints) {
			if (typeHint.getPath().isEmpty() && ((isCollection && typeHint.isCollection()) || (!isCollection && !typeHint.isCollection()))) {
				return typeHint.getType();
			}
		}
		CompiledGetter getter = gettersByName.get(propertyName);
		if (getter == null) {
			throw new UnresolvablePropertyException(
					propertyName, inspectedType, "could not resolve type of " + propertyName + " in " + inspectedType.getSimpleName() + " because no getter method was found."
			);
		}

		if (collectionDescriptor != null) {
			Class<?> listItemType = getter.getCollectionParameterType();
			if (listItemType == null) {
				throw new UnresolvablePropertyException(propertyName, inspectedType, "could not resolve list item type of " + propertyName + " in " + inspectedType.getSimpleName());
			}
			return listItemType;
		} else {
			return getter.getMethod().getReturnType();
		}
	}
	
	private String lowercaseName(String propertyName) {
		if (propertyName.length() > 1) {
			propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
		} else {
			propertyName = propertyName.toLowerCase();
		}
		return propertyName;
	}
	
}
