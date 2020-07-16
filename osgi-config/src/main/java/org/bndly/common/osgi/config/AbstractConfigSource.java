package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractConfigSource implements ConfigSource {

	private final ReadWriteLock listenerLock = new ReentrantReadWriteLock();
	private final List<ConfigSourceListener> listeners = new ArrayList<>();
	
	protected final void fireCreatedEvent(Item item) {
		listenerLock.readLock().lock();
		try {
			for (ConfigSourceListener listener : listeners) {
				listener.itemCreated(item);
			}
		} finally {
			listenerLock.readLock().unlock();
		}
	}
	
	protected final void fireUpdatedEvent(Item item) {
		listenerLock.readLock().lock();
		try {
			for (ConfigSourceListener listener : listeners) {
				listener.itemUpdated(item);
			}
		} finally {
			listenerLock.readLock().unlock();
		}
	}
	
	protected final void fireDeletedEvent(Item item) {
		listenerLock.readLock().lock();
		try {
			for (ConfigSourceListener listener : listeners) {
				listener.itemDeleted(item);
			}
		} finally {
			listenerLock.readLock().unlock();
		}
	}
	
	@Override
	public final void registerListener(ConfigSourceListener listener) {
		listenerLock.writeLock().lock();
		try {
			listeners.add(listener);
		} finally {
			listenerLock.writeLock().unlock();
		}
	}

	@Override
	public final void unregisterListener(ConfigSourceListener listener) {
		listenerLock.writeLock().lock();
		try {
			Iterator<ConfigSourceListener> iterator = listeners.iterator();
			while (iterator.hasNext()) {
				if (iterator.next() == listener) {
					iterator.remove();
				}
			}
		} finally {
			listenerLock.writeLock().unlock();
		}
	}
	
}
