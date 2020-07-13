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

import org.bndly.common.reflection.ReflectionUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeanGraphIterator {

	private final ReferenceDetector referenceDetector;
	private final EntityCollectionDetector entityCollectionDetector;
	private final BeanGraphIteratorListener listener;
	private Object ctx;

	public BeanGraphIterator(ReferenceDetector referenceDetector, EntityCollectionDetector collectionDetector, BeanGraphIteratorListener listener) {
		this(referenceDetector, collectionDetector, listener, null);
	}

	public BeanGraphIterator(ReferenceDetector referenceDetector, EntityCollectionDetector collectionDetector, BeanGraphIteratorListener listener, Object ctx) {
		this.referenceDetector = referenceDetector;
		this.entityCollectionDetector = collectionDetector;
		this.listener = listener;
		this.ctx = ctx;
		if (listener == null) {
			throw new IllegalArgumentException("when creating a " + getClass().getSimpleName() + " the " + BeanGraphIteratorListener.class.getSimpleName() + " can not be null.");
		}
	}

	public void traverse(Object bean) {
		if (bean == null) {
			return;
		}

		Class<?> contextType = listener.getIterationContextType();
		if (contextType != null && ctx == null) {
			try {
				ctx = contextType.newInstance();
			} catch (Exception ex) {
				throw new IllegalStateException("could not instantiate context " + contextType.getSimpleName());
			}
		}

		Collection collection;
		if (Collection.class.isAssignableFrom(bean.getClass())) {
			collection = (Collection) bean;
		} else {
			collection = new ArrayList<Object>();
			collection.add(bean);
		}

		for (Object object : collection) {
			listener.onStart(object, ctx);
			Stack<Object> stack = new Stack<Object>();
			stack.add(object);
			traverse(object, stack);
			listener.onEnd(object, ctx);
		}
	}

	private void traverse(Object bean, Stack<Object> parentObjects) {
		listener.onVisitReference(bean, ctx);
		List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(bean.getClass());
		if (fields != null) {
			for (Field field : fields) {
				Object value = ReflectionUtil.getFieldValue(field, bean);
				if (referenceDetector.isReferencable(value)) {
					if (value != null) {
						listener.beforeVisitReference(value, field, bean, ctx);
						Object possiblyChangedValue = ReflectionUtil.getFieldValue(field, bean);
						if (possiblyChangedValue == value) {
							handleNestedTraverse(value, parentObjects, field, bean);
						} else {
                            // if the value has changed, do not perform a nested traversation,
							// because succeeding listener invocations might be executed on an old graph
						}
						listener.afterVisitReference(value, field, bean, ctx);
					}
				} else if (Collection.class.isAssignableFrom(field.getType())) {
					Collection c = (Collection) value;
					Collection collectionCopy = null;
					if (c != null) {
						try {
							collectionCopy = c.getClass().newInstance();
							collectionCopy.addAll(c);
						} catch (Exception e) {
							throw new IllegalStateException("could not create defensive copy of " + c.getClass().getSimpleName(), e);
						}
					}
					if (entityCollectionDetector.isEntityCollection(field)) {
						listener.beforeVisitReferenceCollection(value, field, bean, ctx);
						Object valueAfterVisit = ReflectionUtil.getFieldValue(field, bean);
						// allow swapping collection within visitor listeners
						if (valueAfterVisit != value && valueAfterVisit != null) {
							try {
								collectionCopy = (Collection) valueAfterVisit.getClass().newInstance();
								collectionCopy.addAll((Collection) valueAfterVisit);
							} catch (Exception ex) {
								throw new IllegalStateException("could not create defensive copy of " + valueAfterVisit.getClass().getSimpleName(), ex);
							}
						}
						if (collectionCopy != null) {
							for (Object object : collectionCopy) {
								listener.beforeVisitReferenceInCollection(object, c, field, bean, ctx);
								handleNestedTraverseInCollection(object, parentObjects, c);
								listener.afterVisitReferenceInCollection(object, c, field, bean, ctx);
							}
						}
						listener.afterVisitReferenceCollection(value, field, bean, ctx);
					} else {
						listener.beforeVisitCollection(value, field, bean, ctx);
						if (collectionCopy != null) {
							for (Object object : collectionCopy) {
								listener.beforeVisitValueInCollection(object, c, field, bean, ctx);
								handleNestedTraverseInCollection(object, parentObjects, c);
								listener.afterVisitValueInCollection(object, c, field, bean, ctx);
							}
						}
						listener.afterVisitCollection(value, field, bean, ctx);
					}
				} else {
					if (!(ReflectionUtil.fieldIsFinal(field) || ReflectionUtil.fieldIsStatic(field))) {
						listener.onVisitValue(value, field, bean, ctx);
					}
				}
			}
		}
	}

	private void handleNestedTraverse(Object value, Stack<Object> parentObjects, Field field, Object owner) {
		if (value != null && referenceDetector.isReferencable(value)) {
			if (!parentObjects.contains(value)) {
				parentObjects.add(value);
				traverse(value, parentObjects);
				parentObjects.pop();
			} else {
				listener.onRevisitReference(value, field, owner, ctx);
			}
		}
	}

	private void handleNestedTraverseInCollection(Object value, Stack<Object> parentObjects, Collection c) {
		if (value != null && referenceDetector.isReferencable(value)) {
			if (!parentObjects.contains(value)) {
				parentObjects.add(value);
				traverse(value, parentObjects);
				parentObjects.pop();
			} else {
				listener.onRevisitReferenceInCollection(value, c, ctx);
			}
		}
	}

}
