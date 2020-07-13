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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBeanPropertyAccessorWriter {
	protected static boolean propertyNameRefersToElementInCollection(String propertyName) {
		int i = propertyName.indexOf("[");
		if (i > -1) {
			int j = propertyName.indexOf("]", i);
			if (j > -1) {
				return true;
			}
		}
		return false;
	}

	protected static CollectionProperty collectionPropertyDescriptor(String propertyName) {
		int i = propertyName.indexOf("[");
		if (i > -1) {
			int j = propertyName.indexOf("]", i);
			if (j > -1) {
				return new CollectionProperty(propertyName.substring(0, i), new Integer(propertyName.substring(i + 1, j)));
			}
		}
		return null;
	}

	protected static List increaseListSizeIfNeeded(List input, int indexToWrite) {
		if (input == null) {
			input = new ArrayList(indexToWrite + 1);
		}
		List l = input;
		if (indexToWrite >= input.size()) {
			for (int i = 0; i <= indexToWrite; i++) {
				if (i >= l.size()) {
					// insert a null padding
					l.add(null);
				}
			}
		}
		return l;
	}
}
