package org.bndly.common.service.cache;

/*-
 * #%L
 * Service Cache
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

import org.bndly.common.service.cache.api.Cache;
import org.bndly.common.service.cache.api.CacheKeyProvider;
import java.util.ArrayList;
import java.util.List;

public class ChainedCache implements Cache {

	private final List<Cache> caches;

	public ChainedCache(Cache... caches) {
		this.caches = new ArrayList<>();
		if (caches != null) {
			for (Cache cache : caches) {
				if (cache != null) {
					this.caches.add(cache);
				}
			}
		}
		if (this.caches.isEmpty()) {
			throw new IllegalArgumentException("created a chained cache without providing any chaches");
		}
	}

	public ChainedCache(List<Cache> caches) {
		this.caches = new ArrayList<>();
		if (caches != null) {
			for (Cache cache : caches) {
				if (cache != null) {
					this.caches.add(cache);
				}
			}
		}
		if (this.caches.isEmpty()) {
			throw new IllegalArgumentException("created a chained cache without providing any chaches");
		}
	}

	@Override
	public boolean existsInCache(CacheKeyProvider key) {
		for (Cache cache : caches) {
			if (cache.existsInCache(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getCachedValue(CacheKeyProvider key) {
		Object value = null;
		for (Cache cache : caches) {
			value = cache.getCachedValue(key);
			if (value != null) {
				break;
			}
		}
		return value;
	}

	@Override
	public void storeValue(CacheKeyProvider key, Object value) {
		for (Cache cache : caches) {
			cache.storeValue(key, value);
		}
	}

}
