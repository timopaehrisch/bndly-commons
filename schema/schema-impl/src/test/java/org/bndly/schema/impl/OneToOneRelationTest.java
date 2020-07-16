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
import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.impl.persistence.PersistenceManager;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class OneToOneRelationTest extends AbstractSchemaTest {
	@Test
	public void testOneToOneRelationPersistence() {
		Deployer deployer = engine.getDeployer();
		AccessorImpl accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
		.type("Foo")
			.attribute("name", StringAttribute.class)
			.typeAttribute("myBar", "Bar")
		
		.type("Bar")
			.typeAttribute("myFoo", "Foo")
			.attribute("name", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext recordContext = accessor.buildRecordContext();
		Record foo = recordContext.create("Foo");
		Record bar = recordContext.create("Bar");
		foo.setAttributeValue("myBar", bar);
		bar.setAttributeValue("myFoo", foo);
		
		new PersistenceManager(engine, engine.getQueryRunner().createTransaction()).append(foo).finalizeTransaction().commit();
		
		Assert.assertNotNull(foo.getId());
		Assert.assertNotNull(bar.getId());
		Assert.assertEquals(foo.getAttributeValue("myBar"), bar);
		Assert.assertEquals(bar.getAttributeValue("myFoo"), foo);
		
		// then load foo and bar from the database and check if they had been persisted correctly
		RecordContext freshRecordContext = accessor.buildRecordContext();
		Record freshFoo = accessor.readById("Foo", foo.getId(), freshRecordContext);
		Record freshBar = accessor.readById("Bar", bar.getId(), freshRecordContext);
		Assert.assertNotNull(freshFoo.getAttributeValue("myBar", Record.class));
		Assert.assertNotNull(freshBar.getAttributeValue("myFoo", Record.class));
		Assert.assertEquals(freshFoo.getAttributeValue("myBar", Record.class), freshBar);
		Assert.assertEquals(freshBar.getAttributeValue("myFoo", Record.class), freshFoo);
		
		// now do the same thing with accessor, because this is the public API
		recordContext = accessor.buildRecordContext();
		foo = recordContext.create("Foo");
		bar = recordContext.create("Bar");
		foo.setAttributeValue("myBar", bar);
		bar.setAttributeValue("myFoo", foo);
		
		Transaction publicApiTransaction = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(foo, publicApiTransaction);
		publicApiTransaction.commit();
		
		Assert.assertNotNull(foo.getId());
		Assert.assertNotNull(bar.getId());
		Assert.assertEquals(foo.getAttributeValue("myBar"), bar);
		Assert.assertEquals(bar.getAttributeValue("myFoo"), foo);
		
		// then load foo and bar from the database and check if they had been persisted correctly
		freshRecordContext = accessor.buildRecordContext();
		freshFoo = accessor.readById("Foo", foo.getId(), freshRecordContext);
		freshBar = accessor.readById("Bar", bar.getId(), freshRecordContext);
		Assert.assertNotNull(freshFoo.getAttributeValue("myBar", Record.class));
		Assert.assertNotNull(freshBar.getAttributeValue("myFoo", Record.class));
		Assert.assertEquals(freshFoo.getAttributeValue("myBar", Record.class), freshBar);
		Assert.assertEquals(freshBar.getAttributeValue("myFoo", Record.class), freshFoo);
		
		// now try an update
		new PersistenceManager(engine, engine.getQueryRunner().createTransaction()).append(freshFoo).finalizeTransaction().commit();
		Assert.assertNotNull(freshFoo.getAttributeValue("myBar", Record.class));
		Assert.assertNotNull(freshBar.getAttributeValue("myFoo", Record.class));
		Assert.assertEquals(freshFoo.getAttributeValue("myBar", Record.class), freshBar);
		Assert.assertEquals(freshBar.getAttributeValue("myFoo", Record.class), freshFoo);
		
		// and load the items again to a fresh context to see, that the update did the right changes
		freshRecordContext = accessor.buildRecordContext();
		freshFoo = accessor.readById("Foo", foo.getId(), freshRecordContext);
		freshBar = accessor.readById("Bar", bar.getId(), freshRecordContext);
		Assert.assertNotNull(freshFoo.getAttributeValue("myBar", Record.class));
		Assert.assertNotNull(freshBar.getAttributeValue("myFoo", Record.class));
		Assert.assertEquals(freshFoo.getAttributeValue("myBar", Record.class), freshBar);
		Assert.assertEquals(freshBar.getAttributeValue("myFoo", Record.class), freshFoo);
	}
	
	@Test
	public void testOneToOneUniqueness() {
		// test unique constraints on a one-to-one field
		Deployer deployer = engine.getDeployer();
		AccessorImpl accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
		.type("Clearance")
			.attribute("name", StringAttribute.class)
			.typeAttribute("product", "RetailerProduct") // this attribute can't be mandatory
		
		.type("RetailerProduct")
			.typeAttribute("clearance", "Clearance")
				.cascadeDelete()
				.mandatory()
			.attribute("name", StringAttribute.class)
			.unique("clearance")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext recordContext = accessor.buildRecordContext();
		
		Record clearance = recordContext.create("Clearance");
		accessor.insert(clearance);
		
		// the clearance has already been persisted. now a retailer might offer a product based on that clearance.
		Record product = recordContext.create("RetailerProduct");
		product.setAttributeValue("clearance", clearance);
		clearance.setAttributeValue("product", product);
		Transaction transaction = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(product, transaction);
		transaction.commit();
		
		// reload the clearance and product
		recordContext = accessor.buildRecordContext();
		clearance = accessor.readById("Clearance", clearance.getId(), recordContext);
		Assert.assertNotNull(clearance);
		Assert.assertNotNull(clearance.getAttributeValue("product", Record.class));
		
		// now try to create another product based on the same clearance
		Record anotherProduct = recordContext.create("RetailerProduct");
		anotherProduct.setAttributeValue("clearance", clearance);
		
		try {
			transaction = engine.getQueryRunner().createTransaction();
			accessor.buildInsertCascadedQuery(anotherProduct, transaction);
			transaction.commit();
			Assert.fail("expected a constraint violation");
		} catch (ConstraintViolationException e) {
			// this is good
			// there should be exactly one product in the database
			Assert.assertEquals(accessor.count("COUNT RetailerProduct").longValue(), 1);
			Assert.assertNull(anotherProduct.getId());
		}
		
		// now create another clearance and persist the anotherProduct
		Record anotherClearance = recordContext.create("Clearance");
		anotherClearance.setAttributeValue("product", anotherProduct);
		anotherProduct.setAttributeValue("clearance", anotherClearance);
		transaction = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(anotherProduct, transaction);
		transaction.commit();
		Assert.assertEquals(accessor.count("COUNT RetailerProduct").longValue(), 2);
		
		anotherProduct.setAttributeValue("clearance", clearance);
		try {
			transaction = engine.getQueryRunner().createTransaction();
			accessor.buildUpdateCascadedQuery(anotherProduct, transaction);
			transaction.commit();
		} catch (ConstraintViolationException e) {
			// this is good
		}
		
		// now remove the anotherClearance and see, if the assigned product is gone
		transaction = engine.getQueryRunner().createTransaction();
		accessor.delete(anotherClearance, transaction);
		transaction.commit();
		
		Record reloadedAnotherProduct = accessor.readById("RetailerProduct", anotherProduct.getId(), accessor.buildRecordContext());
		Assert.assertNull(reloadedAnotherProduct);
		Assert.assertEquals(accessor.count("COUNT RetailerProduct").longValue(), 1);
		
		// ensure that a product can not be created without a clearance
		Record poorProduct = recordContext.create("RetailerProduct");
		try {
			transaction = engine.getQueryRunner().createTransaction();
			accessor.buildInsertCascadedQuery(poorProduct, transaction);
			transaction.commit();
			Assert.fail("expected a constraint violation");
		} catch (ConstraintViolationException e) {
			// this is good
			// there should be exactly one product in the database
			Assert.assertEquals(accessor.count("COUNT RetailerProduct").longValue(), 1);
			Assert.assertNull(poorProduct.getId());
		}
	}
	
	@Test
	public void testOneToOneSchemaExtension() {
		// test unique constraints on a one-to-one field
		Deployer deployer = engine.getDeployer();
		AccessorImpl accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
		.type("Clearance")
			.attribute("name", StringAttribute.class)
			.typeAttribute("product", "RetailerProduct") // this attribute can't be mandatory
		
		.type("RetailerProduct")
			.typeAttribute("clearance", "Clearance")
				.cascadeDelete()
				.mandatory()
				.toOneAttribute("product")
			.attribute("name", StringAttribute.class)
			.unique("clearance")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext recordContext = accessor.buildRecordContext();
		
		Record clearance = recordContext.create("Clearance");
		accessor.insert(clearance);
		
		// the clearance has already been persisted. now a retailer might offer a product based on that clearance.
		Record product = recordContext.create("RetailerProduct");
		product.setAttributeValue("clearance", clearance);
		// we don't set the product on the clearance, because this should now be supported by the schema implementation
		Transaction transaction = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(product, transaction);
		transaction.commit();
		
		Record freshClearance = accessor.readById("Clearance", clearance.getId(), accessor.buildRecordContext());
		Assert.assertNotNull(freshClearance.getAttributeValue("product", Record.class));
	}
}
