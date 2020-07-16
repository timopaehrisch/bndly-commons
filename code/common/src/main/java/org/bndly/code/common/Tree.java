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

import java.util.List;


public class Tree<E> {
	private final TreeNode<E> root;

	public Tree(E root) {
		this.root = new TreeNode<>(root);
	}

	public Tree(TreeNode<E> root) {
		this.root = root;
	}

	public Tree() {
		root = new TreeNode<>(null);
	}

	public TreeNode<E> getRoot() {
		return root;
	}

	public void traverse(TreeTraversionDelegate<E> delegate) {
		handleNodeForTraversionDelegate(root, delegate);

	}

	private void handleNodeForTraversionDelegate(TreeNode<E> node, TreeTraversionDelegate<E> delegate) {
		delegate.stepOver(this, node, node.getValue());
		List<TreeNode<E>> children = node.getChildren();
		if (children != null) {
			delegate.beforeStepInto(this, node, node.getValue());
			for (TreeNode<E> child : children) {
				handleNodeForTraversionDelegate(child, delegate);
			}
			delegate.afterStepOut(this, node, node.getValue());
		}
	}

	@Override
	public String toString() {
		return root.toString();
	}
}
