package org.bndly.rest.responseprocessor.api;

/*-
 * #%L
 * REST Response Postprocessor
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

import org.bndly.common.graph.ReferenceDetector;
import org.bndly.common.graph.TypeBasedReferenceDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AnnotationBasedReferenceDetectorImpl implements ReferenceDetector, TypeBasedReferenceDetector {

	private final Class<? extends Annotation>[] typeAnnotations;
	private final Class<? extends Annotation>[] fieldAnnotations;

	public static AnnotationBasedReferenceDetectorImpl newInstance(Class<? extends Annotation>[] typeAnnotations, Class<? extends Annotation>[] fieldAnnotations) {
		if (typeAnnotations == null) {
			typeAnnotations = new Class[0];
		}
		if (fieldAnnotations == null) {
			fieldAnnotations = new Class[0];
		}
		return new AnnotationBasedReferenceDetectorImpl(typeAnnotations, fieldAnnotations);
	}

	@Deprecated
	public AnnotationBasedReferenceDetectorImpl(Class<? extends Annotation>... typeAnnotations) {
		this(typeAnnotations, new Class[0]);
	}

	private AnnotationBasedReferenceDetectorImpl(Class<? extends Annotation>[] typeAnnotations, Class<? extends Annotation>[] fieldAnnotations) {
		this.typeAnnotations = typeAnnotations;
		this.fieldAnnotations = fieldAnnotations;
	}
	
	@Override
	public boolean isReferencable(Object o) {
		if (o == null) {
			return false;
		}
		Class<? extends Object> cls = o.getClass();
		return classHasReferenceAnnotation(cls);
	}

	public boolean classHasReferenceAnnotation(Class<? extends Object> cls) {
		if (cls == null || Object.class.equals(cls)) {
			return false;
		}
		for (Class<? extends Annotation> typeAnnotation : typeAnnotations) {
			if (cls.isAnnotationPresent(typeAnnotation)) {
				return true;
			}
		}
		return classHasReferenceAnnotation(cls.getSuperclass());
	}

	@Override
	public boolean isReferencableField(Field field) {
		if (field == null) {
			return false;
		}
		for (Class<? extends Annotation> fieldAnnotation : fieldAnnotations) {
			if (field.isAnnotationPresent(fieldAnnotation)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isReferencable(Class<?> type) {
		return classHasReferenceAnnotation(type);
	}
}
