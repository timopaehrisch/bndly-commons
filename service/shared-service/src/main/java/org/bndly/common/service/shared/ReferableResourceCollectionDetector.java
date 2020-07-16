package org.bndly.common.service.shared;

/*-
 * #%L
 * Service Shared Impl
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
import java.lang.reflect.Field;

public class ReferableResourceCollectionDetector implements EntityCollectionDetector {

	private final String packageNameOfEntityTypes;

	public ReferableResourceCollectionDetector(String packageNameOfEntityTypes) {
		if (packageNameOfEntityTypes == null) {
			throw new IllegalArgumentException("package name for entity types is mandatory. otherwise collections of entities can not be detected.");
		}
		this.packageNameOfEntityTypes = packageNameOfEntityTypes;
	}

	@Override
	public boolean isEntityCollection(Field field) {
		Class<?> collectionItemType = ReflectionUtil.getCollectionParameterType(field.getGenericType());
		if (collectionItemType == null) {
			return false;
		}
		if (collectionItemType.getPackage().getName().startsWith(packageNameOfEntityTypes)) {
			return true;
		}
		return false;
	}

}
