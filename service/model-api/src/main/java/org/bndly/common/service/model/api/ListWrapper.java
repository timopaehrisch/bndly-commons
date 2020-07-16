package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListWrapper<M> implements List<M>, Serializable {

	private final List<M> list = new ArrayList<>();

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<M> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		return list.toArray(ts);
	}

	@Override
	public boolean add(M e) {
		return list.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> clctn) {
		return list.containsAll(clctn);
	}

	@Override
	public boolean addAll(Collection<? extends M> clctn) {
		return list.addAll(clctn);
	}

	@Override
	public boolean addAll(int i, Collection<? extends M> clctn) {
		return list.addAll(i, clctn);
	}

	@Override
	public boolean removeAll(Collection<?> clctn) {
		return list.removeAll(clctn);
	}

	@Override
	public boolean retainAll(Collection<?> clctn) {
		return list.removeAll(clctn);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public M get(int i) {
		return list.get(i);
	}

	@Override
	public M set(int i, M e) {
		return list.set(i, e);
	}

	@Override
	public void add(int i, M e) {
		list.add(i, e);
	}

	@Override
	public M remove(int i) {
		return list.remove(i);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<M> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<M> listIterator(int i) {
		return list.listIterator(i);
	}

	@Override
	public List<M> subList(int i, int i1) {
		return list.subList(i, i1);
	}

}
