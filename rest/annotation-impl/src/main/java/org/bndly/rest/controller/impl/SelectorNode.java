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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SelectorNode extends Node<SelectorNode> {

	private final String name;
	private final ExtensionNode extensionNode;

	public SelectorNode(Node parent) {
		this(parent, null);
	}

	public SelectorNode(Node parent, String name) {
		super(parent);
		this.name = name;
		this.extensionNode = new ExtensionNode(this);
	}

	public ExtensionNode getExtensionNode() {
		return extensionNode;
	}

	public boolean isEmpty() {
		return name == null;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean hasBindings() {
		if (hasChildren()) {
			for (SelectorNode selectorNode : getChildren()) {
				if (selectorNode.hasBindings()) {
					return true;
				}
			}
		}
		return extensionNode.hasBindings();
	}

	@Override
	public void removeChild(Node child) {
		if (SelectorNode.class.isInstance(child)) {
			_removeChild((SelectorNode) child);
			if (!hasChildren()) {
				parent.removeChild(this);
			}
		} else if (ExtensionNode.class.isInstance(child)) {
			if (!hasChildren()) {
				parent.removeChild(this);
			}
		} else {
			throw new IllegalStateException("selector nodes do only contain selector nodes or one extension node");
		}
	}
	
}
