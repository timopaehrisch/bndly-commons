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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class ControllerNode {

	private final String pathElement;
	private Map<String, ControllerNode> children;
	private ControllerNode variableChild;
	private List<ControllerBinding> bindingGET;
	private List<ControllerBinding> bindingPOST;
	private List<ControllerBinding> bindingPUT;
	private List<ControllerBinding> bindingDELETE;
	private List<ControllerBinding> bindingHEAD;
	private List<ControllerBinding> bindingOPTIONS;

	public ControllerNode(String pathElement) {
		this.pathElement = pathElement;
	}

	void addChildControllerNode(ControllerNode n, boolean variable) {
		if (n != null && n.pathElement != null) {
			if (variable) {
				if (children != null && !children.isEmpty()) {
					for (ControllerNode controllerNode : children.values()) {
						Iterator<ControllerBinding> bindings = controllerNode.getBindings();
						while (bindings.hasNext()) {
							ControllerBinding controllerBinding = bindings.next();
							HTTPMethod method = controllerBinding.getHTTPMethod();
							if (n.getBindings(method) != null) {
								throw new DoubleBoundControllerException("a variable controller is conflicting the currently added controller");
							}
						}
					}
				}
				if (variableChild != null) {
					throw new DoubleBoundControllerException("a variable controller is conflicting the currently added controller");
				}
				variableChild = n;
			} else {
				if (variableChild != null) {
					throw new DoubleBoundControllerException("a variable controller is conflicting the currently added controller");
				}
				if (children == null) {
					children = new HashMap<>();
				}
				children.put(n.pathElement, n);
			}
		}
	}

	boolean hasChildControllerNode(String pathElement) {
		if (children == null || children.isEmpty()) {
			return false;
		}
		return children.get(pathElement) != null;
	}

	ControllerNode getVariableChild() {
		return variableChild;
	}

	ControllerNode getChild(String pathElement) {
		if (children == null) {
			return null;
		}
		return children.get(pathElement);
	}

	boolean hasBindings() {
		return bindingGET != null || bindingPOST != null || bindingPUT != null || bindingDELETE != null || bindingHEAD != null || bindingOPTIONS != null;
	}

	boolean isVariableSwitchNode() {
		return variableChild != null;
	}

	Iterator<ControllerBinding> getBindings() {
		final HTTPMethod[] v = HTTPMethod.values();
		return new Iterator<ControllerBinding>() {
			int i = 0;
			int k = 0;

			private boolean hasValueFor(HTTPMethod m) {
				return getBindings(m) != null;
			}

			@Override
			public boolean hasNext() {
				for (int j = i; j < v.length; j++) {
					if (hasValueFor(v[j])) {
						List<ControllerBinding> bindings = getBindings(v[j]);
						return bindings.size() > k;
					}
				}
				return false;
			}

			@Override
			public ControllerBinding next() {
				for (int j = i; j < v.length; j++) {
					if (hasValueFor(v[j])) {
						List<ControllerBinding> bindings = getBindings(v[j]);
						if (k < bindings.size()) {
							ControllerBinding binding = bindings.get(k);
							k++;
							return binding;
						}
						if (k == bindings.size()) {
							i = j;
						}
					}
				}
				return null;
			}

			@Override
			public void remove() {
				// not supported
			}
		};
	}

	List<ControllerBinding> getBindings(HTTPMethod method) {
		switch (method) {
			case GET:
				return bindingGET;
			case POST:
				return bindingPOST;
			case PUT:
				return bindingPUT;
			case DELETE:
				return bindingDELETE;
			case HEAD:
				return bindingHEAD;
			case OPTIONS:
				return bindingOPTIONS;
			default:
				throw new IllegalArgumentException("unsupported http method: " + method.name());
		}
	}

	void addBinding(ControllerBinding binding) {
		HTTPMethod method = binding.getHTTPMethod();
		switch (method) {
			case GET:
				bindingGET = appendToList(bindingGET, binding);
				break;
			case POST:
				bindingPOST = appendToList(bindingPOST, binding);
				break;
			case PUT:
				bindingPUT = appendToList(bindingPUT, binding);
				break;
			case DELETE:
				bindingDELETE = appendToList(bindingDELETE, binding);
				break;
			case HEAD:
				bindingHEAD = appendToList(bindingHEAD, binding);
				break;
			case OPTIONS:
				bindingOPTIONS = appendToList(bindingOPTIONS, binding);
				break;
			default:
				// do nothing
		}
	}

	private List<ControllerBinding> appendToList(List<ControllerBinding> list, ControllerBinding binding) {
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(binding);
		return list;
	}

}
