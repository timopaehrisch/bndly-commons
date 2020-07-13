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

public class PathNode extends Node<PathNode> {

	private final String elementName;
	private final boolean variable;
	private final SelectorNode selectorNode;

	public PathNode(PathNode parent) {
		this(parent, null);
	}

	public static final boolean isVariable(String elementName) {
		return elementName != null && elementName.length() > 2 && elementName.charAt(0) == '{' && elementName.charAt(elementName.length() - 1) == '}';
	}
	
	public PathNode(PathNode parent, String elementName) {
		super(parent);
		this.elementName = elementName;
		this.variable = isVariable(elementName);
		selectorNode = new SelectorNode(this);
	}

	public SelectorNode getSelectorNode() {
		return selectorNode;
	}

	public String getElementName() {
		return elementName;
	}

	public boolean isVariable() {
		return variable;
	}

	public boolean isRoot() {
		return elementName == null;
	}

	@Override
	public boolean hasBindings() {
		if (hasChildren()) {
			for (PathNode pathNode1 : getChildren()) {
				if (pathNode1.hasBindings()) {
					return true;
				}
			}
		}
		return selectorNode.hasBindings();
	}

	@Override
	public void removeChild(Node child) {
		if (PathNode.class.isInstance(child)) {
			_removeChild((PathNode) child);
			if (!hasChildren() && !isRoot()) {
				if (parent == null) {
					// this is the case, if this is the root path node
					return;
				}
				parent.removeChild(this);
			}
		} else if (SelectorNode.class.isInstance(child)) {
			if (!hasChildren()) {
				if (parent == null) {
					// this is the case, if this is the root path node
					return;
				}
				parent.removeChild(this);
			}
		} else {
			throw new IllegalStateException("path nodes do only contain path nodes or one selector node");
		}
	}
	
}
