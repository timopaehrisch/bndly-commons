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
public class NullRemovingIterator<E> implements Iterator<E> {

	private final Iterator<E> wrapped;
	private E current;

	public NullRemovingIterator(Iterator<E> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public boolean hasNext() {
		while (current == null && wrapped.hasNext()) {
			current = wrapped.next();
		}
		return current != null;
	}

	@Override
	public E next() {
		if (!hasNext()) {
			return null;
		}
		E tmp = current;
		current = null;
		return tmp;
	}

	@Override
	public void remove() {
		wrapped.remove();
	}
	
}
