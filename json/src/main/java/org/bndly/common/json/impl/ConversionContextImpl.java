package org.bndly.common.json.impl;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.Deserializer;
import org.bndly.common.json.api.Instanciator;
import org.bndly.common.json.api.KeyConverter;
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.Stack;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ConversionContextImpl implements ConversionContext {

	private final List<Serializer> serializers = new ArrayList<>();
	private final List<Deserializer> deserializers = new ArrayList<>();
	private final List<Instanciator> instanciators = new ArrayList<>();
	private final List<KeyConverter> keyConverters = new ArrayList<>();
	private boolean stopAtCycles;
	private final Stack<Object> objectStack = new Stack<>();
	private boolean skipNullValues;

	@Override
	public String memberNameOfMapEntry(Type mapType, Type entryType, Object entryKey, Object entryValue) {
		for (KeyConverter keyConverter : keyConverters) {
			if (keyConverter.canCreateJSONKey(mapType, entryType, this, entryKey, entryValue)) {
				return keyConverter.createJSONKey(mapType, entryType, this, entryKey, entryValue);
			}
		}
		return null;
	}

	@Override
	public JSValue serialize(Type sourceType, Object value) {
		if (skipNullValues && value == null) {
			return null;
		}
		if (stopAtCycles && objectStack.contains(value)) {
			return null;
		}
		try {
			if (stopAtCycles) {
				objectStack.add(value);
			}
			for (Serializer serializer : serializers) {
				if (serializer.canSerialize(sourceType, this, value)) {
					return serializer.serialize(sourceType, this, value);
				}
			}
			return null;
		} finally {
			if (stopAtCycles) {
				objectStack.pop();
			}
		}
	}

	@Override
	public Object deserialize(Type targetType, JSValue value) {
		if (stopAtCycles && objectStack.contains(value)) {
			return null;
		}
		try {
			if (stopAtCycles) {
				objectStack.add(value);
			}
			for (Deserializer deserializer : deserializers) {
				if (deserializer.canDeserialize(targetType, this, value)) {
					return deserializer.deserialize(targetType, this, value);
				}
			}
			return null;
		} finally {
			if (stopAtCycles) {
				objectStack.pop();
			}
		}
	}

	@Override
	public Object newInstance(Type desiredType, JSValue value) {
		for (Instanciator instanciator : instanciators) {
			if (instanciator.canInstantiate(desiredType, this, value)) {
				return instanciator.instantiate(desiredType, this, value);
			}
		}
		return null;
	}

	@Override
	public boolean canInstantiate(Type desiredType, JSValue value) {
		for (Instanciator instanciator : instanciators) {
			if (instanciator.canInstantiate(desiredType, this, value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canKeyMapEntry(Type mapType, Type keyType, Type entryType, JSValue jsValue, String jsKey, Object deserializedJavaValue) {
		for (KeyConverter keyConverter : keyConverters) {
			if (keyConverter.canCreateJavaKey(mapType, keyType, entryType, this, jsValue, jsKey, deserializedJavaValue)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object keyOfMapEntry(Type mapType, Type keyType, Type entryType, JSValue jsValue, String jsKey, Object deserializedJavaValue) {
		for (KeyConverter keyConverter : keyConverters) {
			if (keyConverter.canCreateJavaKey(mapType, keyType, entryType, this, jsValue, jsKey, deserializedJavaValue)) {
				return keyConverter.createJavaKey(mapType, keyType, entryType, this, jsValue, jsKey, deserializedJavaValue);
			}
		}
		return null;
	}
	
	public void addSerializer(Serializer serializer) {
		if (serializer != null) {
			serializers.add(0, serializer);
		}
	}
	
	public void addDeserializer(Deserializer deserializer) {
		if (deserializer != null) {
			deserializers.add(0, deserializer);
		}
	}
	
	public void removeSerializer(Serializer serializer) {
		if (serializer != null) {
			serializers.remove(serializer);
		}
	}

	public void removeSerializer(Deserializer deserializer) {
		if (deserializer != null) {
			deserializers.remove(deserializer);
		}
	}

	public void setStopAtCycles(boolean stopAtCycles) {
		this.stopAtCycles = stopAtCycles;
	}

	public void addInstanciator(Instanciator instanciator) {
		if (instanciator != null) {
			instanciators.add(0, instanciator);
		}
	}

	public void removeInstanciator(Instanciator instanciator) {
		if (instanciator != null) {
			instanciators.remove(instanciator);
		}
	}

	public void addKeyConverter(KeyConverter keyConverter) {
		if (keyConverter != null) {
			keyConverters.add(0, keyConverter);
		}
	}
	
	public void removeKeyConverter(KeyConverter keyConverter) {
		if (keyConverter != null) {
			keyConverters.remove(keyConverter);
		}
	}

	public void setSkipNullValues(boolean skipNullValues) {
		this.skipNullValues = skipNullValues;
	}
	
}
