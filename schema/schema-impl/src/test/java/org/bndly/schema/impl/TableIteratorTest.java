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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TableIteratorTest extends AbstractSchemaTest {
	@Test
	public void testSimple() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("TestEntity")
				.attribute("myAttribute", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		final List<AttributeColumn> columns = new ArrayList<>();
		
		TableHierarchyIterator.iterateTypeHierarchyDown("TestEntity", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback() {

			@Override
			public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				columns.add(column);
			}
		});
		
		Assert.assertEquals(columns.size(), 2);
		Assert.assertEquals(columns.get(0).getAttribute().getName(), "id");
		Assert.assertEquals(columns.get(1).getAttribute().getName(), "myAttribute");
	}
	
	@Test
	public void testHierarchy() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("TestEntity")
				.attribute("myAttribute", StringAttribute.class)
			.type("SubTestEntity")
				.parentType("TestEntity")
				.attribute("mySubAttribute", StringAttribute.class)
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		final List<String> columns = new ArrayList<>();
		
		TableHierarchyIterator.iterateTypeHierarchyDown("TestEntity", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback() {

			@Override
			public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				columns.add(table.getTableName()+"_"+column.getColumnName());
			}
		});
		
		Assert.assertEquals(columns.size(), 8);
		Assert.assertEquals(columns.get(0), "JOIN_TESTENTITY_ID");
		Assert.assertEquals(columns.get(1), "JOIN_TESTENTITY_TESTENTITY_ID");
		Assert.assertEquals(columns.get(2), "TESTENTITY_ID");
		Assert.assertEquals(columns.get(3), "TESTENTITY_MYATTRIBUTE");
		Assert.assertEquals(columns.get(4), "JOIN_TESTENTITY_SUBTESTENTITY_ID");
		Assert.assertEquals(columns.get(5), "SUBTESTENTITY_ID");
		Assert.assertEquals(columns.get(6), "SUBTESTENTITY_MYSUBATTRIBUTE");
		Assert.assertEquals(columns.get(7), "SUBTESTENTITY_MYATTRIBUTE");
	}
	
	@Test
	public void testTree() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Node")
				.attribute("prop", StringAttribute.class)
				.typeAttribute("parent", "Node")
			.type("Something")
				.typeAttribute("tree", "Node")
			.type("SomethingElse")
				.parentType("Something")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		TableHierarchyIterator.iterateTypeHierarchyDown("Something", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback());
		TableHierarchyIterator.iterateTypeHierarchyDownAndFollowAttributes("Something", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback());
		TableHierarchyIterator.iterateTypeHierarchyDown("Node", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback());
		TableHierarchyIterator.iterateTypeHierarchyDownAndFollowAttributes("Node", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback());
		
		final List<String> columns = new ArrayList<>();
		final List<String> attributes = new ArrayList<>();
		
		TableHierarchyIterator.iterateTypeHierarchyDownAndFollowAttributes("Something", engine.getTableRegistry(), new TableHierarchyIterator.NoOpIterationCallback(){

			@Override
			public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				columns.add(table.getTableName() + "_" + column.getColumnName());
				attributes.add(joinAttributeStack(isPrimaryKeyColumn ? attributeStack : domainAttributeStack));
			}
			
		});
		
		Assert.assertEquals(columns.get(0), "JOIN_SOMETHING_ID");
		Assert.assertEquals(columns.get(1), "JOIN_SOMETHING_SOMETHING_ID");
		Assert.assertEquals(columns.get(2), "SOMETHING_ID");
		Assert.assertEquals(columns.get(3), "SOMETHING_TREE_ID");
		Assert.assertEquals(columns.get(4), "NODE_ID");
		Assert.assertEquals(columns.get(5), "NODE_PROP");
		Assert.assertEquals(columns.get(6), "NODE_PARENT_ID");
		Assert.assertEquals(columns.get(7), "JOIN_SOMETHING_SOMETHINGELSE_ID");
		Assert.assertEquals(columns.get(8), "SOMETHINGELSE_ID");
		Assert.assertEquals(columns.get(9), "SOMETHINGELSE_TREE_ID");
		Assert.assertEquals(columns.get(10), "NODE_ID");
		Assert.assertEquals(columns.get(11), "NODE_PROP");
		Assert.assertEquals(columns.get(12), "NODE_PARENT_ID");
		Assert.assertEquals(columns.size(), 13);
		
		Assert.assertEquals(attributes.get(0), "");
		Assert.assertEquals(attributes.get(1), "");
		Assert.assertEquals(attributes.get(2), "id");
		Assert.assertEquals(attributes.get(3), "tree");
		Assert.assertEquals(attributes.get(4), "tree.id");
		Assert.assertEquals(attributes.get(5), "tree.prop");
		Assert.assertEquals(attributes.get(6), "tree.parent");
		Assert.assertEquals(attributes.get(7), "");
		Assert.assertEquals(attributes.get(8), "id");
		Assert.assertEquals(attributes.get(9), "tree");
		Assert.assertEquals(attributes.get(10), "tree.id");
		Assert.assertEquals(attributes.get(11), "tree.prop");
		Assert.assertEquals(attributes.get(12), "tree.parent");
		Assert.assertEquals(attributes.size(), 13);
	}
	
	private String joinAttributeStack(Attribute[] attributeStack) {
		StringBuffer sb = null;
		for (Attribute att : attributeStack) {
			if(sb == null) {
				sb = new StringBuffer();
			} else {
				sb.append(".");
			}
			sb.append(att.getName());
		}
		return sb == null ? "" : sb.toString();
	}
}
