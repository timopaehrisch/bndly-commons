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

import org.bndly.common.reflection.CompiledFieldBeanPropertyAccessorWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CompiledBeanIterator<E> {
	
	private final Class<E> inspectedType;
	private final CompiledFieldBeanPropertyAccessorWriter accessorWriter;
	private final TypeBasedReferenceDetector referenceDetector;
	private final EntityCollectionDetector entityCollectionDetector;
	private final List<CompiledElement> compiledElements;
	private final CompiledBeanIteratorProvider compiledBeanIteratorProvider;
	
	public static interface CompiledBeanIteratorProvider {
		<F> CompiledBeanIterator<F> getCompiledBeanIteratorForType(Class<F> inspectedType);
	}
	
	private static interface CompiledElement {
		void handle(Object bean, BeanGraphIteratorListener listener, Object ctx, Stack<Object> parentObjects);
	}

	public CompiledBeanIterator(CompiledBeanIteratorProvider compiledBeanIteratorProvider, Class<E> inspectedType, TypeBasedReferenceDetector referenceDetector, EntityCollectionDetector entityCollectionDetector) {
		if (compiledBeanIteratorProvider == null) {
			throw new IllegalArgumentException("compiledBeanIteratorProvider is not allowed to be null");
		}
		if (inspectedType == null) {
			throw new IllegalArgumentException("inspectedType is not allowed to be null");
		}
		if (referenceDetector == null) {
			throw new IllegalArgumentException("referenceDetector is not allowed to be null");
		}
		if (entityCollectionDetector == null) {
			throw new IllegalArgumentException("entityCollectionDetector is not allowed to be null");
		}
		this.compiledBeanIteratorProvider = compiledBeanIteratorProvider;
		this.inspectedType = inspectedType;
		this.referenceDetector = referenceDetector;
		this.entityCollectionDetector = entityCollectionDetector;
		accessorWriter = new CompiledFieldBeanPropertyAccessorWriter(inspectedType);
		List<CompiledElement> compiledElementsTmp = new ArrayList<>();
		compile(compiledElementsTmp);
		compiledElements = Collections.unmodifiableList(compiledElementsTmp);
	}

	private void compile(List<CompiledElement> compiledElements) {
		for (String supportedPropertyName : accessorWriter.getSupportedPropertyNames()) {
			final CompiledFieldBeanPropertyAccessorWriter.CompiledField compiledField = accessorWriter.getCompiledField(supportedPropertyName);
			final Field field = compiledField.getField();
			Class<?> fieldType = compiledField.getField().getType();
			if (referenceDetector.isReferencableField(field) || referenceDetector.isReferencable(fieldType)) {
				compiledElements.add(new CompiledElement() {
					@Override
					public void handle(Object bean, BeanGraphIteratorListener listener, Object ctx, Stack<Object> parentObjects) {
						Object value = compiledField.get(bean);
						if (value != null) {
							listener.beforeVisitReference(value, field, bean, ctx);
							Object possiblyChangedValue = compiledField.get(bean);
							if (possiblyChangedValue == value) {
								handleNestedTraverse(value, parentObjects, field, bean, listener, ctx);
							} else {
								// if the value has changed, do not perform a nested traversation,
								// because succeeding listener invocations might be executed on an old graph
							}
							listener.afterVisitReference(value, field, bean, ctx);
						}
					}
				});
			} else if (Collection.class.isAssignableFrom(fieldType)) {
				if (entityCollectionDetector.isEntityCollection(field)) {
					compiledElements.add(new CompiledElement() {
						@Override
						public void handle(Object bean, BeanGraphIteratorListener listener, Object ctx, Stack<Object> parentObjects) {
							Object value = compiledField.get(bean);
							Collection c = (Collection) value;
							Collection collectionCopy = null;
							if (c != null) {
								try {
									collectionCopy = c.getClass().newInstance();
									collectionCopy.addAll(c);
								} catch (InstantiationException | IllegalAccessException e) {
									throw new IllegalStateException("could not create defensive copy of " + c.getClass().getSimpleName(), e);
								}
							}
							listener.beforeVisitReferenceCollection(value, field, bean, ctx);
							Object valueAfterVisit = compiledField.get(bean);
							// allow swapping collection within visitor listeners
							if (valueAfterVisit != value && valueAfterVisit != null) {
								try {
									collectionCopy = (Collection) valueAfterVisit.getClass().newInstance();
									collectionCopy.addAll((Collection) valueAfterVisit);
								} catch (InstantiationException | IllegalAccessException ex) {
									throw new IllegalStateException("could not create defensive copy of " + valueAfterVisit.getClass().getSimpleName(), ex);
								}
							}
							if (collectionCopy != null) {
								for (Object object : collectionCopy) {
									listener.beforeVisitReferenceInCollection(object, c, field, bean, ctx);
									handleNestedTraverseInCollection(object, parentObjects, c, listener, ctx);
									listener.afterVisitReferenceInCollection(object, c, field, bean, ctx);
								}
							}
							listener.afterVisitReferenceCollection(value, field, bean, ctx);
						}
					});
				} else {
					compiledElements.add(new CompiledElement() {
						@Override
						public void handle(Object bean, BeanGraphIteratorListener listener, Object ctx, Stack<Object> parentObjects) {
							Object value = compiledField.get(bean);
							Collection c = (Collection) value;
							Collection collectionCopy = null;
							if (c != null) {
								try {
									collectionCopy = c.getClass().newInstance();
									collectionCopy.addAll(c);
								} catch (InstantiationException | IllegalAccessException e) {
									throw new IllegalStateException("could not create defensive copy of " + c.getClass().getSimpleName(), e);
								}
							}
							listener.beforeVisitCollection(value, field, bean, ctx);
							if (collectionCopy != null) {
								for (Object object : collectionCopy) {
									listener.beforeVisitValueInCollection(object, c, field, bean, ctx);
									handleNestedTraverseInCollection(object, parentObjects, c, listener, ctx);
									listener.afterVisitValueInCollection(object, c, field, bean, ctx);
								}
							}
							listener.afterVisitCollection(value, field, bean, ctx);
						}
					});
				}
			} else {
				if (!compiledField.isFinal() && !compiledField.isStatic()) {
					compiledElements.add(new CompiledElement() {
						@Override
						public void handle(Object bean, BeanGraphIteratorListener listener, Object ctx, Stack<Object> parentObjects) {
							Object value = compiledField.get(bean);
							listener.onVisitValue(value, field, bean, ctx);
						}
					});
				}
			}
		}
	}
	
	public <G> void traverse(E bean, BeanGraphIteratorListener<G> listener, G ctx) {
		if (bean == null) {
			return;
		}
		if (ctx == null) {
			Class<G> contextType = listener.getIterationContextType();
			if (contextType != null) {
				try {
					ctx = contextType.newInstance();
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new IllegalStateException("could not instantiate context " + contextType.getSimpleName());
				}
			}
		}
		
		Collection collection;
		if (Collection.class.isAssignableFrom(bean.getClass())) {
			collection = (Collection) bean;
			for (Object object : collection) {
				listener.onStart(object, ctx);
				Stack<Object> stack = new Stack<>();
				stack.add(object);
				traverse(object, stack, listener, ctx);
				listener.onEnd(object, ctx);
			}
		} else {
			listener.onStart(bean, ctx);
			Stack<Object> stack = new Stack<>();
			stack.add(bean);
			traverse(bean, stack, listener, ctx);
			listener.onEnd(bean, ctx);
		}

	}
	
	private <G> void traverse(Object bean, Stack<Object> parentObjects, BeanGraphIteratorListener<G> listener, G ctx) {
		listener.onVisitReference(bean, ctx);
		for (CompiledElement compiledElement : compiledElements) {
			compiledElement.handle(bean, listener, ctx, parentObjects);
		}
	}
	
	private void handleNestedTraverse(Object value, Stack<Object> parentObjects, Field field, Object owner, BeanGraphIteratorListener listener, Object ctx) {
		if (value != null) {
			Class<? extends Object> cls = value.getClass();
			if (referenceDetector.isReferencable(cls)) {
				if (!parentObjects.contains(value)) {
					parentObjects.add(value);
					CompiledBeanIterator compiledBeanIterator = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(cls);
					if (compiledBeanIterator != null) {
						compiledBeanIterator.traverse(value, parentObjects, listener, ctx);
					}
					parentObjects.pop();
				} else {
					listener.onRevisitReference(value, field, owner, ctx);
				}
			}
		}
	}
	
	private void handleNestedTraverseInCollection(Object value, Stack<Object> parentObjects, Collection c, BeanGraphIteratorListener listener, Object ctx) {
		if (value != null) {
			Class<? extends Object> cls = value.getClass();
			if (referenceDetector.isReferencable(cls)) {
				if (!parentObjects.contains(value)) {
					parentObjects.add(value);
					CompiledBeanIterator compiledBeanIterator = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(cls);
					if (compiledBeanIterator != null) {
						compiledBeanIterator.traverse(value, parentObjects, listener, ctx);
					}
					parentObjects.pop();
				} else {
					listener.onRevisitReferenceInCollection(value, c, ctx);
				}
			}
		}
	}
}
