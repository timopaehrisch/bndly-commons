package org.bndly.schema.impl.repository;

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

import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.services.Engine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Repository.class, immediate = true)
public class RepositoryImpl implements Repository {

	@Reference(target = "(service.pid=org.bndly.schema.api.services.Engine.repository)")
	public Engine engine;
	
	public final List<RepositoryListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();

	@Override
	public RepositorySession createReadOnlySession() throws RepositoryException {
		RepositorySessionImpl session = new RepositorySessionImpl(true, engine, engine.getAccessor().buildRecordContext(), listeners, listenersLock);
		listenersLock.readLock().lock();
		try {
			for (RepositoryListener listener : listeners) {
				listener.onSessionStart(session);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
		return session;
	}
	
	@Override
	public RepositorySession createAdminSession() throws RepositoryException {
		RepositorySessionImpl session = new RepositorySessionImpl(false, engine, engine.getAccessor().buildRecordContext(), listeners, listenersLock);
		listenersLock.readLock().lock();
		try {
			for (RepositoryListener listener : listeners) {
				listener.onSessionStart(session);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
		return session;
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	@Reference(
			bind = "addListener",
			unbind = "removeListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = RepositoryListener.class
	)
	@Override
	public void addListener(RepositoryListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeListener(RepositoryListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				Iterator<RepositoryListener> iterator = listeners.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == listener) {
						iterator.remove();
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	
}
