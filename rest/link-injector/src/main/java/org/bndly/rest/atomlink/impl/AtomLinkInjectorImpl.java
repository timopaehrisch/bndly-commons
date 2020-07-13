package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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

import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.atomlink.api.AtomLinkInjectorListener;
import org.bndly.rest.atomlink.api.LinkFactory;
import org.bndly.rest.atomlink.api.TypeSupportingListableAtomLinkInjector;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.CompiledAtomLinkDescription;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.ControllerResourceRegistryListener;
import org.bndly.rest.descriptor.ParameterExtractor;
import org.bndly.rest.descriptor.ParameterValue;
import org.bndly.rest.descriptor.PathParameterExtractorImpl;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is serves the same purpose the RESTUtils class from RestEasy does.
 * the cause for using a modified implementation results from the poor
 * separation of maven dependencies in the RestEasy framework to be more
 * specific: resteasy-links pulls the entire framework into an API package that
 * only uses JAX-RS and some RestEasy-Annotations the decorator annotations of
 * resteasy have a hard reference to classes from the resteasy core
 * dependencies. in single class loader environments this might lead to
 * unresolvable framework registration issues with other JAX-RS implementations.
 *
 */
@Component(service = { AtomLinkInjector.class, ControllerResourceRegistryListener.class, TypeSupportingListableAtomLinkInjector.class, ParameterExtractor.class }, immediate = true)
public final class AtomLinkInjectorImpl implements TypeSupportingListableAtomLinkInjector, ParameterExtractor, ControllerResourceRegistryListener {

	private static final Logger LOG = LoggerFactory.getLogger(AtomLinkInjectorImpl.class);
	public static final String PREVENT_LINK_INJECTION_PARAM = "preventLinkInjection";

	@Reference
	private ContextProvider contextProvider;
	@Reference
	private PathParameterExtractorImpl pathParameterExtractor;
	@Reference
	private ConverterRegistry converterRegistry;
	private final List<AtomLinkInjectorListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	private final LinkFactoryTreeNode rootNode = new LinkFactoryTreeNode(Object.class);
	private final ReadWriteLock rootNodeLock = new ReentrantReadWriteLock();
	
	private final Map<Class<?>, List<InspectedController>> inspectedControllersByControllerInterface = new HashMap<>();
	private final ReadWriteLock inspectedControllersLock = new ReentrantReadWriteLock();

