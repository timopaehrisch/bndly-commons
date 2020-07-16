package org.bndly.schema.definition.parser.impl;

/*-
 * #%L
 * Schema Definition Parser
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

import org.bndly.schema.definition.parser.api.ParsingException;
import org.bndly.schema.definition.parser.api.SerializingException;
import org.bndly.schema.model.Annotatable;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaDefinitionIOTest {
	@Test
	public void testParsingSingleSchema() throws ParsingException{
		SchemaDefinitionIOImpl parser = new SchemaDefinitionIOImpl();
		Path testFolder = Paths.get("src","test","resources");
		String rootSchemaDefinition = testFolder.resolve("schema.xml").toString();
		Schema parsed = parser.parse(rootSchemaDefinition);
		List<Type> types = parsed.getTypes();
		assertNotNull(types);
		assertEquals(types.size(), 4);
		assertEquals(types.get(0).getName(), "PriceModel");
		assertEquals(types.get(1).getName(), "AbstractProduct");
		assertEquals(types.get(2).getName(), "Product");
		assertEquals(types.get(3).getName(), "Variant");
		List<Mixin> mixins = parsed.getMixins();
		assertNotNull(mixins);
		assertEquals(mixins.size(), 2);
		assertEquals(mixins.get(0).getName(), "Purchasable");
		assertPurchasable(mixins.get(0));
		assertEquals(mixins.get(1).getName(), "DateAware");
		
		assertHasAnnotation(parsed, "createdBy", "bndly");
	}
	
	@Test
	public void testParsingExtendedSchema() throws ParsingException{
		SchemaDefinitionIOImpl parser = new SchemaDefinitionIOImpl();
		Path testFolder = Paths.get("src","test","resources");
		String rootSchemaDefinition = testFolder.resolve("schema.xml").toString();
		String extensionDefinition = testFolder.resolve("schema.extension.xml").toString();
		Schema parsed = parser.parse(rootSchemaDefinition, extensionDefinition);
		List<Type> types = parsed.getTypes();
		assertNotNull(types);
		assertEquals(types.size(), 5);
		assertEquals(types.get(0).getName(), "PriceModel");
		assertEquals(types.get(1).getName(), "AbstractProduct");
		assertEquals(types.get(2).getName(), "Product");
		assertEquals(types.get(3).getName(), "Variant");
		assertEquals(types.get(4).getName(), "Cart");
		List<Mixin> mixins = parsed.getMixins();
		assertNotNull(mixins);
		assertEquals(mixins.size(), 2);
		assertEquals(mixins.get(0).getName(), "Purchasable");
		assertExtendedPurchasable(mixins.get(0));
		assertEquals(mixins.get(1).getName(), "DateAware");
		
		assertHasAnnotation(parsed, "createdBy", "bndly");
	}
	
	private void assertHasAnnotation(Annotatable annotatable, String annotationName, String annotationValue) {
		Map<String, String> annotations = annotatable.getAnnotations();
		assertNotNull(annotations);
		String val = annotations.get(annotationName);
		assertNotNull(val);
		assertEquals(val, annotationValue);
	}
	
	private void assertPurchasable(Mixin purchasable) {
		List<Attribute> attributes = purchasable.getAttributes();
		assertNotNull(attributes);
		assertEquals(attributes.size(), 2);
		
		StringAttribute skuAttribute = (StringAttribute) attributes.get(0);
		assertEquals(skuAttribute.getLength(), Integer.valueOf(255));
		assertNull(skuAttribute.getIsLong());
		assertFalse(skuAttribute.isVirtual());
		assertFalse(skuAttribute.isMandatory());
		assertHasAnnotation(skuAttribute, "description", "also known as product number");
		
		TypeAttribute priceModelAttribute = (TypeAttribute) attributes.get(1);
		assertEquals(priceModelAttribute.getType().getName(), "PriceModel");
		assertTrue(priceModelAttribute.isVirtual());
		assertFalse(priceModelAttribute.isMandatory());
		
		assertHasAnnotation(purchasable, "tags", "products");
	}
	
	private void assertExtendedPurchasable(Mixin purchasable) {
		List<Attribute> attributes = purchasable.getAttributes();
		assertNotNull(attributes);
		assertEquals(attributes.size(), 3);
		
		StringAttribute supplierPIDAttribute = (StringAttribute) attributes.get(2);
		assertNull(supplierPIDAttribute.getIsLong());
		assertNull(supplierPIDAttribute.getLength());
		assertEquals(supplierPIDAttribute.getName(), "supplierPID");
		
		assertHasAnnotation(purchasable, "tags", "products");
	}
	
	@Test
	public void testXMLGeneration() throws IOException, SerializingException, ParsingException {
		Schema schema = new SchemaBuilder("test", "http://www.bndly.org/test")
				.type("TestType")
				.mixin("TestMixin")
					.typeAttribute("type", "TestType")
				.type("Foo").mixWith("TestMixin")
					.attribute("name", StringAttribute.class)
				.getSchema();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		new SchemaDefinitionIOImpl().serialize(schema, outputStream);
		outputStream.flush();
		String schemaAsXML = new String(outputStream.toByteArray(), "UTF-8");
		SchemaDefinitionIOImpl parser = new SchemaDefinitionIOImpl();
		Schema parsed = parser.parse(new ByteArrayInputStream(outputStream.toByteArray()));
		assertEquals(parsed.getTypes().size(), 2);
		assertEquals(parsed.getMixins().size(), 1);
		assertEquals(parsed.getMixins().get(0).getAttributes().get(0).getName(), "type");
		assertEquals(parsed.getTypes().get(1).getAttributes().get(0).getName(), "name");
	}
	
//	@Test
//	public void testXMLGenerationEbx() throws IOException, SerializingException, ParsingException {
//		Schema schema = new ShopSchema().getSchema();
//		
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		new SchemaDefinitionIOImpl().serialize(schema, outputStream);
//		outputStream.flush();
//		String schemaAsXML = new String(outputStream.toByteArray(), "UTF-8");
//		SchemaDefinitionIOImpl parser = new SchemaDefinitionIOImpl();
//		Schema parsed = parser.parse(new ByteArrayInputStream(outputStream.toByteArray()));
//	}
	
	
}
