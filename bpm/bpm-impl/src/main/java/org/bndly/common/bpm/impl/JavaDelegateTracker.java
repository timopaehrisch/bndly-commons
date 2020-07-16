package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

import org.bndly.common.osgi.util.DictionaryAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.activiti.engine.delegate.JavaDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JavaDelegateTracker implements ServiceTrackerCustomizer<JavaDelegate, JavaDelegate> {

	private final BundleContext context;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<String, JavaDelegateWrapper> globalDelegates = new HashMap<>();
	private final Map<String, Map<String, JavaDelegateWrapper>> localDelegates = new HashMap<>();

	public JavaDelegateTracker(BundleContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context is not allowed to be null.");
		}
		this.context = context;
	}

	@Override
	public JavaDelegate addingService(ServiceReference<JavaDelegate> sr) {
		JavaDelegate service = context.getService(sr);
		JavaDelegateWrapper wrapper = new JavaDelegateWrapper(service, new DictionaryAdapter(sr, true));
		String delegateName = wrapper.getName();
		if (delegateName != null) {
			lock.writeLock().lock();
			try {
				addedDelegate(wrapper);
				return wrapper;
			} finally {
				lock.writeLock().unlock();
			}
		}
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<JavaDelegate> sr, JavaDelegate service) {
		if (JavaDelegateWrapper.class.isInstance(service)) {
			lock.writeLock().lock();
			try {
				JavaDelegateWrapper wrapper = (JavaDelegateWrapper) service;
				// remove the wrapper from all maps
				Iterator<JavaDelegateWrapper> iterator = globalDelegates.values().iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == wrapper) {
						iterator.remove();
					}
				}
				for (Map<String, JavaDelegateWrapper> localMap : localDelegates.values()) {
					Iterator<JavaDelegateWrapper> iterator2 = localMap.values().iterator();
					while (iterator2.hasNext()) {
						if (iterator2.next() == wrapper) {
							iterator2.remove();
						}
					}
				}

				wrapper = new JavaDelegateWrapper(wrapper.getWrapped(), new DictionaryAdapter(sr, true));
				if (wrapper.getName() != null) {
					addedDelegate(wrapper);
				}
			} finally {
				lock.writeLock().unlock();
			}
		} else {
			lock.writeLock().lock();
			try {
				removeJavaDelegate(service);
				JavaDelegateWrapper wrapper = new JavaDelegateWrapper(service, new DictionaryAdapter(sr, true));
				if (wrapper.getName() != null) {
					addedDelegate(wrapper);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removedService(ServiceReference<JavaDelegate> sr, JavaDelegate service) {
		if (JavaDelegateWrapper.class.isInstance(service)) {
			lock.writeLock().lock();
			try {
				JavaDelegateWrapper wrapper = (JavaDelegateWrapper) service;
				removedDelegate(wrapper);
			} finally {
				lock.writeLock().unlock();
			}
		} else {
			lock.writeLock().lock();
			try {
				removeJavaDelegate(service);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	private void removeJavaDelegate(JavaDelegate service) {
		Iterator<JavaDelegateWrapper> iterator = globalDelegates.values().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getWrapped() == service) {
				iterator.remove();
			}
		}
		for (Map<String, JavaDelegateWrapper> localMap : localDelegates.values()) {
			Iterator<JavaDelegateWrapper> iterator2 = localMap.values().iterator();
			while (iterator2.hasNext()) {
				if (iterator2.next().getWrapped() == service) {
					iterator2.remove();
				}
			}
		}
	}

	private void addedDelegate(JavaDelegateWrapper javaDelegateWrapper) {
		if (javaDelegateWrapper.isGlobal()) {
			globalDelegates.put(javaDelegateWrapper.getName(), javaDelegateWrapper);
		} else {
			String te = javaDelegateWrapper.getTargetEngine();
			if (te != null) {
				Map<String, JavaDelegateWrapper> localMap = localDelegates.get(te);
				if (localMap == null) {
					localMap = new HashMap<>();
					localDelegates.put(te, localMap);
				}
				localMap.put(javaDelegateWrapper.getName(), javaDelegateWrapper);
			}
		}
	}

	private void removedDelegate(JavaDelegateWrapper javaDelegateWrapper) {
		if (javaDelegateWrapper.isGlobal()) {
			globalDelegates.remove(javaDelegateWrapper.getName());
		} else {
			String te = javaDelegateWrapper.getTargetEngine();
			if (te != null) {
				Map<String, JavaDelegateWrapper> localMap = localDelegates.get(te);
				if (localMap != null) {
					localMap.remove(javaDelegateWrapper.getName());
					// empty local maps will not be removed!!
				}
			}
		}
	}

	public Map<String, JavaDelegate> createBeanMap(String engineName) {
		// try to get a local map
		Map<String, JavaDelegateWrapper> localMap;
		lock.readLock().lock();
		try {
			localMap = localDelegates.get(engineName);
		} finally {
			lock.readLock().unlock();
		}

		// if there is no local map, acquire a write lock, check again and if there is no local map, create one.
		if (localMap == null) {
			lock.writeLock().lock();
			try {
				localMap = localDelegates.get(engineName);
				if (localMap == null) {
					localMap = new HashMap<>();
					localDelegates.put(engineName, localMap);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
		final Map<String, JavaDelegateWrapper> finalLocalMap = localMap;
		return new Map<String, JavaDelegate>() {
			@Override
			public int size() {
				return keySet().size();
			}

			@Override
			public boolean isEmpty() {
				lock.readLock().lock();
				try {
					return (finalLocalMap.isEmpty() && globalDelegates.isEmpty());
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public boolean containsKey(Object key) {
				lock.readLock().lock();
				try {
					return (finalLocalMap.containsKey(key) || globalDelegates.containsKey(key));
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public boolean containsValue(Object value) {
				lock.readLock().lock();
				try {
					return (finalLocalMap.containsValue(value) || globalDelegates.containsValue(value));
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public JavaDelegate get(Object key) {
				lock.readLock().lock();
				try {
					if (finalLocalMap.containsKey((String) key)) {
						return unwrap(finalLocalMap.get(key));
					} else {
						return unwrap(globalDelegates.get(key));
					}
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public JavaDelegate put(String key, JavaDelegate value) {
				if (JavaDelegateWrapper.class.isInstance(value)) {
					lock.writeLock().lock();
					try {
						return unwrap(finalLocalMap.put((String) key, (JavaDelegateWrapper) value));
					} finally {
						lock.writeLock().unlock();
					}
				}
				return null;
			}

			@Override
			public JavaDelegate remove(Object key) {
				lock.writeLock().lock();
				try {
					if (finalLocalMap.containsKey((String) key)) {
						return unwrap(finalLocalMap.remove(key));
					} else {
						return unwrap(globalDelegates.remove(key));
					}
				} finally {
					lock.writeLock().unlock();
				}
			}

			@Override
			public void putAll(Map m) {
				lock.writeLock().lock();
				try {
					for (Map.Entry<String, JavaDelegateWrapper> object : ((Map<String, JavaDelegateWrapper>)m).entrySet()) {
						finalLocalMap.put(object.getKey(), object.getValue());
					}
				} finally {
					lock.writeLock().unlock();
				}
			}

			@Override
			public void clear() {
				lock.writeLock().lock();
				try {
					finalLocalMap.clear();
					globalDelegates.clear();
				} finally {
					lock.writeLock().unlock();
				}
			}

			@Override
			public Set<String> keySet() {
				lock.readLock().lock();
				try {
					Set<String> keys = new LinkedHashSet<>();
					keys.addAll(finalLocalMap.keySet());
					keys.addAll(globalDelegates.keySet());
					return keys;
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public Collection<JavaDelegate> values() {
				lock.readLock().lock();
				try {
					List<JavaDelegate> values = new ArrayList<>(finalLocalMap.size() + globalDelegates.size());
					for (JavaDelegateWrapper value : finalLocalMap.values()) {
						values.add(unwrap(value));
					}
					for (JavaDelegateWrapper value : globalDelegates.values()) {
						values.add(unwrap(value));
					}
					return values;
				} finally {
					lock.readLock().unlock();
				}
			}

			@Override
			public Set<Map.Entry<String, JavaDelegate>> entrySet() {
				lock.readLock().lock();
				try {
					Set<Map.Entry<String, JavaDelegate>> result = new LinkedHashSet<>();
					for (final String key : keySet()) {
						result.add(new Entry<String, JavaDelegate>() {
							@Override
							public String getKey() {
								return key;
							}

							@Override
							public JavaDelegate getValue() {
								return get(key);
							}

							@Override
							public JavaDelegate setValue(JavaDelegate value) {
								return put(key, value);
							}
						});
					}
					return result;
				} finally {
					lock.readLock().unlock();
				}
			}
		};
	}
	
	private JavaDelegate unwrap(JavaDelegateWrapper delegateWrapper) {
		return delegateWrapper.getWrapped();
	}

}
