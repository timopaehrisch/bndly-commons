package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility will inspect a type of a deployed schema in order to create appropriate converters to support the query by example in
 * {@link EntityResource#list(java.lang.Long, java.lang.Long, java.lang.String, java.lang.String, org.bndly.rest.api.Context) }.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QueryParameterUtil {

	private static final Logger LOG = LoggerFactory.getLogger(QueryParameterUtil.class);

	private final Type type;
	private final Class<?> schemaBeanType;
	private final Map<String, Class> javaTypesByParameterName;
	private final Stack<Attribute> inspectedAttributes;

	public QueryParameterUtil(Type type, Class<?> schemaBeanType) {
		this.type = type;
		this.schemaBeanType = schemaBeanType;
		javaTypesByParameterName = new LinkedHashMap<>();
		inspectedAttributes = new Stack<>();
		iterateType(type, schemaBeanType, null);
	}

	public Map<String, Class> getJavaTypesByParameterName() {
		return javaTypesByParameterName;
	}

	private void iterateType(Type type, Class<?> javaType, String prefix) {
		List<Attribute> attributes = type.getAttributes();
		if (attributes != null) {
			for (Attribute attribute : attributes) {
				if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					// dig down
					iterateNamedAttributeHolderAttribute(type, (NamedAttributeHolderAttribute) attribute, prefix);
				} else {
					Method method = getMethodForAttribute(attribute, javaType);
					if (method == null) {
						continue;
					}
					if (prefix == null) {
						javaTypesByParameterName.put(attribute.getName(), method.getReturnType());
					} else {
						javaTypesByParameterName.put(prefix + "_" + attribute.getName(), method.getReturnType());
					}
				}
				
			}
		}
		List<Mixin> mixins = type.getMixins();
		if (mixins != null) {
			for (Mixin mixin : mixins) {
				List<Attribute> mixinAttributes = mixin.getAttributes();
				if (mixinAttributes != null) {
					for (Attribute attribute : mixinAttributes) {
						if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
							// dig down
							iterateNamedAttributeHolderAttribute(type, (NamedAttributeHolderAttribute) attribute, prefix);
						} else {
							Method method = getMethodForAttribute(attribute, javaType);
							if (method == null) {
								continue;
							}
							if (prefix == null) {
								javaTypesByParameterName.put(attribute.getName(), method.getReturnType());
							} else {
								javaTypesByParameterName.put(prefix + "_" + attribute.getName(), method.getReturnType());
							}
						}
					}
				}
			}
		}
		Type superType = type.getSuperType();
		if (superType != null) {
			iterateType(superType, javaType, prefix);
		}
	}

	private Method getMethodForAttribute(Attribute attribute, Class<?> schemaBeanType) {
		String attributeName = attribute.getName();
		String getterName = "get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
		try {
			return schemaBeanType.getMethod(getterName);
		} catch (NoSuchMethodException | SecurityException ex) {
			LOG.error("could not find getter for attribute " + attributeName + " in " + schemaBeanType.getName(), ex);
			return null;
		}
	}

	private void iterateNamedAttributeHolderAttribute(Type type, NamedAttributeHolderAttribute attribute, String prefix) {
		if (inspectedAttributes.contains(attribute)) {
			return;
		}
		inspectedAttributes.push(attribute);
		NamedAttributeHolder namedAttributeHolder = attribute.getNamedAttributeHolder();
		// switch around Type or Mixin
		if (Type.class.isInstance(namedAttributeHolder)) {
			Type referencedType = (Type) namedAttributeHolder;
			// iterate the type and all sub types
			iterateNamedAttributeHolderAttribute(type, attribute, prefix, referencedType);
		} else if (Mixin.class.isInstance(namedAttributeHolder)) {
			Mixin referencedMixin = (Mixin) namedAttributeHolder;
			// iterate all types with this mixin and all the subtypes of those types
			iterateNamedAttributeHolderAttribute(type, attribute, prefix, referencedMixin);
		} else {
			LOG.warn("unsupported attribute holder " + namedAttributeHolder);
		}
		inspectedAttributes.pop();
	}
	
	private void iterateNamedAttributeHolderAttribute(Type type, NamedAttributeHolderAttribute attribute, String prefix, Mixin referencedMixin) {
		List<Type> mixedInto = referencedMixin.getMixedInto();
		if (mixedInto != null) {
			for (Type referencedType : mixedInto) {
				iterateNamedAttributeHolderAttribute(type, attribute, prefix, referencedType);
			}
		}
	}

	private void iterateNamedAttributeHolderAttribute(Type type, NamedAttributeHolderAttribute attribute, String prefix, Type referencedType) {
		String extendedPrefix = prefix == null 
				? 
				attribute.getName() + "_" + referencedType.getName() 
				: 
				prefix + "_" + attribute.getName() + "_" + referencedType.getName()
				;
		// iterate the referenced type
		String attributeHolderJavaTypeName = schemaBeanType.getPackage().getName() + "." + referencedType.getName();
		try {
			Class<?> javaTypeOfReferencedType = schemaBeanType.getClassLoader().loadClass(attributeHolderJavaTypeName);
			iterateType(referencedType, javaTypeOfReferencedType, extendedPrefix);

			List<Type> subTypes = referencedType.getSubTypes();
			if (subTypes != null) {
				for (Type subType : subTypes) {
					iterateNamedAttributeHolderAttribute(type, attribute, prefix, subType);
				}
			}
		} catch (ClassNotFoundException ex) {
			LOG.warn("could not find class " + attributeHolderJavaTypeName);
			return;
		}
	}

}
