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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.bndly.schema.api.repository.NodeTypes;
import java.io.InputStream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class XMLNodeResourceRenderer implements ResourceRenderer {

	private final XMLOutputFactory factory = XMLOutputFactory.newInstance();
	
	@Reference
	private Base64Service base64Service;
	
	@Override
	public boolean supports(Resource resource, Context context) {
		if (!NodeResource.class.isInstance(resource)) {
			return false;
		}
		if (context.getDesiredContentType() == null) {
			return false;
		}
		return ContentType.XML.getName().equals(context.getDesiredContentType().getName());
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		NodeResource nodeResource = (NodeResource) resource;
		Node node = nodeResource.getNode();
		// render it
		context.setOutputContentType(ContentType.XML, "UTF-8");
		OutputStream outputStream = context.getOutputStream();
		try {
			renderNodeXml(node, context, outputStream);
		} catch (XMLStreamException | RepositoryException ex) {
			throw new IllegalStateException("failed to render node as xml", ex);
		}
	}

	private void renderNodeXml(Node node, Context context, OutputStream outputStream) throws IOException, XMLStreamException, RepositoryException {
		SimpleDateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		XMLStreamWriter writer = factory.createXMLStreamWriter(outputStream, "UTF-8");
		writer.writeStartDocument();
		renderNodeXml(node, writer, xmlDateFormat);
		writer.writeEndDocument();
		writer.flush();
	}
	
	private void renderNodeXml(Node node, XMLStreamWriter writer, SimpleDateFormat xmlDateFormat) throws XMLStreamException, RepositoryException {
		if (node.getType().equals(NodeTypes.ROOT)) {
			writer.writeStartElement("root");
		} else {
			writer.writeStartElement(node.getName());
		}
		writer.writeAttribute("type", node.getType());
		writer.writeAttribute("path", node.getPath().toString());
		Iterator<Property> propertiesIter = node.getProperties();
		List<Property> multiProperties = null;
		while (propertiesIter.hasNext()) {
			Property next = propertiesIter.next();
			if (next.isMultiValued()) {
				if (multiProperties == null) {
					multiProperties = new ArrayList<>();
				}
				multiProperties.add(next);
				continue;
			}
			if (next.getType() == Property.Type.BOOLEAN) {
				Boolean val = next.getBoolean();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			} else if (next.getType() == Property.Type.DATE) {
				Date val = next.getDate();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(xmlDateFormat, val));
				}
			} else if (next.getType() == Property.Type.DECIMAL) {
				BigDecimal val = next.getDecimal();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			} else if (next.getType() == Property.Type.DOUBLE) {
				Double val = next.getDouble();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			} else if (next.getType() == Property.Type.LONG) {
				Long val = next.getLong();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			} else if (next.getType() == Property.Type.STRING) {
				String val = next.getString();
				if (val != null) {
					writer.writeAttribute(next.getName(), val);
				}
			} else if (next.getType() == Property.Type.ENTITY) {
				Record val = next.getEntity();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			} else if (next.getType() == Property.Type.BINARY) {
				InputStream val = next.getBinary();
				if (val != null) {
					writer.writeAttribute(next.getName(), toString(val));
				}
			}
		}
		if (multiProperties != null) {
			// multi properties create xml elements of the same name
			for (Property property : multiProperties) {
				if (property.getType() == Property.Type.BOOLEAN) {
					Boolean[] vals = property.getBooleans();
					if (vals == null) {
						return;
					}
					for (Boolean val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				} else if (property.getType() == Property.Type.DATE) {
					Date[] vals = property.getDates();
					if (vals == null) {
						return;
					}
					for (Date val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(xmlDateFormat, val));
						}
					}
				} else if (property.getType() == Property.Type.DECIMAL) {
					BigDecimal[] vals = property.getDecimals();
					if (vals == null) {
						return;
					}
					for (BigDecimal val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				} else if (property.getType() == Property.Type.DOUBLE) {
					Double[] vals = property.getDoubles();
					if (vals == null) {
						return;
					}
					for (Double val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				} else if (property.getType() == Property.Type.LONG) {
					Long[] vals = property.getLongs();
					if (vals == null) {
						return;
					}
					for (Long val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				} else if (property.getType() == Property.Type.STRING) {
					String[] vals = property.getStrings();
					if (vals == null) {
						return;
					}
					for (String val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", val);
						}
					}
				} else if (property.getType() == Property.Type.ENTITY) {
					Record[] vals = property.getEntities();
					if (vals == null) {
						return;
					}
					for (Record val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				} else if (property.getType() == Property.Type.BINARY) {
					InputStream[] vals = property.getBinaries();
					if (vals == null) {
						return;
					}
					for (InputStream val : vals) {
						if (val != null) {
							writer.writeEmptyElement(property.getName());
							writer.writeAttribute("type", property.getType().toString());
							writer.writeAttribute("value", toString(val));
						}
					}
				}
			}

		}
		Iterator<Node> children = node.getChildren();
		while (children.hasNext()) {
			Node child = children.next();
			String type = child.getType();
			if (NodeTypes.UNSTRUCTURED.equals(type)) {
				renderNodeXml(child, writer, xmlDateFormat);
			} else if (NodeTypes.ARRAY.equals(type)) {
				renderNodeXml(child, writer, xmlDateFormat);
			} else {
				// skip
			}

		}
		writer.writeEndElement();
	}

	private String toString(SimpleDateFormat xmlDateFormat, Date val) {
		return xmlDateFormat.format(val);
	}

	private String toString(Boolean val) {
		return Boolean.toString(val);
	}
	
	private String toString(InputStream val) {
		return base64Service.base64Encode(val);
	}
	
	private String toString(BigDecimal val) {
		return val.toString();
	}
	
	private String toString(Double val) {
		return val.toString();
	}
	
	private String toString(Long val) {
		return val.toString();
	}

	private String toString(Record val) {
		return val.getType().getName() + ";" + val.getId().toString();
	}

}
