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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class to perform various common actions when dealing with the Java Reflection API
 */
public final class ReflectionUtil {

	/**
	 * compares two objects by its types and returns a boolean value that indicates whether the first object's type is more specific (a subclass) than the second object's type
	 *
	 * @param object the first object
	 * @param otherObject the second object
	 * @return true if the first object's type is a subclass of the second object's type. false if the types are equal or the first object's type is a super class of the second object's type
	 */
	public static boolean isObjectMoreSpecificThanOther(Object object, Object otherObject) {
		Class<? extends Object> oClass = object.getClass();
		Class<? extends Object> otherClass = otherObject.getClass();
		if (oClass.equals(otherClass)) {
			return false;
		} else {
			if (oClass.isAssignableFrom(otherClass)) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Sets the value of a field via reflection. the implementation does not check, if the value's type is assignable to the provided field.
	 *
	 * @param field the field that shall be accessed
	 * @param value the value that shall be written into the field
	 * @param target the object that holds the provided field
	 * @return true, if the value could be set in the field. false, if the value could not be set.
	 */
	public static boolean setFieldValue(Field field, Object value, Object target) {
		boolean tmp = field.isAccessible();
		field.setAccessible(true);
		try {
			field.set(target, value);
			return true;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// ignore issues during field access
			return false;
		} finally {
			field.setAccessible(tmp);
		}
	}

	public static Class<?> getCollectionParameterType(Type genericType) {
		if (!ParameterizedType.class.isInstance(genericType)) {
			return null;
		}
		ParameterizedType collectionType = (ParameterizedType) genericType;
		Type genericTypeParameter = collectionType.getActualTypeArguments()[0];
		try {
			return (Class<?>) genericTypeParameter;
		} catch (ClassCastException e) {
			try {
				ParameterizedType gp = (ParameterizedType) genericTypeParameter;
				return (Class<?>) gp.getRawType();
			} catch (ClassCastException ex) {
				TypeVariable tv = (TypeVariable) genericTypeParameter;
				Type[] bounds = tv.getBounds();
				return (Class<?>) bounds[0];
			}
		}
	}

	private ReflectionUtil() {
	}

	/**
	 * Iterates through the type of the provided bean and collects all fields, that are annotated with the provided annotation type.
	 *
	 * @param <A> the annotation type that is searched
	 * @param annotationType the annotation type that has to be present on the fields of the bean's type
	 * @param bean the bean that is inspected
	 * @return a list of fields that are annotated with the provided annotationType or an empty list, if no fields have an annotation of the provided annotationType.
	 */
	public static <A extends Annotation> List<Field> getFieldsWithAnnotation(Class<A> annotationType, Object bean) {
		Class<?> clazz = bean.getClass();
		return getFieldsWithAnnotation(annotationType, clazz);
	}

	public static <A extends Annotation> List<Field> getFieldsWithAnnotation(Class<A> annotationType, Class<?> clazz) {
		List<Field> result = new ArrayList<>();
		List<Field> fields = new ArrayList<>();
		collectAllFields(clazz, fields);
		for (Field field : fields) {
			A annotation = field.getAnnotation(annotationType);
			if (annotation != null) {
				result.add(field);
			}
		}
		return result;
	}

	/**
	 * works like {@link ReflectionUtil#getFieldsWithAnnotation(java.lang.Class, java.lang.Object)}, but instead of returning a list, a single field is returned.
	 *
	 * @param <A> the annotation type that is searched
	 * @param annotationType the annotation type that has to be present on the fields of the bean's type
	 * @param bean the bean that is inspected
	 * @return the field that is annotated with the provided annotationType or null, if no or more than one field has an annotation of the provided annotationType
	 * @see ReflectionUtil#getFieldsWithAnnotation(java.lang.Class, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> Field getFieldByAnnotation(Class<A> annotationType, Object bean) {
		if (bean == null) {
			return null;
		}
		return getFieldByAnnotation(annotationType, bean.getClass());
	}

	public static <A extends Annotation> Field getFieldByAnnotation(Class<A> annotationType, Class<?> beanType) {
		List<Field> fields = getFieldsWithAnnotation(annotationType, beanType);
		if (fields != null && fields.size() > 0) {
			if (fields.size() == 1) {
				return fields.get(0);
			} else {
				throw new IllegalStateException("multiple fields in " + beanType.getSimpleName() + " are annotated with " + annotationType.getSimpleName());
			}
		}
		return null;
	}

	/**
	 * Extracts the value of a named field from a provided bean and casts the value to the desired type.
	 *
	 * @param <T> the type that the value should be cast to
	 * @param fieldName the name of the field that holds the value to extract
	 * @param bean the object that holds the field and value
	 * @param desiredType the type class that will be used for the cast.
	 * @return the field's value cast to the desired type or null, no field with the provided name AND the desired type can be found in the bean
	 */
	public static <T> T getFieldValueByFieldName(String fieldName, Object bean, Class<T> desiredType) {
		List<Field> fields = getFieldsOfAssignableType(desiredType, bean);
		if (fields != null) {
			for (Field field : fields) {
				if (field.getName().equals(fieldName)) {
					return (T) getFieldValue(field, bean);
				}
			}
		}
		return null;
	}

	/**
	 * Looks for a field with the provided name in a class and all it's super classes.
	 *
	 * @param fieldName the name of the field
	 * @param type the class that is inspected
	 * @return the found field or null, if no field with the given name can be found.
	 */
	public static Field getFieldByName(String fieldName, Class<?> type) {
		Field field = null;
		try {
			field = type.getDeclaredField(fieldName);
		} catch (Exception ex) {
			if (type.getSuperclass() != null) {
				return getFieldByName(fieldName, type.getSuperclass());
			}
		}
		return field;
	}

	/**
	 * Looks for a field that is annotated with the provided annotation type and casts the field's value to the expected type T
	 *
	 * @param <T> the expected type
	 * @param <A> the type of the annotation that is searched on the fields of the bean
	 * @param annotationType the annotation type class object
	 * @param bean the inspected object
	 * @return the field value of the first found field that has the provided annotation or null, if no field is annotated with the provided annotationType
	 * @see ReflectionUtil#setFieldValueByFieldAnnotation(java.lang.Class, java.lang.Object, java.lang.Object)
	 * @see ReflectionUtil#getFieldValue(java.lang.reflect.Field, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T, A extends Annotation> T getFieldValueByFieldAnnotation(Class<A> annotationType, Object bean) {
		List<Field> fields = getFieldsWithAnnotation(annotationType, bean);
		if (fields != null) {
			for (Field field : fields) {
				return (T) getFieldValue(field, bean);
			}
		}
		return null;
	}

	/**
	 * Sets the value of a field that is annotated with an annotation of the provided annotation type.
	 *
	 * @param <A> the annotation type
	 * @param annotationType the annotation type class object is searched for
	 * @param bean the object that holds the fields that will be inspected
	 * @param value the value that will be written to the field in the provided bean
	 * @see ReflectionUtil#getFieldValueByFieldAnnotation(java.lang.Class, java.lang.Object)
	 * @see ReflectionUtil#setFieldValue(java.lang.reflect.Field, java.lang.Object, java.lang.Object)
	 */
	public static <A extends Annotation> void setFieldValueByFieldAnnotation(Class<A> annotationType, Object bean, Object value) {
		List<Field> fields = getFieldsWithAnnotation(annotationType, bean);
		if (fields != null) {
			for (Field field : fields) {
				setFieldValue(field, value, bean);
			}
		}
	}

	/**
	 * Collects all field of the provided type and adds them to the provided fields list. If the provided has a super class, this method is invoked recursively.
	 *
	 * @param type the inspected type
	 * @param fields the list of fields that have already been found
	 */
	private static void collectAllFields(Class<?> type, List<Field> fields) {
		Field[] tmp = type.getDeclaredFields();
		if (tmp != null) {
			for (Field field : tmp) {
				fields.add(field);
			}
		}
		Class<?> sType = type.getSuperclass();
		if (sType != null) {
			collectAllFields(sType, fields);
		}
	}

	/**
	 * Inspects the type of the provided bean and collects all fields that can contain values of the provided assignable type
	 *
	 * @param <T> the assignable type
	 * @param assignableType the class object of the assignable type
	 * @param bean the bean that will be inspected for fields
	 * @return a list of fields, that can hold values of the provided assignable type, or an empty list, if no matching fields can be found.
	 * @see ReflectionUtil#collectAllFields(java.lang.Class, java.util.List)
	 */
	public static <T> List<Field> getFieldsOfAssignableType(Class<T> assignableType, Object bean) {
		if (bean == null) {
			return null;
		}
		Class<? extends Object> clazz = bean.getClass();
		return getFieldsOfAssignableType(assignableType, clazz);
	}

	public static <T> List<Field> getFieldsOfAssignableType(Class<T> assignableType, Class<?> clazz) {
		if (clazz == null) {
			return null;
		}
		List<Field> result = new ArrayList<>();
		List<Field> fields = new ArrayList<>();
		collectAllFields(clazz, fields);
		for (Field field : fields) {
			if (assignableType.isAssignableFrom(field.getType())) {
				result.add(field);
			}
		}
		return result;
	}

	/**
	 * Checks if a field that holds a collection sub type can store objects that are subtypes of the provided assignable type. Example: the method returns true, when a field of the type List is
	 * inspected and the assignable type is the class object of Foo or any super class of Foo.
	 *
	 * {@code private List<Foo> exampleField; }
	 *
	 * @param <T> the type that should be assignable to the objects within the collection
	 * @param assignableType the assignable type class object
	 * @param field the field that is inspected
	 * @return true if the objects within the collection can be cast to T
	 */
	public static <T> boolean isCollectionFieldFillableWithObjectsInheritedOfType(Class<T> assignableType, Field field) {
		Class<?> genericTypeParameter = getCollectionParameterType(field.getGenericType());
		if (assignableType.isAssignableFrom(genericTypeParameter)) {
			return true;
		}
		return false;
	}

	/**
	 * Collects all fields of the provided beans type, where the field type is a collection that can hold objects of the provided assignable type
	 *
	 * @param <T> the assignable type of the objects within the collection
	 * @param assignableType the assignable type class object
	 * @param bean the inspected bean
	 * @return a list of fields, that are declared as collections that can hold objects that are sub classed from the provided assignable type
	 */
	public static <T> List<Field> getCollectionFieldsOfAssignableType(Class<T> assignableType, Object bean) {
		Class<? extends Object> clazz = bean.getClass();
		List<Field> result = new ArrayList<>();
		List<Field> fields = new ArrayList<>();
		collectAllFields(clazz, fields);
		for (Field field : fields) {
			Class<?> fieldType = field.getType();
			if (Collection.class.isAssignableFrom(fieldType)) {
				if (isCollectionFieldFillableWithObjectsInheritedOfType(assignableType, field)) {
					result.add(field);
				}
			}
		}
		return result;
	}

	/**
	 * Extracts the value of a field in a provided bean.
	 *
	 * @param field the field that holds the value
	 * @param bean the bean that holds the field
	 * @return the fields value or null, if the field can not be accessed
	 */
	public static Object getFieldValue(Field field, Object bean) {
		boolean tmp = field.isAccessible();
		field.setAccessible(true);
		try {
			Object fieldValue = field.get(bean);
			return fieldValue;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// ignore issues during field access
		} finally {
			field.setAccessible(tmp);
		}
		return null;
	}

	public static <B> List<Field> collectAllFieldsFromClass(Class<B> clazz, List<Field> list) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			list.add(field);
		}
		Class<? super B> superClazz = clazz.getSuperclass();
		if (superClazz != null) {
			collectAllFieldsFromClass(superClazz, list);
		}
		return list;
	}

