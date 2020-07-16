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
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.RecordList;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.util.Iterator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class OneToManyRelationTest extends AbstractSchemaTest {
	
	@Test
	public void testOneToManyRelationPersistence() {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.type("Parent")
			.attribute("name", StringAttribute.class)
			.inverseTypeAttribute("children", "Child", "parent");
		
		sb.type("Child")
			.typeAttribute("parent", "Parent")
			.attribute("name", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		{
			RecordContext ctx = accessor.buildRecordContext();
			Record parent = ctx.create("Parent");
			parent.setAttributeValue("name", "p0");
			Record child0 = ctx.create("Child");
			child0.setAttributeValue("name", "c0");
			child0.setAttributeValue("parent", parent);
			Record child1 = ctx.create("Child");
			child1.setAttributeValue("name", "c1");
			child1.setAttributeValue("parent", parent);

			Transaction tx = engine.getQueryRunner().createTransaction();
			accessor.buildInsertQuery(parent, tx);
			accessor.buildInsertQuery(child0, tx);
			accessor.buildInsertQuery(child1, tx);
			tx.commit();
		}
		
		{
			RecordContext ctx = accessor.buildRecordContext();
			Record parent = accessor.query("PICK Parent p IF p.name=?", ctx, null, "p0").next();
			RecordList rl = ctx.createList(parent, "children");
			Iterator<Record> childrenIter = accessor.query("PICK Child c", ctx, null);
			while (childrenIter.hasNext()) {
				Record child = childrenIter.next();
				rl.add(child);
			}

			parent.setAttributeValue("children", rl);

			Record newChild = ctx.create("Child");
			newChild.setAttributeValue("name", "c2");
			newChild.setAttributeValue("parent", parent);

			Transaction tx = engine.getQueryRunner().createTransaction();
			accessor.buildInsertCascadedQuery(newChild, tx);
			tx.commit();

			Long childrenCount = accessor.count("COUNT Child c IF c.parent.id=?", parent.getId());
			Assert.assertEquals(childrenCount, Long.valueOf(3L));
		}
		
		{
			RecordContext ctx = accessor.buildRecordContext();
			Record parent = accessor.query("PICK Parent p IF p.name=?", ctx, null, "p0").next();
			RecordList rl = ctx.createList(parent, "children");
			Iterator<Record> childrenIter = accessor.query("PICK Child c", ctx, null);
			while (childrenIter.hasNext()) {
				Record child = childrenIter.next();
				rl.add(child);
			}
			Assert.assertEquals(rl.size(), 3);
			
			Record newChild = ctx.create("Child");
			newChild.setAttributeValue("name", "c2");
			newChild.setAttributeValue("parent", parent);

			rl.add(newChild);
			Assert.assertEquals(rl.size(), 4);
			
			Transaction tx = engine.getQueryRunner().createTransaction();
			accessor.buildInsertCascadedQuery(newChild, tx);
			tx.commit();
			
			Long childrenCount = accessor.count("COUNT Child c IF c.parent.id=?", parent.getId());
			Assert.assertEquals(childrenCount, Long.valueOf(4L));
		}
	}
}
