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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MapCollectionTypeAdapter implements CollectionTypeAdapter {
	
	public static interface KeyBuilder {
		Object buildKey(Object entry, Map targetMap, MappingState state);
	}
	
	private final List<KeyBuilder> keyBuilders = new ArrayList<>();

	@Override
	public void addObjectToCollection(Object entry, Object collection, MappingState state) {
		if (entry == null) {
			return;
		}
		Object key = null;
		Iterator<KeyBuilder> iterator = keyBuilders.iterator();
		Map targetMap = (Map) collection;
		while (key == null && iterator.hasNext()) {
			KeyBuilder next = iterator.next();
			key = next.buildKey(entry, targetMap, state);
		}
		if (key == null) {
			throw new IllegalStateException("could not build key for map entry");
		}
		targetMap.put(key, entry);
	}

	@Override
	public void iterate(Object collection, IterationHandler handler, MappingState state) {
		Map sourceMap = (Map) collection;
		for (Object value : sourceMap.values()) {
			handler.handle(value, state);
		}
	}

	@Override
	public Class<?> getSupportedCollectionType() {
		return Map.class;
	}

	@Override
	public Object newCollectionInstance(Class<?> type) {
		return new LinkedHashMap();
	}

	public void addKeyBuilder(KeyBuilder keyBuilder) {
		if (keyBuilder != null) {
			keyBuilders.add(keyBuilder);
		}
	}
	public void removeKeyBuilder(KeyBuilder keyBuilder) {
		if (keyBuilder != null) {
			Iterator<KeyBuilder> iterator = keyBuilders.iterator();
			while (iterator.hasNext()) {
				MapCollectionTypeAdapter.KeyBuilder next = iterator.next();
				if (next == keyBuilder) {
					iterator.remove();
				}
			}
		}
	}
}
