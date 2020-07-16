package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.rest.entity.resources.queryutiltest.Foo;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QueryParameterUtilTest {
	
	@Test
	public void testSimpleIteration(){
		Schema schema = new SchemaBuilder("test", "http://www.cybercon.de")
				.type("Foo")
					.attribute("value", StringAttribute.class)
					.typeAttribute("ref", "Bar")
					.mixinAttribute("refMix", "Mix")
				.type("Bar")
					.attribute("barValue", StringAttribute.class)
					.mixWith("Mix")
				.type("Baz")
					.attribute("bazValue", StringAttribute.class)
					.parentType("Bar")
				.mixin("Mix")
					.attribute("mixValue", StringAttribute.class)
				.getSchema();
		
		List<Type> types = schema.getTypes();
		Map<String, Type> typesByName = new HashMap<>();
		for (Type type : types) {
			typesByName.put(type.getName(), type);
		}
		
		QueryParameterUtil queryParameterUtil = new QueryParameterUtil(typesByName.get("Foo"), Foo.class);
		Set<String> keySet = queryParameterUtil.getJavaTypesByParameterName().keySet();
		// Foo should offer the following parameters
		Assert.assertTrue(keySet.contains("value"));
		Assert.assertTrue(keySet.contains("ref_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("ref_Bar_barValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_barValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_bazValue"));
		Assert.assertTrue(keySet.contains("refMix_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("refMix_Bar_barValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_barValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_bazValue"));
		Assert.assertEquals(keySet.size(), 11);
	}
	
	@Test
	public void testRecursiveIteration(){
		Schema schema = new SchemaBuilder("test", "http://www.cybercon.de")
				.type("Foo")
					.attribute("value", StringAttribute.class)
					.typeAttribute("ref", "Bar")
					.typeAttribute("refFoo", "Foo")
					.mixinAttribute("refMix", "Mix")
				.type("Bar")
					.attribute("barValue", StringAttribute.class)
					.mixWith("Mix")
				.type("Baz")
					.attribute("bazValue", StringAttribute.class)
					.parentType("Bar")
				.mixin("Mix")
					.attribute("mixValue", StringAttribute.class)
				.getSchema();
		
		List<Type> types = schema.getTypes();
		Map<String, Type> typesByName = new HashMap<>();
		for (Type type : types) {
			typesByName.put(type.getName(), type);
		}
		
		QueryParameterUtil queryParameterUtil = new QueryParameterUtil(typesByName.get("Foo"), Foo.class);
		Set<String> keySet = queryParameterUtil.getJavaTypesByParameterName().keySet();
		// Foo should offer the following parameters
		Assert.assertTrue(keySet.contains("value"));
		Assert.assertTrue(keySet.contains("ref_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("ref_Bar_barValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_barValue"));
		Assert.assertTrue(keySet.contains("ref_Baz_bazValue"));
		Assert.assertTrue(keySet.contains("refMix_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("refMix_Bar_barValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_barValue"));
		Assert.assertTrue(keySet.contains("refMix_Baz_bazValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_value"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_ref_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_ref_Bar_barValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_ref_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_ref_Baz_barValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_ref_Baz_bazValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_refMix_Bar_mixValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_refMix_Bar_barValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_refMix_Baz_mixValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_refMix_Baz_barValue"));
		Assert.assertTrue(keySet.contains("refFoo_Foo_refMix_Baz_bazValue"));
		Assert.assertEquals(keySet.size(), 22);
	}
}
