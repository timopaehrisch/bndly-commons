package org.bndly.common.json.parsing;

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
import java.util.List;

public class Stack<E> {

	private List<E> elements = new ArrayList<E>();
	private int pos = 0;

	public E add(E object) {
		elements.add(pos, object);
		pos++;
		return object;
	}

	public boolean contains(E entity) {
		for (int index = 0; index < pos; index++) {
			if (elements.get(index) == entity) {
				return true;
			}
		}
		return false;
	}

	public Stack<E> reverse() {
		Stack<E> cloned = clone();
		Stack<E> tmp = new Stack<>();
		E tempElement = cloned.pop();
		while (tempElement != null) {
			tmp.add(tempElement);
			tempElement = cloned.pop();
		}
		return tmp;
	}

	public E pop() {
		if (pos > 0) {
			pos--;
			E el = elements.get(pos);
			elements.set(pos, null);
			return el;
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

	public Stack<E> clone() {
		Stack<E> stack = new Stack<E>();
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
		int index = 0;
		for (E element : elements) {
			if (index < pos) {
				sb.append(spacer);
				sb.append(element.toString());
				spacer = " -> ";
			}
			index++;
		}
		return "Stack [" + sb.toString() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Stack) {
			@SuppressWarnings("unchecked")
			Stack<E> otherStack = ((Stack<E>) obj);
			if (otherStack.size() == size()) {
				int index = 0;
				for (E element : elements) {
					if (!otherStack.elements.get(index).equals(element)) {
						return false;
					}
					index++;
				}
				return true;
			}
		}
		return false;
	}

}
