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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class IdentityBuilder {

	private IdentityBuilder() {
	}

	public static List<Identity> buildAllPossible(final Object source) {
		List<Identity> identities = new ArrayList<>();
		if (source != null) {
			final List<Field> referenceFields = ReflectionUtil.getFieldsWithAnnotation(ReferenceAttribute.class, source);
			if (referenceFields != null && !referenceFields.isEmpty()) {
				String sourceTypeName = source.getClass().getName();
				for (Field field : referenceFields) {
					Object sV = ReflectionUtil.getFieldValue(field, source);
					if (sV != null) {
						identities.add(new IdentityImpl(sourceTypeName, field.getName(), sV));
					}
				}

			}
		}
		return identities;
	}

	public static Identity buildOrIdentity(final Object source) {
		if (source != null) {
			final List<Field> referenceFields = ReflectionUtil.getFieldsWithAnnotation(ReferenceAttribute.class, source);
			if (referenceFields != null && !referenceFields.isEmpty()) {
				String sourceTypeName = source.getClass().getName();
				for (Field field : referenceFields) {
					Object sV = ReflectionUtil.getFieldValue(field, source);
					if (sV != null) {
						String fieldName = field.getName();
						Object fieldValue = sV;
						return new IdentityImpl(sourceTypeName, fieldName, fieldValue);
					}
				}
			}
		}
		return null;
	}

}
