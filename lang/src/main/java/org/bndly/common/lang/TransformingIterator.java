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

import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class TransformingIterator<E,F> implements Iterator<E> {

	private final Iterator<F> wrapped;
	private E current;

	public TransformingIterator(Iterator<F> wrapped) {
		this.wrapped = wrapped;
	}
	
	protected abstract E transform(F toTransform);

	protected boolean isAccepted(E toCheck) {
		return true;
	}

	@Override
	public boolean hasNext() {
		if (current == null) {
			findNext();
		}
		return current != null;
	}

	@Override
	public E next() {
		findNext();
		E t = current;
		current = null;
		return t;
	}

	private void findNext() {
		if (current == null) {
			if (wrapped.hasNext()) {
				current = transform(wrapped.next());
				if (!isAccepted(current)) {
					current = null;
					findNext();
				}
			}
		}
	}

	@Override
	public void remove() {
		wrapped.remove();
	}

}
