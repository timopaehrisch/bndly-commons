package org.bndly.schema.definition.parser.impl;

/*-
 * #%L
 * Schema Definition Parser
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

import org.bndly.schema.definition.parser.api.SchemaDefinitionIO;
import org.bndly.schema.definition.parser.api.ParsingException;
import org.bndly.schema.definition.parser.api.SerializingException;
import org.bndly.schema.model.Annotatable;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import org.bndly.schema.model.UniqueConstraint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.osgi.service.component.annotations.Component;
import org.xml.sax.SAXException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SchemaDefinitionIO.class)
public class SchemaDefinitionIOImpl implements SchemaDefinitionIO {

	@Override
	public Schema parse(String location, String... extensions) throws ParsingException {
		Path path = Paths.get(location);
		if (!Files.isRegularFile(path)) {
			throw new ParsingException(location + " was not a file");
		}
		try (InputStream locationIS = Files.newInputStream(path, StandardOpenOption.READ)) {
			InputStream[] extensionsInputStreams = new InputStream[extensions.length];
			for (int i = 0; i < extensions.length; i++) {
				String extension = extensions[i];
				Path extensionPath = Paths.get(extension);
				if (!Files.isRegularFile(extensionPath)) {
					throw new ParsingException(extension + " was not a file");
				}
				extensionsInputStreams[i] = Files.newInputStream(extensionPath, StandardOpenOption.READ);
			}
			return parse(locationIS, extensionsInputStreams);
		} catch (IOException e) {
			throw new ParsingException("failed to parse schema from " + location + ": " + e.getMessage(), e);
		}
	}

	@Override
	public Schema parse(InputStream rootSchemaInputStream, InputStream... extensions) throws ParsingException {
		try (InputStream root = rootSchemaInputStream) {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			// Read this about XML expansion attacks: http://blog.bdoughan.com/2011/03/preventing-entity-expansion-attacks-in.html
			parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			SAXParser parser = parserFactory.newSAXParser();
			SchemaBuilderSAXHandler handler = new SchemaBuilderSAXHandler();
			parser.parse(root, handler);
			SchemaBuilder schemaBuilder = handler.getSchemaBuilder();
			if (schemaBuilder == null) {
				throw new ParsingException("no schema builder created while parsing");
			}
			for (InputStream extension : extensions) {
				try (InputStream extensionIS = extension) {
					parser = parserFactory.newSAXParser();
					parser.parse(extensionIS, handler);
				}
			}
			Schema schema = schemaBuilder.getSchema();
			return schema;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new ParsingException("failed to parse schema: " + e.getMessage(), e);
		}
	}

	@Override
	public void serialize(Schema schema, OutputStream outputStream) throws SerializingException {
		try {
			XMLOutputFactory output = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = output.createXMLStreamWriter(outputStream, "UTF-8");
			schema.getName();
			schema.getNamespace();
			writer.writeStartDocument();
			writeSchemaStart(writer, schema);
			List<Mixin> mixins = schema.getMixins();
			if (mixins != null) {
				for (Mixin mixin : mixins) {
					writeNamedAttributeHolderStart(writer, mixin);
					List<Attribute> attributes = mixin.getAttributes();
					if (attributes != null) {
						for (Attribute attribute : attributes) {
							writeAttributeStart(writer, attribute);
							writeAnnotations(writer, attribute);
							writeAttributeEnd(writer, attribute);
						}
					}
					writeAnnotations(writer, mixin);
					writeNamedAttributeHolderEnd(writer, mixin);
				}
			}
			List<Type> types = schema.getTypes();
			if (types != null) {
				for (Type type : types) {
					writeNamedAttributeHolderStart(writer, type);
					List<Attribute> attributes = type.getAttributes();
					if (attributes != null) {
						for (Attribute attribute : attributes) {
							writeAttributeStart(writer, attribute);
							writeAnnotations(writer, attribute);
							writeAttributeEnd(writer, attribute);
						}
					}
					writeAnnotations(writer, type);
					writeNamedAttributeHolderEnd(writer, type);
				}
			}
			List<UniqueConstraint> uniqueConstraints = schema.getUniqueConstraints();
			if (uniqueConstraints != null) {
				for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
					List<Attribute> atts = uniqueConstraint.getAttributes();
					if (atts != null && !atts.isEmpty()) {
						writer.writeStartElement("unique");
						NamedAttributeHolder holder = uniqueConstraint.getHolder();
						if (Type.class.isInstance(holder)) {
							writer.writeAttribute("type", holder.getName());
						} else if (Mixin.class.isInstance(holder)) {
							writer.writeAttribute("mixin", holder.getName());
						} else {
							throw new SerializingException("unsupported named attribute holder");
						}

						for (Attribute att : atts) {
							writer.writeEmptyElement(att.getName());
						}
						writer.writeEndElement();
					}
				}
			}
			writeAnnotations(writer, schema);
			writeSchemaEnd(writer, schema);
			writer.flush();
		} catch (XMLStreamException e) {
			throw new SerializingException("could not serialize schema: " + e.getMessage(), e);
		}
	}
	
	private void writeAnnotations(XMLStreamWriter writer, Annotatable annotatable) throws XMLStreamException {
		Map<String, String> annotations = annotatable.getAnnotations();
		if (annotations != null && !annotations.isEmpty()) {
			writer.writeStartElement("annotations");
			for (Map.Entry<String, String> entrySet : annotations.entrySet()) {
				String key = entrySet.getKey();
				String value = entrySet.getValue();
				writer.writeAttribute(key, value);
			}
			writer.writeEndElement();
		}
	}
	private void writeSchemaStart(XMLStreamWriter writer, Schema schema) throws XMLStreamException {
		writer.writeStartElement("schema");
		writer.writeAttribute("namespace", schema.getNamespace());
		writer.writeAttribute("name", schema.getName());
	}
	private void writeNamedAttributeHolderStart(XMLStreamWriter writer, NamedAttributeHolder namedAttributeHolder) throws XMLStreamException {
		writer.writeStartElement(namedAttributeHolder.getName());
		if (Type.class.isInstance(namedAttributeHolder)) {
			Type type = (Type) namedAttributeHolder;
			writer.writeAttribute("kind", "type");
			if (type.isAbstract()) {
				writer.writeAttribute("abstract", "true");
			}
			Type st = type.getSuperType();
			if (st != null) {
				writer.writeAttribute("extend", st.getName());
			}
			List<Mixin> mixins = type.getMixins();
			if (mixins != null && !mixins.isEmpty()) {
				StringBuffer sb = null;
				for (Mixin mixin : mixins) {
					if (sb == null) {
						sb = new StringBuffer(mixin.getName());
					} else {
						sb.append(",").append(mixin.getName());
					}
				}
				if (sb != null) {
					writer.writeAttribute("mixWith", sb.toString());
				}
			}
		} else if (Mixin.class.isInstance(namedAttributeHolder)) {
			writer.writeAttribute("kind", "mixin");
		} else {
			throw new IllegalStateException("unsupported named attribute holder");
		}
		if (namedAttributeHolder.isVirtual()) {
			writer.writeAttribute("virtual", "true");
		}
	}
	private void writeAttributeStart(XMLStreamWriter writer, Attribute attribute) throws XMLStreamException {
		writer.writeStartElement(attribute.getName());
		if (StringAttribute.class.isInstance(attribute)) {
			StringAttribute sa = (StringAttribute) attribute;
			writer.writeAttribute("kind", "string");
			Integer length = sa.getLength();
			if (length != null) {
				writer.writeAttribute("length", Integer.toString(length));
			}
			Boolean isLong = sa.getIsLong();
			if (isLong != null) {
				writer.writeAttribute("isLong", Boolean.toString(isLong));
			}
		} else if (CryptoAttribute.class.isInstance(attribute)) {
			CryptoAttribute ca = (CryptoAttribute) attribute;
			writer.writeAttribute("kind", "crypto");
			String cryptoReference = ca.getCryptoReference();
			if (cryptoReference != null) {
				writer.writeAttribute("ref", cryptoReference);
			}
			writer.writeAttribute("autoDecrypted", Boolean.toString(ca.isAutoDecrypted()));
			writer.writeAttribute("plainString", Boolean.toString(ca.isPlainString()));
		} else if (DecimalAttribute.class.isInstance(attribute)) {
			DecimalAttribute da = (DecimalAttribute) attribute;
			writer.writeAttribute("kind", "decimal");
			Integer dp = da.getDecimalPlaces();
			if (dp != null) {
				writer.writeAttribute("decimalPlaces", Integer.toString(dp));
			}
			Integer length = da.getLength();
			if (length != null) {
				writer.writeAttribute("length", Integer.toString(length));
			}
		} else if (DateAttribute.class.isInstance(attribute)) {
			writer.writeAttribute("kind", "date");
		} else if (BooleanAttribute.class.isInstance(attribute)) {
			writer.writeAttribute("kind", "boolean");
		} else if (TypeAttribute.class.isInstance(attribute)) {
			TypeAttribute ta = (TypeAttribute) attribute;
			writer.writeAttribute("kind", "type");
			writer.writeAttribute("type", ta.getType().getName());
			writeXMLAttributesOfNamedAttributeHolderAttribute(ta, writer);
		} else if (MixinAttribute.class.isInstance(attribute)) {
			MixinAttribute ma = (MixinAttribute) attribute;
			writer.writeAttribute("kind", "mixin");
			writer.writeAttribute("mixin", ma.getMixin().getName());
			writeXMLAttributesOfNamedAttributeHolderAttribute(ma, writer);
		} else if (InverseAttribute.class.isInstance(attribute)) {
			InverseAttribute ia = (InverseAttribute) attribute;
			NamedAttributeHolder referencedAttributeHolder = ia.getReferencedAttributeHolder();
			NamedAttributeHolderAttribute ra = ia.getReferencedAttribute();
			if (Type.class.isInstance(referencedAttributeHolder)) {
				writer.writeAttribute("kind", "inverseType");
				writer.writeAttribute("type", referencedAttributeHolder.getName());
			} else if (Mixin.class.isInstance(referencedAttributeHolder)) {
				writer.writeAttribute("kind", "inverseMixin");
				writer.writeAttribute("mixin", referencedAttributeHolder.getName());
			} else {
				throw new IllegalStateException("unsupported named attribute holder");
			}
			if (ra != null) {
				writer.writeAttribute("attribute", ra.getName());
			}
			if (Boolean.TRUE.equals(ia.getDeleteOrphans())) {
				writer.writeAttribute("deleteOrphans", "true");
			}
		} else if (JSONAttribute.class.isInstance(attribute)) {
			JSONAttribute ja = (JSONAttribute) attribute;
			NamedAttributeHolder attributeHolder = ja.getNamedAttributeHolder();
			if (Type.class.isInstance(attributeHolder)) {
				writer.writeAttribute("kind", "jsonType");
				writer.writeAttribute("type", attributeHolder.getName());
			} else if (Mixin.class.isInstance(attributeHolder)) {
				writer.writeAttribute("kind", "jsonMixin");
				writer.writeAttribute("mixin", attributeHolder.getName());
			} else {
				throw new IllegalStateException("unsupported named attribute holder");
			}
		} else if (BinaryAttribute.class.isInstance(attribute)) {
			BinaryAttribute ba = (BinaryAttribute) attribute;
			writer.writeAttribute("kind", "binary");
			if (ba.getAsByteArray() != null) {
				writer.writeAttribute("asByteArray", Boolean.toString(ba.getAsByteArray()));
			}
		} else {
			throw new IllegalStateException("unsupported attribute");
		}
		if (attribute.isMandatory()) {
			writer.writeAttribute("mandatory", "true");
		}
		if (attribute.isVirtual()) {
			writer.writeAttribute("virtual", "true");
		} else {
			writer.writeAttribute("virtual", "false");
		}
		if (attribute.isIndexed()) {
			writer.writeAttribute("indexed", "true");
		} else {
			writer.writeAttribute("indexed", "false");
		}
	}

	protected void writeXMLAttributesOfNamedAttributeHolderAttribute(NamedAttributeHolderAttribute ma, XMLStreamWriter writer) throws XMLStreamException {
		if (Boolean.TRUE.equals(ma.getCascadeDelete())) {
			writer.writeAttribute("cascadeDelete", "true");
		}
		if (Boolean.TRUE.equals(ma.getDeleteOrphans())) {
			writer.writeAttribute("deleteOrphans", "true");
		}
		if (Boolean.TRUE.equals(ma.getNullOnDelete())) {
			writer.writeAttribute("nullOnDelete", "true");
		}
		if (ma.getToOneAttribute() != null) {
			writer.writeAttribute("toOneAttribute", ma.getToOneAttribute());
		}
	}
	private void writeAttributeEnd(XMLStreamWriter writer, Attribute attribute) throws XMLStreamException {
		writer.writeEndElement();
	}
	private void writeNamedAttributeHolderEnd(XMLStreamWriter writer, NamedAttributeHolder namedAttributeHolder) throws XMLStreamException {
		writer.writeEndElement();
	}
	private void writeSchemaEnd(XMLStreamWriter writer, Schema schema) throws XMLStreamException {
		writer.writeEndElement();
	}
	
}
