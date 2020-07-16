package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.repository.RepositorySession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = RepositoryCacheInvalidator.class, immediate = true)
public class RepositoryCacheInvalidator implements RepositoryListener {

	@Reference
	private Repository repository;
	
	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	
	private final Map<Integer, SessionTracker> sessionTrackersBySessionHash = new HashMap<>();

	@Activate
	public void activate() {
		repository.addListener(this);
	}

	@Deactivate
	public void deactivate() {
		repository.removeListener(this);
	}
	

	private final class SessionTracker {
		private final RepositorySession session;
		private CacheTransaction cacheTransaction;

		public SessionTracker(RepositorySession session) {
			this.session = session;
		}

		public final RepositorySession getSession() {
			return session;
		}

		public final void clear() {
			cacheTransaction = null;
		}

		public final CacheTransaction assertCacheTransactionExists() {
			if (cacheTransaction == null) {
				cacheTransaction = cacheTransactionFactory.createCacheTransaction();
			}
			return cacheTransaction;
		}
		
	}
	
	@Override
	public void onSessionStart(RepositorySession session) throws RepositoryException {
		if (session.isReadOnly()) {
			// then we do not care for this session
			return ;
		}
	}

	@Override
	public void onSessionEnd(RepositorySession session) throws RepositoryException {
		if (session.isReadOnly()) {
			// then we do not care for this session
			return;
		}
		dropCacheTransaction(session);
	}

	@Override
	public void onBeforeFlush(RepositorySession session) throws RepositoryException {
		// do nothing
	}

	@Override
	public void onFlushSuccess(RepositorySession session) throws RepositoryException {
		// commit the cache transaction, if there is any
		CacheTransaction tx = obtainCacheTransaction(session, false);
		if (tx == null) {
			return;
		}
		tx.close();
	}

	@Override
	public void onFlushFailure(RepositorySession session) throws RepositoryException {
		// do nothing
	}

	@Override
	public void onNodeCreated(Node node) throws RepositoryException {
		// flush the direct parents
		while ((node = node.getParent()) != null) {			
			Path path = node.getPath();
			flushPath(path, node.getRepositorySession());
		}
	}

	@Override
	public void onNodeRemoved(Node node) throws RepositoryException {
		// flush the children and the direct parents
		flushPathRecursive(node.getPath(), node.getRepositorySession());
		while ((node = node.getParent()) != null) {			
			flushPath(node.getPath(), node.getRepositorySession());
		}
	}

	@Override
	public void onNodeMoved(Node node, long index) throws RepositoryException {
		// flush the children and the direct parents
		flushPathRecursive(node.getPath(), node.getRepositorySession());
		while ((node = node.getParent()) != null) {			
			flushPath(node.getPath(), node.getRepositorySession());
		}
	}

	@Override
	public void onPropertyCreated(Property property) throws RepositoryException {
		flushPath(property.getNode().getPath(), property.getRepositorySession());
	}

	@Override
	public void onPropertyRemoved(Property property) throws RepositoryException {
		flushPath(property.getNode().getPath(), property.getRepositorySession());
	}

	@Override
	public void onPropertyChanged(Property property) throws RepositoryException {
		flushPath(property.getNode().getPath(), property.getRepositorySession());
	}
	
	private void flushPath(Path path, RepositorySession repositorySession) throws RepositoryException {
		internalFlushPath(path, repositorySession, false);
	}
	private void flushPathRecursive(Path path, RepositorySession repositorySession) throws RepositoryException {
		internalFlushPath(path, repositorySession, true);
	}
	
	private void internalFlushPath(Path path, RepositorySession repositorySession, boolean recursive) throws RepositoryException {
		CacheTransaction transaction = obtainCacheTransaction(repositorySession, true);
		if (recursive) {
			transaction.flushRecursive(RepositoryResource.URL_SEGEMENT + path.toString());
		} else {
			transaction.flush(RepositoryResource.URL_SEGEMENT + path.toString());
		}
	}
	
	private CacheTransaction obtainCacheTransaction(RepositorySession repositorySession, boolean createTransactionIfRequired) throws RepositoryException {
		SessionTracker tracker = getLocalSessionTracker(repositorySession);
		if (tracker == null) {
			if (createTransactionIfRequired) {
				return createLocalSessionTracker(repositorySession).assertCacheTransactionExists();
			} else {
				return null;
			}
		} else {
			return tracker.assertCacheTransactionExists();
		}
	}
	
	private SessionTracker getLocalSessionTracker(RepositorySession repositorySession) {
		try {
			readLock.lock();
			return sessionTrackersBySessionHash.get(System.identityHashCode(repositorySession));
		} finally {
			readLock.unlock();
		}
	}
	
	private SessionTracker createLocalSessionTracker(RepositorySession repositorySession) {
		try {
			writeLock.lock();
			SessionTracker tracker = getLocalSessionTracker(repositorySession);
			if (tracker == null) {
				int sessionHash = System.identityHashCode(repositorySession);
				tracker = sessionTrackersBySessionHash.get(sessionHash);
				if (tracker == null) {
					tracker = new SessionTracker(repositorySession);
					sessionTrackersBySessionHash.put(sessionHash, tracker);
				}
				return tracker;
			} else {
				return tracker;
			}
		} finally {
			writeLock.unlock();
		}
	}

	private void dropLocalSessionTracker(SessionTracker tracker) {
		try {
			writeLock.lock();
			sessionTrackersBySessionHash.remove(System.identityHashCode(tracker.getSession()));
		} finally {
			writeLock.unlock();
		}
	}
	
	private void dropCacheTransaction(RepositorySession session) {
		SessionTracker tracker = getLocalSessionTracker(session);
		if (tracker != null) {
			tracker.clear();
			dropLocalSessionTracker(tracker);
		}
	}
}
