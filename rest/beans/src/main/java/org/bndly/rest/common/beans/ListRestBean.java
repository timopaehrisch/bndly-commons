package org.bndly.rest.common.beans;

/*-
 * #%L
 * REST Common Beans
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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class ListRestBean<E> extends RestBean implements Iterable<E> {

	public static interface ItemHandler<E> {

		void handle(E item);
	}

	public void each(ItemHandler<E> handler) {
		List<E> r = getItems();
		if (r != null) {
			for (E e : r) {
				handler.handle(e);
			}
		}
	}

	public abstract void setItems(List<E> items);

	public abstract List<E> getItems();

	public void add(E item) {
		if (getItems() == null) {
			setItems(new ArrayList<E>());
		}
		getItems().add(item);
	}

	@Override
	public Iterator<E> iterator() {
		List<E> items = getItems();
		if (items == null) {
			return new Iterator<E>() {

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public E next() {
					throw new IllegalStateException("this collection is empty");
				}

				@Override
				public void remove() {
				}
			};
		} else {
			return items.iterator();
		}
	}

}
