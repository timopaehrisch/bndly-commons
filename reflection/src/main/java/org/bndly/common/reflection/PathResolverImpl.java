package org.bndly.common.reflection;

/*-
 * #%L
 * Reflection
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

public class PathResolverImpl implements PathResolver {

	private final BeanPropertyAccessor accessor = new UltimateBeanPropertyAccessor();

	@Override
	public Object resolve(String path, Object root) {
		return resolve(path, root, Object.class);
	}

	@Override
	public <E> E resolve(String path, Object root, Class<E> expectedType) {
		int i = path.indexOf(".");
		if (i > 0) {
			Object subRoot = accessor.get(path.substring(0, i), root);
			if (subRoot != null) {
				String subPath = path.substring(i + 1);
				return resolve(subPath, subRoot, expectedType);
			}
		} else {
			Object value = accessor.get(path, root);
			if (value != null) {
				if (expectedType != null && expectedType.isAssignableFrom(value.getClass())) {
					return expectedType.cast(value);
				}
			}
		}

		return null;
	}

}
