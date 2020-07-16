package org.bndly.rest.repository.resources;

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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.XWWWFormEncodedDataParser;
import org.bndly.schema.api.repository.EntityReference;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.PathBuilder;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = { PayloadToNodeConverter.class, ParameterPayloadToNodeConverter.class }, immediate = true)
public class ParameterPayloadToNodeConverter implements PayloadToNodeConverter {
	private static final Logger LOG = LoggerFactory.getLogger(ParameterPayloadToNodeConverter.class);

	@Reference
	private Base64Service base64Service;

	@Reference
	private ConverterRegistry converterRegistry;
	
	@Override
	public void convertPayload(final Node node, Context context) throws RepositoryException, IOException {
		ResourceURI uri = context.getURI();
		List<ResourceURI.QueryParameter> parameters = uri.getParameters();
		final ModificationManagerImpl modificationManagerImpl = new ModificationManagerImpl();
		if (parameters != null) {
			for (ResourceURI.QueryParameter parameter : parameters) {
				handleParameter(parameter, node, modificationManagerImpl);
			}
		}
		if (context.getMethod() == HTTPMethod.POST || context.getMethod() == HTTPMethod.PUT) {
			ReplayableInputStream is = context.getInputStream();
			String encoding = context.getInputEncoding();
			try (InputStreamReader reader = new InputStreamReader(is, encoding)) {
				XWWWFormEncodedDataParser parser = new XWWWFormEncodedDataParser(context.createPathCoder());
				parser.parse(reader, new XWWWFormEncodedDataParser.Listener() {
					String currentVariable;

					@Override
					public XWWWFormEncodedDataParser.IterationResult onVariable(String variable) {
						if (currentVariable != null) {
							try {
								handleParameter(currentVariable, null, null, node, modificationManagerImpl);
							} catch (RepositoryException ex) {
								LOG.error("repository exception while parsing form data: " + ex.getMessage(), ex);
								return XWWWFormEncodedDataParser.IterationResult.TERMINATE;
							}
						}
						currentVariable = variable;
						return XWWWFormEncodedDataParser.IterationResult.CONTINUE;
					}

					@Override
					public XWWWFormEncodedDataParser.IterationResult onVariableValue(String value) {
						try {
							handleParameter(currentVariable, value, null, node, modificationManagerImpl);
						} catch (RepositoryException ex) {
							LOG.error("repository exception while parsing form data: " + ex.getMessage(), ex);
							return XWWWFormEncodedDataParser.IterationResult.TERMINATE;
						} finally {
							currentVariable = null;
						}
						return XWWWFormEncodedDataParser.IterationResult.CONTINUE;
					}

					@Override
					public void onEnd() {
						if (currentVariable != null) {
							try {
								handleParameter(currentVariable, null, null, node, modificationManagerImpl);
							} catch (RepositoryException ex) {
								LOG.error("repository exception while parsing form data: " + ex.getMessage(), ex);
							}
						}
					}
				});
			}
		}
		modificationManagerImpl.flush();
	}
	
	private void handleParameter(ResourceURI.QueryParameter parameter, Node node, ModificationManager modificationManager) throws RepositoryException {
		handleParameter(parameter.getName(), parameter.getValue(), null, node, modificationManager);
	}
	
