package org.bndly.schema.impl.nquery;

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
import org.bndly.schema.impl.AbstractSchemaTest;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.RecordContextImpl;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NQueryTest extends AbstractSchemaTest {
	
	@Test
	public void testSimplePick() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("TestEntity")
				.attribute("myAttribute", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		// this should run and not throw any exceptions
		Iterator<Record> iter = engine.getAccessor().query("PICK TestEntity");
		Assert.assertNotNull(iter);
		Assert.assertFalse(iter.hasNext());
		
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record testEntityInstance = ctx.create("TestEntity");
		testEntityInstance.setAttributeValue("myAttribute", "hello world");
		long idOfInsertedInstance = engine.getAccessor().insert(testEntityInstance);
		
		iter = engine.getAccessor().query("PICK TestEntity");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		Record loadedInstance = iter.next();
		Assert.assertEquals((long)loadedInstance.getId(), idOfInsertedInstance);
		Assert.assertEquals(loadedInstance.getAttributeValue("myAttribute"), "hello world");
	}
	
	private Iterator<Record> iterate(String typeName, final int bulkSize) {
		final String query = "PICK " + typeName + " OFFSET ? LIMIT ?";
		final Accessor accessor = schemaBeanFactory.getEngine().getAccessor();
		return new Iterator<Record>() {
			
			private boolean reachedEnd = false;
			private Integer offset;
			private Iterator<Record> result;

			@Override
			public boolean hasNext() {
				if (reachedEnd) {
					return false;
				}
				if (offset == null) {
					offset = 0;
					result = accessor.query(query, offset, bulkSize);
					if (!result.hasNext()) {
						reachedEnd = true;
						return false;
					}
				}
				if (result.hasNext()) {
					return true;
				} else {
					offset = offset + bulkSize;
					result = accessor.query(query, offset, bulkSize);
					if (result.hasNext()) {
						return true;
					} else {
						reachedEnd = true;
						return false;
					}
				}
			}

			@Override
			public Record next() {
				if (!hasNext()) {
					throw new NoSuchElementException("no more records available");
				}
				return result.next();
			}

			@Override
			public void remove() {
				// not supported
			}
		};
	}
	
	private List<Record> toList(Iterator<Record> iterator) {
		List<Record> list = new ArrayList<>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}
	
	@Test
	public void testIteration() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		int items = 5;
		Transaction tx = engine.getQueryRunner().createTransaction();
		for (int i = 0; i < items; i++) {
			RecordContext ctx = engine.getAccessor().buildRecordContext();
			Record foo = ctx.create("Foo");
			foo.setAttributeValue("bar", "" + i);
			engine.getAccessor().buildInsertQuery(foo, tx);
		}
		tx.commit();

		List<Record> list = toList(iterate("Foo", items));
		Assert.assertEquals(list.size(), items);
		for (int i = 0; i < items; i++) {
			Assert.assertEquals(list.get(i).getAttributeValue("bar", String.class), "" + i);
		}
		list = toList(iterate("Foo", items - 1));
		Assert.assertEquals(list.size(), items);
		for (int i = 0; i < items; i++) {
			Assert.assertEquals(list.get(i).getAttributeValue("bar", String.class), "" + i);
		}

		list = toList(iterate("Foo", items + 1));
		Assert.assertEquals(list.size(), items);
		for (int i = 0; i < items; i++) {
			Assert.assertEquals(list.get(i).getAttributeValue("bar", String.class), "" + i);
		}

		list = toList(iterate("Foo", items / 2));
		Assert.assertEquals(list.size(), items);
		for (int i = 0; i < items; i++) {
			Assert.assertEquals(list.get(i).getAttributeValue("bar", String.class), "" + i);
		}
	}
	
	@Test
	public void testSimpleIfClause() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF f.bar=?", "AAAA");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		while (iter.hasNext()) {
			Record loadedInstance = iter.next();
			Assert.assertEquals(loadedInstance.getId(), fooA.getId());
			Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
		}
	}
	
	@Test
	public void testSimpleIfClauseWithWrapper() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF (f.bar=?)", "AAAA");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		while (iter.hasNext()) {
			Record loadedInstance = iter.next();
			Assert.assertEquals(loadedInstance.getId(), fooA.getId());
			Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
		}
	}
	
	@Test
	public void testSimpleIfClauseWithOR() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBBB");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCCC");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF (f.bar=? OR f.bar=?)", "AAAA", "BBBB");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		while (iter.hasNext()) {
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
			} else if(loadedInstance.getId().equals(fooB.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooB.getAttributeValue("bar"));
			} else {
				Assert.fail("wrong record in result");
			}
		}
	}
	
	@Test
	public void testSimpleIfClauseWithJoinedType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("FooMix")
				.attribute("bar", StringAttribute.class)
			.type("Foo")
				.mixWith("FooMix")
			.type("Bar")
				.mixWith("FooMix")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record barA = ctx.create("Bar");
		barA.setAttributeValue("bar", "AAAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCCC");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(barA, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK FooMix f IF f.bar=?", "AAAA");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		while (iter.hasNext()) {
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
			} else if(loadedInstance.getId().equals(barA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), barA.getAttributeValue("bar"));
			} else {
				Assert.fail("wrong record in result");
			}
		}
	}
	
	@Test
	public void testSimpleIfClauseWithJoinedTypeAndOR() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("FooMix")
				.attribute("bar", StringAttribute.class)
			.type("Foo")
				.mixWith("FooMix")
			.type("Bar")
				.mixWith("FooMix")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record barA = ctx.create("Bar");
		barA.setAttributeValue("bar", "AAAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCCC");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(barA, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK FooMix f IF f.bar=? OR f.bar=?", "AAAA", "CCCC");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		while (iter.hasNext()) {
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
			} else if(loadedInstance.getId().equals(barA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), barA.getAttributeValue("bar"));
			} else if(loadedInstance.getId().equals(fooC.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar"), fooC.getAttributeValue("bar"));
			} else {
				Assert.fail("wrong record in result");
			}
		}
	}
	
	@Test
	public void testIfClauseMixinAttribute() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("FooMix")
				.attribute("bar", StringAttribute.class)
			.type("Foo")
				.mixWith("FooMix")
			.type("Bar")
				.mixWith("FooMix")
			.type("Baz")
				.mixinAttribute("fooMix", "FooMix")
			;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAAA");
		Record barA = ctx.create("Bar");
		barA.setAttributeValue("bar", "AAAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCCC");
		
		Record bazFooA = ctx.create("Baz");
		bazFooA.setAttributeValue("fooMix", fooA);
		Record bazBarA = ctx.create("Baz");
		bazBarA.setAttributeValue("fooMix", barA);
		Record bazFooC = ctx.create("Baz");
		bazFooC.setAttributeValue("fooMix", fooC);
		
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(barA, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		
		engine.getAccessor().buildInsertQuery(bazFooA, tx);
		engine.getAccessor().buildInsertQuery(bazBarA, tx);
		engine.getAccessor().buildInsertQuery(bazFooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Baz b IF b.fooMix.bar=?", "AAAA");
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		int results = 0;
		while (iter.hasNext()) {
			results++;
			Record loadedInstance = iter.next();
			Record fooMix = loadedInstance.getAttributeValue("fooMix", Record.class);
			if(loadedInstance.getId().equals(bazFooA.getId())) {
				Assert.assertEquals(fooMix.getAttributeValue("bar"), fooA.getAttributeValue("bar"));
			} else if(loadedInstance.getId().equals(bazBarA.getId())) {
				Assert.assertEquals(fooMix.getAttributeValue("bar"), barA.getAttributeValue("bar"));
			} else {
				Assert.fail("wrong record in result");
			}
		}
		
		Assert.assertEquals(results, 2);
	}
	
	@Test
	public void testSimpleIfClauseWithBetweenExpression() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", DateAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		long oneDay = 24*60*60*1000;
		Date d1 = new Date();
		Date d0 = new Date(d1.getTime() - oneDay);
		Date d2 = new Date(d1.getTime() + oneDay);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", d0);
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", d1);
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", d2);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF f.bar INRANGE ?,?", d0, d1);
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		int results = 0;
		while (iter.hasNext()) {
			results++;
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooA.getAttributeValue("bar").toString());
			} else if(loadedInstance.getId().equals(fooB.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooB.getAttributeValue("bar").toString());
			} else {
				Assert.fail("wrong record in result");
			}
		}
		Assert.assertEquals(results, 2);
		
		iter = engine.getAccessor().query("PICK Foo f IF f.bar !INRANGE ?,?", d0, d1);
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		results = 0;
		while (iter.hasNext()) {
			results++;
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooC.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooC.getAttributeValue("bar").toString());
			} else {
				Assert.fail("wrong record in result");
			}
		}
		Assert.assertEquals(results, 1);
	}
	
	@Test
	public void testSimpleIfClauseWithGreaterLowerExpression() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", DateAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		long oneDay = 24*60*60*1000;
		Date d1 = new Date();
		Date d0 = new Date(d1.getTime() - oneDay);
		Date d2 = new Date(d1.getTime() + oneDay);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", d0);
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", d1);
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", d2);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF f.bar > ?", d0);
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		int results = 0;
		while (iter.hasNext()) {
			results++;
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooC.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooC.getAttributeValue("bar").toString());
			} else if(loadedInstance.getId().equals(fooB.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooB.getAttributeValue("bar").toString());
			} else {
				Assert.fail("wrong record in result");
			}
		}
		Assert.assertEquals(results, 2);
	}
	
	@Test
	public void testSimpleIfClauseWithGreaterEqualExpression() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", DateAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		long oneDay = 24*60*60*1000;
		Date d1 = new Date();
		Date d0 = new Date(d1.getTime() - oneDay);
		Date d2 = new Date(d1.getTime() + oneDay);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", d0);
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", d1);
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", d2);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f IF f.bar >= ?", d0);
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		
		int results = 0;
		while (iter.hasNext()) {
			results++;
			Record loadedInstance = iter.next();
			if(loadedInstance.getId().equals(fooC.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooC.getAttributeValue("bar").toString());
			} else if(loadedInstance.getId().equals(fooB.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooB.getAttributeValue("bar").toString());
			} else if(loadedInstance.getId().equals(fooA.getId())) {
				Assert.assertEquals(loadedInstance.getAttributeValue("bar").toString(), fooA.getAttributeValue("bar").toString());
			} else {
				Assert.fail("wrong record in result");
			}
		}
		Assert.assertEquals(results, 3);
	}
	
	@Test
	public void testSimpleOrdering() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCC");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		tx.commit();

		Iterator<Record> iter = engine.getAccessor().query("PICK Foo f ORDERBY f.bar");
		String prev = null;
		while (iter.hasNext()) {
			Record next = iter.next();
			String stringValue = next.getAttributeValue("bar", String.class);
			if(prev == null) {
				prev = stringValue;
			} else {
				Assert.assertTrue(prev.compareTo(stringValue) < 0, prev+" was not smaller than "+stringValue);
				prev = stringValue;
			}
		}
		
		iter = engine.getAccessor().query("PICK Foo f ORDERBY f.bar DESC");
		prev = null;
		while (iter.hasNext()) {
			Record next = iter.next();
			String stringValue = next.getAttributeValue("bar", String.class);
			if(prev == null) {
				prev = stringValue;
			} else {
				Assert.assertTrue(prev.compareTo(stringValue) > 0, prev+" was not bigger than "+stringValue);
				prev = stringValue;
			}
		}
	}
	
	@Test
	public void testSortRecursiveField() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Customer")
				.attribute("name", StringAttribute.class)
				.typeAttribute("partner", "Customer")
				;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record thomas = ctx.create("Customer");
		thomas.setAttributeValue("name", "Thomas");
		
		Record jasmin = ctx.create("Customer");
		jasmin.setAttributeValue("name", "Jasmin");
		
		Record imp0 = ctx.create("Customer");
		imp0.setAttributeValue("name", "Imp0");
		imp0.setAttributeValue("partner", thomas);
		
		Record imp1 = ctx.create("Customer");
		imp1.setAttributeValue("name", "Imp1");
		imp1.setAttributeValue("partner", jasmin);
		
		Transaction tx = engine.getQueryRunner().createTransaction();
		AccessorImpl accessor = engine.getAccessor();
		accessor.buildInsertQuery(thomas, tx);
		accessor.buildInsertQuery(jasmin, tx);
		accessor.buildInsertQuery(imp0, tx);
		accessor.buildInsertQuery(imp1, tx);
		tx.commit();
		
		Iterator<Record> result = engine.getAccessor().query("PICK Customer c IF c.partner != ? ORDERBY c.partner.name", new Object[]{null});
		Record reloadedImp1 = result.next();
		Assert.assertEquals(reloadedImp1.getId(), imp1.getId());
		Record reReadById = accessor.readById("Customer", reloadedImp1.getId(), accessor.buildRecordContext());
		Assert.assertEquals(reReadById.getAttributeValue("name"), imp1.getAttributeValue("name"));
		Assert.assertEquals(reloadedImp1.getAttributeValue("name"), imp1.getAttributeValue("name"));
		Assert.assertNotNull(reloadedImp1.getAttributeValue("partner"));
		
		Record reloadedImp0 = result.next();
		Assert.assertEquals(reloadedImp0.getId(), imp0.getId());
		Assert.assertEquals(reloadedImp0.getAttributeValue("name"), imp0.getAttributeValue("name"));
		Assert.assertNotNull(reloadedImp0.getAttributeValue("partner"));
	}
	
	@Test
	public void testLimitAndOffset() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCC");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> result = engine.getAccessor().query("PICK Foo f ORDERBY f.bar LIMIT ? OFFSET ?", 1, 0);
		Record v = result.next();
		Assert.assertEquals(v.getAttributeValue("bar", String.class), "AAA");
		Assert.assertFalse(result.hasNext());
		
		result = engine.getAccessor().query("PICK Foo f ORDERBY f.bar LIMIT ? OFFSET ?", 1, 1);
		v = result.next();
		Assert.assertEquals(v.getAttributeValue("bar", String.class), "BBB");
		Assert.assertFalse(result.hasNext());
		
		result = engine.getAccessor().query("PICK Foo f ORDERBY f.bar LIMIT ? OFFSET ?", 2, 1);
		v = result.next();
		Assert.assertEquals(v.getAttributeValue("bar", String.class), "BBB");
		Assert.assertTrue(result.hasNext());
		v = result.next();
		Assert.assertEquals(v.getAttributeValue("bar", String.class), "CCC");
		Assert.assertFalse(result.hasNext());
	}
	
	@Test
	public void testIfClauseAndLimitAndOffset() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCC");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Iterator<Record> result = engine.getAccessor().query("PICK Foo f IF f.bar=? LIMIT ? OFFSET ?", "AAA", 1, 0);
		Record v = result.next();
		Assert.assertEquals(v.getAttributeValue("bar", String.class), "AAA");
		Assert.assertFalse(result.hasNext());
	}
	
	@Test
	public void testSimpleCount() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCC");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Long result = engine.getAccessor().count("COUNT Foo f");
		Assert.assertNotNull(result);
		Assert.assertEquals(result, Long.valueOf(3));
	}
	
	@Test
	public void testCountWithIfClause() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("Foo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooC = ctx.create("Foo");
		fooC.setAttributeValue("bar", "CCC");
		Record fooB = ctx.create("Foo");
		fooB.setAttributeValue("bar", "BBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(fooC, tx);
		tx.commit();
		
		Long result = engine.getAccessor().count("COUNT Foo f IF f.bar=? OR f.bar=?", "AAA", "BBB");
		Assert.assertNotNull(result);
		Assert.assertEquals(result, Long.valueOf(2));
	}
	
	@Test
	public void testIfClauseAmbigiousId() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("Foo")
				.attribute("bar", StringAttribute.class)
			.type("AFoo")
				.mixWith("Foo")
			.type("BFoo")
				.mixWith("Foo")
			.type("Baz")
				.mixinAttribute("foo", "Foo")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 0);
		Record fooA = ctx.create("AFoo");
		fooA.setAttributeValue("bar", "AAA");
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 1);
		Record fooB = ctx.create("BFoo");
		fooB.setAttributeValue("bar", "BBB");
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 2);
		Record fooBB = ctx.create("BFoo");
		fooBB.setAttributeValue("bar", "BBBBBB");
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 3);
		Record baz = ctx.create("Baz");
		baz.setAttributeValue("foo", fooBB);
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 4);
		Assert.assertEquals(ctx.persistedEntriesSize(), 0);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooBB, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(baz, tx);
		tx.commit();
		Assert.assertEquals(ctx.persistedEntriesSize(), 4);
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 0);
		
		Assert.assertEquals(fooA.getId(), Long.valueOf(1));
		Assert.assertEquals(fooBB.getId(), Long.valueOf(1));
		Assert.assertEquals(fooB.getId(), Long.valueOf(2));
		Assert.assertEquals(baz.getId(), Long.valueOf(1));
		
		long fooAFooId = engine.getAccessor().readIdAsNamedAttributeHolder(schema.getMixins().get(0), fooA.getType(), fooA.getId(), ctx);
		long fooBFooId = engine.getAccessor().readIdAsNamedAttributeHolder(schema.getMixins().get(0), fooB.getType(), fooB.getId(), ctx);
		long fooBBFooId = engine.getAccessor().readIdAsNamedAttributeHolder(schema.getMixins().get(0), fooBB.getType(), fooBB.getId(), ctx);
		
		Assert.assertEquals(fooAFooId, 1L);
		Assert.assertEquals(fooBBFooId, 2L);
		Assert.assertEquals(fooBFooId, 3L);
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Baz b IF b.foo.id=?", 1);
		Assert.assertTrue(iter.hasNext());
		Record rec = iter.next();
		Record expectedFooBB = rec.getAttributeValue("foo", Record.class);
		
		Assert.assertEquals(expectedFooBB.getId(), fooBB.getId());
		
		iter = engine.getAccessor().query("PICK Baz b IF b.foo=?", 2);
		Assert.assertTrue(iter.hasNext());
		rec = iter.next();
		expectedFooBB = rec.getAttributeValue("foo", Record.class);
		
		Assert.assertEquals(expectedFooBB.getId(), fooBB.getId());
	}
	
	@Test
	public void testSimpleIfClausWithTypedExpression() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("Foo")
				.attribute("bar", StringAttribute.class)
			.type("AFoo")
				.mixWith("Foo")
			.type("BFoo")
				.mixWith("Foo")
			.type("Baz")
				.mixinAttribute("foo", "Foo")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record fooA = ctx.create("AFoo");
		fooA.setAttributeValue("bar", "AAA");
		Record fooB = ctx.create("BFoo");
		fooB.setAttributeValue("bar", "BBB");
		Record fooBB = ctx.create("BFoo");
		fooBB.setAttributeValue("bar", "BBBBBB");
		Record baz = ctx.create("Baz");
		Assert.assertEquals(ctx.size(), 4);
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 4);
		Assert.assertEquals(ctx.persistedEntriesSize(), 0);
		
		Assert.assertFalse(ctx.listReferencesToRecord(fooBB).hasNext(), "there should be no references to fooBB");
		baz.setAttributeValue("foo", fooBB);
		Iterator<RecordContext.RecordReference> refsToFooBB = ctx.listReferencesToRecord(fooBB);
		Assert.assertTrue(refsToFooBB.hasNext(), "there should be references to fooBB");
		RecordContext.RecordReference firstRef = refsToFooBB.next();
		Assert.assertTrue(firstRef.getReferencedBy() == baz, "fooBB was not referenced by baz");
		Assert.assertEquals(firstRef.getReferencedAs().getName(), "foo");
		Assert.assertFalse(refsToFooBB.hasNext(), "there should be no more references to fooBB");
		Assert.assertEquals(ctx.size(), 4);
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 4);
		Assert.assertEquals(ctx.persistedEntriesSize(), 0);
		
		String statsBefore = ((RecordContextImpl)ctx).dumpStats();
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(fooA, tx);
		engine.getAccessor().buildInsertQuery(fooBB, tx);
		engine.getAccessor().buildInsertQuery(fooB, tx);
		engine.getAccessor().buildInsertQuery(baz, tx);
		tx.commit();
		Assert.assertEquals(ctx.size(), 4);
		Assert.assertEquals(ctx.unpersistedEntriesSize(), 0);
		Assert.assertEquals(ctx.persistedEntriesSize(), 4);
		
		String statsAfter = ((RecordContextImpl)ctx).dumpStats();
		
		Record fooAFromCtx = ctx.get(fooA.getType(), fooA.getId());
		Assert.assertTrue(fooAFromCtx == fooA, "record instance had changed after persistence");
		Record fooBFromCtx = ctx.get(fooB.getType(), fooB.getId());
		Assert.assertTrue(fooBFromCtx == fooB, "record instance had changed after persistence");
		Record fooBBFromCtx = ctx.get(fooBB.getType(), fooBB.getId());
		Assert.assertTrue(fooBBFromCtx == fooBB, "record instance had changed after persistence");
		Record bazFromCtx = ctx.get(baz.getType(), baz.getId());
		Assert.assertTrue(bazFromCtx == baz, "record instance had changed after persistence");
		
		Iterator<Record> iter = engine.getAccessor().query("PICK Baz b IF b.foo TYPED ?", "BFoo");
		Assert.assertTrue(iter.hasNext());
		final Record rec = iter.next();
		Record expectedFooBB = rec.getAttributeValue("foo", Record.class);
		
		Assert.assertEquals(expectedFooBB.getId(), fooBB.getId());
		
		iter = engine.getAccessor().query("PICK Baz b IF b.foo TYPED ?", "AFoo");
		Assert.assertFalse(iter.hasNext());
	}
	
	@Test
	public void testRecursiveCountQuery() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("HasAccount")
				.attribute("accountName", StringAttribute.class)
			.type("Customer")
				.mixWith("HasAccount")
				.typeAttribute("partner", "Customer")
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		AccessorImpl accessor = engine.getAccessor();
		RecordContext recordContext = accessor.buildRecordContext();
		Record customerRec = recordContext.create("Customer");
		Record partnerRec = recordContext.create("Customer");
		partnerRec.setAttributeValue("accountName", "maxmustermann");
		customerRec.setAttributeValue("partner", partnerRec);
		Transaction tx = engine.getQueryRunner().createTransaction();
		accessor.buildInsertCascadedQuery(customerRec, tx);
		tx.commit();
		
		Assert.assertEquals(accessor.count("COUNT Customer IF partner.accountName=?", "lieschenmueller").longValue(), 0L);
		Assert.assertEquals(accessor.count("COUNT Customer IF partner.accountName=?", "maxmustermann").longValue(), 1L);
	}
}
