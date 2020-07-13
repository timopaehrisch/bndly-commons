package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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

public abstract class Node<E extends Node> {
	private List<E> children;
	protected final Node parent;

	public Node(Node parent) {
		this.parent = parent;
	}

	public Node getParent() {
		return parent;
	}

	public List<E> getChildren() {
		return children;
	}

	public void addChild(E child) {
		if (children == null) {
			children = new ArrayList<>();
		}
		children.add(child);
	}

	public abstract void removeChild(Node child);

	protected void _removeChild(E child) {
		if (children == null) {
			return;
		}
		children.remove(child);
	}
	
	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}
	
	public abstract boolean hasBindings();

	public void removeFromTree() {
		if (parent != null) {
			parent.removeChild(this);
		}
	}
}
