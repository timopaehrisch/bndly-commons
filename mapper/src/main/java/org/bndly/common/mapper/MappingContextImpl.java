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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class MappingContextImpl implements MappingContext {

	private final MissingMapperHandler missingMapperHandler;
	private final List<MappingContext.Listener> listeners = new ArrayList<>();
	private final Map<MappingContextKey, Object> mappedObjects = new HashMap<>();

	public MappingContextImpl(MissingMapperHandler missingMapperHandler) {
		if (missingMapperHandler == null) {
			throw new IllegalArgumentException("missingMapperHandler is not allowed to be null");
		}
		this.missingMapperHandler = missingMapperHandler;
	}

	protected abstract <T> T buildInstance(Class<T> type, MappingState state);

	protected abstract CollectionTypeAdapter getCollectionTypeAdapterFor(Class<?> type);

	protected abstract boolean isCollectionType(Class<?> type);

	protected abstract MappingContextKey buildMappingContextKey(Object source);

	protected abstract Mapper getMapper(Class<?> inputType, Class<?> desiredOutputType, Object inputObject);

	@Override
	public Object map(Object source, MappingState mappingState) {
		if (source == null) {
			return null;
		}

		Mapper mapper = getMapper(source.getClass(), null, source);
		if (mapper == null) {
			return missingMapperHandler.handleMissingMapper(source, mappingState);
		}
		Class<?> outputType = null;
		if (TypeSpecificMapper.class.isInstance(mapper)) {
			outputType = TypeSpecificMapper.class.cast(mapper).getSupportedOutput();
		}
		if (outputType == null) {
			return missingMapperHandler.handleMissingMapperOutputType(source, mappingState, mapper);
		}
		return _map(source, outputType, mapper, mappingState);
	}

	@Override
	public <T> T map(Object source, Class<T> outputType) {
		MappingState state = new MappingState(source, null, this, null, isCollectionType(outputType));
		return map(source, outputType, state);
	}

	@Override
	public <T> T map(Object source, Class<T> outputType, MappingState mappingState) {
		if (source == null) {
			return null;
		}

		Mapper mapper = getMapper(source.getClass(), outputType, source);
		// we propably need to exchange the outputType because the 
		// mapper support a more specific type than the outputType 
		// provided by this method call
		if (TypeSpecificMapper.class.isInstance(mapper)) {
			Class<?> mapperOutType = TypeSpecificMapper.class.cast(mapper).getSupportedOutput();
			if (outputType.isAssignableFrom(mapperOutType)) {
				return (T) _map(source, mapperOutType, mapper, mappingState);
			}
		}
		return _map(source, outputType, mapper, mappingState);
	}

	@Override
	public void map(Object source, Object target, Class<?> outputType) {
		MappingState state = new MappingState(source, target, this, null, isCollectionType(outputType));
		map(source, target, outputType, state);
	}

	@Override
	public void map(Object source, Object target, Class<?> outputType, MappingState state) {
		if (source == null || target == null || outputType == null) {
			return;
		}

		Mapper mapper = getMapper(source.getClass(), outputType, source);
		if (mapper != null) {
			_map(source, target, outputType, mapper, state);
		} else {
			missingMapperHandler.handleMissingOutputMapper(source, target, outputType, state);
		}
	}

	private <T> T _map(Object source, Class<T> outputType, Mapper mapper, MappingState parentState) {
		if (source == null) {
			return null;
		}

		MappingContextKey key = buildMappingContextKey(source);
		if (key != null) {
			if (mappedObjects.containsKey(key)) {
				return (T) mappedObjects.get(key);
			}
		}

		if (mapper != null) {
			boolean isCollection = isCollectionType(outputType);
			MappingState intermediate = new MappingState(source, null, this, parentState, isCollection);
			T target;
			if (isCollection) {
				target = (T) getCollectionTypeAdapterFor(outputType).newCollectionInstance(outputType);
			} else {
				target = buildInstance(outputType, intermediate);
			}
			if (key != null) {
				mappedObjects.put(key, target);
			}
			if (target != null) {
				MappingState state = new MappingState(source, target, this, parentState, isCollection);
				_map(source, target, outputType, mapper, state);
			}
			return target;
		}
		return (T) missingMapperHandler.handleMissingMapper(source, parentState);
	}

	private void _map(Object source, Object output, Class<?> outputType, Mapper mapper, MappingState state) {
		if (source == null || output == null || mapper == null || outputType == null) {
			return;
		}

		for (Listener listener : listeners) {
			listener.beforeMapping(source, output, outputType, state);
		}
		mapper.map(source, output, this, state);
		for (Listener listener : listeners) {
			listener.afterMapping(source, output, outputType, state);
		}
	}

	@Override
	public MappingContextImpl removeListener(Listener listener) {
		if (listener != null) {
			Iterator<Listener> iterator = listeners.iterator();
			while (iterator.hasNext()) {
				Listener l = iterator.next();
				if (l == listener) {
					iterator.remove();
				}
			}
		}
		return this;
	}

	@Override
	public MappingContextImpl addListener(Listener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
		return this;
	}

}
