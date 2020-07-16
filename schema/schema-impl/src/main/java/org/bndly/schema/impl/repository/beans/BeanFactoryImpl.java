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
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.PathBuilder;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.repository.beans.Bean;
import org.bndly.schema.api.repository.beans.BeanFactory;
import org.bndly.schema.api.repository.beans.BeanResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = BeanFactory.class)
@Designate(ocd = BeanFactoryImpl.Configuration.class)
public class BeanFactoryImpl implements BeanFactory {

	@ObjectClassDefinition(
			name = "Bean Factory",
			description = "The bean factory converts node instances to beans, which have a less strict API than nodes."
	)
	public @interface Configuration {

	}
	
	private static final Logger LOG = LoggerFactory.getLogger(BeanFactoryImpl.class);
	
	@Override
	public Bean createBeanFromNode(final Node node) {
		BeanResolver beanResolver = createBeanResolver(node.getRepositorySession());
		return createBeanFromNode(node, beanResolver);
	}
	
	public Bean createBeanFromNode(final Node node, final BeanResolver beanResolver) {
		if (!NodeTypes.BEAN.equals(node.getType())) {
			return null;
		}
		final String beanType;
		try {
			beanType = node.getProperty("beanType").getString();
		} catch (PropertyNotFoundException e) {
			return null;
		} catch (RepositoryException e) {
			LOG.error("could not get beanType from bean node: " + e.getMessage(), e);
			return null;
		}
		if (beanType == null) {
			return null;
		}
		return new Bean() {
			
			private Map<String, Object> properties;
			private boolean didLoadAllProperties;
			private boolean didInitParent;
			private Bean parent;
			
			@Override
			public String getName() {
				return node.getName();
			}

			@Override
			public String getPath() {
				return node.getPath().toString();
			}

			@Override
			public Bean getParent() {
				if (didInitParent) {
					return parent;
				}
				didInitParent = true;
				try {
					Node parentNode = node.getParent();
					parent = createBeanFromNode(parentNode, beanResolver);
				} catch (RepositoryException ex) {
					LOG.error("could not get parent bean: " + ex.getMessage(), ex);
				}
				return parent;
			}

			@Override
			public String getBeanType() {
				return beanType;
			}

			@Override
			public Bean getChild(String name) {
				try {
					return createBeanFromNode(node.getChild(name), beanResolver);
				} catch (NodeNotFoundException e) {
					return null;
				} catch (RepositoryException e) {
					LOG.error("could not get child bean: " + e.getMessage(), e);
					return null;
				}
			}

			@Override
			public Iterator<Bean> getChildren() {
				return createChildBeanIterator(node, beanResolver);
			}

			@Override
			public Map<String, Object> getProperties() {
				if (!didLoadAllProperties) {
					didLoadAllProperties = true;
					try {
						Iterator<Property> propertiesIter = node.getProperties();
						if (!propertiesIter.hasNext()) {
							properties = Collections.EMPTY_MAP;
						} else {
							properties = new LinkedHashMap<>();
							while (propertiesIter.hasNext()) {
								Property next = propertiesIter.next();
								if (next.isMultiValued()) {
									properties.put(next.getName(), next.getValues());
								} else {
									properties.put(next.getName(), next.getValue());
								}
							}
						}
					} catch (RepositoryException e) {
						LOG.error("could not load properties of node: " + e.getMessage(), e);
						if (properties == null) {
							properties = Collections.EMPTY_MAP;
						}
					}
				}
				return properties;
			}

			@Override
			public Object getProperty(String name) {
				if (name == null || name.isEmpty()) {
					return null;
				}
				if (properties != null && properties.containsKey(name)) {
					return properties.get(name);
				}
				try {
					Property prop = node.getProperty(name);
					if (properties == null) {
						properties = new HashMap<>();
					}
					Object returnValue;
					if (prop.isMultiValued()) {
						returnValue = prop.getValues();
					} else {
						returnValue = prop.getValue();
					}
					properties.put(name, returnValue);
					return returnValue;
				} catch (PropertyNotFoundException e) {
					return null;
				} catch (RepositoryException e) {
					LOG.error("could not get property: " + e.getMessage(), e);
					return null;
				}
			}

			@Override
			public Object get(String name) {
				return getProperty(name);
			}

			@Override
			public <T> T morphTo(Class<T> type) {
				if (Node.class.equals(type)) {
					return type.cast(node);
				}
				return null;
			}

			@Override
			public BeanResolver getBeanResolver() {
				return beanResolver;
			}

		};
	}
	
	private Iterator<Bean> createChildBeanIterator(Node node, final BeanResolver beanResolver) {
		try {
			final Iterator<Node> childIter = node.getChildren();
			return new Iterator<Bean>() {
				Bean current = null;
				@Override
				public boolean hasNext() {
					while (current == null && childIter.hasNext()) {
						current = createBeanFromNode(childIter.next(), beanResolver);
					}
					return current != null;
				}

				@Override
				public Bean next() {
					if (current == null) {
						if (!hasNext()) {
							return null;
						}
					}
					Bean tmp = current;
					current = null;
					return tmp;
				}

				@Override
				public void remove() {
					childIter.remove();
				}
				
			};
		} catch (RepositoryException e) {
			LOG.error("could not get children: " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public BeanResolver createBeanResolver(final RepositorySession repositorySession) {
		return new BeanResolver() {
			@Override
			public Bean resolve(String path) {
				if (path == null || path.isEmpty()) {
					return null;
				}
				try {
					return createBeanFromNode(repositorySession.getNode(PathBuilder.newInstance(path).build()), this);
				} catch (NodeNotFoundException e) {
					return null;
				} catch (RepositoryException e) {
					LOG.error("could not resolve bean of path " + path + ": " + e.getMessage(), e);
					return null;
				}
			}
		};
	}
}
