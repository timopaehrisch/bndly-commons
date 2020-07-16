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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.PathBuilder;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.repository.beans.BeanDefinition;
import org.bndly.schema.api.repository.beans.BeanDefinitionRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = BeanDefinitionRegistry.class)
@Designate(ocd = BeanDefinitionRegistryImpl.Configuration.class)
public class BeanDefinitionRegistryImpl implements BeanDefinitionRegistry {

	@ObjectClassDefinition(
			name = "Bean Definition Registry",
			description = "This registry keeps track of the meta model for bean definitions"
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Locations",
				description = "The locations in the repository to check for bean definitions"
		)
		String[] locations() default {"/apps", "/libs"};
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(BeanDefinitionRegistryImpl.class);
	
	@Reference
	private Repository repository;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<String, BeanDefinitionImpl> beanDefinitionsByPath = new HashMap<>();
	private final Map<String, BeanDefinitionImpl> beanDefinitions = new HashMap<>();
	private Map<String, BeanDefinitionImpl> beanDefinitionsUnmodifieable = Collections.EMPTY_MAP;
	
	private static final BeanDefinition.PropertyDefinition BEAN_TYPE_PROPERTY_DEFINITION = new BeanDefinition.PropertyDefinition() {
		@Override
		public String getName() {
			return "beanType";
		}

		@Override
		public boolean isMulti() {
			return false;
		}

		@Override
		public Property.Type getType() {
			return Property.Type.STRING;
		}

		@Override
		public Object get(String metaDataName) {
			return null;
		}

		@Override
		public Object getMetaData(String metaDataName) {
			return null;
		}

		@Override
		public Map<String, Object> getMetaDataMap() {
			return Collections.EMPTY_MAP;
		}
		
	};
	private BeanDefinitionRepositoryListener listener;

	private class PropertyDefinitionImpl implements BeanDefinition.PropertyDefinition {

		private final String path;
		private final String name;
		private final boolean multi;
		private final Property.Type type;
		private final Map<String, Object> metaData;

		public PropertyDefinitionImpl(String path, String name, boolean multi, Property.Type type, Map<String, Object> metaData) {
			this.path = path;
			this.name = name;
			this.multi = multi;
			this.type = type;
			this.metaData = metaData == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(metaData);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isMulti() {
			return multi;
		}

		@Override
		public Property.Type getType() {
			return type;
		}

		@Override
		public Object get(String metaDataName) {
			return getMetaData(metaDataName);
		}

		@Override
		public Object getMetaData(String metaDataName) {
			return metaData.get(metaDataName);
		}

		@Override
		public Map<String, Object> getMetaDataMap() {
			return metaData;
		}
		
	}
	
	private class BeanDefinitionImpl implements BeanDefinition {
		private final String path;
		private final String name;
		private final String superTypeName;
		private final Map<String, PropertyDefinitionImpl> propertiesInternalByPath = new LinkedHashMap<>();
		private List<PropertyDefinition> properties = new ArrayList<>();
		private Map<String, PropertyDefinition> propertiesByName = Collections.EMPTY_MAP;
		private List<PropertyDefinition> allProperties;
		private final Map<String, Object> metaData;

		public BeanDefinitionImpl(String path, String name, String superTypeName, Map<String, Object> metaData) {
			this.path = path;
			this.name = name;
			this.superTypeName = superTypeName;
			this.metaData = metaData == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(metaData);
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public BeanDefinition getParent() {
			return getBeanDefinition(superTypeName);
		}

		@Override
		public List<BeanDefinition.PropertyDefinition> getProperties() {
			return properties;
		}

		@Override
		public List<BeanDefinition.PropertyDefinition> getAllProperties() {
			return allProperties;
		}

		@Override
		public PropertyDefinition getProperty(String name) {
			return propertiesByName.get(name);
		}
		
		private PropertyDefinitionImpl getPropertyImpl(String name) {
			PropertyDefinition definition = propertiesByName.get(name);
			return (PropertyDefinitionImpl) (PropertyDefinitionImpl.class.isInstance(definition) ? definition : null);
		}

		private void init() {
			properties = Collections.unmodifiableList(new ArrayList<PropertyDefinition>(propertiesInternalByPath.values()));
			List<PropertyDefinition> propertiesCopy = new ArrayList<>();
			propertiesCopy.add(BEAN_TYPE_PROPERTY_DEFINITION); // every bean has this property! it identifies the type of the bean
			getPropertiesOfBeanDefinition(this, propertiesCopy);
			this.allProperties = Collections.unmodifiableList(propertiesCopy);
			Map<String, PropertyDefinition> definitionsByName = new LinkedHashMap<>();
			for (PropertyDefinition propertyDefinition : allProperties) {
				definitionsByName.put(propertyDefinition.getName(), propertyDefinition);
			}
			propertiesByName = Collections.unmodifiableMap(definitionsByName);
		}
		
		@Override
		public Object get(String metaDataName) {
			return getMetaData(metaDataName);
		}

		@Override
		public Object getMetaData(String metaDataName) {
			return metaData.get(metaDataName);
		}
	}
	
