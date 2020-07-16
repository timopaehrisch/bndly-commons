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

import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.listener.PreDeleteListener;
import org.bndly.schema.api.listener.PreMergeListener;
import org.bndly.schema.api.listener.PrePersistListener;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.listener.TransactionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ListenerRegistry {
	private static final class TypeBoundListenerKey<E> {
		private final Class<E> listenerType;
		private final String typeName;

		public TypeBoundListenerKey(Class<E> listenerType, String typeName) {
			this.listenerType = listenerType;
			this.typeName = typeName;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 97 * hash + Objects.hashCode(this.listenerType);
			hash = 97 * hash + Objects.hashCode(this.typeName);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final TypeBoundListenerKey<?> other = (TypeBoundListenerKey<?>) obj;
			if (!Objects.equals(this.typeName, other.typeName)) {
				return false;
			}
			if (!Objects.equals(this.listenerType, other.listenerType)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return listenerType.getSimpleName()+"->"+typeName;
		}

		
	}
	
	private final Map<TypeBoundListenerKey<?>, List<Object>> listenersByJavaTypeAndSchemaType = new HashMap<>();
	private Map<TypeBoundListenerKey<?>, List<Object>> listenersByJavaTypeAndSchemaTypeDefensive = new HashMap<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	void addListener(Object object) {
		addListenerForTypes(object);
	}

	void addListenerForTypes(Object object, String... typeNames) {
		if (object == null) {
			return;
		}
		listenersLock.writeLock().lock();
		try {
			addAsListener(object, SchemaDeploymentListener.class);
			addAsListener(object, TransactionListener.class);

			addAsListener(object, PreMergeListener.class, typeNames);
			addAsListener(object, MergeListener.class, typeNames);

			addAsListener(object, PrePersistListener.class, typeNames);
			addAsListener(object, PersistListener.class, typeNames);

			addAsListener(object, PreDeleteListener.class, typeNames);
			addAsListener(object, DeleteListener.class, typeNames);
			updateListeners(GLOBAL_LISTENER_TYPES, TYPE_RELATED_LISTENER_TYPES, typeNames);
		} finally {
			listenersLock.writeLock().unlock();
		}
	}
	

	void removeListener(Object object) {
		if (object == null) {
			return;
		}
		listenersLock.writeLock().lock();
		try {
			Set<String> affectedTypeNames = new HashSet<>();
			removeAsListener(object, SchemaDeploymentListener.class, affectedTypeNames);
			removeAsListener(object, TransactionListener.class, affectedTypeNames);
			removeAsListener(object, PreMergeListener.class, affectedTypeNames);
			removeAsListener(object, MergeListener.class, affectedTypeNames);
			removeAsListener(object, PrePersistListener.class, affectedTypeNames);
			removeAsListener(object, PersistListener.class, affectedTypeNames);
			removeAsListener(object, PreDeleteListener.class, affectedTypeNames);
			removeAsListener(object, DeleteListener.class, affectedTypeNames);
			updateListeners(GLOBAL_LISTENER_TYPES, TYPE_RELATED_LISTENER_TYPES, affectedTypeNames.toArray(new String[affectedTypeNames.size()]));
		} finally {
			listenersLock.writeLock().unlock();
		}
	}
	
	private static final Class[] TYPE_RELATED_LISTENER_TYPES = new Class[]{
		PreMergeListener.class,
		MergeListener.class,
		PrePersistListener.class,
		PersistListener.class,
		PreDeleteListener.class,
		DeleteListener.class
	};
	
	private static final Class[] GLOBAL_LISTENER_TYPES = new Class[]{
		SchemaDeploymentListener.class,
		TransactionListener.class
	};
	
	void updateListeners(Class[] globalListenerTypes, Class[] typeRelatedListenerTypes, String... typeNames) {
		listenersByJavaTypeAndSchemaTypeDefensive = new HashMap<>();
		for (Class listenerType : typeRelatedListenerTypes) {
			for (String typeName : typeNames) {
				// pre-generate the listener list
				compileListeners(listenerType, typeName);
			}
		}
		for (Class listenerType : globalListenerTypes) {
			// pre-generate the listener list
			compileListeners(listenerType, null);
		}
	}

	private void removeAsListener(Object object, Class<?> listenerType, Collection<String> affectedTypeNames) {
		if (listenerType.isInstance(object)) {
			Iterator<Map.Entry<TypeBoundListenerKey<?>, List<Object>>> entrySetIter = listenersByJavaTypeAndSchemaType.entrySet().iterator();
			while (entrySetIter.hasNext()) {
				Map.Entry<TypeBoundListenerKey<?>, List<Object>> entry = entrySetIter.next();
				TypeBoundListenerKey<?> key = entry.getKey();
				if (key.listenerType.equals(listenerType)) {
					List<Object> l = entry.getValue();
					if (l != null) {
						Iterator<Object> iterator = l.iterator();
						while (iterator.hasNext()) {
							if (iterator.next() == object) {
								iterator.remove();
								affectedTypeNames.add(key.typeName);
							}
						}
						if (l.isEmpty()) {
							entrySetIter.remove();
						}
					}
				}
			}
		}
	}

	private void addAsListener(Object object, Class<?> listenerType, String... typeNames) {
		if (listenerType.isInstance(object)) {
			if (typeNames != null && typeNames.length > 0) {
				for (String typeName : typeNames) {
					TypeBoundListenerKey key = new TypeBoundListenerKey(listenerType, typeName);
					List<Object> l = listenersByJavaTypeAndSchemaType.get(key);
					if (l == null) {
						l = new ArrayList<>();
						listenersByJavaTypeAndSchemaType.put(key, l);
					}
					l.add(object);
				}
			} else {
				TypeBoundListenerKey key = new TypeBoundListenerKey(listenerType, null);
				List<Object> l = listenersByJavaTypeAndSchemaType.get(key);
				if (l == null) {
					l = new ArrayList<>();
					listenersByJavaTypeAndSchemaType.put(key, l);
				}
				l.add(object);
			}
		}
	}

	<E> List<E> getListeners(Class<E> listenerType, String typeName) {
		return compileListeners(listenerType, typeName);
	}
	
	private <E> List<E> compileListeners(Class<E> listenerType, String typeName) {
		TypeBoundListenerKey<E> key = new TypeBoundListenerKey<>(listenerType, typeName);
		// don't access the field again, because the map might have been replaced while this method is executed.
		Map<TypeBoundListenerKey<?>, List<Object>> map = listenersByJavaTypeAndSchemaTypeDefensive;
		List<Object> result = map.get(key);
		if (result == null) {
			listenersLock.writeLock().lock();
			try {
				// create the list on the fly
				result = new ArrayList<>();
				if (typeName != null) {
					result.addAll(compileListeners(listenerType, null));
				}
				List<Object> typeSpecificListeners = listenersByJavaTypeAndSchemaType.get(key);
				if (typeSpecificListeners != null) {
					result.addAll(typeSpecificListeners);
				}
				map.put(key, result);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
		return (List<E>) result;
	}
}
