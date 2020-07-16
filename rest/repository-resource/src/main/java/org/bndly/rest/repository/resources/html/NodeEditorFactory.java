package org.bndly.rest.repository.resources.html;

/*-
 * #%L
 * REST Repository Resource
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
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.Property.Type;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.beans.BeanDefinition;
import org.bndly.schema.api.repository.beans.BeanDefinitionRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NodeEditorFactory {

	private final BeanDefinitionRegistry beanDefinitionRegistry;
	
	NodeEditorFactory(BeanDefinitionRegistry beanDefinitionRegistry) {
		this.beanDefinitionRegistry = beanDefinitionRegistry;
	}

	private Iterable<NamedValue> createNamedValues(Property property) {
		final String fieldName = property.getName() + "@" + property.getType().toString();
		final List<NamedValue> result;
		if (property.isMultiValued()) {
			if (property.getType() == Property.Type.STRING) {
				String[] values = property.getStrings();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						String value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else if (property.getType() == Property.Type.BOOLEAN) {
				Boolean[] values = property.getBooleans();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						Boolean value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value == null ? null : value.toString()));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else if (property.getType() == Property.Type.DATE) {
				Date[] values = property.getDates();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						Date value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value == null ? null : value.toString()));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else if (property.getType() == Property.Type.DECIMAL) {
				BigDecimal[] values = property.getDecimals();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						BigDecimal value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value == null ? null : value.toString()));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else if (property.getType() == Property.Type.DOUBLE) {
				Double[] values = property.getDoubles();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						Double value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value == null ? null : value.toString()));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else if (property.getType() == Property.Type.LONG) {
				Long[] values = property.getLongs();
				if (values != null) {
					result = new ArrayList<>(values.length);
					for (int i = 0; i < values.length; i++) {
						Long value = values[i];
						result.add(createdNamedValue(fieldName + "[" + i + "]", value == null ? null : value.toString()));
					}
				} else {
					result = Collections.EMPTY_LIST;
				}
			} else {
				result = Collections.EMPTY_LIST;
			}
		} else {
			result = new ArrayList<>(1);
			if (property.getType() == Property.Type.STRING) {
				String value = property.getString();
				result.add(createdNamedValue(fieldName, value));
			} else if (property.getType() == Property.Type.BOOLEAN) {
				Boolean value = property.getBoolean();
				result.add(createdNamedValue(fieldName, value == null ? null : value.toString()));
			} else if (property.getType() == Property.Type.DATE) {
				Date value = property.getDate();
				result.add(createdNamedValue(fieldName, value == null ? null : value.toString()));
			} else if (property.getType() == Property.Type.DECIMAL) {
				BigDecimal value = property.getDecimal();
				result.add(createdNamedValue(fieldName, value == null ? null : value.toString()));
			} else if (property.getType() == Property.Type.DOUBLE) {
				Double value = property.getDouble();
				result.add(createdNamedValue(fieldName, value == null ? null : value.toString()));
			} else if (property.getType() == Property.Type.LONG) {
				Long value = property.getLong();
				result.add(createdNamedValue(fieldName, value == null ? null : value.toString()));
			} else {
				result.add(createdNamedValue(fieldName, null));
			}
		}
		
		return result;
	}

	private NamedValue createdNamedValue(final String name, final String value) {
		return new NamedValue() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getValue() {
				return value;
			}
		};
	}
	
	public static interface NamedValue {
		String getName();
		String getValue();
	}
	
	public static interface Widget {
		Property getProperty();
		Type getPropertyType();
		boolean isPropertyMultiValued();
		String getPropertyName();
		String getTitle();
		String getInputType();
		Iterable<NamedValue> getNamedValues();
		Map<String, Object> getMetaData();
	}
	
	public static interface Editor {
		Node getNode();
		Iterable<Widget> getWidgets();
	}
	
	private static interface Function<IN,OUT>{
		OUT apply(IN input);
	}
	
	private static final Function<Property, Boolean> ALL_ITEMS = new Function<Property, Boolean>() {
		@Override
		public Boolean apply(Property input) {
			return true;
		}
	};
	
	public Object createEditor(final Node node) throws RepositoryException {
		final List<Widget> widgets = new ArrayList<>();
		//TODO: 
		// 1. if there is a "beanType" property on the node, look up the bean type definition and create the widgets via the bean type definition
		// 2. support custom editors (such as javascript editors)
		if (node.isHavingProperty("beanType")) {
			Property beanType = node.getProperty("beanType");
			if (!beanType.isMultiValued() && beanType.getType() == Property.Type.STRING) {
				BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanType.getString());
				if (beanDefinition != null) {
					final Set<String> iteratedPropertyNames = new HashSet<>();
					List<BeanDefinition.PropertyDefinition> allPropertyDefs = beanDefinition.getAllProperties();
					for (final BeanDefinition.PropertyDefinition propertyDef : allPropertyDefs) {
						String propertyName = propertyDef.getName();
						iteratedPropertyNames.add(propertyName);
						if (node.isHavingProperty(propertyName)) {
							Property prop = node.getProperty(propertyName);
							widgets.add(createWidgetFromProperty(prop, propertyDef));
						} else {
							final String inputType = propertyTypeToInputType(propertyDef.getType());
							final List<NamedValue> namedValues;
							if (propertyDef.isMulti()) {
								namedValues = Collections.EMPTY_LIST;
							} else {
								namedValues = new ArrayList<>(1);
								namedValues.add(createdNamedValue(propertyName + "@" + propertyDef.getType().toString(), null));
							}
							widgets.add(new Widget() {
								@Override
								public Property getProperty() {
									return null;
								}

								@Override
								public String getTitle() {
									return propertyDef.getName();
								}

								@Override
								public String getInputType() {
									return inputType;
								}

								@Override
								public Iterable<NamedValue> getNamedValues() {
									return namedValues;
								}

								@Override
								public Type getPropertyType() {
									return propertyDef.getType();
								}

								@Override
								public boolean isPropertyMultiValued() {
									return propertyDef.isMulti();
								}

								@Override
								public String getPropertyName() {
									return propertyDef.getName();
								}

								@Override
								public Map<String, Object> getMetaData() {
									return propertyDef.getMetaDataMap();
								}
								
							});
						}
					}
					createWidgetsByNodeProperties(node, widgets, new Function<Property, Boolean>() {
						@Override
						public Boolean apply(Property input) {
							return !iteratedPropertyNames.contains(input.getName());
						}
					});
				} else {
					createWidgetsByNodeProperties(node, widgets, ALL_ITEMS);
				}
			} else {
				createWidgetsByNodeProperties(node, widgets, ALL_ITEMS);
			}
		} else {
			createWidgetsByNodeProperties(node, widgets, ALL_ITEMS);
		}
		return new Editor() {
			@Override
			public Node getNode() {
				return node;
			}

			@Override
			public Iterable<Widget> getWidgets() {
				return widgets;
			}
		};
	}

	private void createWidgetsByNodeProperties(final Node node, final List<Widget> widgets, Function<Property, Boolean> filter) throws RepositoryException {
		createWidgetsByNodeProperties(node.getProperties(), widgets, filter);
	}
	
	private void createWidgetsByNodeProperties(final Iterator<Property> propertiesIter, final List<Widget> widgets, Function<Property, Boolean> filter) throws RepositoryException {
		while (propertiesIter.hasNext()) {
			final Property property = propertiesIter.next();
			if (filter.apply(property)) {
				widgets.add(createWidgetFromProperty(property, null));
			}
		}
	}

	private Widget createWidgetFromProperty(final Property property, BeanDefinition.PropertyDefinition propertyDefinition) {
		final String inputType = propertyToInputType(property);
		final Iterable<NamedValue> namedValues = createNamedValues(property);
		final Map<String, Object> metaData = propertyDefinition == null ? Collections.EMPTY_MAP : propertyDefinition.getMetaDataMap();
		Widget widget = new Widget() {
			@Override
			public String getTitle() {
				return property.getName();
			}
			
			@Override
			public Property getProperty() {
				return property;
			}
			
			@Override
			public Iterable<NamedValue> getNamedValues() {
				return namedValues;
			}
			
			@Override
			public String getInputType() {
				return inputType;
			}

			@Override
			public Type getPropertyType() {
				return property.getType();
			}

			@Override
			public boolean isPropertyMultiValued() {
				return property.isMultiValued();
			}

			@Override
			public String getPropertyName() {
				return property.getName();
			}

			@Override
			public Map<String, Object> getMetaData() {
				return metaData;
			}
			
		};
		return widget;
	}

	private String propertyToInputType(final Property property) {
		return propertyTypeToInputType(property.getType());
	}
	
	private String propertyTypeToInputType(final Type type) {
		String inputType;
		if (type == Property.Type.STRING) {
			inputType = "text";
		} else if (type == Property.Type.BOOLEAN) {
			inputType = "text";
		} else if (type == Property.Type.DATE) {
			inputType = "text";
		} else if (type == Property.Type.DECIMAL) {
			inputType = "number";
		} else if (type == Property.Type.DOUBLE) {
			inputType = "number";
		} else if (type == Property.Type.LONG) {
			inputType = "number";
		} else if (type == Property.Type.BINARY) {
			inputType = "file";
		} else {
			inputType = "text";
		}
		return inputType;
	}
	
}
