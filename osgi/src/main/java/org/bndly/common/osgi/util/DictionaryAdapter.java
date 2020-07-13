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

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class DictionaryAdapter {

	private final Dictionary<String, Object> props;
	private final boolean emptyStringAsNull;
	
	public final Dictionary<String, Object> getProps() {
		return props;
	}
	
	public DictionaryAdapter(ServiceReference serviceReference) {
		this(serviceReference, false);
	}
	
	public DictionaryAdapter(ServiceReference serviceReference, boolean emptyStringAsNull) {
		String[] keys = serviceReference.getPropertyKeys();
		props = new Hashtable<>(keys.length);
		for (String key : keys) {
			props.put(key, serviceReference.getProperty(key));
		}
		this.emptyStringAsNull = emptyStringAsNull;
	}

	public DictionaryAdapter(Dictionary<String, Object> props, boolean emptyStringAsNull) {
		if (props == null) {
			props = new Hashtable<>();
		}
		this.props = props;
		this.emptyStringAsNull = emptyStringAsNull;
	}

	public DictionaryAdapter(Dictionary<String, Object> props) {
		this(props, false);
	}
	
	public final DictionaryAdapter emptyStringAsNull() {
		return new DictionaryAdapter(props, true);
	}
	
	private Object filterEmptyStringValue(Object value) {
		if (String.class.isInstance(value)) {
			if (emptyStringAsNull && ((String) value).isEmpty()) {
				return null;
			}
		}
		return value;
	}

	public final Collection<String> getStringCollection(String key, String... defaultValues) {
		Object raw = get(key);
		if (String[].class.isInstance(raw)) {
			defaultValues = (String[]) raw;
		} else if (String.class.isInstance(raw)) {
			defaultValues = ((String) raw).split(",");
		}
		if (defaultValues == null || defaultValues.length == 0) {
			return null;
		}
		return Arrays.asList(defaultValues);
	}
	
	public final String getString(String key) {
		return getString(key, null);
	}

	public final String getString(String key, String defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = filterEmptyStringValue(props.get(key));
		if (String.class.isInstance(val)) {
			return (String) val;
		}
		return defaultValue;
	}

	public final Long getLong(String key) {
		return getLong(key, null);
	}

	public final Long getLong(String key, Long defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = filterEmptyStringValue(props.get(key));
		if (Long.class.isInstance(val)) {
			return (Long) val;
		} else if (Number.class.isInstance(val)) {
			return ((Number) val).longValue();
		} else if (String.class.isInstance(val)) {
			try {
				return Long.valueOf((String) val);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public final Integer getInteger(String key) {
		return getInteger(key, null);
	}

	public final Integer getInteger(String key, Integer defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = props.get(key);
		if (Integer.class.isInstance(val)) {
			return (Integer) val;
		} else if (Number.class.isInstance(val)) {
			return ((Number) val).intValue();
		} else if (String.class.isInstance(val)) {
			try {
				return Integer.valueOf((String) val);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public final Float getFloat(String key) {
		return getFloat(key, null);
	}

	public final Float getFloat(String key, Float defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = filterEmptyStringValue(props.get(key));
		if (Float.class.isInstance(val)) {
			return (Float) val;
		} else if (Number.class.isInstance(val)) {
			return ((Number) val).floatValue();
		} else if (String.class.isInstance(val)) {
			try {
				return Float.valueOf((String) val);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public final Double getDouble(String key) {
		return getDouble(key, null);
	}

	public final Double getDouble(String key, Double defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = filterEmptyStringValue(props.get(key));
		if (Double.class.isInstance(val)) {
			return (Double) val;
		} else if (Number.class.isInstance(val)) {
			return ((Number) val).doubleValue();
		} else if (String.class.isInstance(val)) {
			try {
				return Double.valueOf((String) val);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public final Boolean getBoolean(String key) {
		return getBoolean(key, null);
	}

	public final Boolean getBoolean(String key, Boolean defaultValue) {
		if (props == null) {
			return defaultValue;
		}
		Object val = filterEmptyStringValue(props.get(key));
		if (Boolean.class.isInstance(val)) {
			return (Boolean) val;
		} else if (String.class.isInstance(val)) {
			return Boolean.parseBoolean((String) val);
		}
		return defaultValue;
	}

	public final Object get(Object key) {
		if (props == null) {
			return null;
		}
		return filterEmptyStringValue(props.get(key));
	}

}
