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
import org.bndly.schema.api.Record;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.TypeAttribute;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RecordMappingSaxHandler extends DefaultHandler {

	private final Stack<Record> recordStack = new Stack<>();
	private final ConverterRegistry converterRegistry;
	private boolean isFirstElement;

	public RecordMappingSaxHandler(Record record, ConverterRegistry converterRegistry) {
		recordStack.push(record);
		this.converterRegistry = converterRegistry;
	}

	@Override
	public void startDocument() throws SAXException {
		isFirstElement = true;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// we apply the present attributes to the record
		Record parentRecord = recordStack.peek();
		if (parentRecord == null) {
			return;
		}

		Record record = null;
		String type = attributes.getValue("type");
		boolean isRef = false;
		if (type != null && !isFirstElement) {
			String isReference = attributes.getValue("isReference");
			String rawId = attributes.getValue("id");
			Long id = null;
			try {
				id = new Long(rawId);
			} catch (NumberFormatException e) {
				// then there is no id
			}
			if (isReference != null) {
				if (id == null) {
					// this should not happen
				} else {
					isRef = true;
					record = parentRecord.getContext().create(type, id);
					record.setIsReference(true);
				}
			} else {
				record = parentRecord.getContext().create(type);
			}
			if (record != null) {
				// set the instane as attribute in the parent record
				// TODO: deal with references
				String attributeName = localName;
				if ("".equals(attributeName)) {
					attributeName = qName;
				}
				parentRecord.setAttributeValue(attributeName, record);
			}
		} else {
			record = parentRecord;
		}
		recordStack.push(record);
		if (record == null || isRef) {
			return;
		}
		mapAttributesToRecord(attributes, record);
		isFirstElement = false;
	}

	private void mapAttributesToRecord(Attributes attributes, Record record) {
		int attributeCount = attributes.getLength();
		for (int i = 0; i < attributeCount; i++) {
			String attributeName = attributes.getLocalName(i);
			if ("type".equals(attributeName)) {
				continue;
			}
			if ("id".equals(attributeName)) {
				if (!isFirstElement) {
					Converter converter = converterRegistry.getConverter(String.class, Long.class);
					try {
						Long id = (Long) converter.convert(attributes.getValue(i));
						record.setId(id);
					} catch (ConversionException ex) {
						// do nothing
					}
				}
				continue;
			}
			Attribute def = record.getAttributeDefinition(attributeName);
			// it is a known attribute
			if (def != null) {
				// binary attributes have to be handled differently
				if (!BinaryAttribute.class.isInstance(def)) {
					String attributeValue = attributes.getValue(i);
					if (StringAttribute.class.isInstance(def)) {
						record.setAttributeValue(attributeName, attributeValue);
					} else {
						Class targetType = null;
						// map the attribute to the correct java target type
						if (DecimalAttribute.class.isInstance(def)) {
							targetType = BigDecimal.class;
						} else if (BooleanAttribute.class.isInstance(def)) {
							targetType = Boolean.class;
						} else if (DateAttribute.class.isInstance(def)) {
							targetType = Date.class;
						} else if (MixinAttribute.class.isInstance(def)) {
							// do nothing
						} else if (TypeAttribute.class.isInstance(def)) {
							// do nothing
						} else if (InverseAttribute.class.isInstance(def)) {
							// do nothing
						} else if (JSONAttribute.class.isInstance(def)) {
							// do nothing
						} else {
							// do nothing
						}
						if (null != targetType) {
							Converter converter = converterRegistry.getConverter(String.class, targetType);
							if (converter != null) {
								converter = converter.canConvertFromTo(String.class, targetType);
							}
							if (converter != null) {
								try {
									Object convertedValue = converter.convert(attributeValue);
									record.setAttributeValue(attributeName, convertedValue);
								} catch (ConversionException ex) {
									// do nothing
								}
							}
						}

					}
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		recordStack.pop();
	}
	
}
