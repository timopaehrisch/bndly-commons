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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class LazyCollection<E> implements Collection<E> {

	protected final ArrayList<E> wrapped = new ArrayList<>();
	private boolean didInit;
	
	@Override
	public final int size() {
		init();
		return wrapped.size();
	}

	@Override
	public final boolean isEmpty() {
		init();
		return wrapped.isEmpty();
	}

	@Override
	public final boolean contains(Object o) {
		init();
		return wrapped.contains(o);
	}

	@Override
	public final Iterator<E> iterator() {
		init();
		return wrapped.iterator();
	}

	@Override
	public final Object[] toArray() {
		init();
		return wrapped.toArray();
	}

	@Override
	public final <T> T[] toArray(T[] a) {
		init();
		return wrapped.toArray(a);
	}

	@Override
	public final boolean add(E e) {
		init();
		return wrapped.add(e);
	}

	@Override
	public final boolean remove(Object o) {
		init();
		return wrapped.remove(o);
	}

	@Override
	public final boolean containsAll(Collection<?> c) {
		init();
		return wrapped.containsAll(c);
	}

	@Override
	public final boolean addAll(Collection<? extends E> c) {
		init();
		return wrapped.addAll(c);
	}

	@Override
	public final boolean removeAll(Collection<?> c) {
		init();
		return wrapped.removeAll(c);
	}

	@Override
	public final boolean retainAll(Collection<?> c) {
		init();
		return wrapped.retainAll(c);
	}

	@Override
	public final void clear() {
		init();
		wrapped.clear();
	}

	private void init() {
		if (didInit) {
			return;
		}
		didInit = true;
		doInit();
	}

	protected abstract void doInit();

}
