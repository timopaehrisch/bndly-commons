package org.bndly.common.antivirus.impl;

/*-
 * #%L
 * Antivirus Impl
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class Pool<T> {

	private int maxSize = 1;

	public static interface Callback<E, T> {

		E handle(T poolItem);

		E onInterruption();

		long getTimeOutMillis();
	}

	private final List<T> createdItems = new ArrayList<>();
	private BlockingQueue<T> queue;

	public final synchronized void init() {
		queue = new LinkedBlockingQueue<>(maxSize);
		for (int i = 0; i < maxSize; i++) {
			T item = createItem();
			if (item == null) {
				throw new IllegalStateException("createItem is not allowed to return null");
			}
			createdItems.add(item);
			queue.add(item);
		}
	}

	public final synchronized void destruct() {
		for (T createdItem : createdItems) {
			destroyItem(createdItem);
		}
		createdItems.clear();
		queue.clear();
	}

	public final void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	protected abstract T createItem();

	protected abstract void destroyItem(T item);

	/**
	 * this method is invoked before handing out an item of the pool. it can be
	 * used to filter out broken items and replace them with new instances.
	 *
	 * @param item the item from the original queue
	 * @return never null. either the item from the internal item queue or a
	 * fixed item, if the validation failed.
	 */
	protected T validateItem(T item) {
		return item;
	}

	private class GetCallback<T> implements Callback<T,T> {

		private final long timeoutInMillis;
		private T item;
		private boolean interrupted;

		public GetCallback(long timeoutInMillis) {
			this.timeoutInMillis = timeoutInMillis;
		}

		@Override
		public T handle(T poolItem) {
			this.item = poolItem;
			return item;
		}

		@Override
		public T onInterruption() {
			interrupted = true;
			return null;
		}

		@Override
		public long getTimeOutMillis() {
			return timeoutInMillis;
		}

		public T getItem() {
			return item;
		}

		public boolean isInterrupted() {
			return interrupted;
		}
		
	}
	
	public final T get() throws PoolExhaustedException {
		return get(-1);
	}
	
	public final T get(long timeoutInMillis) throws PoolExhaustedException {
		GetCallback<T> cb = new GetCallback<>(timeoutInMillis);
		doWithPooledItemWithoutReturningToPool(cb);
		if (cb.isInterrupted()) {
			return null;
		} else {
			T poolItem = validateItem(cb.getItem());
			if (poolItem == null) {
				throw new IllegalStateException("item was null after validation");
			}
			return poolItem;
		}
		
	}

	public final void put(T item) {
		if (!queue.offer(item)) {
			// failed to put the item back to the queue immediatly
			throw new IllegalStateException("pool size exceeded");
		}
	}

	public final <E> E doWithPooledItemWithoutReturningToPool(Callback<E, T> cb) throws PoolExhaustedException {
		return internalDoWithPooledItem(cb, false);
	}
	
	public final <E> E doWithPooledItem(Callback<E, T> cb) throws PoolExhaustedException {
		return internalDoWithPooledItem(cb, true);
	}
	
	private <E> E internalDoWithPooledItem(Callback<E, T> cb, boolean returnItemAfterPollOrTake) throws PoolExhaustedException {
		if (cb == null) {
			return null;
		}
		try {
			long millis = cb.getTimeOutMillis();
			T poolItem;
			if(millis > -1) {
				poolItem = queue.poll(millis, TimeUnit.MILLISECONDS);
				if (poolItem == null) {
					throw new PoolExhaustedException("pool has size " + maxSize + " and could not provide an item");
				}
			} else {
				poolItem = queue.take();
			}
			
			
			try {
				return cb.handle(poolItem);
			} finally {
				if (returnItemAfterPollOrTake) {
					try {
						queue.put(poolItem);
					} catch (InterruptedException e) {
						// we have been interrupted while putting the pool item back in the pool
						// for the callback it is too late to compensate.
					}
				}
			}
		} catch (InterruptedException ex) {
			// we have been interrupted while waiting for the pool item.
			return cb.onInterruption();
		}
	}
}
