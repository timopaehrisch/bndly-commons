package org.bndly.common.lang;

/*-
 * #%L
 * Lang
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class IteratorChain<E> implements Iterator<E> {

	private final Iterator<E>[] iterators;
	private int pos;

	public static <E> IteratorChain<E> fromIterators(Iterator<Iterator<E>> iterators) {
		ArrayList<Iterator<E>> arrayList = new ArrayList<>();
		while (iterators.hasNext()) {
			Iterator<E> next = iterators.next();
			arrayList.add(next);
		}
		Iterator[] arr = arrayList.toArray(new Iterator[arrayList.size()]);
		return new IteratorChain<E>(arr);
	}
	
	public IteratorChain(Iterator<E>... iterators) {
		this.iterators = iterators;
		pos = 0;
	}

	@Override
	public boolean hasNext() {
		if (pos >= iterators.length) {
			return false;
		}
		boolean tmp = iterators[pos].hasNext();
		if (!tmp) {
			pos++;
			return hasNext();
		}
		return tmp;
	}

	@Override
	public E next() {
		if (hasNext()) {
			return iterators[pos].next();
		} else {
			return null;
		}
	}

	@Override
	public void remove() {
		if (iterators.length > pos) {
			iterators[pos].remove();
		}
	}

}
