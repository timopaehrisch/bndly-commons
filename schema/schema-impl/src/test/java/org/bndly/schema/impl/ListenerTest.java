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
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.listener.PreDeleteListener;
import org.bndly.schema.api.listener.PreMergeListener;
import org.bndly.schema.api.listener.PrePersistListener;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ListenerTest extends AbstractSchemaTest {

	private static abstract class AbstractInvokedListener {
		private boolean invoked = false;

		protected void markAsInvoked() {
			this.invoked = true;
		}

		protected void reset() {
			this.invoked = false;
		}

		public boolean isInvoked() {
			return invoked;
		}
	}

	private static class InvokedPrePersistListener extends AbstractInvokedListener implements PrePersistListener {
		@Override
		public void onBeforePersist(Record record, Transaction transaction) {
			markAsInvoked();
		}
	}

	private static class InvokedPersistListener extends AbstractInvokedListener implements PersistListener {
		@Override
		public void onPersist(Record record) {
			markAsInvoked();
		}
	}

	private static class InvokedPreMergeListener extends AbstractInvokedListener implements PreMergeListener {
		@Override
		public void onBeforeMerge(Record record, Transaction transaction) {
			markAsInvoked();
		}
	}

	private static class InvokedMergeListener extends AbstractInvokedListener implements MergeListener {
		@Override
		public void onMerge(Record record) {
			markAsInvoked();
		}
	}

	private static class InvokedPreDeleteListener extends AbstractInvokedListener implements PreDeleteListener {
		@Override
		public void onBeforeDelete(Record record, Transaction transaction) {
			markAsInvoked();
		}
	}

	private static class InvokedDeleteListener extends AbstractInvokedListener implements DeleteListener {
		@Override
		public void onDelete(Record record) {
			markAsInvoked();
		}
	}

	private static class InvokedSchemaDeploymentListener extends AbstractInvokedListener implements SchemaDeploymentListener {

		@Override
		public void schemaDeployed(Schema deployedSchema, Engine engine) {
			markAsInvoked();
		}

		@Override
		public void schemaUndeployed(Schema deployedSchema, Engine engine) {
			markAsInvoked();
		}

	}

	private interface SimpleType {
		String getName();
		void setName(String name);
	}

	private static final Schema schema = new SchemaBuilder("test", "http://test.bndly.org")
			.type("SimpleType")
				.attribute("name", StringAttribute.class)
			.type("OtherType")
				.attribute("name", StringAttribute.class)
			.getSchema();
	
	@Test
	public void testSingleFlatType() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();

		InvokedPersistListener global = new InvokedPersistListener();
		InvokedPersistListener typeBound = new InvokedPersistListener();
		InvokedSchemaDeploymentListener nonTypeListener = new InvokedSchemaDeploymentListener();
		InvokedSchemaDeploymentListener nonTypeListener2 = new InvokedSchemaDeploymentListener();
		engine.addListener(global);
		engine.addListener(nonTypeListener);
		engine.addListenerForTypes(nonTypeListener2, "SimpleType"); // wrong api usage test
		engine.addListenerForTypes(typeBound, "SimpleType");

		deployer.deploy(schema);

		Record newRecord = accessor.buildRecordContext().create("SimpleType");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);

		Assert.assertTrue(global.isInvoked(), "global listener should have been invoked");
		Assert.assertTrue(typeBound.isInvoked(), "type bound listener should have been invoked");
		Assert.assertTrue(nonTypeListener.isInvoked(), "non-type bound listener should have been invoked");
		Assert.assertTrue(nonTypeListener2.isInvoked(), "non-type bound listener with wrong api usage should have been invoked");

		global.reset();
		typeBound.reset();
		nonTypeListener.reset();
		nonTypeListener2.reset();

		newRecord = accessor.buildRecordContext().create("OtherType");
		newRecord.setAttributeValue("name", "test123");
		accessor.insert(newRecord);

		Assert.assertTrue(global.isInvoked(), "global listener should have been invoked");
		Assert.assertFalse(typeBound.isInvoked(), "type bound listener should NOT have been invoked");
		Assert.assertFalse(nonTypeListener.isInvoked(), "non-type bound listener should have been invoked");
		Assert.assertFalse(nonTypeListener2.isInvoked(), "non-type bound listener with wrong api usage should have been invoked");
	}

	@Test
	public void persistSchemaBean_withoutTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register persist listeners to be invoked
		final InvokedPrePersistListener prePersistListener = new InvokedPrePersistListener();
		engine.addListener(prePersistListener);
		final InvokedPersistListener persistListener = new InvokedPersistListener();
		engine.addListener(persistListener);


		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);

		// Persist the record using an implicit transaction
		((ActiveRecord)sampleBean).persist();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(prePersistListener.isInvoked(), "prePersist listener has been invoked");
		Assert.assertTrue(persistListener.isInvoked(), "persist listener has been invoked");
	}

	@Test
	public void persistSchemaBean_withTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register persist listeners to be invoked
		final InvokedPrePersistListener prePersistListener = new InvokedPrePersistListener();
		engine.addListener(prePersistListener);
		final InvokedPersistListener persistListener = new InvokedPersistListener();
		engine.addListener(persistListener);

		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);

		// Persist the record using an explicit transaction
		final Transaction tx = this.engine.getQueryRunner().createTransaction();
		((ActiveRecord)sampleBean).persist(tx);
		tx.commit();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(prePersistListener.isInvoked(), "prePersist listener has been invoked");
		Assert.assertTrue(persistListener.isInvoked(), "persist listener has been invoked");
	}

	@Test
	public void updateSchemaBean_withoutTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register persist listeners to be invoked
		final InvokedPreMergeListener preMergeListener = new InvokedPreMergeListener();
		engine.addListener(preMergeListener);
		final InvokedMergeListener mergeListener = new InvokedMergeListener();
		engine.addListener(mergeListener);

		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);
		record.setId(1L);
		sampleBean.setName("someValue");

		// Update the record using an implicit transaction
		((ActiveRecord)sampleBean).update();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(preMergeListener.isInvoked(), "preMerge listener has been invoked");
		Assert.assertTrue(mergeListener.isInvoked(), "merge listener has been invoked");
	}

	@Test
	public void updateSchemaBean_withTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register persist listeners to be invoked
		final InvokedPreMergeListener preMergeListener = new InvokedPreMergeListener();
		engine.addListener(preMergeListener);
		final InvokedMergeListener mergeListener = new InvokedMergeListener();
		engine.addListener(mergeListener);

		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);
		record.setId(1L);
		sampleBean.setName("someValue");

		// Update the record using an implicit transaction
		final Transaction tx = this.engine.getQueryRunner().createTransaction();
		((ActiveRecord)sampleBean).update(tx);
		tx.commit();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(preMergeListener.isInvoked(), "preMerge listener has been invoked");
		Assert.assertTrue(mergeListener.isInvoked(), "merge listener has been invoked");
	}

	@Test
	public void deleteSchemaBean_withoutTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register delete listeners to be invoked
		final InvokedPreDeleteListener preDeleteListener = new InvokedPreDeleteListener();
		engine.addListener(preDeleteListener);
		final InvokedDeleteListener deleteListener = new InvokedDeleteListener();
		engine.addListener(deleteListener);

		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);
		record.setId(1L);

		// Delete the record using an implicit transaction
		((ActiveRecord)sampleBean).delete();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(preDeleteListener.isInvoked(), "preDelete listener has been invoked");
		Assert.assertTrue(deleteListener.isInvoked(), "delete listener has been invoked");
	}

	@Test
	public void deleteSchemaBean_withTransaction_willInvokeListener() {
		// Set up a simple schema
		engine.getDeployer().deploy(schema);
		schemaBeanFactory.registerTypeBinding(SimpleType.class);

		// Register delete listeners to be invoked
		final InvokedPreDeleteListener preDeleteListener = new InvokedPreDeleteListener();
		engine.addListener(preDeleteListener);
		final InvokedDeleteListener deleteListener = new InvokedDeleteListener();
		engine.addListener(deleteListener);

		// Create a record and map it to a schema bean
		final Record record = engine.getAccessor().buildRecordContext().create(SimpleType.class.getSimpleName());
		final SimpleType sampleBean = schemaBeanFactory.getSchemaBean(SimpleType.class, record);
		record.setId(1L);

		// Delete the record using an explicit transaction
		final Transaction tx = this.engine.getQueryRunner().createTransaction();
		((ActiveRecord)sampleBean).delete(tx);
		tx.commit();

		// Make sure the persist listener has been invoked
		Assert.assertTrue(preDeleteListener.isInvoked(), "preDelete listener has not been invoked");
		Assert.assertTrue(deleteListener.isInvoked(), "delete listener has been invoked");
	}
}
