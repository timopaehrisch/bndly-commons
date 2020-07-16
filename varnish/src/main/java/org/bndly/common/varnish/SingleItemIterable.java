package org.bndly.common.varnish;

/*-
 * #%L
 * Varnish
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
import java.util.NoSuchElementException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SingleItemIterable<T> implements Iterable<T> {

	private final T item;

	public SingleItemIterable(T item) {
		this.item = item;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private boolean didRead = false;

			@Override
			public boolean hasNext() {
				return !didRead;
			}

			@Override
			public T next() {
				if (didRead) {
					throw new NoSuchElementException();
				}
				didRead = true;
				return item;
			}

			@Override
			public void remove() {
				// no-op
			}

		};
	}

}