	/**
	 * Checks if the provided field is declared as static.
	 *
	 * @param field the inspected field
	 * @return true, if the field has been declared as static.
	 */
	public static boolean fieldIsStatic(Field field) {
		return fieldHasModifier(field, Modifier.STATIC);
	}

	/**
	 * Checks if the provided field is declared as final.
	 *
	 * @param field the inspected field
	 * @return true, if the field has been declared as final.
	 */
	public static boolean fieldIsFinal(Field field) {
		return fieldHasModifier(field, Modifier.FINAL);
	}

	/**
	 * Checks if the provided field has a certain modifier
	 *
	 * @param field the inspected field
	 * @param modifier the modifier that exists on the field
	 * @return true, if the field has been declared with the provided modifier.
	 * @see Modifier
	 */
	public static boolean fieldHasModifier(Field field, int modifier) {
		int mods = field.getModifiers();
		return (mods & modifier) == modifier;
	}

	public static <B> List<Field> collectAllFieldsFromClass(Class<B> clazz) {
		List<Field> list = new ArrayList<>();
		collectAllFields(clazz, list);
		return list;
	}

	public static List<Class<?>> collectGenericTypeParametersFromType(Class<?> clazz) {
		return collectGenericTypeParametersFromType(clazz, clazz);
	}

