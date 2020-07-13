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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DictionaryToPropertiesAdapter extends Properties {
	
	private final DictionaryAdapter dictionaryAdapter;

	public DictionaryToPropertiesAdapter(DictionaryAdapter dictionaryAdapter) {
		if (dictionaryAdapter == null) {
			throw new IllegalArgumentException("dictionaryAdapter is not allowed to be null");
		}
		this.dictionaryAdapter = dictionaryAdapter;
	}
	
	public final Properties toStringProperties() {
		Properties properties = new Properties();
		for (Map.Entry<Object, Object> entrySet : entrySet()) {
			Object k = entrySet.getKey();
			Object val = entrySet.getValue();
			if (val != null && String.class.isInstance(k)) {
				String key = (String) k;
				String value = (String) (String.class.isInstance(val) ? val : val.toString());
				if (value != null) {
					properties.setProperty(key, value);
				}
			}
		}
		return properties;
	}

	@Override
	public synchronized Enumeration<Object> elements() {
		List<Object> values = new ArrayList<>();
		for (Map.Entry<Object, Object> entrySet : entrySet()) {
			Object val = entrySet.getValue();
			if (val != null) {
				values.add(val);
			}
		}
		return Collections.enumeration(values);
	}

	@Override
	public Set<Map.Entry<Object, Object>> entrySet() {
		Set<Map.Entry<Object, Object>> set = new LinkedHashSet<>();
		Set<Object> ks = keySet();
		for (final Object key : ks) {
			final Object val = dictionaryAdapter.get(key);
			if (val == null) {
				continue;
			}
			set.add(new Map.Entry<Object, Object>() {

				@Override
				public Object getKey() {
					return key;
				}

				@Override
				public Object getValue() {
					return val;
				}

				@Override
				public Object setValue(Object value) {
					dictionaryAdapter.getProps().put((String) key, value);
					return value;
				}
			});
		}
		return set;
	}

	@Override
	public Set<Object> keySet() {
		Set<Object> set = new HashSet<>();
		Enumeration keys = keys();
		while (keys.hasMoreElements()) {
			Object nextElement = keys.nextElement();
			if (get(nextElement) != null) {
				set.add(nextElement);
			}
		}
		return set;
	}

	@Override
	public synchronized int size() {
		return keySet().size();
	}

	@Override
	public synchronized Enumeration keys() {
		Enumeration<String> tmp = dictionaryAdapter.getProps().keys();
		List<String> nonNullKeys = new ArrayList<>();
		while (tmp.hasMoreElements()) {
			String nextElement = tmp.nextElement();
			if (dictionaryAdapter.get(nextElement) != null) {
				nonNullKeys.add(nextElement);
			}
		}
		return Collections.enumeration(nonNullKeys);
	}
	
	@Override
	public synchronized Object get(Object key) {
		return dictionaryAdapter.get(key);
	}

	@Override
	public String getProperty(String key) {
		return dictionaryAdapter.getString(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return dictionaryAdapter.getString(key, defaultValue);
	}
	
}
