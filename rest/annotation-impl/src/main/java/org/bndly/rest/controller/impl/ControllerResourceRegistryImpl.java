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

import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.PUT;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.controller.api.ControllerResourceRegistryListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ControllerResourceRegistry.class)
public class ControllerResourceRegistryImpl implements ControllerResourceRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(ControllerResourceRegistryImpl.class);
	
	@Reference
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;
	
	private interface DestructionLogic {
		public void destruct();
	}
	
	private class KnownController {
		private final Object controller;
		private final String baseUri;
		private final List<DestructionLogic> destructionLogic = new ArrayList<>();
		private final List<ControllerBinding> bindings = new ArrayList<>();

		public KnownController(Object controller, String baseUri) {
			this.controller = controller;
			this.baseUri = baseUri;
		}

		public Object getController() {
			return controller;
		}

		public List<DestructionLogic> getDestructionLogic() {
			return destructionLogic;
		}

		public List<ControllerBinding> getBindings() {
			return bindings;
		}

		public String getBaseUri() {
			return baseUri;
		}
		
	}
	
	private final List<KnownController> knownControllers = new ArrayList<>();
	private final List<ControllerBinding> bindings = new ArrayList<>();
	private final PathNode controllerTree = new PathNode(null);
	private final ReadWriteLock controllerLock = new ReentrantReadWriteLock();
	
	private final List<ControllerResourceRegistryListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ControllerResourceRegistryListener.class,
			bind = "addControllerResourceRegistryListener",
			unbind = "removeControllerResourceRegistryListener"
	)
	@Override
	public void addControllerResourceRegistryListener(ControllerResourceRegistryListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(0, listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
			listener.boundTo(this);
		}
	}

	@Override
	public void removeControllerResourceRegistryListener(ControllerResourceRegistryListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
			listener.unboundFrom(this);
		}
	}

	/*
	 Idee: Controller Baum aufbauen um dann schnell einen Aufruf an den richtigen 
	 Controller weiterleiten zu können.
	 */
	@Override
	public void deploy(Object controller) {
		deploy(controller, null);
	}

	@Override
	public void deploy(Object controller, String baseURI) {
		controllerLock.writeLock().lock();
		try {
			if (baseURI != null) {
				LOG.info("deploying controller {} with base uri {}", controller.getClass().getName(), baseURI);
			} else {
				LOG.info("deploying controller {}", controller.getClass().getName());
			}
			KnownController kc = new KnownController(controller, baseURI);
			buildControllerBindings(controller.getClass(), kc, baseURI);
			knownControllers.add(kc);
			listenersLock.readLock().lock();
			try {
				for (ControllerResourceRegistryListener controllerResourceRegistryListener : listeners) {
					controllerResourceRegistryListener.deployedController(controller, controller.getClass(), kc.getBindings(), baseURI, this);
				}
			} finally {
				listenersLock.readLock().unlock();
			}
		} catch (DoubleBoundControllerException e) {
			for (KnownController knownController : knownControllers) {
				if (knownController.getController() == controller) {
					return;
				}
			}
			LOG.error("deployment of controller {} with base uri {} failed", controller.getClass().getName(), baseURI, e);
			throw e;
		} catch (Exception e) {
			LOG.error("deployment of controller {} with base uri {} failed", controller.getClass().getName(), baseURI, e);
		} finally {
			controllerLock.writeLock().unlock();
		}
	}

	@Override
	public ControllerBinding resolveBindingForResourceURI(ResourceURI uri, HTTPMethod httpMethod) {
		PathNode node = findPathNodeForUri(controllerTree, 0, uri);
		if (node == null) {
			// path could not be found in nodes
			return null;
		}
		SelectorNode sNode = findSelectorNodeInPathNodeForResourceURI(node, uri);
		if (sNode == null) {
			return null;
		}
		ExtensionNode eNode = findExtensionNodeInSelectorNodeForResourceURI(sNode, uri);
		if (eNode == null) {
			return null;
		}
		MethodNode mNode = findMethodNodeInExtensionNode(eNode, httpMethod);
		if (mNode == null) {
			return null;
		}
		return mNode.getBinding();
	}

	private boolean controllerBindingAppliesToUri(ControllerBinding controllerBinding, ResourceURI uri) {
		ResourceURI pattern = controllerBinding.getResourceURIPattern();
		List<ResourceURI.Selector> patternSelectors = pattern.getSelectors();
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (patternSelectors != null) {
			if (selectors == null) {
				return false;
			}
			if (patternSelectors.size() != selectors.size()) {
				return false;
			}
			for (int i = 0; i < selectors.size(); i++) {
				ResourceURI.Selector selector = selectors.get(i);
				ResourceURI.Selector patternSelector = patternSelectors.get(i);
				if (!selector.getName().equals(patternSelector.getName())) {
					return false;
				}
			}
		}
		ResourceURI.Extension patternExtension = controllerBinding.getResourceURIPattern().getExtension();
		ResourceURI.Extension ext = uri.getExtension();
		if (patternExtension != null) {
			if (ext == null) {
				return false;
			}
			return ext.getName().equalsIgnoreCase(patternExtension.getName());
		} else {
			if (ext != null) {
				return false;
			}
		}
		return true;
	}

	private void buildControllerBindings(Class<?> controllerType, KnownController knownController, String baseURI) {
		Object controller = knownController.getController();
		Path classPath = controllerType.getAnnotation(Path.class);
		for (Method method : controllerType.getMethods()) {
			Path methodPath = method.getAnnotation(Path.class);
			// if there is no method path, the method should at leas have a HTTP method assigned
			List<Annotation> httpMethods = getHttpMethodForJavaMethod(method);
			if (httpMethods == null) {
				continue;
			}
			String pathToMethod = joinPathElements(baseURI, classPath, methodPath);
			if (pathToMethod != null) {
				// iterate the path to method and split it into static and variable elements
				ResourceURI uri = new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), pathToMethod).parse().getResourceURI();
				for (Annotation httpMethodAnnotation : httpMethods) {
					ControllerBinding binding = createBinding(controllerType, method, controller, baseURI, httpMethodAnnotation, uri);
					PathNode pn = appendToPathNode(0, controllerTree, binding, uri);
					SelectorNode sn = appendToSelectorNode(0, pn.getSelectorNode(), binding, uri);
					ExtensionNode en = appendToExtensionNode(sn.getExtensionNode(), binding, uri);
					final MethodNode mn = appendToMethodNode(en.getMethodNode(), binding);
					knownController.getDestructionLogic().add(new DestructionLogic() {

						@Override
						public void destruct() {
							mn.removeFromTree();
						}
					});
					knownController.getBindings().add(binding);
					controllerLock.writeLock().lock();
					try {
						bindings.add(binding);
					} finally {
						controllerLock.writeLock().unlock();
					}
				}
			}
		}
	}

	private SelectorNode appendToSelectorNode(int currentIndex, SelectorNode selectorNode, ControllerBinding binding, ResourceURI uri) {
		if (uri.getSelectors() != null) {
			if (currentIndex < uri.getSelectors().size()) {
				ResourceURI.Selector currentSelector = uri.getSelectors().get(currentIndex);
				if (selectorNode.getChildren() != null) {
					for (SelectorNode sn : selectorNode.getChildren()) {
						if (sn.getName().equals(currentSelector.getName())) {
							return appendToSelectorNode(currentIndex + 1, sn, binding, uri);
						}
					}
				}
				SelectorNode sn = new SelectorNode(selectorNode, currentSelector.getName());
				selectorNode.addChild(sn);
				return appendToSelectorNode(currentIndex + 1, sn, binding, uri);
			} else {
				return selectorNode;
			}
		} else {
			return selectorNode;
		}
	}

	private ExtensionNode appendToExtensionNode(ExtensionNode extensionNode, ControllerBinding binding, ResourceURI uri) {
		if (uri.getExtension() != null) {
			if (extensionNode.getChildren() != null) {
				for (ExtensionNode m : extensionNode.getChildren()) {
					if (m.getName().equals(uri.getExtension().getName())) {
						return m;
					}
				}
			}
			ExtensionNode en = new ExtensionNode(extensionNode, uri.getExtension().getName());
			extensionNode.addChild(en);
			return en;
		} else {
			return extensionNode;
		}
	}

	private MethodNode appendToMethodNode(MethodNode methodNode, ControllerBinding binding) {
		if (methodNode.getChildren() != null) {
			for (MethodNode m : methodNode.getChildren()) {
				if (m.getHttpMethod().equals(binding.getHTTPMethod())) {
					throw new DoubleBoundControllerException();
				}
			}
		}
		MethodNode mn = new MethodNode(methodNode, binding.getHTTPMethod(), binding);
		methodNode.addChild(mn);
		return mn;
	}

	private PathNode appendToPathNode(int currentIndex, PathNode currentNode, ControllerBinding binding, ResourceURI uri) {
		ResourceURI.Path p = uri.getPath();
		if (p != null && currentIndex < p.getElements().size()) {
			String currentElement = p.getElements().get(currentIndex);
			boolean currentElementIsVariable = PathNode.isVariable(currentElement);
			PathNode variableNode = null;
			if (currentNode.getChildren() != null) {
				for (PathNode pathNode : currentNode.getChildren()) {
					if (pathNode.isVariable()) {
						variableNode = pathNode;
					}
					if (pathNode.getElementName().equals(currentElement)) {
						return appendToPathNode(currentIndex + 1, pathNode, binding, uri);
					}
				}
			}
			PathNode newNode;
			if (currentElementIsVariable) {
				if (variableNode == null) {
					newNode = new PathNode(currentNode, currentElement);
				} else {
					return appendToPathNode(currentIndex + 1, variableNode, binding, uri);
				}
			} else {
				newNode = new PathNode(currentNode, currentElement);
			}
			currentNode.addChild(newNode);
			return appendToPathNode(currentIndex + 1, newNode, binding, uri);
		} else {
			return currentNode;
		}
	}

	private ControllerBinding createBinding(final Class<?> controllerType, final Method method, final Object controller, String baseURI, Annotation httpMethodAnnotation, final ResourceURI uri) {
		return new ControllerBindingImpl(controllerType, controller, baseURI, uri, method, httpMethodAnnotation);
	}

	private String joinPathElements(String baseURI, Path classPath, Path methodPath) {
		if (classPath == null && methodPath == null) {
			return assertStartsWithSlash(baseURI);
		}
		String c = null;
		if (classPath != null) {
			c = classPath.value();
		}
		String m = null;
		if (methodPath != null) {
			m = methodPath.value();
		}
		if (c != null) {
			c = assertStartsWithSlash(c);
		}
		if (m != null) {
			m = assertStartsWithSlash(m);
		}

		StringBuffer sb = new StringBuffer();
		if (baseURI == null && c == null && m == null) {
			sb.append('/');
		} else {
			if (baseURI != null) {
				sb.append(assertStartsWithSlash(baseURI));
			}
			if (c != null) {
				sb.append(c);
			}
			if (m != null) {
				sb.append(m);
			}
		}
		return sb.toString();
	}

	private String assertStartsWithSlash(String in) {
		if (in == null || in.length() == 0) {
			return null;
		}
		if (in.charAt(0) != '/') {
			return "/" + in;
		}
		return in;
	}

	private List<Annotation> getHttpMethodForJavaMethod(Method method) {
		List<Annotation> methods = null;
		methods = testMethodFor(GET.class, method, methods);
		methods = testMethodFor(PUT.class, method, methods);
		methods = testMethodFor(POST.class, method, methods);
		methods = testMethodFor(DELETE.class, method, methods);
		return methods;
	}

	private List<Annotation> testMethodFor(Class<? extends Annotation> annotation, Method method, List<Annotation> listOfFound) {
		Annotation a = method.getAnnotation(annotation);
		if (a != null) {
			if (listOfFound == null) {
				listOfFound = new ArrayList<>();
			}
			listOfFound.add(a);
		}
		return listOfFound;
	}

	@Override
	public boolean isVariableElement(String element) {
		int l = element.length();
		return l > 2 && element.charAt(0) == '{' && element.charAt(l - 1) == '}';
	}

	@Override
	public boolean isVariableElementOfName(String element, String name) {
		if (!isVariableElement(element)) {
			return false;
		}
		return element.substring(1, element.length() - 1).equals(name);
	}

	@Override
	public void undeploy(Object controller) {
		controllerLock.writeLock().lock();
		try {
			LOG.info("undeploying controller {}", controller.getClass().getName());
			List<KnownController> toRemove = new ArrayList<>();
			for (KnownController knownController : knownControllers) {
				if (knownController.getController() == controller) {
					toRemove.add(knownController);
					bindings.removeAll(knownController.getBindings());
					for (DestructionLogic destructionLogic : knownController.getDestructionLogic()) {
						// remove the controllerBinding from the lookup tree
						destructionLogic.destruct();
					}
					listenersLock.readLock().lock();
					try {
						for (ControllerResourceRegistryListener controllerResourceRegistryListener : listeners) {
							controllerResourceRegistryListener.undeployedController(
								controller, controller.getClass(), knownController.getBindings(), knownController.getBaseUri(), this
							);
						}
					} finally {
						listenersLock.readLock().unlock();
					}
				}
			}
			if (toRemove.isEmpty()) {
				LOG.warn("undeploying controller {} did not affect anything, because it was not a known controller.", controller.getClass().getName());
			}
			knownControllers.removeAll(toRemove);
		} catch (RuntimeException e) {
			LOG.error("failed to undeploy controller: " + e.getMessage(), e);
			throw e;
		} finally {
			controllerLock.writeLock().unlock();
		}
	}

	@Override
	public Iterator<ControllerBinding> listDeployedControllerBindings() {
		return bindings.iterator();
	}

	private PathNode findPathNodeForUri(PathNode currentNode, int currentIndex, ResourceURI uri) {
		ResourceURI.Path p = uri.getPath();
		if (currentNode != null && p != null && currentIndex < p.getElements().size()) {
			String currentElementName = p.getElements().get(currentIndex);
			PathNode subNode = null;
			PathNode variableNode = currentNode.isVariable() ? currentNode : null;
			if (currentNode.getChildren() != null) {
				for (PathNode pathNode : currentNode.getChildren()) {
					if (pathNode.getElementName().equals(currentElementName)) {
						subNode = pathNode;
						break;
					} else if (pathNode.isVariable()) {
						if (variableNode != null && variableNode != currentNode) {
							throw new IllegalStateException("there are two variable nodes with the same parent");
						}
						variableNode = pathNode;
					}
				}
			}
			if (subNode == null && variableNode != null) {
				subNode = variableNode;
			}
			return findPathNodeForUri(subNode, currentIndex + 1, uri);
		}
		return currentNode;
	}

	private SelectorNode findSelectorNodeInPathNodeForResourceURI(PathNode node, ResourceURI uri) {
		return findSelectorNodeInPathNodeForResourceURI(node.getSelectorNode(), 0, uri);
	}

	private SelectorNode findSelectorNodeInPathNodeForResourceURI(SelectorNode node, int currentIndex, ResourceURI uri) {
		List<ResourceURI.Selector> s = uri.getSelectors();
		if (s != null && currentIndex < s.size()) {
			ResourceURI.Selector current = s.get(currentIndex);
			if (node.getChildren() != null) {
				for (SelectorNode selectorNode : node.getChildren()) {
					if (selectorNode.getName().equals(current.getName())) {
						return findSelectorNodeInPathNodeForResourceURI(selectorNode, currentIndex + 1, uri);
					}
				}
			}
			return node;
		}
		return node;
	}

	private ExtensionNode findExtensionNodeInSelectorNodeForResourceURI(SelectorNode sNode, ResourceURI uri) {
		ResourceURI.Extension ext = uri.getExtension();
		if (ext != null) {
			List<ExtensionNode> children = sNode.getExtensionNode().getChildren();
			ExtensionNode subNode = null;
			ExtensionNode variableExtensionNode = null;
			if (children != null) {
				for (ExtensionNode extensionNode : children) {
					if (extensionNode.getName().equals(ext.getName())) {
						subNode = extensionNode;
						break;
					} else if (extensionNode.isVariable()) {
						variableExtensionNode = extensionNode;
					}
				}
			}
			if (subNode == null && variableExtensionNode != null) {
				return variableExtensionNode;
			} else if (subNode != null) {
				return subNode;
			}
			return sNode.getExtensionNode();
		} else {
			return sNode.getExtensionNode();
		}
	}

	private MethodNode findMethodNodeInExtensionNode(ExtensionNode eNode, HTTPMethod httpMethod) {
		List<MethodNode> children = eNode.getMethodNode().getChildren();
		if (children != null) {
			for (MethodNode methodNode : children) {
				if (methodNode.getHttpMethod().equals(httpMethod)) {
					return methodNode;
				}
			}
		}
		return eNode.getMethodNode();
	}
	
	/**
	 * This method returns the internal list of all controller bindings.
	 * @return 
	 */
	public List<ControllerBinding> getBindings() {
		return bindings;
	}

	public void setDefaultCharacterEncodingProvider(DefaultCharacterEncodingProvider defaultCharacterEncodingProvider) {
		this.defaultCharacterEncodingProvider = defaultCharacterEncodingProvider;
	}
	
}
