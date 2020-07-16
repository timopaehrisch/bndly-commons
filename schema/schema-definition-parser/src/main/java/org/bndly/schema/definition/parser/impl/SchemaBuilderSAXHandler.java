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

import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaBuilderSAXHandler extends DefaultHandler {

	private int level;
	private boolean isFirstElement;
	private Boolean isReadingRootSchema;
	private boolean inUniqueConstraint;
	private List<String> attributesInUniqueConstraint;
	private SchemaBuilder schemaBuilder;
	private final AnnotationCallback schemaAnnotationCallback = new AnnotationCallback() {

		@Override
		public void onAnnotation(String name, String value) {
			schemaBuilder.annotateSchema(name, value);
		}
	};
	private final AnnotationCallback typeOrMixinAnnotationCallback = new AnnotationCallback() {

		@Override
		public void onAnnotation(String name, String value) {
			schemaBuilder.annotateTypeOrMixin(name, value);
		}
	};
	private final AnnotationCallback attributeAnnotationCallback = new AnnotationCallback() {

		@Override
		public void onAnnotation(String name, String value) {
			schemaBuilder.annotateAttribute(name, value);
		}
	};

	public SchemaBuilder getSchemaBuilder() {
		return schemaBuilder;
	}

	@Override
	public void startDocument() throws SAXException {
		isFirstElement = true;
		level = 0;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		level++;
		if (isFirstElement) {
			handleRootElement(uri, localName, qName, attributes);
		} else {
			if (level == 2) {
				// type or mixin definition
				if (isAnnotationsElement(uri, localName, qName, attributes)) {
					handleAnnotationsElement(uri, localName, qName, attributes, schemaAnnotationCallback);
				} else if (isUniqueElement(uri, localName, qName, attributes)) {
					inUniqueConstraint = true;
					handleUniqueElement(uri, localName, qName, attributes);
				} else {
					handleTypeOrMixinElement(uri, localName, qName, attributes);
				}
			} else if (level == 3) {
				if (isAnnotationsElement(uri, localName, qName, attributes)) {
					// annotate type or mixin
					handleAnnotationsElement(uri, localName, qName, attributes, typeOrMixinAnnotationCallback);
				} else if (inUniqueConstraint) {
					// collect attributes for unique constraint
					attributesInUniqueConstraint.add(qName);
				} else {
					handleAttributeInTypeOrMixin(uri, localName, qName, attributes);
				}
			} else if (level == 4) {
				if (isAnnotationsElement(uri, localName, qName, attributes)) {
					handleAnnotationsElement(uri, localName, qName, attributes, attributeAnnotationCallback);
				}
			}
		}
		isFirstElement = false;
	}

	private boolean isAnnotationsElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		return "annotations".equals(qName);
	}
	private boolean isUniqueElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		return "unique".equals(qName);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (level == 2) {
			if (inUniqueConstraint) {
				String[] attributeNames = new String[attributesInUniqueConstraint.size()];
				for (int i = 0; i < attributesInUniqueConstraint.size(); i++) {
					String attributeName = attributesInUniqueConstraint.get(i);
					attributeNames[i] = attributeName;
				}
				schemaBuilder.unique(attributeNames);
				inUniqueConstraint = false;
				attributesInUniqueConstraint = null;
			}
		}
		level--;
	}

	private void handleRootElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (isReadingRootSchema == null && !"schema".equals(qName)) {
			throw new IllegalStateException("root element is not 'schema'");
		} else if (isReadingRootSchema == null) {
			isReadingRootSchema = true;
		} else if (isReadingRootSchema) {
			if (!"schemaExtension".equals(qName)) {
				throw new IllegalStateException("root element is not 'schemaExtension'");
			} else {
				isReadingRootSchema = false;
			}
		}
		String namespace = attributes.getValue("namespace");
		if (namespace == null) {
			throw new IllegalStateException("namespace not defined on schema root");
		}
		String name = attributes.getValue("name");
		if (name == null) {
			throw new IllegalStateException("namespace not defined on schema root");
		}
		if (isReadingRootSchema) {
			schemaBuilder = new SchemaBuilder(name, namespace);
		} else {
			// TODO: check the namespace and name
		}
	}

	private void handleTypeOrMixinElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String kind = getStringAttribute(attributes, "kind", "type", "mixin");
		boolean isVirtual = getBooleanAttribute(attributes, "virtual", false);
			
		if ("type".equals(kind)) {
			if (isVirtual) {
				schemaBuilder.virtualType(qName);
			} else {
				schemaBuilder.type(qName);
			}
			lookForAbstract(attributes);
			lookForParentType(attributes);
			lookForMixins(attributes);
		} else if ("mixin".equals(kind)) {
			if (isVirtual) {
				schemaBuilder.virtualMixin(qName);
			} else {
				schemaBuilder.mixin(qName);
			}
		} else {
			throw new IllegalStateException("unsupported kind '" + kind + "' on type or mixin element " + qName);
		}
	}
	
	private void handleAnnotationsElement(String uri, String localName, String qName, Attributes attributes, AnnotationCallback annotationCallback) throws SAXException {
		int length = attributes.getLength();
		for (int i = 0; i < length; i++) {
			String annotationName = attributes.getQName(i);
			String annotationValue = attributes.getValue(i);
			if (annotationValue != null) {
				annotationCallback.onAnnotation(annotationName, annotationValue);
			}
		}
	}
	
	private void handleUniqueElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		this.attributesInUniqueConstraint = new ArrayList<>();
		String type = attributes.getValue("type");
		String mixin = attributes.getValue("mixin");
		if (type == null && mixin == null) {
			throw new IllegalStateException("either type or mixin has to be provided when declaring a unique constraint");
		} else if (type != null) {
			schemaBuilder.type(type);
		} else {
			schemaBuilder.mixin(mixin);
		}
	}
	
	private void handleAttributeInTypeOrMixin(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String kind = getStringAttribute(
				attributes, 
				"kind", 
				"string", 
				"crypto", 
				"decimal", 
				"boolean", 
				"date", 
				"binary", 
				"jsonType", 
				"jsonMixin", 
				"type", 
				"mixin", 
				"inverseType", 
				"inverseMixin"
		);

		boolean virtual = getBooleanAttribute(attributes, "virtual", false);

		if ("string".equals(kind)) {
			schemaBuilder.attribute(qName, StringAttribute.class);
			Integer length = getIntegerAttribute(attributes, "length", null);
			if (length != null) {
				schemaBuilder.attributeValue("length", length);
			}
			Boolean isLong = getBooleanAttribute(attributes, "isLong", null);
			if (isLong != null) {
				schemaBuilder.attributeValue("isLong", isLong);
			}
		} else if ("crypto".equals(kind)) {
			schemaBuilder.attribute(qName, CryptoAttribute.class);
			String ref = getStringAttribute(attributes, "ref");
			if (ref != null) {
				schemaBuilder.attributeValue("cryptoReference", ref);
			}
			boolean autoDecrypted = getBooleanAttribute(attributes, "autoDecrypted", false);
			schemaBuilder.attributeValue("autoDecrypted", autoDecrypted);
			boolean plainString = getBooleanAttribute(attributes, "plainString", false);
			schemaBuilder.attributeValue("plainString", plainString);
			Integer decimalPlaces = getIntegerAttribute(attributes, "decimalPlaces", null);
			if (decimalPlaces != null) {
				schemaBuilder.attributeValue("decimalPlaces", decimalPlaces);
			}
		} else if ("decimal".equals(kind)) {
			schemaBuilder.attribute(qName, DecimalAttribute.class);
			Integer length = getIntegerAttribute(attributes, "length", null);
			if (length != null) {
				schemaBuilder.attributeValue("length", length);
			}
			Integer decimalPlaces = getIntegerAttribute(attributes, "decimalPlaces", null);
			if (decimalPlaces != null) {
				schemaBuilder.attributeValue("decimalPlaces", decimalPlaces);
			}
		} else if ("boolean".equals(kind)) {
			schemaBuilder.attribute(qName, BooleanAttribute.class);
		} else if ("date".equals(kind)) {
			schemaBuilder.attribute(qName, DateAttribute.class);
		} else if ("binary".equals(kind)) {
			schemaBuilder.attribute(qName, BinaryAttribute.class);
			Boolean asByteArray = getBooleanAttribute(attributes, "asByteArray", null);
			if (asByteArray != null) {
				schemaBuilder.attributeValue("asByteArray", asByteArray);
			}
		} else if ("jsonType".equals(kind)) {
			String typeName = getStringAttribute(attributes, "type");
			if (typeName == null) {
				throw new IllegalStateException(qName + " did declare a jsonType attribute, but the actual type was not provided");
			}
			schemaBuilder.jsonTypeAttribute(qName, typeName);
		} else if ("jsonMixin".equals(kind)) {
			String mixinName = getStringAttribute(attributes, "mixin");
			if (mixinName == null) {
				throw new IllegalStateException(qName + " did declare a jsonMixin attribute, but the actual mixin was not provided");
			}
			schemaBuilder.jsonTypeAttribute(qName, mixinName);
		} else if ("type".equals(kind)) {
			String typeName = getStringAttribute(attributes, "type");
			if (typeName == null) {
				throw new IllegalStateException(qName + " did declare a type attribute, but the actual type was not provided");
			}
			schemaBuilder.typeAttribute(qName, typeName);
			String toOneAttribute = getStringAttribute(attributes, "toOneAttribute");
			if(toOneAttribute != null) {
				schemaBuilder.toOneAttribute(toOneAttribute);
			}
			handleXMLAttributesOfNamedAttributeHolderAttribute(attributes);
		} else if ("mixin".equals(kind)) {
			String mixinName = getStringAttribute(attributes, "mixin");
			if (mixinName == null) {
				throw new IllegalStateException(qName + " did declare a mixin attribute, but the actual mixin was not provided");
			}
			schemaBuilder.mixinAttribute(qName, mixinName);
			String toOneAttribute = getStringAttribute(attributes, "toOneAttribute");
			if(toOneAttribute != null) {
				schemaBuilder.toOneAttribute(toOneAttribute);
			}
			handleXMLAttributesOfNamedAttributeHolderAttribute(attributes);
		} else if ("inverseType".equals(kind)) {
			String typeName = getStringAttribute(attributes, "type");
			if (typeName == null) {
				throw new IllegalStateException(qName + " did declare an inverseType attribute, but the actual type was not provided");
			}
			String typeAttributeName = getStringAttribute(attributes, "attribute");
			// the attribute name may be null, when the type is virtual.
			schemaBuilder.inverseTypeAttribute(qName, typeName, typeAttributeName);
			handleXMLAttributesOfInverseAttribute(attributes);
		} else if ("inverseMixin".equals(kind)) {
			String mixinName = getStringAttribute(attributes, "mixin");
			if (mixinName == null) {
				throw new IllegalStateException(qName + " did declare an inverseMixin attribute, but the actual mixin was not provided");
			}
			String mixinAttributeName = getStringAttribute(attributes, "attribute");
			if (mixinAttributeName == null) {
				throw new IllegalStateException(qName + " did declare an inverseMixin attribute, but the referred attribute in '" + mixinName + "' was not provided");
			}
			schemaBuilder.inverseMixinAttribute(qName, mixinName, mixinAttributeName);
			handleXMLAttributesOfInverseAttribute(attributes);
		} else {
			throw new IllegalStateException("unsupported kind '" + kind + "' on attribute element " + qName);
		}

		// across all kinds of attributes we can declare the following
		if (virtual) {
			schemaBuilder.virtual();
		} else {
			schemaBuilder.nonVirtual();
		}
		// across all kinds of attributes we can declare the following
		if (getBooleanAttribute(attributes, "mandatory", false)) {
			schemaBuilder.mandatory();
		}
		
		Boolean indexed = getBooleanAttribute(attributes, "indexed", null);
		if (indexed != null) {
			if (indexed) {
				schemaBuilder.indexAttribute();
			} else {
				schemaBuilder.preventIndexAttribute();
			}
		}
	}

	protected void handleXMLAttributesOfInverseAttribute(Attributes attributes) {
		Boolean deleteOrphans = getBooleanAttribute(attributes, "deleteOrphans", false);
		if (deleteOrphans) {
			schemaBuilder.deleteOrphans();
		}
	}
	
	protected void handleXMLAttributesOfNamedAttributeHolderAttribute(Attributes attributes) {
		Boolean deleteOrphans = getBooleanAttribute(attributes, "deleteOrphans", false);
		if (deleteOrphans) {
			schemaBuilder.deleteOrphans();
		}
		Boolean nullOnDelete = getBooleanAttribute(attributes, "nullOnDelete", false);
		if (nullOnDelete) {
			schemaBuilder.nullOnDelete();
		}
		Boolean cascadeDelete = getBooleanAttribute(attributes, "cascadeDelete", false);
		if (cascadeDelete) {
			schemaBuilder.cascadeDelete();
		}
	}

	private Boolean getBooleanAttribute(Attributes attributes, String name, Boolean defaultValue) {
		String val = attributes.getValue(name);
		if (val != null) {
			if ("true".equals(val)) {
				return true;
			} else if ("false".equals(val)) {
				return false;
			} else {
				throw new IllegalStateException(name + " attribute should be 'true' or 'false'");
			}
		}
		return defaultValue;
	}
	private Integer getIntegerAttribute(Attributes attributes, String name, Integer defaultValue) {
		String val = attributes.getValue(name);
		if (val != null) {
			return Integer.valueOf(val);
		}
		return defaultValue;
	}
	
	private String getStringAttribute(Attributes attributes, String name, String... allowedValues) {
		String val = attributes.getValue(name);
		if (allowedValues == null || allowedValues.length == 0) {
			return val;
		}
		for (String allowedValue : allowedValues) {
			if (allowedValue.equals(val)) {
				return val;
			}
		}
		throw new IllegalStateException(name + " attribute had an unallowed value '" + val + "'");
	}

	private void lookForAbstract(Attributes attributes) {
		if (getBooleanAttribute(attributes, "abstract", false)) {
			schemaBuilder.abstractType();
		}
	}

	private void lookForParentType(Attributes attributes) {
		String extend = attributes.getValue("extend");
		if (extend != null) {
			schemaBuilder.parentType(extend);
		}
	}

	private void lookForMixins(Attributes attributes) {
		String mixWith = attributes.getValue("mixWith");
		if (mixWith != null) {
			String[] split = mixWith.split(",");
			for (String mixinName : split) {
				schemaBuilder.mixWith(mixinName);
			}
		}
	}
}
