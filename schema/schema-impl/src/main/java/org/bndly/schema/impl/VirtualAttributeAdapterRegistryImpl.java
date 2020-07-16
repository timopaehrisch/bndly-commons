package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.VirtualAttributeAdapter;
import org.bndly.schema.api.services.VirtualAttributeAdapterRegistry;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualAttributeAdapterRegistryImpl implements VirtualAttributeAdapterRegistry, Resetable {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final List<VirtualAttributeAdapter> adapters = new ArrayList<>();
	private final Map<AdapterCacheKey, VirtualAttributeAdapter> cachedAdapters = new HashMap<>();

	private static final class AdapterCacheKey {

		private final String typeName;
		private final String attributeName;

		public AdapterCacheKey(String typeName, String attributeName) {
			this.typeName = typeName;
			this.attributeName = attributeName;
		}

		public String getAttributeName() {
			return attributeName;
		}

		public String getTypeName() {
			return typeName;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 79 * hash + Objects.hashCode(this.typeName);
			hash = 79 * hash + Objects.hashCode(this.attributeName);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final AdapterCacheKey other = (AdapterCacheKey) obj;
			if (!Objects.equals(this.typeName, other.typeName)) {
				return false;
			}
			if (!Objects.equals(this.attributeName, other.attributeName)) {
				return false;
			}
			return true;
		}

	}

	@Override
	public void register(VirtualAttributeAdapter adapter) {
		if (adapter != null) {
			lock.writeLock().lock();
			try {
				adapters.add(adapter);
				invalidateAdapterCache();
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public void unregister(VirtualAttributeAdapter adapter) {
		if (adapter != null) {
			lock.writeLock().lock();
			try {
				adapters.remove(adapter);
				invalidateAdapterCache();
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	private void invalidateAdapterCache() {
		cachedAdapters.clear();
	}

	@Override
	public <E extends Attribute> VirtualAttributeAdapter<E> getAdapterForAttributeAndType(E att, Type type) {
		AdapterCacheKey key = new AdapterCacheKey(type.getName(), att.getName());
		lock.readLock().lock();
		try {
			if (cachedAdapters.containsKey(key)) {
				return cachedAdapters.get(key);
			}
		} finally {
			lock.readLock().unlock();
		}
		lock.writeLock().lock();
		try {
			if (cachedAdapters.containsKey(key)) {
				return cachedAdapters.get(key);
			}
			for (VirtualAttributeAdapter virtualAttributeAdapter : adapters) {
				try {
					if (virtualAttributeAdapter.supports(att, type)) {
						cachedAdapters.put(key, virtualAttributeAdapter);
						return virtualAttributeAdapter;
					}
				} catch (ClassCastException e) {
					// adapter does not work with the attribute
				}
			}
			cachedAdapters.put(key, null);
			return null;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void reset() {
		lock.writeLock().lock();
		try {
			adapters.clear();
			cachedAdapters.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

}
