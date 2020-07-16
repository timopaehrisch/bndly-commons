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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CollectionSorter<E> {

	public List<E> sort(Collection<E> input, final SortFunction<E> fn) {
		List<E> result = new ArrayList<>();
		if (input != null) {
			result.addAll(input);
			Collections.sort(result, new Comparator<E>() {
				@Override
				public int compare(E t, E t1) {
					return fn.compareWith(t, t1);
				}
			});
		}
		return result;
	}

}
