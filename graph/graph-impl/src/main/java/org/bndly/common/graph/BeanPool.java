package org.bndly.common.graph;

/*-
 * #%L
 * Graph Impl
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

import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.graph.api.GraphComplexType;
import org.bndly.common.graph.api.GraphID;
import org.bndly.common.graph.api.IDExtractor;
import org.bndly.common.graph.api.Identity;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanPool {

	private final List equalsForClasses = Arrays.asList(
		BigDecimal.class,
		Float.class,
		Double.class,
		Long.class,
		Integer.class,
		Short.class,
		String.class,
		Date.class,
		Boolean.class,
		Character.class
	);

	private static class BeanPoolEntry {

		private Object source;
		private Object target;

		public Object getSource() {
			return source;
		}

		public void setSource(Object source) {
			this.source = source;
		}

		public Object getTarget() {
			return target;
		}

		public void setTarget(Object target) {
			this.target = target;
		}

	}

	private final Map<Object, BeanPoolEntry> entries = new HashMap<>();

	// unpersisted entries might all have the same hashcode. hence they need a different treatment, that does not rely on the same hashcode function
	private final List<BeanPoolEntry> unpersistedEntries = new ArrayList<>();

	boolean contains(Object source) {
		return find(source) != null;
	}

	public void add(Object bean) {
		if (isNeverPersistedObject(bean)) {
			// do nothing
		} else if (isUnpersistedObject(bean)) {
			BeanPoolEntry entry = new BeanPoolEntry();
			entry.setSource(bean);
			unpersistedEntries.add(entry);
		} else {
			BeanPoolEntry existingEntry = find(bean);
			if (existingEntry != null) {
				Object s = existingEntry.getSource();
				if (s != bean) {
					Class<? extends Object> existingEntryType = s.getClass();
					Class<? extends Object> beanType = bean.getClass();
					if (beanType.equals(existingEntryType)) {
						return;
					} else if (beanType.isAssignableFrom(existingEntryType)) {
						// bean is reference of existing entry
						return;
					} else {
						// bean is full object while existing entry is only a reference
						existingEntry.setSource(bean);
						existingEntry.setTarget(bean);
						entries.put(bean, existingEntry);
					}
				}
			} else {
				BeanPoolEntry entry = new BeanPoolEntry();
				entry.setSource(bean);
				entry.setTarget(bean);
				entries.put(bean, entry);
			}
		}
	}

	public void link(Object source, Object target) {
		BeanPoolEntry entry = find(source);
		if (entry != null) {
			entry.setTarget(target);
		}
	}

	public Object getTarget(Object source) {
		if (isNeverPersistedObject(source)) {
			return source;
		} else {
			BeanPoolEntry entry = find(source);
			if (entry != null) {
				return entry.getTarget();
			} else {
				throw new IllegalStateException("object could not be found in bean pool.");
			}
		}
	}

	private BeanPoolEntry find(Object source) {
		if (isUnpersistedObject(source)) {
			for (BeanPoolEntry entry : unpersistedEntries) {
				if (entry.getSource() == source) {
					return entry;
				}
			}
			return null;
		} else {
			return entries.get(source);
		}
	}

	public void linkUnpersistedObjects() {
		for (BeanPoolEntry beanPoolEntry : unpersistedEntries) {
			if (beanPoolEntry.getTarget() == null) {
				List<BeanPoolEntry> identical = findIdenticalObjects(beanPoolEntry);
				if (identical != null) {
					BeanPoolEntry mostSpecificEntry = findMostSpecific(identical);
					for (BeanPoolEntry entry : identical) {
						if (entry != mostSpecificEntry) {
							entry.setTarget(mostSpecificEntry.getSource());
						}
					}
				}
			}

			// if a link can not be established...
			if (beanPoolEntry.getTarget() == null) {
				beanPoolEntry.setTarget(beanPoolEntry.getSource());
			}
		}
	}

	private BeanPoolEntry findMostSpecific(List<BeanPoolEntry> identical) {
		BeanPoolEntry mostSpecific = null;
		for (BeanPoolEntry beanPoolEntry : identical) {
			if (mostSpecific == null) {
				mostSpecific = beanPoolEntry;
			} else {
				Object ms = mostSpecific.getSource();
				Object s = beanPoolEntry.getSource();
				if (ReflectionUtil.isObjectMoreSpecificThanOther(s, ms)) {
					mostSpecific = beanPoolEntry;
				}
			}
		}
		return mostSpecific;
	}

	private List<BeanPoolEntry> findIdenticalObjects(BeanPoolEntry entry) {
		if (entry.getTarget() == null) {
			Identity sourceIdentity = buildIdentityFor(entry.getSource());
			Class<?> idFieldDeclaringClass = null;
			Object id = null;
			if (sourceIdentity != null) {
				idFieldDeclaringClass = sourceIdentity.getIdentityForType();
				id = sourceIdentity.getValue();
			}

			List<BeanPoolEntry> identical = null;
			for (BeanPoolEntry e : unpersistedEntries) {
				Object s = e.getSource();
				Identity sIdentity = buildIdentityFor(s);
				Object sid = null;
				Class<?> sidFieldDeclaringClass = null;
				if (sIdentity != null) {
					sid = sIdentity.getValue();
					sidFieldDeclaringClass = sIdentity.getIdentityForType();
				}
				if (idFieldDeclaringClass != null && sidFieldDeclaringClass != null && idFieldDeclaringClass.equals(sidFieldDeclaringClass)) {
					boolean isIdentical = false;
					// this is a candidate because they are interpreting the beanId equal
					if (id != null && id.equals(sid)) {
						// both objects share the same idField and the same value for the id
						// this means they are considered to have the same identity
						isIdentical = true;
					} else if (id == null && sid == null) {
						// there is the chance, that both objects are identical, if all attributes are identical
						isIdentical = isIdenticalByDeepCompare(entry.getSource(), s);
					}
					if (isIdentical) {
						if (identical == null) {
							identical = new ArrayList<>();
						}
						identical.add(e);
					}

				}
			}
			return identical;
		}
		return null;
	}

	private Identity buildIdentityFor(Object source) {
		Identity id = new FieldAnnotationBasedIdentityExtractor<>(GraphID.class).getId(source);
		if (id == null) {
			GraphComplexType ct = ReflectionUtil.searchAnnotation(GraphComplexType.class, source.getClass());
			IDExtractor idExtractor = InstantiationUtil.instantiateType(ct.idExtractor());
			id = idExtractor.getId(source);
		}
		return id;
	}

	private boolean isNeverPersistedObject(Object source) {
		return buildIdentityFor(source).getIdentityForType() == null;
	}

	private boolean isUnpersistedObject(Object source) {
		Identity id = buildIdentityFor(source);
		return id.getIdentityForType() != null && id.getValue() == null;
	}

	private boolean isIdenticalByDeepCompare(Object source, Object other) {
		Class<? extends Object> sType = source.getClass();
		Class<? extends Object> oType = other.getClass();
		if (sType.equals(oType)) {
			List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(oType);
			for (Field field : fields) {
				Object sV = ReflectionUtil.getFieldValue(field, source);
				Object oV = ReflectionUtil.getFieldValue(field, other);
				if (sV == null ^ oV == null) {
					return false;
				} else {
					if (sV != null && oV != null) {
						boolean r = false;
						if (shouldUseEquals(sV.getClass())) {
							r = sV.equals(oV);
						} else {
							r = isIdenticalByDeepCompare(sV, oV);
						}
						if (!r) {
							return false;
						}
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean shouldUseEquals(Class<?> type) {
		return equalsForClasses.contains(type);
	}
}