	public void handleParameter(final String name, final String value, final InputStream valueInputStream, Node node, ModificationManager modificationManager) throws RepositoryException {
		PropertyDescription propertyDescription = derivePropertyDescriptionFromParameterName(name, node);
		if (propertyDescription == null) {
			LOG.warn("could not create property description for parameter {}", name);
			return;
		}
		final boolean skipEmpty = "skipEmpty".equals(propertyDescription.getAction());
		final boolean dropProperty = "remove".equals(propertyDescription.getAction()) && propertyDescription.getIndex() == null;
		final boolean dropValue = "remove".equals(propertyDescription.getAction()) && propertyDescription.getIndex() != null;
		Node targetNode = assertNodeExists(node.getRepositorySession(), propertyDescription.getNodePath());
		Property prop;
		try {
			prop = targetNode.getProperty(propertyDescription.getName());
			if (dropProperty) {
				prop.remove();
				return;
			}
			if (prop.getType() != propertyDescription.getType()) {
				prop.remove();
				prop = createProperty(propertyDescription, targetNode);
			} else if (prop.isMultiValued() != propertyDescription.isMulti()) {
				prop.remove();
				prop = createProperty(propertyDescription, targetNode);
			}
		} catch (PropertyNotFoundException e) {
			if (!dropProperty) {
				prop = createProperty(propertyDescription, targetNode);
			} else {
				// property does not exist. hence we are fine
				return;
			}
		}
		Integer valueIndex = propertyDescription.getIndex();
		final Modification modification = modificationManager.createModification(prop);
		if (dropValue) {
			modification.dropValue(valueIndex);
			return;
		}
		boolean isAdd = valueIndex == null && prop.isMultiValued();
		boolean isSetAtIndex = valueIndex != null && prop.isMultiValued();
		try {
			if (prop.getType() == Property.Type.STRING) {
				if (!prop.isMultiValued()) {
					prop.setValue(value);
				} else {
					if (isAdd) {
						modification.addValue(value);
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, value);
					} else {
						modification.setValues(value);
					}
				}
			} else if (prop.getType() == Property.Type.BOOLEAN) {
				if (!prop.isMultiValued()) {
					prop.setValue(convert(value, Boolean.class));
				} else {
					if (isAdd) {
						modification.addValue(convert(value, Boolean.class));
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, convert(value, Boolean.class));
					} else {
						modification.setValues(convert(value, Boolean.class));
					}
				}
			} else if (prop.getType() == Property.Type.DATE) {
				if (!prop.isMultiValued()) {
					prop.setValue(convert(value, Date.class));
				} else {
					if (isAdd) {
						modification.addValue(convert(value, Date.class));
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, convert(value, Date.class));
					} else {
						modification.setValues(convert(value, Date.class));
					}
				}
			} else if (prop.getType() == Property.Type.DECIMAL) {
				if (!prop.isMultiValued()) {
					prop.setValue(convert(value, BigDecimal.class));
				} else {
					if (isAdd) {
						modification.addValue(convert(value, BigDecimal.class));
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, convert(value, BigDecimal.class));
					} else {
						modification.setValues(convert(value, BigDecimal.class));
					}
				}
			} else if (prop.getType() == Property.Type.DOUBLE) {
				if (!prop.isMultiValued()) {
					prop.setValue(convert(value, Double.class));
				} else {
					if (isAdd) {
						modification.addValue(convert(value, Double.class));
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, convert(value, Double.class));
					} else {
						modification.setValues(convert(value, Double.class));
					}
				}
			} else if (prop.getType() == Property.Type.LONG) {
				if (!prop.isMultiValued()) {
					prop.setValue(convert(value, Long.class));
				} else {
					if (isAdd) {
						modification.addValue(convert(value, Long.class));
					} else if (isSetAtIndex) {
						modification.setValue(valueIndex, convert(value, Long.class));
					} else {
						modification.setValues(convert(value, Long.class));
					}
				}
			} else if (prop.getType() == Property.Type.BINARY) {
				InputStream is;
				if (valueInputStream != null) {
					is = valueInputStream;
				} else {
					if (value != null && !value.isEmpty()) {
						is = ReplayableInputStream.newInstance(base64Service.base64Decode(value));
					} else {
						is = null;
					}
				}
				if (skipEmpty && is == null) {
					// ignore empty
				} else {
					if (!prop.isMultiValued()) {
						prop.setValue(is);
					} else {
						if (isAdd) {
							modification.addValue(is);
						} else if (isSetAtIndex) {
							modification.setValue(valueIndex, is);
						} else {
							modification.setValues(is);
						}
					}
				}
			} else if (prop.getType() == Property.Type.ENTITY) {
				int index = value.indexOf(";");
				if (index > -1) {
					EntityReference entityReference = prop.getRepositorySession().createEntityReference(value.substring(0, index), convert(value.substring(index + 1), Long.class));
					if (!prop.isMultiValued()) {
						prop.setValue(entityReference);
					} else {
						if (isAdd) {
							modification.addValue(entityReference);
						} else if (isSetAtIndex) {
							modification.setValue(valueIndex, entityReference);
						} else {
							modification.setValues(entityReference);
						}
					}
				} else {
					LOG.warn("entity reference has wrong format {}", value);
				}
			} else {
				LOG.error("can not convert string to {}", propertyDescription.getType());
				// can not convert yet
			}
		} catch (ConversionException e) {
			LOG.debug("could not convert value for property: " + e.getMessage(), e);
		}
	}
	
	private <E> E convert(String input, Class<E> type) throws ConversionException {
		return (E) converterRegistry.getConverter(String.class, type).convert(input);
	}
	
	private Property createProperty(PropertyDescription propertyDescription, Node targetNode) throws RepositoryException {
		Property prop;
		if (propertyDescription.isMulti()) {
			prop = targetNode.createMultiProperty(propertyDescription.getName(), propertyDescription.getType());
		} else {
			prop = targetNode.createProperty(propertyDescription.getName(), propertyDescription.getType());
		}
		return prop;
	}
	
	public PropertyDescription derivePropertyDescriptionFromParameterName(String name, Node node) throws RepositoryException {
		int startOfProperty = name.indexOf(".");
		if (startOfProperty < 0) {
			startOfProperty = -1;
		}
		int startOfTypeHint = name.indexOf("@", startOfProperty);
		int startOfArray = name.indexOf("[", startOfProperty);
		int startOfAction = name.indexOf("#", startOfProperty);
		
		final Path path;
		if (startOfProperty == -1) {
			path = node.getPath();
		} else {
			path = PathBuilder.newInstance(name.substring(0, startOfProperty)).build();
		}
		
		final String propertyName;
		if (startOfTypeHint > -1) {
			propertyName = name.substring(startOfProperty + 1, startOfTypeHint);
		} else if (startOfArray > -1) {
			propertyName = name.substring(startOfProperty + 1, startOfArray);
		} else if (startOfAction > -1) {
			propertyName = name.substring(startOfProperty + 1, startOfAction);
		} else {
			propertyName = name.substring(startOfProperty + 1);
		}
		
		final Property.Type typeHint;
		if (startOfTypeHint > -1) {
			String hint;
			if (startOfArray > -1) {
				hint = name.substring(startOfTypeHint + 1, startOfArray);
			} else if (startOfAction > -1) {
				hint = name.substring(startOfTypeHint + 1, startOfAction);
			} else {
				hint = name.substring(startOfTypeHint + 1);
			}
			typeHint = Property.Type.valueOf(hint);
		} else {
			typeHint = null;
		}

		final boolean isMulti;
		final Integer index;
		if (startOfArray > -1) {
			int endOfArray = name.indexOf("]", startOfArray);
			if (endOfArray > -1) {
				isMulti = true;
				if (endOfArray == (startOfArray + 1)) {
					// no index
					index = null;
				} else {
					index = Integer.valueOf(name.substring(startOfArray + 1, endOfArray));
				}
			} else {
				// then the array is not closed properly
				return null;
			}
		} else {
			isMulti = false;
			index = null;
		}
		
		final String action;
		if (startOfAction > -1) {
			action = name.substring(startOfAction + 1);
		} else {
			action = null;
		}
		
		
		final Property.Type finalType;
		if (typeHint == null) {
			Property.Type tmp;
			try {
				Node targetNode = node.getRepositorySession().getNode(path);
				try {
					Property prop = targetNode.getProperty(propertyName);
					tmp = prop.getType();
				} catch (PropertyNotFoundException ex) {
					tmp = Property.Type.STRING;
				}
			} catch (NodeNotFoundException e) {
				tmp = Property.Type.STRING;
			}
			finalType = tmp;
		} else {
			finalType = typeHint;
		}
		return new PropertyDescription() {

			@Override
			public Path getNodePath() {
				return path;
			}

			@Override
			public String getName() {
				return propertyName;
			}

			@Override
			public Property.Type getType() {
				return finalType;
			}

			@Override
			public Integer getIndex() {
				return index;
			}

			@Override
			public boolean isMulti() {
				return isMulti;
			}

			@Override
			public String getAction() {
				return action;
			}
			
		};
	}
	
	private Node assertNodeExists(RepositorySession session, Path path) throws RepositoryException {
		return assertNodeExists(session, path.getElementNames());
	}
	
	private Node assertNodeExists(RepositorySession session, List<String> nodeNames) throws RepositoryException {
		Node current = session.getRoot();
		for (String nodeName : nodeNames) {
			try {
				current = current.getChild(nodeName);
			} catch (NodeNotFoundException e) {
				current = current.createChild(nodeName, NodeTypes.UNSTRUCTURED);
			}
		}
		return current;
	}

	@Override
	public ContentType getSupportedContentType() {
		return new ContentType() {

			@Override
			public String getName() {
				return "application/x-www-form-urlencoded";
			}

			@Override
			public String getExtension() {
				return null;
			}
		};
	}

}
