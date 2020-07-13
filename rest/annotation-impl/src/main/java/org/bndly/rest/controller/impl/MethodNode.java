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

import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.api.HTTPMethod;

public class MethodNode extends Node<MethodNode> {

	private final HTTPMethod httpMethod;
	private final ControllerBinding binding;

	public MethodNode(Node parent) {
		this(parent, null, null);
	}

	public MethodNode(Node parent, HTTPMethod httpMethod, ControllerBinding binding) {
		super(parent);
		if (httpMethod == null && binding != null) {
			throw new IllegalArgumentException("a method node is either nulled completly or has http method and controller binding.");
		}
		if (httpMethod != null && binding == null) {
			throw new IllegalArgumentException("a method node is either nulled completly or has http method and controller binding.");
		}
		this.httpMethod = httpMethod;
		this.binding = binding;
	}

	public ControllerBinding getBinding() {
		return binding;
	}

	@Override
	public void addChild(MethodNode child) {
		if (httpMethod != null) {
			throw new IllegalStateException("http methods can not be used hierarchically");
		}
		super.addChild(child);
	}

	public HTTPMethod getHttpMethod() {
		return httpMethod;
	}

	@Override
	public void removeChild(Node child) {
		if (MethodNode.class.isInstance(child)) {
			_removeChild((MethodNode) child);
			if (!hasChildren()) {
				parent.removeChild(this);
			}
		} else {
			throw new IllegalStateException("method nodes do only contain method nodes");
		}
	}

	@Override
	public boolean hasBindings() {
		if (hasChildren()) {
			for (MethodNode methodNode : getChildren()) {
				if (methodNode.hasBindings()) {
					return true;
				}
			}
			return false;
		} else {
			return binding != null;
		}
	}

}
