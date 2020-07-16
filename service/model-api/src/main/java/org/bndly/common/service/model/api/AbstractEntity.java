package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import org.bndly.common.reflection.ReflectionUtil;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEntity<E> implements ReferableResource<E>, Serializable {

	private Boolean isReference;
	private Map<String, Link> links;
	private Boolean smartRef;

	protected Class<E> getReferenceModelType() {
		List<Class<?>> typeParameters = ReflectionUtil.collectGenericTypeParametersFromType(getClass());
		if (typeParameters.size() == 1) {
			Class<E> domainReferenceModel = (Class<E>) typeParameters.get(0);
			return domainReferenceModel;
		} else {
			return null;
		}
	}

	@Override
	public <B extends E> boolean isReferenceFor(B otherObject) {
		List<Field> fields = ReflectionUtil.getFieldsWithAnnotation(ReferenceAttribute.class, this);
		if (fields != null && !fields.isEmpty()) {
			boolean equals = true;
			for (Field f : fields) {
				ReferenceAttribute a = f.getAnnotation(ReferenceAttribute.class);
				Object otherValue = ReflectionUtil.getFieldValue(f, otherObject);
				Object myValue = ReflectionUtil.getFieldValue(f, this);
				if (a.nullIdentifies()) {
					equals = equals && otherValue == null ? myValue == null : otherValue.equals(myValue);
				} else {
					if (otherValue == null && myValue == null) {
						continue;
					}
					equals = equals && otherValue != null && otherValue.equals(myValue);
				}
			}
			return equals;
		} else {
			throw new IllegalStateException("no fields are annotated with " + ReferenceAttribute.class.getSimpleName() + " in type " + this.getClass().getSimpleName());
		}

	}

	public void markAsReference() {
		isReference = true;
	}
	
	public void markAsSmartReference() {
		smartRef = true;
	}
	
	public void markAsFullModel() {
		isReference = false;
		smartRef = false;
	}

	@Override
	public E buildReference() throws ReferenceBuildingException {
		if (!isResourceReference()) {
			Class<E> domainReferenceModelType = getReferenceModelType();
			if (domainReferenceModelType == null) {
				throw new ReferenceBuildingException("could not build reference, because reference model type was null");
			}
			Class thisCls = getClass();
			try {
				Constructor defaultConstructor = thisCls.getConstructor();
				AbstractEntity reference = (AbstractEntity) defaultConstructor.newInstance();
				reference.isReference = true;
				copyReferenceAttributes(reference);
				return (E) reference;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
				// we can not create a reference object
				return null;
			}
		} else {
			return (E) this;
		}
	}

	private void copyReferenceAttributes(Object reference) throws ReferenceBuildingException {
		List<Field> fields = ReflectionUtil.getFieldsWithAnnotation(ReferenceAttribute.class, this);

		boolean couldCopyReferenceAttribute = false;
		if (fields != null && !fields.isEmpty()) {
			for (Field field : fields) {
				boolean nullIdentifies = field.getAnnotation(ReferenceAttribute.class).nullIdentifies();
				Object value = ReflectionUtil.getFieldValue(field, this);
				if (value != null || nullIdentifies) {
					if (!"id".equals(field.getName())) {
						if (AbstractEntity.class.isInstance(reference)) {
							((AbstractEntity) reference).smartRef = true;
						}
					}
					couldCopyReferenceAttribute = true;
					ReflectionUtil.setFieldValue(field, value, reference);
				}
			}
		}

		// copy all links
		if (Linkable.class.isInstance(reference)) {
			for (String rel : getAllLinkNames()) {
				if ("self".equals(rel)) {
					couldCopyReferenceAttribute = true;
				}
				((Linkable) reference).addLink(rel, follow(rel), followForMethod(rel));
			}
		}
		if (!couldCopyReferenceAttribute) {
			throw new ReferenceBuildingException("could not build reference because no field or link that could be used for a lookup was found.");
		}
	}

	@Override
	public boolean isResourceReference() {
		return isReference == null ? false : isReference;
	}

	@Override
	public boolean isSmartReference() {
		return smartRef == null ? false : smartRef;
	}

	@Override
	public void removeLink(String rel) {
		assertLinksIsNotNull();
		links.remove(rel);
	}

	@Override
	public void addLink(String rel, String url, String method) {
		assertLinksIsNotNull();
		if (rel == null) {
			throw new IllegalArgumentException("can not create a link without a 'relation' attribute.");
		}
		if (url == null) {
			throw new IllegalArgumentException("can not create a link without a 'url' attribute.");
		}
		Link l = new Link();
		l.setRel(rel);
		l.setUrl(url);
		l.setMethod(method);
		links.put(rel, l);
	}

	@Override
	public String follow(String rel) {
		assertLinksIsNotNull();
		Link link = links.get(rel);
		if (link != null) {
			return link.getUrl();
		}
		return null;
	}

	@Override
	public String followForMethod(String rel) {
		assertLinksIsNotNull();
		Link link = links.get(rel);
		if (link != null) {
			return link.getMethod();
		}
		return null;
	}

	@Override
	public Collection<String> getAllLinkNames() {
		assertLinksIsNotNull();
		return links.keySet();
	}

	private void assertLinksIsNotNull() {
		if (links == null) {
			links = new HashMap<>();
		}
	}
}
