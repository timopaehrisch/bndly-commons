package org.bndly.common.mapper;

/*-
 * #%L
 * Mapper
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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TypeSpecificMapperImpl implements TypeSpecificMapper {
	private static final Logger LOG = LoggerFactory.getLogger(TypeSpecificMapperImpl.class);
	
	private final Class<?> inputType;
	private final Class<?> outputType;
	private final List<Tuple> tuples;
	private final boolean swapMappingDirection;

	public TypeSpecificMapperImpl(Class<?> inputType, Class<?> outputType, List<Tuple> tuples, boolean swapMappingDirection) {
		this.inputType = inputType;
		this.outputType = outputType;
		this.tuples = tuples;
		this.swapMappingDirection = swapMappingDirection;
	}

	protected abstract List<MappingPreInterceptor> getMappingPreInterceptors();

	protected abstract List<MappingPostInterceptor> getMappingPostInterceptors();

	protected abstract boolean isComplexType(Class<?> propertyType, Object propertyValue);

	protected abstract boolean isCollectionType(Class<?> type);

	protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type);

	protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object);

	@Override
	public void map(Object source, Object target, final MappingContext context, MappingState parentState) {
		MappingState intermediate = new MappingState(source, target, context, parentState, false);
		for (MappingPreInterceptor mappingPreInterceptor : getMappingPreInterceptors()) {
			mappingPreInterceptor.preIntercept(intermediate);
		}
		for (Tuple tuple : tuples) {
			MappedProperty s = tuple.getSource();
			MappedProperty t = tuple.getTarget();
			if (swapMappingDirection) {
				t = tuple.getSource();
				s = tuple.getTarget();
			}

			Object sourceValue = s.getAccessor().get(s.getName(), source);

			boolean isReferenceable = isComplexType(s.getType(), sourceValue);
			boolean isCollection = false;
			if (!isReferenceable) {
				isCollection = isCollectionType(s.getType());
			}

			MappingState tupleMappingState = new MappingState(source, target, s, t, context, intermediate, isCollection);
			for (MappingPreInterceptor mappingPreInterceptor : getMappingPreInterceptors()) {
				mappingPreInterceptor.preIntercept(tupleMappingState);
			}
			if (!isReferenceable) {
				if (!isCollection) {
					// simple properties can be copied
					t.getWriter().set(t.getName(), sourceValue, target);
				} else {
					// collections need more love
					if (sourceValue == null) {
						t.getWriter().set(t.getName(), null, target);
					} else {
						CollectionTypeAdapter sourceCollectionAdapter = assertCollectionTypeAdapterExists(sourceValue);

						Class<?> targetType = t.getAccessor().typeOf(t.getName(), target);
						final CollectionTypeAdapter targetCollectionAdapter = assertCollectionTypeAdapterExists(targetType);
						final Object targetCollection = targetCollectionAdapter.newCollectionInstance(targetType);
						if (targetCollection == null) {
							throw new IllegalStateException("could not instantiate " + targetType + " with collection adapter " + targetCollectionAdapter.getClass());
						}
						t.getWriter().set(t.getName(), targetCollection, target);
						// map all collection entries of the source collection
						sourceCollectionAdapter.iterate(sourceValue, new CollectionTypeAdapter.IterationHandler() {
							@Override
							public void handle(Object entry, MappingState state) {
								Object mapped = context.map(entry, state);
								targetCollectionAdapter.addObjectToCollection(mapped, targetCollection, state);
							}
						}, tupleMappingState);
					}
				}
			} else {
				// composed structures have to delegate to other mappers
				Object targetValue;
				try {
					// if i can somehow get a hold of the target type of the property,
					// then i should try to use it.
					Class<?> typeOfPropertyInTarget = t.getAccessor().typeOf(t.getName(), target);
					targetValue = context.map(sourceValue, typeOfPropertyInTarget, tupleMappingState);
				} catch (Exception e) {
					throw new IllegalStateException("could not map: " + e.getMessage(), e);
				}
				if (!t.getWriter().set(t.getName(), targetValue, target)) {
					LOG.error("could not set {} in ", t.getName(), target);
				}
			}
			for (MappingPostInterceptor mappingPostInterceptor : getMappingPostInterceptors()) {
				mappingPostInterceptor.postIntercept(tupleMappingState);
			}
		}
		for (MappingPostInterceptor mappingPostInterceptor : getMappingPostInterceptors()) {
			mappingPostInterceptor.postIntercept(intermediate);
		}
	}

	@Override
	public Class<?> getSupportedInput() {
		return inputType;
	}

	@Override
	public Class<?> getSupportedOutput() {
		return outputType;
	}

}
