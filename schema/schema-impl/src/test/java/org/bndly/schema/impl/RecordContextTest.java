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
public class RecordContextTest extends AbstractSchemaTest {
	@Test
	public void testListContextRecords() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("Foo")
				.attribute("bar", StringAttribute.class)
			.type("Bingo")
				.attribute("bar", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record foo = ctx.create("Foo");
		foo.setAttributeValue("bar", "AAAA");
		Record bingo = ctx.create("Bingo");
		bingo.setAttributeValue("bar", "BBBB");
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(foo, tx);
		engine.getAccessor().buildInsertQuery(bingo, tx);
		tx.commit();
		
		
		Record foo2 = null;
		Record bingo2 = null;
		Iterator<Record> persistedRecordsIter = ctx.listPersistedRecords();
		while (persistedRecordsIter.hasNext()) {
			Record persisted = persistedRecordsIter.next();
			if("Foo".equals(persisted.getType().getName())) {
				foo2 = persisted;
			} else if("Bingo".equals(persisted.getType().getName())) {
				bingo2 = persisted;
			}
		}
		Assert.assertNotNull(foo2);
		Assert.assertNotNull(bingo2);
	}
}
