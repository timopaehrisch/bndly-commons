package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SimpleSchemaQueryTest extends AbstractSchemaTest {
	
	@Test
	public void testSingleFlatType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("SimpleType")
				.attribute("name", StringAttribute.class)
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		Record newRecord = accessor.buildRecordContext().create("SimpleType");
		newRecord.setAttributeValue("name", "test123");
		long id = accessor.insert(newRecord);
		
		Record read = accessor.readById("SimpleType", id, accessor.buildRecordContext());
		String returnedValue = read.getAttributeValue("name", String.class);
		Assert.assertEquals(returnedValue, "test123");
	}

	@Test
	public void testTypeWithTypeAttribute() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("SimpleType")
				.attribute("name", StringAttribute.class)
			.type("OtherType")
				.typeAttribute("simple", "SimpleType")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		Record newRecord = accessor.buildRecordContext().create("SimpleType");
		newRecord.setAttributeValue("name", "test123");
		long id = accessor.insert(newRecord);
		Record otherRecord = newRecord.getContext().create("OtherType");
		otherRecord.setAttributeValue("simple", newRecord);
		accessor.insert(otherRecord);
		
		Record read = accessor.readById("OtherType", id, accessor.buildRecordContext());
		Record returnedValue = read.getAttributeValue("simple", Record.class);
		Assert.assertNotNull(returnedValue);
		Assert.assertTrue(returnedValue.isAttributePresent("name"), "name attribute was not present but it should have been");
		String name = returnedValue.getAttributeValue("name", String.class);
		Assert.assertNotNull(name);
		Assert.assertEquals(name, "test123");
	}
	
	@Test
	public void testTypeWithMixinAttribute() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("MixMe")
				.attribute("name", StringAttribute.class)
			.type("SimpleType")
				.mixWith("MixMe")
			.type("OtherType")
				.typeAttribute("simple", "SimpleType")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		Record newRecord = accessor.buildRecordContext().create("SimpleType");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);
		Record otherRecord = newRecord.getContext().create("OtherType");
		otherRecord.setAttributeValue("simple", newRecord);
		long id = accessor.insert(otherRecord);
		
		Record read = accessor.readById("OtherType", id, accessor.buildRecordContext());
		Record returnedValue = read.getAttributeValue("simple", Record.class);
		Assert.assertNotNull(returnedValue);
		Assert.assertTrue(returnedValue.isAttributePresent("name"), "name attribute was not present but it should have been");
		String name = returnedValue.getAttributeValue("name", String.class);
		Assert.assertNotNull(name);
		Assert.assertEquals(name, "test123");
	}
	
	@Test
	public void testSubType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("ParentType")
				.attribute("pname", StringAttribute.class)
			.type("SimpleType")
				.attribute("name", StringAttribute.class)
			.type("OtherType")
				.parentType("ParentType")
				.typeAttribute("simple", "SimpleType")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		Record newRecord = accessor.buildRecordContext().create("SimpleType");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);
		Record otherRecord = newRecord.getContext().create("OtherType");
		otherRecord.setAttributeValue("simple", newRecord);
		long id = accessor.insert(otherRecord);
		
		Record read = accessor.readById("OtherType", id, accessor.buildRecordContext());
		Record returnedValue = read.getAttributeValue("simple", Record.class);
		Assert.assertNotNull(returnedValue);
		Assert.assertTrue(returnedValue.isAttributePresent("name"), "name attribute was not present but it should have been");
		String name = returnedValue.getAttributeValue("name", String.class);
		Assert.assertNotNull(name);
		Assert.assertEquals(name, "test123");
	}
	
	@Test
	public void testTreeType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Node")
				.typeAttribute("parent", "Node")
				.attribute("name", StringAttribute.class)
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		Record newRecord = accessor.buildRecordContext().create("Node");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);
		Record otherRecord = newRecord.getContext().create("Node");
		otherRecord.setAttributeValue("parent", newRecord);
		otherRecord.setAttributeValue("name", "abcdef");
		long id = accessor.insert(otherRecord);
		
		Record read = accessor.readById("Node", id, accessor.buildRecordContext());
		String name1 = read.getAttributeValue("name", String.class);
		Assert.assertEquals(name1, "abcdef");
		Record returnedValue = read.getAttributeValue("parent", Record.class);
		Assert.assertNotNull(returnedValue);
		String name2 = returnedValue.getAttributeValue("name", String.class);
		Assert.assertEquals(name2, "test123");
	}
	
	@Test
	public void testInheritenceTreeType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Node")
				.typeAttribute("parent", "Node")
			.type("StringNode")
				.parentType("Node")
				.attribute("name", StringAttribute.class)
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		TableHierarchyIterator.iterateTypeHierarchyDownAndFollowAttributes("StringNode", engine.getTableRegistry(), new LoggingTableIteratorListener());
		
		Record newRecord = accessor.buildRecordContext().create("StringNode");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);
		Record otherRecord = newRecord.getContext().create("StringNode");
		otherRecord.setAttributeValue("parent", newRecord);
		otherRecord.setAttributeValue("name", "abcdef");
		long id = accessor.insert(otherRecord);
		
		Record read = accessor.readById("StringNode", id, accessor.buildRecordContext());
		String name1 = read.getAttributeValue("name", String.class);
		Assert.assertEquals(name1, "abcdef");
		Record returnedValue = read.getAttributeValue("parent", Record.class);
		Assert.assertNotNull(returnedValue);
		String name2 = returnedValue.getAttributeValue("name", String.class);
		Assert.assertEquals(name2, "test123");
	}
}
