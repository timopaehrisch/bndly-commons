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

import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Engine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractRepositoryItem implements RepositoryItem {

	private final RepositorySessionImpl session;
	private final Engine engine;
	private final Record record;
	private final RecordContext recordContext;
	private TransactionItem persistItem;
	private TransactionItem removeItem;
	private List<TransactionItem> dependents;

	public AbstractRepositoryItem(RepositorySessionImpl session, Record record, Engine engine, RecordContext recordContext) {
		if (session == null) {
			throw new IllegalArgumentException("session is not allowed to be null");
		}
		this.session = session;
		if (record == null) {
			throw new IllegalArgumentException("record is not allowed to be null");
		}
		this.record = record;
		if (engine == null) {
			throw new IllegalArgumentException("engine is not allowed to be null");
		}
		this.engine = engine;
		if (recordContext == null) {
			throw new IllegalArgumentException("recordContext is not allowed to be null");
		}
		this.recordContext = recordContext;
	}

	protected AbstractRepositoryItem(RepositorySessionImpl session, Engine engine, RecordContext recordContext) {
		record = null;
		if (session == null) {
			throw new IllegalArgumentException("session is not allowed to be null");
		}
		this.session = session;
		if (engine == null) {
			throw new IllegalArgumentException("engine is not allowed to be null");
		}
		this.engine = engine;
		if (recordContext == null) {
			throw new IllegalArgumentException("recordContext is not allowed to be null");
		}
		this.recordContext = recordContext;
	}

	public final Record getRecord() {
		return record;
	}

	public final RecordContext getRecordContext() {
		return recordContext;
	}
	
	protected final List<RepositoryListener> getRepositoryListeners() {
		return getRepositorySession().getListeners();
	}
	
	protected final ReadWriteLock getRepositoryListenersLock() {
		return getRepositorySession().getListenersLock();
	}
	
	protected final boolean isReadOnly() {
		return getRepositorySession().isReadOnly();
	}
	
	public final void addDependent(TransactionItem item) {
		if (dependents == null) {
			dependents = new ArrayList<>();
		}
		dependents.add(item);
		getRepositorySession().addTransactionItem(item);
	}

	public final void removeDependent(TransactionItem item) {
		if (dependents == null) {
			return;
		}
		Iterator<TransactionItem> iter = dependents.iterator();
		boolean found = false;
		while (iter.hasNext()) {
			TransactionItem next = iter.next();
			if (next == item) {
				iter.remove();
				found = true;
			}
		}
		if (dependents.isEmpty()) {
			dependents = null;
		}
		if (found) {
			getRepositorySession().removeTransactionItem(item);
		}
	}
	
	@Override
	public boolean isTransient() {
		return getRecord() != null && getRecord().getId() == null;
	}

	@Override
	public final RepositorySessionImpl getRepositorySession() {
		return session;
	}

	public final Engine getEngine() {
		return engine;
	}
	
	public final Accessor getAccessor() {
		return engine.getAccessor();
	}

	protected final TransactionItem createPersist(final AbstractRepositoryItem parentItem) {
		if (removeItem != null) {
			removeItem.skip();
			removeItem = null;
		}
		if (persistItem == null) {
			persistItem = new TransactionItem() {
				private boolean skipped;

				@Override
				public void doWithTransaction(Transaction transaction) {
					if (record.getId() == null) {
						getAccessor().buildInsertQuery(record, transaction);
					} else {
						getAccessor().buildUpdateQuery(record, transaction);
					}
				}

				@Override
				public void afterFlush() {
					persistItem = null;
					afterCreate();
				}

				@Override
				public void skip() {
					skipped = true;
					persistItem = null;
					parentItem.removeDependent(this);
				}

				@Override
				public boolean skipped() {
					return skipped;
				}
				
			};
			parentItem.addDependent(persistItem);
		}
		return persistItem;
	}

	protected final TransactionItem createRemovable(final AbstractRepositoryItem parentItem) {
		if (persistItem != null) {
			persistItem.skip();
			persistItem = null;
		}
		if (removeItem == null) {
			removeItem = new TransactionItem() {
				private boolean skipped;

				@Override
				public void doWithTransaction(Transaction transaction) throws RepositoryException {
					if (record.getId() != null) {
						getAccessor().buildDeleteQuery(record, transaction);
					}
				}

				@Override
				public void afterFlush() {
					removeItem = null;
					afterRemove();
				}

				@Override
				public void skip() {
					skipped = true;
					removeItem = null;
					parentItem.removeDependent(this);
				}

				@Override
				public boolean skipped() {
					return skipped;
				}
				
			};
			parentItem.addDependent(removeItem);
		}
		return removeItem;
	}
	
	public final boolean isRemovalScheduled() {
		TransactionItem tmp = removeItem;
		return tmp != null && !tmp.skipped();
	}
	
	public final boolean isPersistenceScheduled() {
		TransactionItem tmp = persistItem;
		return tmp != null && !tmp.skipped();
	}
	
	protected void afterRemove() {}
	protected void afterCreate() {}
}
