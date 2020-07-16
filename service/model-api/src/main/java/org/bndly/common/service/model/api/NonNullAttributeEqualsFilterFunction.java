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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * compares the non null values in the example object to the values in the compared object.
 * for referable resources this leads to recursive invocation. object cycles are detected.
 * NOTE: nested maps and collections are ignored.
 */
public class NonNullAttributeEqualsFilterFunction<O> implements FilterFunction<O> {

	private final O example;
	private final Stack visited;
	private final Set<String> fieldNamesToIgnore;

	public NonNullAttributeEqualsFilterFunction(O example, String... fieldsToIgnore) {
		this(example, new Stack(), new HashSet<String>());
		fieldNamesToIgnore.addAll(Arrays.asList(fieldsToIgnore));
	}

	public NonNullAttributeEqualsFilterFunction(O example) {
		this(example, new Stack(), new HashSet<String>());
	}

	public NonNullAttributeEqualsFilterFunction(O example, Stack visited, Set<String> fieldNamesToIgnore) {
		if (example == null) {
			throw new IllegalArgumentException("can not instantiate a " + NonNullAttributeEqualsFilterFunction.class.getSimpleName() + " with a null-object as the example object.");
		}
		this.example = example;
		this.visited = visited;
		this.fieldNamesToIgnore = fieldNamesToIgnore;
	}

	@Override
	public boolean applies(O o) {
		if (visited.contains(example)) {
			return true;
		}

		visited.add(example);
		boolean result = example.getClass().equals(o.getClass());
		if (result) {
			List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(example.getClass());
			for (Field field : fields) {
				if (!result) {
					break;
				}

				if (fieldNamesToIgnore.contains(field.getName())) {
					break;
				}

				Object exampleValue = ReflectionUtil.getFieldValue(field, example);
				Object oValue = ReflectionUtil.getFieldValue(field, o);
				if (exampleValue != null) {
					if (oValue == null) {
						result = false;
						break;
					} else {
						if (ReferableResource.class.isInstance(exampleValue)) {
							// compare with another NonNullAttributeEqualsFilterFunction
							result = result && new NonNullAttributeEqualsFilterFunction(exampleValue, visited, fieldNamesToIgnore).applies(oValue);
						} else {
							if (Collection.class.isInstance(oValue)) {
								// ignore collections because it is not clear how they should be treated (equal or exist)
							} else if (Map.class.isInstance(oValue)) {
								// ignore maps because it is not clear how they should be treated (equal or exist)
							} else {
								// compare with equals
								result = result && exampleValue.equals(oValue);
							}
						}
					}
				}
			}
		}
		visited.pop();
		return result;
	}

}
