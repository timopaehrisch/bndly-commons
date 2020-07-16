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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ChildManager<E> {
	private final ChildManagingItem childManagingItem;
	private List<E> loadedItems;

	public static interface ChildManagingItem<E> {
		boolean isTransient();
		Iterator<E> loadChildren() throws RepositoryException;
		Iterator<E> getChildren() throws RepositoryException;
	}
	
	public ChildManager(ChildManagingItem<E> childManagingItem) {
		if (childManagingItem == null) {
			throw new IllegalArgumentException("childManagingItem is not allowed to be null");
		}
		this.childManagingItem = childManagingItem;
	}
	
	public Iterator<E> getChildren() throws RepositoryException {
		if (childManagingItem.isTransient()) {
			return childManagingItem.getChildren();
		} else {
			if (loadedItems != null) {
				return loadedItems.iterator();
			} else {
				final List<E> tmp = new ArrayList<>();
				final Iterator<E> iter = childManagingItem.loadChildren();
				return new Iterator<E>() {
					@Override
					public boolean hasNext() {
						boolean r = iter.hasNext();
						if (!r) {
							loadedItems = tmp;
						}
						return r;
					}

					@Override
					public E next() {
						E r = iter.next();
						tmp.add(r);
						return r;
					}

					@Override
					public void remove() {
						iter.remove();
					}
				};
			}
		}
	}
}
