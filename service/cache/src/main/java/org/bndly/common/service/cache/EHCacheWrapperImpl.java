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
import org.bndly.common.service.cache.api.CacheKey;
import org.bndly.common.service.cache.api.CacheKeyProvider;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public abstract class EHCacheWrapperImpl implements Cache {

	private Ehcache ehCache;

	public EHCacheWrapperImpl() {
	}

	public EHCacheWrapperImpl(Ehcache cache) {
		this.ehCache = cache;
	}

	protected abstract Class<? extends CacheKey> getCacheKeyType();

	@Override
	public boolean existsInCache(CacheKeyProvider key) {
		CacheKey k = key.getKey(getCacheKeyType());
		Element element = ehCache.get(k);
		if (element != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Object getCachedValue(CacheKeyProvider key) {
		Element el = ehCache.get(key.getKey(getCacheKeyType()));
		if (el == null) {
			return null;
		}
		return el.getObjectValue();
	}

	@Override
	public void storeValue(CacheKeyProvider key, Object value) {
		CacheKey k = key.getKey(getCacheKeyType());
		ehCache.put(new Element(k, value));
	}

	public void setEhCache(Ehcache ehCache) {
		this.ehCache = ehCache;
	}
}
