package org.bndly.schema.model;

/*-
 * #%L
 * Schema Model
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaBuilder implements SchemaProvider {

	private final Schema schema;
	private final Map<String, NamedAttributeHolder> namedAttributeHoldersByName;
	private final Map<String, Type> typesByName;
	private final Map<String, Mixin> mixinsByName;
	private final List<InverseAttribute> inverseAttributes;
	private NamedAttributeHolder currentAttributeHolder;
	private Type currentType;
	private Mixin currentMixin;
	private Attribute currentAttribute;

	public SchemaBuilder(String name, String namespace) {
		schema = new Schema(name, namespace);
		typesByName = new HashMap<>();
		mixinsByName = new HashMap<>();
		namedAttributeHoldersByName = new HashMap<>();
		inverseAttributes = new ArrayList<>();
	}

	@Override
	public String getSchemaName() {
		return schema.getName();
	}

	@Override
	public Schema getSchema() {
		for (InverseAttribute inverseAttribute : inverseAttributes) {
			Map<String, Attribute> atts = SchemaUtil.collectAttributesAsMap(inverseAttribute.getReferencedAttributeHolder());
			NamedAttributeHolderAttribute namedAttributeHolderAttribute = null;
			Attribute att = atts.get(inverseAttribute.getReferencedAttributeName());
			if (NamedAttributeHolderAttribute.class.isInstance(att)) {
				namedAttributeHolderAttribute = (NamedAttributeHolderAttribute) att;
			}
			if (namedAttributeHolderAttribute == null) {
				if (!inverseAttribute.isVirtual()) {
					throw new IllegalStateException("could not find referenced attribute for inverse attribute");
				}
			}
			inverseAttribute.setReferencedAttribute(namedAttributeHolderAttribute);
		}
		return schema;
	}

	public SchemaBuilder abstractType() {
		currentType.setAbstract(true);
		return this;
	}

	public SchemaBuilder abstractType(String name) {
		type(name);
		return abstractType();
	}

	public SchemaBuilder virtualType(String name) {
		Type type = assertTypeExists(name);
		type.setVirtual(true);
		currentType = type;
		currentAttributeHolder = type;
		return this;
	}

	public SchemaBuilder type(String name) {
		Type type = assertTypeExists(name);
		currentType = type;
		currentAttributeHolder = type;
		return this;
	}

	public SchemaBuilder parentType(String parentTypeName) {
		if (currentType != null) {
			Type superType = assertTypeExists(parentTypeName);
			currentType.setSuperType(superType);
			List<Type> subs = superType.getSubTypes();
			if (subs == null) {
				subs = new ArrayList<>();
				superType.setSubTypes(subs);
			}
			subs.add(currentType);
		}
		return this;
	}

	public SchemaBuilder annotateTypeOrMixin(String key, String value) {
		if (currentAttributeHolder != null) {
			currentAttributeHolder.annotate(key, value);
		}
		return this;
	}

	public SchemaBuilder annotateAttribute(String key, String value) {
		if (currentAttribute != null) {
			currentAttribute.annotate(key, value);
		}
		return this;
	}

	public SchemaBuilder annotateSchema(String key, String value) {
		schema.annotate(key, value);
		return this;
	}

	public SchemaBuilder inverseTypeAttribute(String attributeName, String referencedAttributeHolderName, String referencedAttributeName) {
		attribute(attributeName, InverseAttribute.class);
		Type type = assertTypeExists(referencedAttributeHolderName);
		attributeValue("referencedAttributeHolder", type);
		attributeValue("referencedAttributeName", referencedAttributeName);
		inverseAttributes.add((InverseAttribute) currentAttribute);
		return this;
	}

	public SchemaBuilder inverseMixinAttribute(String attributeName, String referencedAttributeHolderName, String referencedAttributeName) {
		attribute(attributeName, InverseAttribute.class);
		Mixin mixin = assertMixinExists(referencedAttributeHolderName);
		attributeValue("referencedAttributeHolder", mixin);
		attributeValue("referencedAttributeName", referencedAttributeName);
		inverseAttributes.add((InverseAttribute) currentAttribute);
		return this;
	}

	public SchemaBuilder binaryAttribute(String attributeName) {
		attribute(attributeName, BinaryAttribute.class);
		return this;
	}

	public SchemaBuilder jsonTypeAttribute(String attributeName, String typeName) {
		attribute(attributeName, JSONAttribute.class);
		Type type = assertTypeExists(typeName);
		attributeValue("namedAttributeHolder", type);
		return this;
	}

	public SchemaBuilder typeAttribute(String attributeName, String typeName) {
		attribute(attributeName, TypeAttribute.class);
		Type type = assertTypeExists(typeName);
		attributeValue("type", type);
		if (type.isVirtual()) {
			virtual();
		}
		return this;
	}

	public SchemaBuilder jsonMixinAttribute(String attributeName, String mixinName) {
		attribute(attributeName, JSONAttribute.class);
		Mixin mixin = assertMixinExists(mixinName);
		attributeValue("namedAttributeHolder", mixin);
		return this;
	}

	public SchemaBuilder mixinAttribute(String attributeName, String mixinName) {
		attribute(attributeName, MixinAttribute.class);
		Mixin mixin = assertMixinExists(mixinName);
		attributeValue("mixin", mixin);
		if (mixin.isVirtual()) {
			virtual();
		}
		return this;
	}

	public SchemaBuilder attribute(String name, Class<? extends Attribute> attributeType) {
		if (currentAttributeHolder != null) {
			try {
				List<Attribute> attributes = currentAttributeHolder.getAttributes();
				Attribute attribute = null;
				if (attributes != null) {
					for (Attribute a : attributes) {
						if (name.equals(a.getName())) {
							// assert that the attribute matches the desired attribute type
							attributeType.cast(a);
							attribute = a;
						}
					}
				}
				if (attribute == null) {
					attribute = attributeType.newInstance();
					attribute.setName(name);
					if (attributes == null) {
						attributes = new ArrayList<>();
						currentAttributeHolder.setAttributes(attributes);
					}
					attributes.add(attribute);
				}
				if (currentAttributeHolder.isVirtual()) {
					attribute.setVirtual(true);
				}
				currentAttribute = attribute;
			} catch (InstantiationException | IllegalAccessException ex) {
				// ignore this
			}
		}
		return this;
	}

	public SchemaBuilder attributeValue(String propertyName, Object value) {
		if (currentAttribute != null) {
			Method[] methods = currentAttribute.getClass().getMethods();
			String setterName = "set" + propertyName.substring(0, 1).toUpperCase();
			if (propertyName.length() > 1) {
				setterName += propertyName.substring(1);
			}
			for (Method method : methods) {
				if (setterName.equals(method.getName())) {
					try {
						method.invoke(currentAttribute, value);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
						// ignore this
					}
				}
			}
		}
		return this;
	}
	
	public SchemaBuilder indexAttribute() {
		if (currentAttribute != null) {
			currentAttribute.setIndexed(true);
		}
		return this;
	}
	
	public SchemaBuilder preventIndexAttribute() {
		if (currentAttribute != null) {
			currentAttribute.setIndexed(false);
		}
		return this;
	}

	public SchemaBuilder nullOnDelete() {
		return attributeValue("nullOnDelete", true);
	}

	public SchemaBuilder cascadeDelete() {
		return attributeValue("cascadeDelete", true);
	}

	public SchemaBuilder deleteOrphans() {
		return attributeValue("deleteOrphans", true);
	}
	
	public SchemaBuilder toOneAttribute(String attributeName) {
		return attributeValue("toOneAttribute", attributeName);
	}

	public SchemaBuilder virtual() {
		if (currentAttribute != null) {
			currentAttribute.setVirtual(true);
		}
		return this;
	}

	public SchemaBuilder nonVirtual() {
		if (currentAttribute != null) {
			currentAttribute.setVirtual(false);
		}
		return this;
	}

	public SchemaBuilder mandatory() {
		if (currentAttribute != null) {
			currentAttribute.setMandatory(true);
		}
		return this;
	}

	public SchemaBuilder unique(String... attributeNames) {
		if (currentAttributeHolder != null) {
			UniqueConstraint c = new UniqueConstraint();
			c.setHolder(currentAttributeHolder);
			List<Attribute> participatingAttributes = new ArrayList<>();
			c.setAttributes(participatingAttributes);
			if (attributeNames == null || attributeNames.length == 0) {
				if (currentAttribute != null) {
					participatingAttributes.add(currentAttribute);
				}
			} else {
				Map<String, Attribute> atts = SchemaUtil.collectAttributesAsMap(currentAttributeHolder);
				for (String attributeName : attributeNames) {
					Attribute att = atts.get(attributeName);
					if (att == null) {
						throw new IllegalArgumentException("could not find attribute " + attributeName + " for attribute holder " + currentAttributeHolder.getName());
					}
					participatingAttributes.add(att);
				}
			}
			if (!participatingAttributes.isEmpty()) {
				List<UniqueConstraint> uq = schema.getUniqueConstraints();
				if (uq == null) {
					uq = new ArrayList<>();
					schema.setUniqueConstraints(uq);
				}
				uq.add(c);
			}
		}
		return this;
	}

	public SchemaBuilder mixWith(String name) {
		if (currentType != null) {
			List<Mixin> mixins = currentType.getMixins();
			if (mixins != null) {
				for (Mixin mixin : mixins) {
					if (name.equals(mixin.getName())) {
						return this;
					}
				}
			} else {
				mixins = new ArrayList<>();
				currentType.setMixins(mixins);
			}
			Mixin mixin = assertMixinExists(name);
			mixins.add(mixin);
			List<Type> mixedInto = mixin.getMixedInto();
			if (mixedInto == null) {
				mixedInto = new ArrayList<>();
				mixin.setMixedInto(mixedInto);
			} else {
				for (Type type : mixedInto) {
					if (type == currentType) {
						return this;
					}
				}
			}
			mixedInto.add(currentType);
		}
		return this;
	}

	public SchemaBuilder virtualMixin(String name) {
		Mixin mixin = assertMixinExists(name);
		mixin.setVirtual(true);
		currentMixin = mixin;
		currentAttributeHolder = mixin;
		return this;
	}

	public SchemaBuilder mixin(String name) {
		Mixin mixin = assertMixinExists(name);
		currentMixin = mixin;
		currentAttributeHolder = mixin;
		return this;
	}

	private Type assertTypeExists(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name of type is not allowed to be null");
		}
		Type type = typesByName.get(name);
		if (type == null) {
			type = new Type(schema);
			type.setName(name);
			typesByName.put(name, type);
			namedAttributeHoldersByName.put(name, type);
			List<Type> types = schema.getTypes();
			if (types == null) {
				types = new ArrayList<>();
				schema.setTypes(types);
			}
			types.add(type);
		}
		return type;
	}

	private Mixin assertMixinExists(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name of mixin is not allowed to be null");
		}
		Mixin mixin = mixinsByName.get(name);
		if (mixin == null) {
			mixin = new Mixin(schema);
			mixin.setName(name);
			mixinsByName.put(name, mixin);
			namedAttributeHoldersByName.put(name, mixin);
			List<Mixin> mixins = schema.getMixins();
			if (mixins == null) {
				mixins = new ArrayList<>();
				schema.setMixins(mixins);
			}
			mixins.add(mixin);
		}
		return mixin;
	}
}
