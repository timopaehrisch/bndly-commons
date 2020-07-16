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
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class InsertQueryTest extends AbstractSchemaTest {
	
	private static final String TYPE_SPECIAL_CHILD = "SpecialChild";
	private static final String TYPE_CHILD = "Child";
	private static final String TYPE_PARENT = "Parent";
	
	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_CHILD = "child";
	
	@Test
	public void testInsertCascadedWithReferencedSubtypedEntity() {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.type(TYPE_PARENT)
			.typeAttribute(ATTRIBUTE_CHILD, TYPE_CHILD);
		
		sb.type(TYPE_CHILD)
			.attribute(ATTRIBUTE_NAME, StringAttribute.class);
		
		sb.type(TYPE_SPECIAL_CHILD)
			.parentType(TYPE_CHILD);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext recordContext = accessor.buildRecordContext();
		Record parent = recordContext.create(TYPE_PARENT);
		Record child = recordContext.create(TYPE_CHILD);
		child.setAttributeValue(ATTRIBUTE_NAME, "testInsertCascadedWithReferencedSubtypedEntity");
		parent.setAttributeValue(ATTRIBUTE_CHILD, child);
		Transaction tx = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(parent, tx);
		tx.commit();
		Assert.assertNotNull(parent.getId());
		Assert.assertNotNull(child.getId());

		Record parentReloaded = accessor.readById(TYPE_PARENT, parent.getId(), accessor.buildRecordContext());
		Record childReloaded = parentReloaded.getAttributeValue(ATTRIBUTE_CHILD, Record.class);
		Assert.assertNotNull(childReloaded);
		Assert.assertNotNull(childReloaded.getId());
		Assert.assertEquals(childReloaded.getType().getName(), TYPE_CHILD);

		Record parent2 = recordContext.create(TYPE_PARENT);
		Record child2 = recordContext.create(TYPE_SPECIAL_CHILD);
		child2.setAttributeValue(ATTRIBUTE_NAME, "testInsertCascadedWithReferencedSubtypedEntity");
		parent2.setAttributeValue(ATTRIBUTE_CHILD, child2);
		Transaction tx2 = engine.getQueryRunner().createTransaction();
		accessor.buildInsertQuery(child2, tx2);
		accessor.buildInsertQuery(parent2, tx2);
		tx2.commit();
		Assert.assertNotNull(parent2.getId());
		Assert.assertNotNull(child2.getId());

		Record parentReloaded2 = accessor.readById(TYPE_PARENT, parent2.getId(), accessor.buildRecordContext());
		Record childReloaded2 = parentReloaded2.getAttributeValue(ATTRIBUTE_CHILD, Record.class);
		Assert.assertNotNull(childReloaded2);
		Assert.assertNotNull(childReloaded2.getId());
		Assert.assertEquals(childReloaded2.getType().getName(), TYPE_SPECIAL_CHILD);

		Record parent3 = recordContext.create(TYPE_PARENT);
		Record child3 = recordContext.create(TYPE_CHILD);
		child3.setAttributeValue(ATTRIBUTE_NAME, "testInsertCascadedWithReferencedSubtypedEntity");
		parent3.setAttributeValue(ATTRIBUTE_CHILD, child3);
		Transaction tx3 = engine.getQueryRunner().createTransaction();
		accessor.buildInsertQuery(child3, tx3);
		accessor.buildInsertQuery(parent3, tx3);
		tx3.commit();
		Assert.assertNotNull(parent3.getId());
		Assert.assertNotNull(child3.getId());

		Record parentReloaded3 = accessor.readById(TYPE_PARENT, parent3.getId(), accessor.buildRecordContext());
		Record childReloaded3 = parentReloaded3.getAttributeValue(ATTRIBUTE_CHILD, Record.class);
		Assert.assertNotNull(childReloaded3);
		Assert.assertNotNull(childReloaded3.getId());
		Assert.assertEquals(childReloaded3.getType().getName(), TYPE_CHILD);
	}
}