	@Reference(
			bind = "addListener",
			unbind = "removeListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = AtomLinkInjectorListener.class
	)
	public void addListener(AtomLinkInjectorListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
				inspectedControllersLock.readLock().lock();
				try {
					for (List<InspectedController> controllers : inspectedControllersByControllerInterface.values()) {
						if (controllers == null) {
							continue;
						}
						for (InspectedController controller : controllers) {
							List<CompiledAtomLinkDescription> bindings = controller.getRuntimeBindings();
							if (bindings == null) {
								continue;
							}
							for (CompiledAtomLinkDescription binding : bindings) {
								listener.addedAtomLink(binding, this);
							}
						}
					}
				} finally {
					inspectedControllersLock.readLock().unlock();
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	public void removeListener(AtomLinkInjectorListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				Iterator<AtomLinkInjectorListener> iterator = listeners.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == listener) {
						iterator.remove();
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "addLinkFactory",
			unbind = "removeLinkFactory",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = LinkFactory.class
	)
	public void addLinkFactory(LinkFactory linkFactory) {
		if (linkFactory != null) {
			rootNodeLock.writeLock().lock();
			try {
				LinkFactoryTreeNode target = rootNode.getOrCreateTypeHierarchy(linkFactory.getTargetType());
				if (target != null) {
					target.addLinkFactory(linkFactory);
				}
			} finally {
				rootNodeLock.writeLock().unlock();
			}
		}
	}
	public void removeLinkFactory(LinkFactory linkFactory) {
		if (linkFactory != null) {
			rootNodeLock.writeLock().lock();
			try {
				LinkFactoryTreeNode node = rootNode.findNodeForType(linkFactory.getTargetType());
				if (node != null) {
					node.removeLinkFactory(linkFactory);
					node.cleanUp();
				}
			} finally {
				rootNodeLock.writeLock().unlock();
			}
		}
	}

	@Activate
	public void activate() {
		rootNodeLock.writeLock().lock();
		try {
			rootNode.setLinkSetter(new DefaultLinkSetter());
		} finally {
			rootNodeLock.writeLock().unlock();
		}
	}

	@Override
	public List<AtomLinkDescription> listAtomLinks() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void unboundFrom(ControllerResourceRegistry registry) {
		try {
			rootNodeLock.writeLock().lock();
			try {
				Iterator<ControllerBinding> bindings = registry.listDeployedControllerBindings();
				while (bindings.hasNext()) {
					ControllerBinding controllerBinding = bindings.next();
					removeAtomLinkInjectorsForControllerInterface(controllerBinding, true);
				}
				rootNode.cleanUp();
			} finally {
				rootNodeLock.writeLock().unlock();
			}
		} catch (Exception e) {
			LOG.error("unbinding from resource registry failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void boundTo(ControllerResourceRegistry registry) {
		try {
			rootNodeLock.writeLock().lock();
			try {
				Iterator<ControllerBinding> bindings = registry.listDeployedControllerBindings();
				while (bindings.hasNext()) {
					ControllerBinding controllerBinding = bindings.next();
					createOrModifyAtomLinkInjectorsForControllerInterface(controllerBinding);
				}
			} finally {
				rootNodeLock.writeLock().unlock();
			}
		} catch (Exception e) {
			LOG.error("binding to resource registry failed: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Collection<AtomLinkDescription> listAtomLinksForType(Class<?> linkedIn) {
		rootNodeLock.readLock().lock();
		try {
			LinkFactoryTreeNode node = rootNode.findNodeForType(linkedIn);
			if (node == null) {
				return Collections.EMPTY_LIST;
			}
			Collection<AtomLinkDescription> list = null;
			Iterator<LinkFactory> it = node.getLinkFactories();
			while (it.hasNext()) {
				LinkFactory next = it.next();
				if (AtomLinkDescriptionLinkFactory.class.isInstance(next)) {
					if (list == null) {
						list = new ArrayList<>();
					}
					list.add(((AtomLinkDescriptionLinkFactory) next).getAtomLinkDescription());
				}
			}
			return list;
		} finally {
			rootNodeLock.readLock().unlock();
		}
	}

	@Override
	public void deployedController(Object controller, Class<?> controllerInterface, List<ControllerBinding> bindings, String baseURI, ControllerResourceRegistry registry) {
		LOG.info("deploying controller " + controllerInterface.getName());
		if (bindings != null) {
			for (ControllerBinding binding : bindings) {
				createOrModifyAtomLinkInjectorsForControllerInterface(binding);
			}
		}
	}

	@Override
	public void undeployedController(Object controller, Class<?> controllerInterface, List<ControllerBinding> bindings, String baseURI, ControllerResourceRegistry registry) {
		LOG.info("undeploying controller " + controllerInterface.getName());
		if (bindings != null) {
			rootNodeLock.writeLock().lock();
			try {
				for (ControllerBinding binding : bindings) {
					removeAtomLinkInjectorsForControllerInterface(binding, true);
				}
				rootNode.cleanUp();
			} finally {
				rootNodeLock.writeLock().unlock();
			}
		}
	}

	private void removeAtomLinkInjectorsForControllerInterface(ControllerBinding controllerBinding, boolean preventLinkCleanUp) {
		rootNodeLock.writeLock().lock();
		try {
			Object controller = controllerBinding.getController();
			Class<?> controllerInterface = controllerBinding.getControllerType();
			boolean cleanUpRequired = false;
			try {
				InspectedController ic = getInspectedControllerFor(controller, controllerInterface);
				if (ic != null) {
					List<RegisteredAtomLinkDescription> descs = ic.getRegisteredAtomLinkDescriptions();
					for (RegisteredAtomLinkDescription desc : descs) {
						LinkFactoryTreeNode node = rootNode.findNodeForType(desc.getLinkedInClass());
						if (node != null) {
							for (AtomLinkDescription atomLinkDescriptionOfControllerBinding : controllerBinding.getAtomLinkDescriptions()) {
								if (desc.getAtomLinkDescription() == atomLinkDescriptionOfControllerBinding) {
									node.removeLinkFactory(desc.getAtomLinkInjector());
									cleanUpRequired = true;
									listenersLock.readLock().lock();
									try {
										for (AtomLinkInjectorListener atomLinkInjectorListener : listeners) {
											atomLinkInjectorListener.removedAtomLink(desc.getAtomLinkDescription(), this);
										}
									} finally {
										listenersLock.readLock().unlock();
									}
								}
							}
						}
					}
					dropInspectedController(ic);
				}
			} catch (Exception e) {
				LOG.error("removing atom link injectors for {} failed: " + e.getMessage(), controllerInterface.getName(), e);
			} finally {
				if (cleanUpRequired && !preventLinkCleanUp) {
					rootNode.cleanUp();
				}
			}
		} finally {
			rootNodeLock.writeLock().unlock();
		}
	}

	private InspectedController getInspectedControllerFor(Object controller, Class<?> controllerInterface) {
		inspectedControllersLock.readLock().lock();
		try {
			List<InspectedController> list = inspectedControllersByControllerInterface.get(controllerInterface);
			if (list == null) {
				return null;
			}
			for (InspectedController inspectedController : list) {
				if (inspectedController.getController() == controller) {
					return inspectedController;
				}
			}
			return null;
		} finally {
			inspectedControllersLock.readLock().unlock();
		}
	}

	private void registerInspectedController(InspectedController ic) {
		inspectedControllersLock.writeLock().lock();
		try {
			List<InspectedController> list = inspectedControllersByControllerInterface.get(ic.getControllerInterface());
			if (list == null) {
				list = new ArrayList<>();
				inspectedControllersByControllerInterface.put(ic.getControllerInterface(), list);
			}
			list.add(ic);
		} finally {
			inspectedControllersLock.writeLock().unlock();
		}
	}

	private void dropInspectedController(InspectedController ic) {
		inspectedControllersLock.writeLock().lock();
		try {
			List<InspectedController> list = inspectedControllersByControllerInterface.get(ic.getControllerInterface());
			if (list != null) {
				list.remove(ic);
				if (list.isEmpty()) {
					inspectedControllersByControllerInterface.remove(ic.getControllerInterface());
				}
			}
		} finally {
			inspectedControllersLock.writeLock().unlock();
		}
	}

	private void createOrModifyAtomLinkInjectorsForControllerInterface(ControllerBinding controllerBinding) {
		rootNodeLock.writeLock().lock();
		try {
			Class<?> controllerInterface = controllerBinding.getControllerType();
			Object controller = controllerBinding.getController();
			try {
				InspectedController ic = getInspectedControllerFor(controller, controllerInterface);
				if (ic != null) {
					return;
				}
				LOG.info("creating atom link injectors from " + controllerInterface.getName());
				ic = new InspectedController(controllerInterface, controller);
				Class<? extends Object> cls = controllerInterface;
				while (!Object.class.equals(cls) && cls != null) {
					// for each public method of the controller...
					Method[] methods = cls.getMethods();
					for (Method method : methods) {
						pathParameterExtractor.processMethod(method, ic.getRuntimeBindings(), controller, cls, controllerBinding.getBaseURI());
					}
					cls = cls.getSuperclass();
				}
				for (CompiledAtomLinkDescription desc : ic.getRuntimeBindings()) {
					controllerBinding.getAtomLinkDescriptions().add(desc);
					AtomLinkDescriptionLinkFactory linkFactory = new AtomLinkDescriptionLinkFactory(desc, contextProvider, converterRegistry);
					LinkFactoryTreeNode target = rootNode.getOrCreateTypeHierarchy(linkFactory.getTargetType());
					if (target != null) {
						target.addLinkFactory(linkFactory);
					}
					listenersLock.readLock().lock();
					try {
						for (AtomLinkInjectorListener atomLinkInjectorListener : listeners) {
							atomLinkInjectorListener.addedAtomLink(desc, this);
						}
					} finally {
						listenersLock.readLock().unlock();
					}
					ic.getRegisteredAtomLinkDescriptions().add(new RegisteredAtomLinkDescription(linkFactory, linkFactory.getTargetType()));
				}
				registerInspectedController(ic);
			} catch (Exception e) {
				LOG.error("creating atom link injectors for {} failed: " + e.getMessage(), controllerInterface.getName(), e);
			}
		} finally {
			rootNodeLock.writeLock().unlock();
		}
	}

	@Override
	public <T> T addDiscovery(T entity) {
		return addDiscovery(entity, true);
	}

	@Override
	public <T> T addDiscovery(T entity, boolean isRoot) {
		Context currentContext = contextProvider.getCurrentContext();
		if (currentContext != null) {
			ResourceURI.QueryParameter preventLinkInjection = currentContext.getURI().getParameter(PREVENT_LINK_INJECTION_PARAM);
			if (preventLinkInjection != null) {
				return entity;
			}
		}
		if (entity == null) {
			return null;
		}
		rootNodeLock.readLock().lock();
		try {
			LinkFactoryTreeNode node = rootNode.findClosestNodeForType(entity.getClass());
			if (node != null) {
				node.injectInto(entity, isRoot);
			}
			return entity;
		} finally {
			rootNodeLock.readLock().unlock();
		}
	}
	
	@Override
	public AtomLinkBean getLinkByName(String rel, String targetClassName) {
		rootNodeLock.readLock().lock();
		try {
			Iterator<LinkFactory> linkFactories = rootNode.getLinkFactoriesForSimpleClassName(targetClassName);
			while (linkFactories.hasNext()) {
				LinkFactory next = linkFactories.next();
				Iterator<AtomLinkBean> links = next.buildLinks();
				while (links.hasNext()) {
					AtomLinkBean link = links.next();
					if (rel.equals(link.getRel())) {
						return link;
					}
				}
			}
			return null;
		} finally {
			rootNodeLock.readLock().unlock();
		}
	}
	
	@Override
	public ParameterValue[] getParametersFromLink(Class<?> entityType, String rel, String url) {
		rootNodeLock.readLock().lock();
		try {
			LinkFactoryTreeNode node = rootNode.findNodeForType(entityType);
			if (node == null) {
				return null;
			}
			Iterator<LinkFactory> factories = node.getLinkFactories();
			while (factories.hasNext()) {
				LinkFactory factory = factories.next();
				if (AtomLinkDescriptionLinkFactory.class.isInstance(factory)) {
					CompiledAtomLinkDescription linkDescription = ((AtomLinkDescriptionLinkFactory) factory).getAtomLinkDescription();
					if (rel == null || rel.equals(linkDescription.getRel())) {
						Context ctx = contextProvider.getCurrentContext();
						if (ctx != null) {
							String baseUrlForAllLinks = ctx.createURIBuilder().build().asString();
							if (url.startsWith(baseUrlForAllLinks)) {
								url = url.substring(baseUrlForAllLinks.length());
							}
						}
						return pathParameterExtractor.extractPathParameters(linkDescription, url);
					}
				}
			}
			return null;
		} finally {
			rootNodeLock.readLock().unlock();
		}
	}

	public void setPathParameterExtractor(PathParameterExtractorImpl pathParameterExtractor) {
		this.pathParameterExtractor = pathParameterExtractor;
	}

	public void setContextProvider(ContextProvider contextProvider) {
		this.contextProvider = contextProvider;
	}

	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

}
