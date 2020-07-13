package org.bndly.common.graph;

/*-
 * #%L
 * Graph Impl
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

import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.graph.api.GraphAggregationType;
import org.bndly.common.graph.api.GraphComplexType;
import org.bndly.common.graph.api.GraphIgnoredType;
import org.bndly.common.graph.api.GraphReferenceType;
import org.bndly.common.graph.api.ReferenceBuilder;
import org.bndly.common.graph.api.ReferenceDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanGraphBuilder {

	private BeanPool beanPool;

	public static <T> T rebuildCycles(T source) {
		return maximize(source);
	}

	public static <T> T breakCycles(T source) {
		return breakCycles(source, false);
	}

	public static <T> T breakCycles(T source, boolean minimized) {
		if (minimized) {
			return minimize(source);
		} else {
			BeanGraphBuilder graphBuilder = new BeanGraphBuilder();
			T target = null;
			target = graphBuilder.breakCycles(source, target, new Stack<>());
			return target;
		}
	}

	private static <T> T minimize(T source) {
		BeanGraphBuilder graphBuilder = new BeanGraphBuilder();
		return (T) graphBuilder.minimize(source, new Stack<>());
	}

	private static <T> T maximize(T source) {
		BeanGraphBuilder graphBuilder = new BeanGraphBuilder();
		Map<Object, List<ValueFieldObjectBinding>> referencesToReplaceAfterwards = new HashMap<>();
		Map<Object, Object> entities = new HashMap<>();
		T result = graphBuilder.maximize(source, null, null, entities, referencesToReplaceAfterwards);
		Set<Object> refs = referencesToReplaceAfterwards.keySet();
		for (Object ref : refs) {
			Object full = entities.get(ref);
			if (full != null) {
				List<ValueFieldObjectBinding> toReplace = referencesToReplaceAfterwards.get(ref);
				if (toReplace != null) {
					for (ValueFieldObjectBinding valueFieldObjectBinding : toReplace) {
						Field field = valueFieldObjectBinding.getField();
						Object value = full;
						Object target = valueFieldObjectBinding.getObject();
						Object fieldValue = ReflectionUtil.getFieldValue(field, target);
						if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
							Collection collection = (Collection) fieldValue;
							collection.remove(valueFieldObjectBinding.getValue());
							collection.add(value);
						} else {
							ReflectionUtil.setFieldValue(field, value, target);
						}
					}
				}
			}
		}
		return result;
	}

	private <T extends Object> T maximize(T source, Field f, Object owner, Map<Object, Object> parentObjects, Map<Object, List<ValueFieldObjectBinding>> referencesToReplaceAfterwards) {
		if (source == null) {
			return source;
		}
		boolean sourceIsAggregation = isAggregationObject(source);
		if (isComplexObject(source) || sourceIsAggregation) {
			Object fullObject = parentObjects.get(source);
			// there might be confusion when a reference is visited before the full object
			if (fullObject != null) {
				return (T) fullObject;
			}
			if (fullObject == null) {
				if (!sourceIsAggregation && isReference(source)) {
					List<ValueFieldObjectBinding> others = referencesToReplaceAfterwards.get(source);
					if (others == null) {
						others = new ArrayList<>();
						referencesToReplaceAfterwards.put(source, others);
					}
					ValueFieldObjectBinding toReplace = new ValueFieldObjectBinding(fullObject, f, owner);
					others.add(toReplace);

				} else {
					parentObjects.put(source, source);
					List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(source.getClass());
					if (fields != null) {
						for (Field field : fields) {
							Object value = ReflectionUtil.getFieldValue(field, source);
							if (value != null) {
								if (isComplexObject(value) || isAggregationObject(value)) {
									Object maximizedValue = maximize(value, field, source, parentObjects, referencesToReplaceAfterwards);
									if (maximizedValue != value) {
										ReflectionUtil.setFieldValue(field, maximizedValue, source);
									}
								} else if (Collection.class.isAssignableFrom(value.getClass())) {
									Class<?> listedObjectsType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
									if (isComplexObjectType(listedObjectsType)) {
										Collection c = (Collection) value;
										Collection maximizedCollection = null;
										try {
											maximizedCollection = c.getClass().newInstance();
										} catch (Exception ex) {
											throw new IllegalStateException("could not create a new instance of " + c.getClass().getName(), ex);
										}
										for (Object object : c) {
											Object maximizedObject = maximize(object, field, source, parentObjects, referencesToReplaceAfterwards);
											maximizedCollection.add(maximizedObject);
										}
										ReflectionUtil.setFieldValue(field, maximizedCollection, source);
									}
								}
							}
						}
					}
				}
			}
		}
		return source;
	}

	private <T> Object minimize(T source, Stack<Object> parentObjects) {
		if (source != null && isComplexObject(source)) {
			if (!parentObjects.contains(source)) {
				parentObjects.add(source);
				List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(source.getClass());
				if (fields != null) {
					for (Field field : fields) {
						Object value = ReflectionUtil.getFieldValue(field, source);
						if (value != null) {
							if (isComplexObject(value)) {
								Object minimizedValue = minimize(value, parentObjects);
								if (minimizedValue != value) {
									ReflectionUtil.setFieldValue(field, minimizedValue, source);
								}
							} else if (Collection.class.isAssignableFrom(value.getClass())) {
								Class<?> listedObjectsType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
								if (isComplexObjectType(listedObjectsType)) {
									// a collection of entities
									Collection c = (Collection) value;
									Collection minimizedCollection = null;
									try {
										minimizedCollection = c.getClass().newInstance();
									} catch (Exception ex) {
										throw new IllegalStateException("could not create a new instance of " + c.getClass().getName(), ex);
									}
									for (Object object : c) {
										Object minimizedObject = minimize(object, parentObjects);
										minimizedCollection.add(minimizedObject);
									}
									ReflectionUtil.setFieldValue(field, minimizedCollection, source);
								}
							} else {
								if (!(ReflectionUtil.fieldIsFinal(field) || ReflectionUtil.fieldIsStatic(field))) {
									ReflectionUtil.setFieldValue(field, value, source);
								}
							}
						}
					}
				}
				parentObjects.pop();
			} else {
				Object ref = buildReference(source);
				return ref;
			}

		}
		return source;
	}

	private void collectObjectsFromSource(Object source) {
		if (source != null) {
			addToBeanPool(source);
		}
	}

	private <T extends Object> T breakCycles(T source, T target, Stack<Object> parentObjects) {
		if (source != null) {
			if (isComplexObject(source) || isAggregationObject(source)) {
				parentObjects.add(source);
				try {
					target = (T) source.getClass().newInstance();
				} catch (Exception ex) {
					throw new IllegalStateException("could not break cycles of " + source.getClass().getName(), ex);
				}

				List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(source.getClass());
				if (fields != null) {
					for (Field field : fields) {
						Object value = ReflectionUtil.getFieldValue(field, source);
						if (value != null) {
							boolean isComplex = isComplexObject(value);
							boolean isAggregation = isAggregationObject(value);
							if (isComplex || isAggregation) {
								if (parentObjects.contains(value) && isComplex) {
									Object ref = buildReference(value);
									ReflectionUtil.setFieldValue(field, ref, target);
								} else {
									Object brokenValue = null;
									brokenValue = breakCycles(value, brokenValue, parentObjects);
									ReflectionUtil.setFieldValue(field, brokenValue, target);
								}
							} else if (Collection.class.isAssignableFrom(value.getClass())) {
								Class<?> listedObjectsType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
								if (isComplexObjectType(listedObjectsType)) {
									// a collection of entities
									Collection c = (Collection) value;
									Collection brokenCollection = null;
									try {
										brokenCollection = c.getClass().newInstance();
									} catch (Exception ex) {
										throw new IllegalStateException("could not create a new instance of " + c.getClass().getName(), ex);
									}
									for (Object object : c) {
										Object brokenObject = null;
										brokenObject = breakCycles(object, brokenObject, parentObjects);
										brokenCollection.add(brokenObject);
									}
									ReflectionUtil.setFieldValue(field, brokenCollection, target);
								} else {
									ReflectionUtil.setFieldValue(field, value, target);
								}
							} else {
								if (!(ReflectionUtil.fieldIsFinal(field) || ReflectionUtil.fieldIsStatic(field))) {
									ReflectionUtil.setFieldValue(field, value, target);
								}
							}
						}
					}
				}
				parentObjects.pop();
			} else {
				return source;
			}
		}
		return target;
	}

	private boolean isAggregationObject(Object source) {
		Class<? extends Object> type = source.getClass();
		return isAggregationObjectType(type);
	}

	private boolean isAggregationObjectType(Class<?> type) {
		return isAnnotationPresent(GraphAggregationType.class, type);
	}

	private boolean isComplexObject(Object source) {
		Class<? extends Object> type = source.getClass();
		return isComplexObjectType(type);
	}

	private boolean isComplexObjectType(Class<?> type) {
		return isAnnotationPresent(GraphComplexType.class, type);
	}

	private boolean isIgnoredComplexObject(Object source) {
		Class<? extends Object> type = source.getClass();
		return isIgnoredComplexObjectType(type);
	}

	private boolean isIgnoredComplexObjectType(Class<?> type) {
		return isAnnotationPresent(GraphIgnoredType.class, type);
	}

	private boolean isReference(Object source) {
		Class<? extends Object> type = source.getClass();
		if (type.isAnnotationPresent(GraphReferenceType.class)) {
			return true;
		} else {
			GraphComplexType annotation = getAnnotation(GraphComplexType.class, type);
			ReferenceDetector refDetector = InstantiationUtil.instantiateType(annotation.referenceDetector());
			return refDetector.isReference(source);
		}
	}

	private boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Class<?> objectType) {
		return getAnnotation(annotationType, objectType) != null;
	}

	private <E extends Annotation> E getAnnotation(Class<E> annotationType, Class<?> objectType) {
		if (objectType.isAnnotationPresent(annotationType)) {
			return objectType.getAnnotation(annotationType);
		} else {
			Class<?>[] interfaces = objectType.getInterfaces();
			if (interfaces != null) {
				for (Class<?> interfaceType : interfaces) {
					E a = getAnnotation(annotationType, interfaceType);
					if (a != null) {
						return a;
					}
				}
			}

			Class<?> superType = objectType.getSuperclass();
			if (superType != null) {
				E a = getAnnotation(annotationType, superType);
				if (a != null) {
					return a;
				}
			}
		}
		return null;
	}

	private Object buildReference(Object value) {
		ReferenceBuilder referenceBuilder;
		if (ReferenceBuilder.class.isAssignableFrom(value.getClass())) {
			referenceBuilder = ReferenceBuilder.class.cast(value);
		} else {
			GraphComplexType annotation = getAnnotation(GraphComplexType.class, value.getClass());
			referenceBuilder = InstantiationUtil.instantiateType(annotation.referenceBuilder());
		}
		Object ref = referenceBuilder.ref(value);
		return ref;
	}

	private void addToBeanPool(Object source) {
		if (!isIgnoredComplexObject(source)) {
			beanPool.add(source);

			// look for other beans in the object tree and add them to the pool
			List<Field> fields = ReflectionUtil.getFieldsOfAssignableType(Object.class, source);
			if (fields != null) {
				for (Field field : fields) {
					Object value = ReflectionUtil.getFieldValue(field, source);
					if (value != null && isComplexObject(value)) {
						collectObjectsFromSource(value);
					}
				}
			}
			List<Field> collectionFields = ReflectionUtil.getCollectionFieldsOfAssignableType(Object.class, source);
			if (collectionFields != null) {
				for (Field field : collectionFields) {
					Class<?> collectionItemType = ReflectionUtil.getCollectionParameterType(field.getType());
					if (isComplexObjectType(collectionItemType)) {
						Collection collection = (Collection) ReflectionUtil.getFieldValue(field, source);
						if (collection != null) {
							for (Object item : collection) {
								collectObjectsFromSource(item);
							}
						}
					}
				}
			}
		}
	}

}
