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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.repository.EntityReference;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.services.Engine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class RepositorySessionImpl implements RepositorySession {

	private final boolean readOnly;
	private final Engine engine;
	private final RecordContext recordContext;
	private final List<TransactionItem> transactionItems = new ArrayList<>();
	private final List<RepositoryListener> listeners;
	private final ReadWriteLock listenersLock;

	private Node root;

	public RepositorySessionImpl(boolean readOnly, Engine engine, RecordContext recordContext, List<RepositoryListener> listeners, ReadWriteLock lock) {
		if (engine == null) {
			throw new IllegalArgumentException("engine is not allowed to be null");
		}
		this.engine = engine;
		if (recordContext == null) {
			throw new IllegalArgumentException("recordContext is not allowed to be null");
		}
		this.recordContext = recordContext;
		if (listeners == null) {
			listeners = Collections.EMPTY_LIST;
		}
		this.listeners = listeners;
		this.listenersLock = lock;
		this.readOnly = readOnly;
	}

	@Override
	public final boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void close() throws RepositoryException {
		listenersLock.readLock().lock();
		try {
			for (RepositoryListener listener : listeners) {
				listener.onSessionEnd(this);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	public List<RepositoryListener> getListeners() {
		return listeners;
	}

	public ReadWriteLock getListenersLock() {
		return listenersLock;
	}

	public Engine getEngine() {
		return engine;
	}

	public RecordContext getRecordContext() {
		return recordContext;
	}

	@Override
	public EntityReference createEntityReference(String typeName, long id) throws RepositoryException {
		final Record record = recordContext.create(typeName, id);
		return new EntityReferenceImpl(record);
	}

	@Override
	public Node getRoot() throws RepositoryException {
		if (root == null) {
			root = NodeImpl.createRootNode(recordContext, this, engine);
		}
		return root;
	}

	@Override
	public RepositorySession flush() throws RepositoryException {
		listenersLock.readLock().lock();
		try {
			for (RepositoryListener listener : listeners) {
				listener.onBeforeFlush(this);
			}
			Transaction tx = engine.getQueryRunner().createTransaction();
			for (TransactionItem transactionItem : transactionItems) {
				transactionItem.doWithTransaction(tx);
			}
			try {
				tx.commit();
				for (TransactionItem transactionItem : transactionItems) {
					transactionItem.afterFlush();
				}
				transactionItems.clear();
				for (RepositoryListener listener : listeners) {
					listener.onFlushSuccess(this);
				}
			} catch (SchemaException ex) {
				for (RepositoryListener listener : listeners) {
					listener.onFlushFailure(this);
				}
				throw new RepositoryException("could not flush repository session: " + ex.getMessage(), ex);
			}
		} finally {
			listenersLock.readLock().unlock();
		}
		return this;
	}
	
	public void addTransactionItem(TransactionItem item) {
		transactionItems.add(item);
	}
	
	public void removeTransactionItem(TransactionItem item) {
		Iterator<TransactionItem> iterator = transactionItems.iterator();
		while (iterator.hasNext()) {
			TransactionItem next = iterator.next();
			if (next == item) {
				iterator.remove();
			}
		}
		
	}

	@Override
	public Node getNode(Path path) throws NodeNotFoundException, RepositoryException {
		Node rootNode = getRoot();
		Node current = rootNode;
		for (String elementName : path.getElementNames()) {
			current = current.getChild(elementName);
		}
		return current;
	}

}
