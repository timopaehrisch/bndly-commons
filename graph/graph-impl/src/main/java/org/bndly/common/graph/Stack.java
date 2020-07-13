package org.bndly.common.graph;

/*-
 * #%L
 * Graph Impl
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

public class Stack<E> {

	private final List<E> elements = new ArrayList<>();
	private int pos = 0;

	public void add(E object) {
		elements.add(pos, object);
		pos++;
	}

	public boolean contains(E entity) {
		for (int i = 0; i < pos; i++) {
			if (elements.get(i) == entity) {
				return true;
			}
		}
		return false;
	}

	public Stack<E> reverse() {
		Stack<E> c = clone();
		Stack<E> tmp = new Stack<>();
		E t = c.pop();
		while (t != null) {
			tmp.add(t);
			t = c.pop();
		}
		return tmp;
	}

	public E pop() {
		if (pos > 0) {
			pos--;
			return elements.get(pos);
		} else {
			return null;
		}
	}

	public E top() {
		if (size() > 0) {
			return elements.get(pos - 1);
		} else {
			return null;
		}
	}

	public int size() {
		return pos;
	}

	@Override
	public Stack<E> clone() {
		Stack<E> stack = new Stack<>();
		if (elements != null) {
			for (E element : elements) {
				stack.add(element);
			}
		}
		stack.pos = pos;
		return stack;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String spacer = "";
		int i = 0;
		for (E e : elements) {
			if (i < pos) {
				sb.append(spacer);
				sb.append(e.toString());
				spacer = " -> ";
			}
			i++;
		}
		return "Stack [" + sb.toString() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Stack) {
			@SuppressWarnings("unchecked")
			Stack<E> otherStack = ((Stack<E>) obj);
			if (otherStack.size() == size()) {
				int i = 0;
				for (E element : elements) {
					if (!otherStack.elements.get(i).equals(element)) {
						return false;
					}
					i++;
				}
				return true;
			}
		}
		return false;
	}

}
