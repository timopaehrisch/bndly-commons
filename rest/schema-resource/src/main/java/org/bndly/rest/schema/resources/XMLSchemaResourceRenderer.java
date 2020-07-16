package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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
import org.bndly.common.converter.api.Converter;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.StringAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class XMLSchemaResourceRenderer implements ResourceRenderer {

	private ConverterRegistry converterRegistry;

	@Override
	public boolean supports(Resource resource, Context context) {
		ResourceURI.Extension ext = resource.getURI().getExtension();
		return SchemaResourceInstance.class.isInstance(resource) && ext != null && "xml".equals(ext.getName());
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		try {
			createElementForRecord(SchemaResourceInstance.class.cast(resource).getRecord(), context);
		} catch (XMLStreamException ex) {
			throw new IOException(ex);
		}
	}

	private void createElementForRecord(Record r, Context context) throws XMLStreamException, IOException {
		OutputStream out = context.getOutputStream();
		// create an XMLOutputFactory
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		// create XMLEventWriter
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(out, "UTF-8");
		// create an EventFactory
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		// create and write Start Tag
		StartDocument startDocument = eventFactory.createStartDocument();
		eventWriter.add(startDocument);

		writeRecordAsElement(r, eventFactory, eventWriter, context);
		// create config open tag

		eventWriter.add(eventFactory.createEndDocument());
		eventWriter.flush();
		eventWriter.close();
	}

	private void writeRecordAsElement(Record r, final XMLEventFactory eventFactory, final XMLEventWriter eventWriter, Context context) throws XMLStreamException {
		writeRecordAsElement(r, eventFactory, eventWriter, null, context);
	}

	private void writeRecordAsElement(Record r, final XMLEventFactory eventFactory, final XMLEventWriter eventWriter, String elementName, final Context context) throws XMLStreamException {
		final List<javax.xml.stream.events.Attribute> xmlAtts = new ArrayList<>();

		if (r.getId() != null) {
			try {
				String idAsString = (String) converterRegistry.getConverter(Long.class, String.class).convert(r.getId());
				javax.xml.stream.events.Attribute xmlAtt = eventFactory.createAttribute("id", idAsString);
				xmlAtts.add(xmlAtt);
			} catch (ConversionException ex) {
				// ignore this
			}
		}

		if (r.isReference()) {
			try {
				String isReferenceAsString = (String) converterRegistry.getConverter(Boolean.class, String.class).convert(Boolean.TRUE);
				javax.xml.stream.events.Attribute xmlAtt = eventFactory.createAttribute("isReference", isReferenceAsString);
				xmlAtts.add(xmlAtt);
			} catch (ConversionException ex) {
				// ignore this
			}
		}

		final List<AttributeValue> valuesForNestedElements = new ArrayList<>();
		r.iteratePresentValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				String valueAsString = null;
				if (StringAttribute.class.isInstance(attribute)) {
					Object value = record.getAttributeValue(attribute.getName());
					if (value == null) {
						return;
					}
					valueAsString = (String) value;
				} else if (DecimalAttribute.class.isInstance(attribute) || DateAttribute.class.isInstance(attribute) || BooleanAttribute.class.isInstance(attribute)) {
					Object value = record.getAttributeValue(attribute.getName());
					if (value == null) {
						return;
					}
					Converter<Object, String> converter = converterRegistry.getConverter(value.getClass(), String.class);
					if (converter != null) {
						try {
							valueAsString = converter.convert(value);
						} catch (ConversionException ex) {
							// do nothing
						}
					}
				} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					Record nestedRecord = record.getAttributeValue(attribute.getName(), Record.class);
					valuesForNestedElements.add(new AttributeValue(attribute, nestedRecord));
				} else if (InverseAttribute.class.isInstance(attribute)) {
					Object value = record.getAttributeValue(attribute.getName());
					if (value == null) {
						return;
					}
					valuesForNestedElements.add(new AttributeValue(attribute, value));
				} else if (JSONAttribute.class.isInstance(attribute)) {
					Object value = record.getAttributeValue(attribute.getName());
					if (value == null) {
						return;
					}
					valuesForNestedElements.add(new AttributeValue(attribute, value));
				} else if (BinaryAttribute.class.isInstance(attribute)) {
					// create a link to the binary data
					if (record.getId() != null) {
						ResourceURI binary = context.createURIBuilder()
								.pathElement("schema")
								.pathElement(record.getType().getName())
								.pathElement(record.getId().toString())
								.pathElement(attribute.getName())
							.build();
						valueAsString = binary.asString();
					}
				}
				if (valueAsString != null) {
					javax.xml.stream.events.Attribute xmlAtt = eventFactory.createAttribute(attribute.getName(), valueAsString);
					xmlAtts.add(xmlAtt);
				}
			}
		});
		if (elementName == null) {
			elementName = r.getType().getName();
		} else {
			javax.xml.stream.events.Attribute xmlAtt = eventFactory.createAttribute("type", r.getType().getName());
			xmlAtts.add(xmlAtt);
		}
		StartElement element = eventFactory.createStartElement("", "", elementName, xmlAtts.iterator(), null);
		eventWriter.add(element);
		// write the nested Records
		for (AttributeValue attributeValue : valuesForNestedElements) {
			Attribute att = attributeValue.getAttribute();
			Object v = attributeValue.getValue();
			if (v != null) {
				if (Record.class.isInstance(v)) {
					writeRecordAsElement((Record) v, eventFactory, eventWriter, att.getName(), context);
				} else if (List.class.isInstance(v)) {
					StartElement listWrapperElement = eventFactory.createStartElement("", "", att.getName(), null, null);
					eventWriter.add(listWrapperElement);
					for (Object object : (List) v) {
						if (object != null && Record.class.isInstance(object)) {
							writeRecordAsElement((Record) object, eventFactory, eventWriter, context);
						}
					}
					eventWriter.add(eventFactory.createEndElement(listWrapperElement.getName(), null));
				} else if (InputStream.class.isInstance(v)) {
					JSObject parsed = (JSObject) new JSONParser().parse((InputStream) v, "UTF-8");
					// map json to xml
					writeJSObjectAsElement(parsed, eventFactory, eventWriter);
				}
			}
		}
		eventWriter.add(eventFactory.createEndElement(element.getName(), null));
	}

	private void writeJSObjectAsElement(JSObject parsed, XMLEventFactory eventFactory, XMLEventWriter eventWriter) throws XMLStreamException {
		writeJSObjectAsElement(parsed, eventFactory, eventWriter, null);
	}

	private void writeJSObjectAsElement(JSObject parsed, XMLEventFactory eventFactory, XMLEventWriter eventWriter, String elementName) throws XMLStreamException {
		if (parsed != null) {
			Set<JSMember> members = parsed.getMembers();
			JSMember typeMember = null;
			final List<javax.xml.stream.events.Attribute> xmlAtts = new ArrayList<>();
			if (members != null) {
				List<JSMember> nestedMembers = new ArrayList<>();
				for (JSMember jSMember : members) {
					String memberName = jSMember.getName().getValue();
					if ("_type".equals(memberName)) {
						typeMember = jSMember;
						JSValue v = typeMember.getValue();
						if (!JSString.class.isInstance(v)) {
							typeMember = null;
						} else {
							if (elementName == null) {
								elementName = ((JSString) v).getValue();
							}
							xmlAtts.add(eventFactory.createAttribute("type", ((JSString) v).getValue()));
						}
					} else {
						JSValue v = jSMember.getValue();
						if (JSString.class.isInstance(v)) {
							String string = ((JSString) v).getValue();
							xmlAtts.add(eventFactory.createAttribute(memberName, string));
						} else if (JSBoolean.class.isInstance(v)) {
							boolean bv = ((JSBoolean) v).isValue();
							try {
								String string = (String) converterRegistry.getConverter(Boolean.class, String.class).convert(bv);
								xmlAtts.add(eventFactory.createAttribute(memberName, string));
							} catch (ConversionException ex) {
								// do nothing
							}
						} else if (JSNull.class.isInstance(v)) {
							// nulls will not be rendered
						} else if (JSNumber.class.isInstance(v)) {
							BigDecimal bv = ((JSNumber) v).getValue();
							try {
								String string = (String) converterRegistry.getConverter(BigDecimal.class, String.class).convert(bv);
								xmlAtts.add(eventFactory.createAttribute(memberName, string));
							} catch (ConversionException ex) {
								// do nothing
							}
						} else {
							nestedMembers.add(jSMember);
						}
					}
				}
				if (typeMember != null) {
					StartElement wrapper = eventFactory.createStartElement("", "", elementName, xmlAtts.iterator(), null);
					eventWriter.add(wrapper);
					if (!nestedMembers.isEmpty()) {
						for (JSMember jSMember : nestedMembers) {
							JSValue v = jSMember.getValue();
							if (JSObject.class.isInstance(v)) {
								// create element with name and type
								writeJSObjectAsElement(parsed, eventFactory, eventWriter, jSMember.getName().getValue());
							} else if (JSArray.class.isInstance(v)) {
								// create wrapper
								StartElement listWrapper = eventFactory.createStartElement("", "", elementName, xmlAtts.iterator(), null);
								eventWriter.add(listWrapper);
								for (JSValue jSValue : ((JSArray) v).getItems()) {
									if (JSObject.class.isInstance(jSValue)) {
										writeJSObjectAsElement((JSObject) jSValue, eventFactory, eventWriter);
									}
								}
								eventWriter.add(eventFactory.createEndElement(listWrapper.getName(), null));
							}
						}
					}
					eventWriter.add(eventFactory.createEndElement(wrapper.getName(), null));
				}
			}
		}
	}

	private class AttributeValue {

		private final Attribute attribute;
		private final Object value;

		public AttributeValue(Attribute attribute, Object value) {
			this.attribute = attribute;
			this.value = value;
		}

		public Attribute getAttribute() {
			return attribute;
		}

		public Object getValue() {
			return value;
		}
	}

	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

}
