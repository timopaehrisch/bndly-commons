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

import org.bndly.common.json.impl.StringDeSerializer;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import java.io.IOException;
import java.io.StringWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CompiledObjectDeSerializerFactoryTest {

	public static class Pojo {

		private final static String STATIC_THING = "static";
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public static String getStaticThing() {
			return STATIC_THING;
		}
	}

	public static class PojoWithAnnotationsOnMethods {

		private final static String STATIC_THING = "static";
		private String name;

		@JSONProperty("NAME")
		public String getName() {
			return name;
		}

		@JSONProperty("NAME")
		public void setName(String name) {
			this.name = name;
		}

		public static String getStaticThing() {
			return STATIC_THING;
		}
	}

	public static class PojoWithAnnotationsOnField {

		private final static String STATIC_THING = "static";
		@JSONProperty("NAME")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public static String getStaticThing() {
			return STATIC_THING;
		}
	}

	@Test
	public void testPojo() throws IOException {
		Pojo pojo = new Pojo();
		pojo.name = "testPojo";
		Serializer serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(Pojo.class);
		ConversionContext conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		JSValue serialized = conversionContext.serialize(Pojo.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojo\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(Pojo.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.FIELDS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(Pojo.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojo\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(Pojo.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.GETTERS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(Pojo.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojo\"}");
	}

	private String toString(JSValue serialized) throws IOException {
		Assert.assertNotNull(serialized);
		StringWriter sw = new StringWriter();
		new JSONSerializer().serialize(serialized, sw);
		sw.flush();
		String s = sw.toString();
		return s;
	}

	@Test
	public void testPojoMethods() throws IOException {
		PojoWithAnnotationsOnMethods pojo = new PojoWithAnnotationsOnMethods();
		pojo.name = "testPojoMethods";
		Serializer serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnMethods.class);
		ConversionContext conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		JSValue serialized = conversionContext.serialize(PojoWithAnnotationsOnMethods.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"NAME\":\"testPojoMethods\"}"); // getters only be default

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnMethods.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.FIELDS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnMethods.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojoMethods\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnMethods.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.GETTERS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnMethods.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"NAME\":\"testPojoMethods\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnMethods.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.GETTERS_AND_FIELDS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnMethods.class, pojo);
		// we first inspected getters and then fields, and since both have different names, we have two values in the json
		Assert.assertEquals(toString(serialized), "{\"NAME\":\"testPojoMethods\",\"name\":\"testPojoMethods\"}");
	}

	@Test
	public void testPojoFields() throws IOException {
		PojoWithAnnotationsOnField pojo = new PojoWithAnnotationsOnField();
		pojo.name = "testPojoFields";
		Serializer serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnField.class);
		ConversionContext conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		JSValue serialized = conversionContext.serialize(PojoWithAnnotationsOnField.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojoFields\"}"); // getters only be default

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnField.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.FIELDS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnField.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"NAME\":\"testPojoFields\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnField.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.GETTERS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnField.class, pojo);
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojoFields\"}");

		serializer = CompiledObjectDeSerializerFactory.INSTANCE.compileSerializer(PojoWithAnnotationsOnField.class, null, CompiledObjectDeSerializerFactory.CompilationFlavor.GETTERS_AND_FIELDS);
		conversionContext = new ConversionContextBuilder().add(new StringDeSerializer()).serializer(serializer).build();
		serialized = conversionContext.serialize(PojoWithAnnotationsOnField.class, pojo);
		// we first inspected getters and then fields, and since both have different names, we have two values in the json
		Assert.assertEquals(toString(serialized), "{\"name\":\"testPojoFields\",\"NAME\":\"testPojoFields\"}");
	}
}
