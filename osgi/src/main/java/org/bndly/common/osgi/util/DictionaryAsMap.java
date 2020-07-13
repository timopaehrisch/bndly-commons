package org.bndly.common.osgi.util;

/*-
 * #%L
 * OSGI Utilities
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

import java.util.AbstractMap;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DictionaryAsMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

	private final Dictionary<K,V> dictionary;

	public DictionaryAsMap(Dictionary<K, V> dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		LinkedHashSet<Map.Entry<K, V>> set = new LinkedHashSet<>();
		Enumeration<K> keyEnum = dictionary.keys();
		while (keyEnum.hasMoreElements()) {
			final K key = keyEnum.nextElement();
			set.add(new Entry<K, V>() {
				@Override
				public K getKey() {
					return key;
				}

				@Override
				public V getValue() {
					return dictionary.get(key);
				}

				@Override
				public V setValue(V value) {
					V old = dictionary.get(key);
					dictionary.put(key, value);
					return old;
				}
			});
		}
		return set;
	}
	
}