	public static List<Class<?>> collectGenericTypeParametersFromType(Class<?> clazz, Type targetType) {
		List<Class<?>> genericTypeParameters = null;

		ParameterizedType parameterizedType = null;
		try {
			parameterizedType = (ParameterizedType) targetType;
		} catch (ClassCastException e) {
			/*the Type interface has not "isAssignable" method. :( */
			if (Class.class.isAssignableFrom(targetType.getClass())) {
				return collectGenericTypeParametersFromType(clazz, Class.class.cast(targetType).getGenericSuperclass());
			}

		}

		if (parameterizedType == null) {
			return null;
		}
		Type[] typeArguments = parameterizedType.getActualTypeArguments();
		for (Type type : typeArguments) {
			if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
				type = ((ParameterizedType) type).getRawType();
			}
			if (Class.class.isAssignableFrom(type.getClass())) {
				Class<?> typeClass = (Class<?>) type;
				if (genericTypeParameters == null) {
					genericTypeParameters = new ArrayList<>();
				}
				genericTypeParameters.add(typeClass);
			}
		}

		return genericTypeParameters;
	}

	public static Method getMethodByAnnotation(Class<? extends Annotation> aClass, Object root) {
		return getMethodByAnnotation(aClass, root.getClass());
	}

	public static Method getMethodByAnnotation(Class<? extends Annotation> aClass, Class<?> type) {
		Map<String, Method> r = collectAnnotatedMethodsImplementedBy(type, aClass);

		if (r.size() == 1) {
			for (Map.Entry<String, Method> entry : r.entrySet()) {
				Method method = entry.getValue();
				return method;
			}
		}

		return null;
	}

	public static Map<String, Method> collectMethodsImplementedBy(Object target) {
		return collectMethodsImplementedBy(target.getClass());
	}

	public static Map<String, Method> collectMethodsImplementedBy(Class<?> type) {
		if (type == null) {
			return null;
		}
		return collectAnnotatedMethodsImplementedBy(type, null, new HashMap<String, Method>());

	}

	public static List<Method> listAnnotatedMethodsImplementedBy(Class<?> type, Class<? extends Annotation> annotationType) {
		List<Method> list = new ArrayList<>();
		listAnnotatedMethodsImplementedBy(type, annotationType, list);
		return list;
	}

	public static Map<String, Method> collectAnnotatedMethodsImplementedBy(Class<?> type, Class<? extends Annotation> annotationType) {
		if (type == null) {
			return null;
		}
		return collectAnnotatedMethodsImplementedBy(type, annotationType, new HashMap<String, Method>());

	}

	private static List<Method> listAnnotatedMethodsImplementedBy(Class<?> type, Class<? extends Annotation> annotationType, List<Method> found) {
		if (type == null) {
			return null;
		}
		Class<?> superType = type.getSuperclass();
		if (superType != null) {
			listAnnotatedMethodsImplementedBy(superType, annotationType, found);
		}
		Method[] methods = type.getDeclaredMethods();
		for (Method method : methods) {
			if (annotationType == null || method.isAnnotationPresent(annotationType)) {
				found.add(method);
			}
		}

		return found;
	}

	private static Map<String, Method> collectAnnotatedMethodsImplementedBy(Class<?> type, Class<? extends Annotation> annotationType, Map<String, Method> found) {
		if (type == null) {
			return null;
		}
		Class<?> superType = type.getSuperclass();
		if (superType != null) {
			collectAnnotatedMethodsImplementedBy(superType, annotationType, found);
		}
		Method[] methods = type.getDeclaredMethods();
		for (Method method : methods) {
			if (annotationType == null || method.isAnnotationPresent(annotationType)) {
				found.put(method.getName(), method);
			}
		}

		return found;
	}

	public static <E extends Annotation> E searchAnnotation(Class<E> annotationType, Class<?> objectType) {
		if (objectType.isAnnotationPresent(annotationType)) {
			return objectType.getAnnotation(annotationType);
		} else {
			Class<?>[] interfaces = objectType.getInterfaces();
			if (interfaces != null) {
				for (Class<?> interfaceType : interfaces) {
					E a = searchAnnotation(annotationType, interfaceType);
					if (a != null) {
						return a;
					}
				}
			}

			Class<?> superType = objectType.getSuperclass();
			if (superType != null) {
				E a = searchAnnotation(annotationType, superType);
				if (a != null) {
					return a;
				}
			}
		}
		return null;
	}

	private static boolean areMethodParametersMoreSpecific(Method isMoreSpecifc, Method than) {
		Class<?>[] mPT = isMoreSpecifc.getParameterTypes();
		Class<?>[] rPT = than.getParameterTypes();
		Boolean moreSpecific = null;
		for (int i = 0; i < mPT.length; i++) {
			Class<?> mPT1 = mPT[i];
			Class<?> rPT1 = rPT[i];
			boolean isMoreSpecific = !mPT1.isAssignableFrom(rPT1) && rPT1.isAssignableFrom(mPT1);
			if (moreSpecific == null) {
				moreSpecific = isMoreSpecific;
			} else {
				moreSpecific = moreSpecific || isMoreSpecific;
			}
			if (moreSpecific) {
				break;
			}
		}
		if (moreSpecific == null) {
			return false;
		}
		return moreSpecific;
	}

	private static boolean areMethodParametersEqual(Method isMoreSpecifc, Method than) {
		Class<?>[] mPT = isMoreSpecifc.getParameterTypes();
		Class<?>[] rPT = than.getParameterTypes();
		Boolean eq = null;
		for (int i = 0; i < mPT.length; i++) {
			Class<?> mPT1 = mPT[i];
			Class<?> rPT1 = rPT[i];
			boolean isEqual = mPT1.equals(rPT1);
			if (eq == null) {
				eq = isEqual;
			} else {
				eq = eq || isEqual;
			}
			if (eq) {
				break;
			}
		}
		if (eq == null) {
			return false;
		}
		return eq;
	}

	public static Method findMostSpecificInterfaceMethod(Class<?> aClass, Method method) {
		if (aClass.isInterface()) {
            // we don't have to inspect the declaring class.
			// that's what we are trying to avoid here.
			if (!aClass.equals(method.getDeclaringClass())) {
				Method[] methods = aClass.getDeclaredMethods();
				List<Method> tmp = new ArrayList<>();
				for (Method m : methods) {
					if (m.getName().equals(method.getName())) {
						Class<?>[] mParamTypes = m.getParameterTypes();
						Class<?>[] methodParamTypes = method.getParameterTypes();
						if (mParamTypes.length == methodParamTypes.length) {
							boolean parameterTypesAreCompatible = true;
							for (int i = 0; i < mParamTypes.length; i++) {
								Class<?> mPT = mParamTypes[i];
								Class<?> methodPT = methodParamTypes[i];
								if (!methodPT.isAssignableFrom(mPT)) {
									parameterTypesAreCompatible = false;
									break;
								}
							}

							// found a more specific method
							if (parameterTypesAreCompatible) {
								// if there is some interface inheritence
								if (method.getDeclaringClass().isAssignableFrom(m.getDeclaringClass())) {
									tmp.add(m);
								}
							}
						}
					}
				}
				if (!tmp.isEmpty()) {
					if (tmp.size() > 1) {
						// check the generic type parameters
						List<Method> moreSpecific = new ArrayList<>();
						for (Method m : tmp) {
							if (areMethodParametersMoreSpecific(m, method)) {
								moreSpecific.add(m);
							}
						}
						if (!moreSpecific.isEmpty()) {
							if (moreSpecific.size() == 1) {
								return moreSpecific.get(0);
							} else {
								throw new IllegalStateException("could not find unique more specific method");
							}
						}
					} else {
						return tmp.get(0);
					}
				}
			}
		} else {
			Class<?>[] interfaces = aClass.getInterfaces();
			Set<Method> c = new HashSet<>();
			for (Class<?> interfaceType : interfaces) {
				Method tmp = findMostSpecificInterfaceMethod(interfaceType, method);
				if (tmp != method) {
					c.add(tmp);
				}
			}
			if (!c.isEmpty()) {
				if (c.size() > 1) {
					throw new IllegalStateException("could not find unique more specific method");
				} else {
					for (Method result : c) {
						return result;
					}
				}
			}
		}
		return method;
	}

	public static Class getSimpleClassType(Type type) {
		Class simpleType;
		if (ParameterizedType.class.isInstance(type)) {
			simpleType = getSimpleClassType(((ParameterizedType) type).getRawType());
		} else if (Class.class.isInstance(type)) {
			simpleType = (Class) type;
		} else if (GenericArrayType.class.isInstance(type)) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			simpleType = getSimpleClassType(componentType);
		} else if (TypeVariable.class.isInstance(type)) {
			Type[] bounds = ((TypeVariable) type).getBounds();
			if (bounds != null && bounds.length == 1) {
				simpleType = getSimpleClassType(bounds[0]);
			} else {
				throw new IllegalArgumentException("could not retrieve type from type variable");
			}
		} else {
			throw new IllegalArgumentException("unsupported type object");
		}
		return simpleType;
	}
}
