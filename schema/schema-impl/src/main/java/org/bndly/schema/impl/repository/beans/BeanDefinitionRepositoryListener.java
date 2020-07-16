package org.bndly.schema.impl.repository.beans;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositoryListener;
import org.bndly.schema.api.repository.RepositorySession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BeanDefinitionRepositoryListener implements RepositoryListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(BeanDefinitionRepositoryListener.class);
	private final Map<Integer, List<Callable>> callablesPerSession = new HashMap<>();
	private final BeanDefinitionRegistryImpl beanDefinitionRegistryImpl;

	public BeanDefinitionRepositoryListener(BeanDefinitionRegistryImpl beanDefinitionRegistryImpl) {
		this.beanDefinitionRegistryImpl = beanDefinitionRegistryImpl;
	}

	@Override
	public void onSessionStart(RepositorySession session) throws RepositoryException {
		if (session.isReadOnly()) {
			return;
		}
		callablesPerSession.put(System.identityHashCode(session), new ArrayList<Callable>());
	}

	@Override
	public void onSessionEnd(RepositorySession session) throws RepositoryException {
		if (session.isReadOnly()) {
			return;
		}
		callablesPerSession.remove(System.identityHashCode(session));
	}

	@Override
	public void onBeforeFlush(RepositorySession session) throws RepositoryException {
	}

	@Override
	public void onFlushSuccess(RepositorySession session) throws RepositoryException {
		if (session.isReadOnly()) {
			return;
		}
		List<Callable> callables = callablesPerSession.get(System.identityHashCode(session));
		if (callables != null && !callables.isEmpty()) {
			// do an init at the end
			callables.add(beanDefinitionRegistryImpl.createCallableForBeanDefinitionInit());
			try {
				for (Callable callable : callables) {
					callable.call();
				}
				LOG.info("applied {} changes to the bean definition registry", (callables.size() - 1));
			} catch (Exception e) {
				LOG.error("failed to apply changes to the bean definition registry", e);
				if (RepositoryException.class.isInstance(e)) {
					throw (RepositoryException) e;
				}
				throw new RepositoryException("could not apply bean definition modifications", e);
			}
		}
	}

	@Override
	public void onFlushFailure(RepositorySession session) throws RepositoryException {
	}

	@Override
	public void onNodeCreated(Node node) throws RepositoryException {
		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
			onNewBeanDefinition(node);
		} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
			onNewBeanPropertyDefinition(node);
		}
	}

	@Override
	public void onNodeRemoved(Node node) throws RepositoryException {
		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
			onRemovedBeanDefinition(node);
		} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
			onRemovedBeanPropertyDefinition(node);
		}
	}

	@Override
	public void onNodeMoved(Node node, long index) throws RepositoryException {
	}

	@Override
	public void onPropertyCreated(Property property) throws RepositoryException {
		Node node = property.getNode();
		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
			onUpdatedBeanDefinition(node, property);
		} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
			onUpdatedBeanPropertyDefinition(node, property);
		}
	}

	@Override
	public void onPropertyRemoved(Property property) throws RepositoryException {
//		Node node = property.getNode();
//		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
//			onUpdatedBeanDefinition(node, property);
//		} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
//			onUpdatedBeanPropertyDefinition(node, property);
//		}
	}

	@Override
	public void onPropertyChanged(Property property) throws RepositoryException {
		Node node = property.getNode();
		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
			onUpdatedBeanDefinition(node, property);
		} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
			onUpdatedBeanPropertyDefinition(node, property);
		}
	}

	private void onNewBeanDefinition(Node node) {
		LOG.info("detected a new bean definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForNewBeanDefinition(node));
	}

	private void onNewBeanPropertyDefinition(Node node) {
		LOG.info("detected a new bean property definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForNewBeanPropertyDefinition(node));
	}

	private void onRemovedBeanDefinition(Node node) {
		LOG.info("detected a removed bean definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForRemovedBeanDefinition(node));
	}

	private void onRemovedBeanPropertyDefinition(Node node) {
		LOG.info("detected a removed bean property definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForRemovedBeanPropertyDefinition(node));
	}
	
	private void addCallableForNodeSession(Node node, Callable callable) {
		List<Callable> callablesForNodeSession = getCallableForNodeSession(node);
		if (callablesForNodeSession != null) {
			callablesForNodeSession.add(callable);
		}
	}
	
	private List<Callable> getCallableForNodeSession(Node node) {
		return getCallableForSession(node.getRepositorySession());
	}
	
	private List<Callable> getCallableForSession(RepositorySession repositorySession) {
		return callablesPerSession.get(System.identityHashCode(repositorySession));
	}

	private void onUpdatedBeanDefinition(Node node, Property property) {
		LOG.info("detected a modification of bean definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForUpdatedBeanDefinition(node, property));
	}

	private void onUpdatedBeanPropertyDefinition(Node node, Property property) {
		LOG.info("detected a modification of bean property definition at {}", node.getPath());
		addCallableForNodeSession(node, beanDefinitionRegistryImpl.createRunnableForUpdatedBeanPropertyDefinition(node, property));
	}
}
