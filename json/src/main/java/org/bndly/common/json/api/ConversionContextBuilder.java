package org.bndly.common.json.api;

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

import org.bndly.common.json.impl.ArrayDeSerializer;
import org.bndly.common.json.impl.BooleanDeSerializer;
import org.bndly.common.json.impl.CollectionDeSerializer;
import org.bndly.common.json.impl.ConversionContextImpl;
import org.bndly.common.json.impl.DateDeSerializer;
import org.bndly.common.json.impl.DefaultInstanciator;
import org.bndly.common.json.impl.DefaultKeyConverter;
import org.bndly.common.json.impl.InterfaceDeserializer;
import org.bndly.common.json.impl.MapDeSerializer;
import org.bndly.common.json.impl.NullDeSerializer;
import org.bndly.common.json.impl.NumberDeSerializer;
import org.bndly.common.json.impl.ObjectDeSerializer;
import org.bndly.common.json.impl.StringDeSerializer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ConversionContextBuilder {
	private final List<Serializer> serializers = new ArrayList<>();
	private final List<Deserializer> deserializers = new ArrayList<>();
	private final List<Instanciator> instanciators = new ArrayList<>();
	private final List<KeyConverter> keyConverters = new ArrayList<>();
	private boolean stopAtCycles;
	private boolean skipNullValues;
	
	public final ConversionContextBuilder serializer(Serializer serializer) {
		if (serializer != null) {
			serializers.add(serializer);
		}
		return this;
	}

	public final ConversionContextBuilder deserializer(Deserializer deserializer) {
		if (deserializer != null) {
			deserializers.add(deserializer);
		}
		return this;
	}

	public final ConversionContextBuilder instanciator(Instanciator instanciator) {
		if (instanciator != null) {
			instanciators.add(instanciator);
		}
		return this;
	}

	public final ConversionContextBuilder keyConverter(KeyConverter keyConverter) {
		if (keyConverter != null) {
			keyConverters.add(keyConverter);
		}
		return this;
	}
	
	public final ConversionContextBuilder stopAtCycles() {
		stopAtCycles = true;
		return this;
	}
	
	public final ConversionContextBuilder skipNullValues() {
		skipNullValues = true;
		return this;
	}
	
	public final ConversionContextBuilder initDefaults() {
		add(new ObjectDeSerializer()); // the object serializer has to be the last one, because he can serialize anything that is not null
		add(new InterfaceDeserializer()); // interfaces can be deserialized via dynamic proxies.
		add(new DateDeSerializer());
		add(new BooleanDeSerializer());
		add(new NumberDeSerializer());
		add(new StringDeSerializer());
		add(new ArrayDeSerializer());
		add(new CollectionDeSerializer());
		add(new MapDeSerializer());
		add(new NullDeSerializer()); // the null serializer has to be in front, because he can serialize null objects.
		
		add(new DefaultInstanciator());
		
		add(new DefaultKeyConverter());
		return this;
	} 

	public final ConversionContextBuilder add(Object object) {
		if (Serializer.class.isInstance(object)) {
			serializer((Serializer) object);
		}
		if (Deserializer.class.isInstance(object)) {
			deserializer((Deserializer) object);
		}
		if (Instanciator.class.isInstance(object)) {
			instanciator((Instanciator) object);
		}
		if (KeyConverter.class.isInstance(object)) {
			keyConverter((KeyConverter) object);
		}
		return this;
	}
	
	public final ConversionContext build()  {
		ConversionContextImpl conversionContextImpl = new ConversionContextImpl();
		for (Serializer serializer : serializers) {
			conversionContextImpl.addSerializer(serializer);
		}
		for (Deserializer deserializer : deserializers) {
			conversionContextImpl.addDeserializer(deserializer);
		}
		for (Instanciator instanciator : instanciators) {
			conversionContextImpl.addInstanciator(instanciator);
		}
		for (KeyConverter keyConverter : keyConverters) {
			conversionContextImpl.addKeyConverter(keyConverter);
		}
		conversionContextImpl.setStopAtCycles(stopAtCycles);
		conversionContextImpl.setSkipNullValues(skipNullValues);
		return conversionContextImpl;
	}
}
