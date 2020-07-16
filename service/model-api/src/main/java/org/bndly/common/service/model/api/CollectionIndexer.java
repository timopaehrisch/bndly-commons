package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionIndexer<I, O> {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionIndexer.class);
	Class<? extends Map> targetMapClass;

	public CollectionIndexer() {
	}

	public <TARGET_MAP_CLASS extends Class<? extends Map>> CollectionIndexer(TARGET_MAP_CLASS targetMapClass) {
		this.targetMapClass = targetMapClass;
	}

	public Map<I, O> index(Collection<O> collection, IndexerFunction<I, O> indexFunction) {
		if (indexFunction == null) {
			throw new IllegalStateException("can't index a collection without an IndexFunction");
		}
		Map<I, O> map = null;
		if (collection != null) {
			if (targetMapClass != null) {
				try {
					map = targetMapClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					LOG.error("failed to create target map class instance while indexing a collection: " + e.getMessage(), e);
				}
			} else {
				map = new HashMap<>(collection.size());
			}
			if (map != null) {
				for (O object : collection) {
					I key = indexFunction.index(object);
					if (map.containsKey(key)) {
						throw new IllegalStateException("could not create index without overwriting existing entry");
					}
					map.put(key, object);
				}
			}
		} else {
			map = new HashMap<>(1);
		}
		return map;
	}

	public <T extends Class<? extends Map>> void setTargetMapClass(T targetMapClass) {
		this.targetMapClass = targetMapClass;
	}
}
