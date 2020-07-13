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

public class SubTypeReferenceDetector implements ReferenceDetector {

	private Class<?> objectRefBaseType;

	public SubTypeReferenceDetector(Class<?> objectRefBaseType) {
		if (objectRefBaseType == null) {
			throw new IllegalArgumentException("cant construct a " + SubTypeReferenceDetector.class.getSimpleName() + " without reference base type");
		}
		this.objectRefBaseType = objectRefBaseType;
	}

	@Override
	public boolean isReferencable(Object o) {
		if (o == null) {
			return false;
		}
		return objectRefBaseType.isAssignableFrom(o.getClass());
	}

}
