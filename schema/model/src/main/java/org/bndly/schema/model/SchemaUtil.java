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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SchemaUtil {

	private SchemaUtil() {
	}

	public static Map<String, Attribute> collectAttributesAsMap(NamedAttributeHolder namedAttributeHolder) {
		return collectAttributesAsMap(namedAttributeHolder, new HashMap<String, Attribute>());
	}

	private static Map<String, Attribute> collectAttributesAsMap(NamedAttributeHolder namedAttributeHolder, Map<String, Attribute> map) {
		if (namedAttributeHolder != null) {
			List<Attribute> atts = namedAttributeHolder.getAttributes();
			if (atts != null) {
				for (Attribute attribute : atts) {
					map.put(attribute.getName(), attribute);
				}
			}
			if (Type.class.isInstance(namedAttributeHolder)) {
				Type type = (Type) namedAttributeHolder;
				List<Mixin> mixins = type.getMixins();
				if (mixins != null) {
					for (Mixin mixin : mixins) {
						List<Attribute> mixinAtts = mixin.getAttributes();
						if (mixinAtts != null) {
							for (Attribute attribute : mixinAtts) {
								map.put(attribute.getName(), attribute);
							}
						}
					}
				}
				collectAttributesAsMap(type.getSuperType(), map);
			}
		}
		return map;
	}

	public static List<Attribute> collectAttributes(NamedAttributeHolder namedAttributeHolder) {
		return collectAttributes(namedAttributeHolder, new ArrayList<Attribute>());
	}

	private static List<Attribute> collectAttributes(NamedAttributeHolder namedAttributeHolder, List<Attribute> attributes) {
		if (namedAttributeHolder != null) {
			List<Attribute> atts = namedAttributeHolder.getAttributes();
			if (atts != null) {
				attributes.addAll(atts);
			}
			if (Type.class.isInstance(namedAttributeHolder)) {
				Type type = (Type) namedAttributeHolder;
				List<Mixin> mixins = type.getMixins();
				if (mixins != null) {
					for (Mixin mixin : mixins) {
						List<Attribute> mixinAtts = mixin.getAttributes();
						if (mixinAtts != null) {
							attributes.addAll(mixinAtts);
						}
					}
				}
				collectAttributes(type.getSuperType(), attributes);
			}
		}
		return attributes;
	}
}
