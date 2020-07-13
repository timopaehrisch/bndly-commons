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

import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.graph.api.IDExtractor;
import org.bndly.common.graph.api.Identity;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FieldAnnotationBasedIdentityExtractor<E> implements IDExtractor<E> {

	private final Class<? extends Annotation> markerType;

	public FieldAnnotationBasedIdentityExtractor(Class<? extends Annotation> markerType) {
		this.markerType = markerType;
	}

	@Override
	public Identity getId(E source) {
		Field field = ReflectionUtil.getFieldByAnnotation(markerType, source);
		if (field != null) {
			final Object id = ReflectionUtil.getFieldValue(field, source);
			final Class<?> type = field.getDeclaringClass();
			return new Identity() {

				@Override
				public Object getValue() {
					return id;
				}

				@Override
				public Class<?> getIdentityForType() {
					return type;
				}
			};
		} else {
			return null;
		}
	}

}
