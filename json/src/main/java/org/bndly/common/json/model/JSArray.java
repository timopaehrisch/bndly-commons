package org.bndly.common.json.model;

/*-
 * #%L
 * JSON
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
import java.util.List;

public final class JSArray extends JSValue implements Iterable<JSValue> {

	private List<JSValue> items;
	private static final Iterator<JSValue> EMPTY_ITERATOR = new Iterator<JSValue>() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public JSValue next() {
			return null;
		}

		@Override
		public void remove() {
		}
	};

	public final List<JSValue> getItems() {
		return items;
	}

	public final void setItems(List<JSValue> items) {
		this.items = items;
	}

	public final void add(JSValue item) {
		if (item == null) {
			throw new IllegalArgumentException("item to add should not be null");
		}
		if (getItems() == null) {
			setItems(new ArrayList<JSValue>());
		}
		getItems().add(item);
	}

	public final boolean remove(JSValue item) {
		if (item == null) {
			throw new IllegalArgumentException("item to remove should not be null");
		}
		if (getItems() == null) {
			return false;
		}
		return getItems().remove(item);
	}
	
	public final int size() {
		if (getItems() == null) {
			return 0;
		}
		return getItems().size();
	}
	
	@Override
	public Iterator<JSValue> iterator() {
		if (items == null) {
			return EMPTY_ITERATOR;
		} else {
			return items.iterator();
		}
	}

}
