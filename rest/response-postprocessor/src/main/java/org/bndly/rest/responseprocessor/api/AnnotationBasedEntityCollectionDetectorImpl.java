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

import org.bndly.common.graph.EntityCollectionDetector;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.common.beans.RestBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AnnotationBasedEntityCollectionDetectorImpl implements EntityCollectionDetector {

	private final AnnotationBasedReferenceDetectorImpl d;

	public AnnotationBasedEntityCollectionDetectorImpl(Class<? extends Annotation>... annotations) {
		d = new AnnotationBasedReferenceDetectorImpl(annotations);
	}
	
	@Override
	public boolean isEntityCollection(Field field) {
		Class<?> entryType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
		return d.classHasReferenceAnnotation(entryType) || RestBean.class.isAssignableFrom(entryType);
	}

}
