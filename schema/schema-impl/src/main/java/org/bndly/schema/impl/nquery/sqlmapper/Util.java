package org.bndly.schema.impl.nquery.sqlmapper;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.AliasBinding;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Util {

	private Util() {
	}
	
	public static void addAttributeToSet(Set<String> set, String attributePath) {
		int index = attributePath.indexOf(".");
		while (index > -1) {
			set.add(attributePath.substring(0, index));
			index = attributePath.indexOf(".", index + 1);
		}
		set.add(attributePath);
	}

	public static String stripEntityAliasPrefix(String alias, String name) {
		if (alias == null) {
			return name;
		}
		if (name.startsWith(alias + ".")) {
			return name.substring(alias.length() + 1);
		} else {
			throw new IllegalStateException("string did not start with expected alias prefix");
		}
	}
	
	public static List<AliasBinding> resolveAliasBindingsFromMappingBinding(MappingBinding mappingBinding, String attributePath, TableRegistry tableRegistry) {
		int index = attributePath.indexOf(".");
		List<AliasBinding> result = null;
		if (index > -1) {
			String attName = attributePath.substring(0, index);
			String nestedAttName = attributePath.substring(index + 1);
			Map<String, List<MappingBinding>> subs = mappingBinding.getSubBindings();
			if (subs != null) {
				List<MappingBinding> subLists = subs.get(attributePath.substring(0, index));
				if (subLists != null) {
					String subPath = attributePath.substring(index + 1);
					for (MappingBinding sub : subLists) {
						List<AliasBinding> aliases = resolveAliasBindingsFromMappingBinding(sub, subPath, tableRegistry);
						if (result == null) {
							result = new ArrayList<>();
						}
						result.addAll(aliases);
					}
				}
			} else {
				// there are no subbindings. this means no attributes of the nested record will be loaded
				// if we are dealing with the id, then this might be ok, if the attribute refers to a type attribute, where the type has no sub types
				List<AliasBinding> aliases = mappingBinding.getAliases();
				if (aliases != null) {
					for (AliasBinding alias : aliases) {
						Attribute att = alias.getAttribute();
						if (attName.equals(att.getName())) {
							if (TypeAttribute.class.isInstance(att)) {
								Type type = ((TypeAttribute) att).getType();
								List<Type> st = type.getSubTypes();
								if (st == null || st.isEmpty()) {
									// if it is the primary attribute
									TypeTable table = tableRegistry.getTypeTableByType(type);
									if (table.getPrimaryKeyColumn().getAttribute().getName().equals(nestedAttName)) {
										if (result == null) {
											result = new ArrayList<>();
										}
										result.add(alias);
									}
								}
							}
						}
					}
				}
			}
		} else {
			List<AliasBinding> aliases = mappingBinding.getAliases();
			if (aliases != null) {
				for (AliasBinding alias : aliases) {
					if (alias.getAttribute().getName().equals(attributePath)) {
						if (result == null) {
							result = new ArrayList<>();
						}
						result.add(alias);
					}
				}
			}
			AliasBinding pkAlias = mappingBinding.getPrimaryKeyAlias();
			if (pkAlias != null && pkAlias.getAttribute().getName().equals(attributePath)) {
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(pkAlias);
			}
		}
		return result == null ? Collections.EMPTY_LIST : result;
	}
	
}
