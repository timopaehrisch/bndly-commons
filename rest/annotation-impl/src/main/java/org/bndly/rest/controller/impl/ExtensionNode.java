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

public class ExtensionNode extends Node<ExtensionNode> {

	private final String name;
	private final boolean variable;
	private final MethodNode methodNode;

	public ExtensionNode(Node parent) {
		this(parent, null);
	}

	public ExtensionNode(Node parent, String name) {
		super(parent);
		this.name = name;
		this.variable = PathNode.isVariable(name);
		methodNode = new MethodNode(this);
	}

	public MethodNode getMethodNode() {
		return methodNode;
	}

	@Override
	public void addChild(ExtensionNode child) {
		if (name != null) {
			throw new IllegalStateException("extensions can only be used in a flat node hierarchy");
		}
		super.addChild(child);
	}

	public String getName() {
		return name;
	}
	
	public boolean isVariable() {
		return variable;
	}
	
	@Override
	public boolean hasBindings() {
		return methodNode.hasBindings();
	}

	@Override
	public void removeChild(Node child) {
		if (ExtensionNode.class.isInstance(child)) {
			_removeChild((ExtensionNode) child);
			if (!hasChildren()) {
				parent.removeChild(this);
			}
		} else if (MethodNode.class.isInstance(child)) {
			if (!hasChildren()) {
				parent.removeChild(this);
			}
		} else {
			throw new IllegalStateException("extension nodes do only contain method nodes or 1 layer of other extension nodes");
		}
	}
	
}