	@Activate
	public void activate(ComponentContext componentContext) {
		lock.writeLock().lock();
		DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());
		try (RepositorySession session = repository.createReadOnlySession()) {
			Collection<String> locations = dictionaryAdapter.getStringCollection("locations", "/apps", "/libs");
			for (String location : locations) {
				// look for bean definitions only in defined places
				Node apps = session.getNode(PathBuilder.newInstance(location).build());
				iterateNode(apps, new IterationCallback() {
					private final Stack<BeanDefinitionImpl> definitionStack = new Stack<>();
					@Override
					public void onNode(Node node) throws RepositoryException {
						if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
							BeanDefinitionImpl beanDefinitionImpl = createBeanDefinitionFromNode(node);
							if (beanDefinitionImpl != null) {
								definitionStack.push(beanDefinitionImpl);
								beanDefinitions.put(beanDefinitionImpl.getName(), beanDefinitionImpl);
								beanDefinitionsByPath.put(beanDefinitionImpl.path, beanDefinitionImpl);
							}
						} else if (node.getType().equals(NodeTypes.BEAN_PROPERTY_DEFINITION)) {
							PropertyDefinitionImpl propertyDefinition = createBeanPropertyDefinitionFromNode(node);
							if (propertyDefinition != null && !definitionStack.isEmpty()) {
								BeanDefinitionImpl peek = definitionStack.peek();
								peek.propertiesInternalByPath.put(propertyDefinition.path, propertyDefinition);
							}
						}
					}

					@Override
					public void afterNode(Node node) throws RepositoryException {
						if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
							definitionStack.pop();
						}
					}

				});
				for (BeanDefinition beanDefinition : beanDefinitions.values()) {
					((BeanDefinitionImpl)beanDefinition).init();
				}
			}
			beanDefinitionsUnmodifieable = Collections.unmodifiableMap(beanDefinitions);
			LOG.info("found a total of {} bean definitions", beanDefinitions.size());
		} catch (NodeNotFoundException ex) {
			LOG.warn("no apps found");
		} catch (RepositoryException ex) {
			LOG.error("could not initially inspect repository: " + ex.getMessage(), ex);
		} finally {
			lock.writeLock().unlock();
		}
		listener = new BeanDefinitionRepositoryListener(this);
		repository.addListener(listener);
	}

	@Deactivate
	public void deactivate() {
		repository.removeListener(listener);
		listener = null;
		lock.writeLock().lock();
		try {
			beanDefinitions.clear();
			beanDefinitionsUnmodifieable = Collections.EMPTY_MAP;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private BeanDefinitionImpl createDefinition(String path, String name, String superType, Map<String, Object> metaData) {
		return new BeanDefinitionImpl(path, name, superType, metaData);
	}

	private PropertyDefinitionImpl createPropertyDefinition(String path, String propertyName, Property.Type type, boolean multi, Map<String, Object> metaData) {
		return new PropertyDefinitionImpl(path, propertyName, multi, type, metaData);
	}
	
	private Map<String, Object> createMetaData(Node node) throws RepositoryException, IOException {
		Map<String, Object> metaData = new LinkedHashMap<>();
		Iterator<Property> properties = node.getProperties();
		while (properties.hasNext()) {
			Property property = properties.next();
			String name = property.getName();
			if (property.getType() == Property.Type.ENTITY) {
				continue;
			}
			Object value;
			if (property.isMultiValued()) {
				if (property.getType() == Property.Type.BINARY) {
					InputStream[] binaries = property.getBinaries();
					if (binaries != null) {
						InputStream[] copy = new InputStream[binaries.length];
						for (int i = 0; i < binaries.length; i++) {
							InputStream binary = binaries[i];
							if (binary != null) {
								byte[] bytes = IOUtils.read(binary);
								copy[i] = new ByteArrayInputStream(bytes);
							} else {
								copy[i] = null;
							}
						}
						value = copy;
					} else {
						value = null;
					}
				} else {
					value = property.getValues();
				}
			} else {
				if (property.getType() == Property.Type.BINARY) {
					InputStream binary = property.getBinary();
					if (binary != null) {
						byte[] bytes = IOUtils.read(binary);
						value = new ByteArrayInputStream(bytes);
					} else {
						value = null;
					}
				} else {
					value = property.getValue();
				}
			}
			metaData.put(name, value);
		}
		return metaData;
	}
	
	private PropertyDefinitionImpl createBeanPropertyDefinitionFromNode(Node node) throws RepositoryException {
		if (node == null) {
			return null;
		}
		final String propertyName;
		final Property.Type type;
		try {
			propertyName = node.getProperty("name").getString();
			type = org.bndly.schema.api.repository.Property.Type.valueOf(node.getProperty("type").getString());
		} catch (PropertyNotFoundException e) {
			// this does not matter
			LOG.warn("incomplete bean property definition at {}", node.getPath());
			return null;
		}
		boolean multi = false;
		try {
			Boolean tmp = node.getProperty("multi").getBoolean();
			if (tmp != null) {
				multi = tmp;
			}
		} catch (PropertyNotFoundException e) {
			multi = false;
		}
		try {
			PropertyDefinitionImpl propertyDefinition = createPropertyDefinition(node.getPath().toString(), propertyName, type, multi, createMetaData(node));
			return propertyDefinition;
		} catch (IOException e) {
			LOG.error("could not build bean property definition: " + e.getMessage(), e);
			return null;
		}
	}
	private BeanDefinitionImpl createBeanDefinitionFromNode(Node node) throws RepositoryException {
		if (node == null) {
			return null;
		}
		final String name;
		try {
			name = node.getProperty("name").getString();
		} catch (PropertyNotFoundException e) {
			// this does not matter
			LOG.warn("incomplete bean definition at {}", node.getPath());
			return null;
		}
		String superType;
		try {
			superType = node.getProperty("superType").getString();
		} catch (PropertyNotFoundException e) {
			// this does not matter
			superType = null;
		}
		if (name != null) {
			try {
				BeanDefinitionImpl def = createDefinition(node.getPath().toString(), name, superType, createMetaData(node));
				return def;
			} catch (IOException e) {
				LOG.error("could not build bean definition: " + e.getMessage(), e);
			}
		}
		return null;
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanTypeName) {
		if (beanTypeName == null) {
			return null;
		}
		lock.readLock().lock();
		try {
			return beanDefinitions.get(beanTypeName);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Map<String, BeanDefinition> getBeanDefinitions() {
		lock.readLock().lock();
		try {
			return (Map) beanDefinitionsUnmodifieable;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	private void getPropertiesOfBeanDefinition(BeanDefinition beanDefinition, List<BeanDefinition.PropertyDefinition> properties) {
		if (beanDefinition == null) {
			return;
		}
		getPropertiesOfBeanDefinition(beanDefinition.getParent(), properties);
		properties.addAll(beanDefinition.getProperties());
	}
	
	private interface IterationCallback {
		void onNode(Node node) throws RepositoryException;

		void afterNode(Node node) throws RepositoryException;
	}
	
	private void iterateNode(Node node, IterationCallback callback) throws RepositoryException {
		callback.onNode(node);
		Iterator<Node> iter = node.getChildren();
		while (iter.hasNext()) {
			iterateNode(iter.next(), callback);
		}
	}

	Callable<BeanDefinition> createRunnableForNewBeanDefinition(final Node node) {
		return new Callable<BeanDefinition>() {
			@Override
			public BeanDefinition call() throws RepositoryException {
				lock.writeLock().lock();
				try {
					BeanDefinitionImpl beanDefinitionImpl = createBeanDefinitionFromNode(node);
					if (beanDefinitionImpl != null) {
						beanDefinitions.put(beanDefinitionImpl.getName(), beanDefinitionImpl);
						beanDefinitionsByPath.put(beanDefinitionImpl.path, beanDefinitionImpl);
					}
					return beanDefinitionImpl;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}

	Callable<BeanDefinition.PropertyDefinition> createRunnableForNewBeanPropertyDefinition(final Node node) {
		return new Callable<BeanDefinition.PropertyDefinition>() {
			@Override
			public BeanDefinition.PropertyDefinition call() throws Exception {
				lock.writeLock().lock();
				try {
					PropertyDefinitionImpl propertyDefinition = createBeanPropertyDefinitionFromNode(node);
					if (propertyDefinition != null) {
						BeanDefinitionImpl beanDefinitionImpl = getBeanDefinitionFromNode(node);
						if (beanDefinitionImpl != null) {
							beanDefinitionImpl.propertiesInternalByPath.put(propertyDefinition.path, propertyDefinition);
						}
					}
					return propertyDefinition;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}

	Callable<BeanDefinition> createRunnableForRemovedBeanDefinition(final Node node) {
		return new Callable<BeanDefinition>() {
			@Override
			public BeanDefinition call() throws Exception {
				lock.writeLock().lock();
				try {
					BeanDefinitionImpl beanDefinitionImpl = getBeanDefinitionFromNode(node);
					if (beanDefinitionImpl != null) {
						beanDefinitions.remove(beanDefinitionImpl.getName());
						beanDefinitionsByPath.remove(beanDefinitionImpl.path);
					}
					return beanDefinitionImpl;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}

	Callable<BeanDefinition.PropertyDefinition> createRunnableForRemovedBeanPropertyDefinition(final Node node) {
		return new Callable<BeanDefinition.PropertyDefinition>() {
			@Override
			public BeanDefinition.PropertyDefinition call() throws Exception {
				lock.writeLock().lock();
				try {
					BeanDefinitionImpl beanDefinitionImpl = getBeanDefinitionFromNode(node);
					if (beanDefinitionImpl != null) {
						try {
							String name = node.getProperty("name").getString();
							PropertyDefinitionImpl property = beanDefinitionImpl.getPropertyImpl(name);
							beanDefinitionImpl.propertiesInternalByPath.remove(property.path);
							return property;
						} catch (PropertyNotFoundException e) {
							LOG.warn("incomplete bean property definition at {}", node.getPath());
						}
					}
					return null;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}
	
	Callable<BeanDefinition> createRunnableForUpdatedBeanDefinition(final Node node, final Property property) {
		return new Callable<BeanDefinition>() {
			@Override
			public BeanDefinition call() throws Exception {
				lock.writeLock().lock();
				try {
					// either the meta data or the name or superType of a bean definition have updated/added
					// there might be an already existing beandefinition. we have to drop it, because it might contain old values
					BeanDefinitionImpl oldBeanDefinitionImpl = getBeanDefinitionFromNode(node);
					if (oldBeanDefinitionImpl != null) {
						beanDefinitions.remove(oldBeanDefinitionImpl.getName());
						beanDefinitionsByPath.remove(oldBeanDefinitionImpl.path);
					}
					// when we drop it, we have to copy the properties to the new definition instance, because those might be untouched
					BeanDefinitionImpl newBeanDefinitionImpl = createBeanDefinitionFromNode(getBeanDefinitionNode(node));
					if (newBeanDefinitionImpl != null) {
						if (oldBeanDefinitionImpl != null) {
							newBeanDefinitionImpl.propertiesInternalByPath.putAll(oldBeanDefinitionImpl.propertiesInternalByPath);
						}
						beanDefinitions.put(newBeanDefinitionImpl.getName(), newBeanDefinitionImpl);
						beanDefinitionsByPath.put(newBeanDefinitionImpl.path, newBeanDefinitionImpl);
					}
					return newBeanDefinitionImpl;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}

	Callable<BeanDefinition.PropertyDefinition> createRunnableForUpdatedBeanPropertyDefinition(final Node node, Property property) {
		return new Callable<BeanDefinition.PropertyDefinition>() {
			@Override
			public BeanDefinition.PropertyDefinition call() throws Exception {
				lock.writeLock().lock();
				try {
					// either the meta data or the name or the cardinality ('multi') of a bean property definition have updated/added
					// there has to be an existing bean definition
					BeanDefinitionImpl beanDefinitionImpl = getBeanDefinitionFromNode(node);
					if (beanDefinitionImpl != null) {
						PropertyDefinitionImpl oldPropertyDefinition = beanDefinitionImpl.propertiesInternalByPath.remove(node.getPath().toString());
						PropertyDefinitionImpl newPropertyDefinition = createBeanPropertyDefinitionFromNode(node);
						if (newPropertyDefinition != null) {
							beanDefinitionImpl.propertiesInternalByPath.put(newPropertyDefinition.path, newPropertyDefinition);
						}
						return newPropertyDefinition;
					}
					return null;
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}
	
	Callable<Map<String, BeanDefinition>> createCallableForBeanDefinitionInit() {
		return new Callable<Map<String, BeanDefinition>>() {
			@Override
			public Map<String, BeanDefinition> call() throws Exception {
				lock.writeLock().lock();
				try {
					for (BeanDefinitionImpl value : beanDefinitions.values()) {
						value.init();
					}
					beanDefinitionsUnmodifieable = Collections.unmodifiableMap(beanDefinitions);
					return getBeanDefinitions();
				} finally {
					lock.writeLock().unlock();
				}
			}
		};
	}
	
	BeanDefinitionImpl getBeanDefinitionFromNode(Node node) throws RepositoryException {
		node = getBeanDefinitionNode(node);
		if (node == null) {
			return null;
		}
		try {
			String name = node.getProperty("name").getString();
			return beanDefinitions.get(name);
		} catch (PropertyNotFoundException e) {
			LOG.warn("incomplete bean definition at {}", node.getPath());
			return null;
		}
	}
	
	Node getBeanDefinitionNode(Node node) throws RepositoryException {
		if (node == null) {
			return null;
		}
		if (node.getType().equals(NodeTypes.BEAN_DEFINITION)) {
			return node;
		} else {
			return getBeanDefinitionNode(node.getParent());
		}
	}
}
