package org.bndly.code.common;

/*-
 * #%L
 * Code Common
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

public class TreeNode<E> {
	private E value;
	private List<TreeNode<E>> subNodes;
	private TreeNode<E> parent;
	public TreeNode(E e) {
		value = e;
	}

	public E getValue() {
		return value;
	}

	public void setValue(E value) {
		this.value = value;
	}

	public TreeNode<E> add(E e) {
		if (subNodes == null) {
			subNodes = new ArrayList<>();
		}
		TreeNode<E> n = new TreeNode<>(e);
		n.parent = this;
		subNodes.add(n);
		return n;
	}

	public List<TreeNode<E>> getChildren() {
		if (subNodes != null) {
			return new ArrayList<>(subNodes);
		}
		return null;
	}

	public TreeNode<E> getNodeOf(E e) {
		if (subNodes != null) {
			for (TreeNode<E> node : subNodes) {
				if (node.getValue() == e) {
					return node;
				}
			}
		}
		return null;
	}

	public TreeNode<E> searchNodeOf(E e) {
		if (subNodes != null) {
			TreeNode<E> result = null;
			for (TreeNode<E> node : subNodes) {
				if (node.getValue() == e) {
					result = node;
					break;
				} else {
					result = node.searchNodeOf(e);
					if (result != null) {
						break;
					}
				}
			}
			return result;
		}
		return null;
	}

	public TreeNode<E> getParent() {
		return parent;
	}

	public boolean isLeaf() {
		return subNodes == null;
	}

	private String asString(int indent) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		sb.append(value);
		if (subNodes != null) {
			for (TreeNode<E> sub : subNodes) {
				sb.append('\n');
				sb.append(sub.asString(indent + 1));
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return asString(0);
	}
	
}
