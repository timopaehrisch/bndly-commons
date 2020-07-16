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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionFilter<OBJECT> {
	public List<OBJECT> filter(Collection<OBJECT> collection, FilterFunction<OBJECT> filterFunction) {
		return _filter(collection, filterFunction, false);
	}

	public List<OBJECT> filterReverse(Collection<OBJECT> collection, FilterFunction<OBJECT> filterFunction) {
		return _filter(collection, filterFunction, true);
	}

	private List<OBJECT> _filter(Collection<OBJECT> collection, FilterFunction<OBJECT> filterFunction, boolean reversed) {
		if (filterFunction == null) {
			throw new IllegalStateException("can't filter a collection without a FilterFunction");
		}
		List<OBJECT> list;
		if (collection != null) {
			list = new ArrayList<>(collection.size());
			for (OBJECT object : collection) {
				if (reversed) {
					if (!filterFunction.applies(object)) {
						list.add(object);
					}
				} else {
					if (filterFunction.applies(object)) {
						list.add(object);
					}
				}
			}
		} else {
			list = new ArrayList<>(1);
		}
		return list;
	}
}
