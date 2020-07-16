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

import org.bndly.common.crypto.impl.Base64ServiceImpl;
import org.bndly.common.data.io.IOUtils;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.PathBuilder;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.Property.Type;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.impl.repository.PackageImporterImpl;
import org.bndly.schema.impl.repository.PropertyImpl;
import org.bndly.schema.impl.repository.RepositoryImporterImpl;
import org.bndly.schema.impl.repository.RepositorySessionImpl;
import org.bndly.schema.impl.repository.Value;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class KeyValueStoreTest extends AbstractSchemaTest {

	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
	
	@Test
	public void test() throws RepositoryException {
		Accessor accessor = deployNodeSchema();

		RecordContext ctx = accessor.buildRecordContext();
		Record fooNode = ctx.create("Node");
		try {
			accessor.insert(fooNode);
			Assert.fail("expected an exception, because name is not set");
		} catch (ConstraintViolationException e) {
			// that's ok
		}
		fooNode.setAttributeValue("name", "foo");
		try {
			accessor.insert(fooNode);
			Assert.fail("expected an exception, because parentIndex is not set");
		} catch (ConstraintViolationException e) {
			// that's ok
		}
		fooNode.setAttributeValue("nodeType", NodeTypes.UNSTRUCTURED);
		try {
			accessor.insert(fooNode);
			Assert.fail("expected an exception, because parentIndex is not set");
		} catch (ConstraintViolationException e) {
			// that's ok
		}
		fooNode.setAttributeValue("parentIndex", 1L);
		long fooId = accessor.insert(fooNode);
		Record barNode = ctx.create("Node");
		barNode.setAttributeValue("parent", fooNode);
		barNode.setAttributeValue("name", "bar");
		barNode.setAttributeValue("nodeType", NodeTypes.UNSTRUCTURED);
		barNode.setAttributeValue("parentIndex", 0L);
		long barId = accessor.insert(barNode);
		Record bingoNode = ctx.create("Node");
		bingoNode.setAttributeValue("name", "bingo");
		bingoNode.setAttributeValue("parentIndex", 0L);
		bingoNode.setAttributeValue("nodeType", NodeTypes.UNSTRUCTURED);
		long bingoId = accessor.insert(bingoNode);
		
		RepositorySession repo = createRepositorySession();
		Node root = repo.getRoot();
		Assert.assertNotNull(root);
		Node foo = root.getChild("foo");
		Assert.assertNotNull(foo);
		Assert.assertEquals(foo.getName(), "foo");
		Assert.assertEquals(foo.getPath().toString(), "/foo");
		Node bingo = root.getChild("bingo");
		Assert.assertNotNull(bingo);
		Assert.assertEquals(bingo.getName(), "bingo");
		Assert.assertEquals(bingo.getPath().toString(), "/bingo");
		Iterator<Node> children = root.getChildren();
		List<Node> tmp = new ArrayList<>();
		while (children.hasNext()) {
			tmp.add(children.next());
		}
		Assert.assertEquals(tmp.size(), 2);
		Assert.assertEquals(tmp.get(0).getName(), "bingo");
		Assert.assertEquals(tmp.get(1).getName(), "foo");
		Node bar = foo.getChild("bar");
		Assert.assertNotNull(bar);
		Assert.assertEquals(bar.getName(), "bar");
		Assert.assertEquals(bar.getPath().toString(), "/foo/bar");
		
		Node baz = repo.getRoot().createChild("baz", NodeTypes.UNSTRUCTURED);
		try {
			repo.getRoot().createChild("baz", NodeTypes.UNSTRUCTURED);
			Assert.fail("expected exception, because the node already exists");
		} catch (RepositoryException e) {
			// that's ok
		}
		repo.flush();

		repo = createRepositorySession();
		root = repo.getRoot();
		children = root.getChildren();
		tmp.clear();
		while (children.hasNext()) {
			tmp.add(children.next());
		}
		Assert.assertEquals(tmp.size(), 3);
		Assert.assertEquals(tmp.get(0).getName(), "bingo");
		Assert.assertEquals(tmp.get(1).getName(), "foo");
		Assert.assertEquals(tmp.get(2).getName(), "baz");
	}
	
	@Test
	public void testPropertyCreation() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node foo = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		// drop the millis
		Date dateValue = new Date((new Date().getTime() / 1000) * 1000);
		foo.createProperty("stringProp", Type.STRING).setValue("hello world");
		foo.createProperty("dateProp", Type.DATE).setValue(dateValue);
		repo.flush();
		
		repo = createRepositorySession();
		foo = repo.getNode(PathBuilder.newInstance("/foo").build());
		Iterator<Property> propsIter = foo.getProperties();
		Map<String, Property> props = new LinkedHashMap<>();
		while (propsIter.hasNext()) {
			Property next = propsIter.next();
			props.put(next.getName(), next);
		}
		Assert.assertEquals(props.size(), 2);
		Assert.assertNotNull(props.get("stringProp"));
		Assert.assertEquals(props.get("stringProp").getType(), Property.Type.STRING);
		Assert.assertEquals(props.get("stringProp").getString(), "hello world");
		Assert.assertNotNull(props.get("dateProp"));
		Assert.assertEquals(props.get("dateProp").getType(), Property.Type.DATE);
		Assert.assertEquals(props.get("dateProp").getDate(), dateValue);
		
	}
	
	@Test
	public void testNodeRemoval() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		repo.flush();
		repo = createRepositorySession();
		repo.getRoot().getChild("foo").remove();
		repo.flush();
		repo = createRepositorySession();
		try {
			repo.getRoot().getChild("foo");
			Assert.fail("expected node to not exist");
		} catch (NodeNotFoundException e) {
			// that's ok
		}
	}
	
	@Test
	public void testPropertyRemoval() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		fooNode.createProperty("propA", Type.STRING).setValue("value A");
		fooNode.createProperty("propB", Type.STRING).setValue("value B");
		repo.flush();
		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		Property propertyA = fooReloaded.getProperty("propA");
		Assert.assertEquals(propertyA.getString(), "value A");
		Property propertyB = fooReloaded.getProperty("propB");
		Assert.assertEquals(propertyB.getString(), "value B");
		propertyA.remove();
		propertyB.remove();
		repo.flush();
		repo = createRepositorySession();
		fooReloaded = repo.getRoot().getChild("foo");
		try {
			fooReloaded.getProperty("propA");
			Assert.fail("expected node to not exist");
		} catch (PropertyNotFoundException e) {
			// that's ok
		}
	}
	
	@Test
	public void testPropertyTypes() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		Record entity = engine.getAccessor().buildRecordContext().create("Unrelated");
		engine.getAccessor().insert(entity);
		
		RepositorySession repo = createRepositorySession();
		final byte[] testBytes = "Hello World".getBytes("UTF-8");
		Date date = new Date((new Date().getTime() / 1000) * 1000);
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		fooNode.createProperty("propString", Type.STRING).setValue("string");
		fooNode.createProperty("propBoolean", Type.BOOLEAN).setValue(Boolean.TRUE);
		fooNode.createProperty("propDate", Type.DATE).setValue(date);
		fooNode.createProperty("propDecimal", Type.DECIMAL).setValue(new BigDecimal("3.99"));
		fooNode.createProperty("propDouble", Type.DOUBLE).setValue(3.99D);
		fooNode.createProperty("propLong", Type.LONG).setValue(3L);
		fooNode.createProperty("propBinary", Type.BINARY).setValue(new ByteArrayInputStream(testBytes));
		fooNode.createProperty("propEntity", Type.ENTITY).setValue(entity);
		repo.flush();
		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		Assert.assertEquals(fooReloaded.getProperty("propString").getString(), "string");
		Assert.assertEquals(fooReloaded.getProperty("propBoolean").getBoolean(), Boolean.TRUE);
		Assert.assertEquals(fooReloaded.getProperty("propDate").getDate(), date);
		Assert.assertEquals(fooReloaded.getProperty("propDecimal").getDecimal(), new BigDecimal("3.99000"));
		Assert.assertEquals(fooReloaded.getProperty("propDouble").getDouble(), 3.99D);
		Assert.assertEquals(fooReloaded.getProperty("propLong").getLong().longValue(), 3L);
		InputStream is = fooReloaded.getProperty("propBinary").getBinary();
		Assert.assertEquals(IOUtils.read(is), testBytes);
		Assert.assertEquals(fooReloaded.getProperty("propEntity").getEntity().getId(), entity.getId());
	}
	
	@Test
	public void testMultiValueProperties() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		final byte[] testBytes = "Hello World".getBytes("UTF-8");
		final byte[] testBytes2 = "Hello World, again".getBytes("UTF-8");
		Date date = new Date((new Date().getTime() / 1000) * 1000);
		Date date2 = new Date((date.getTime() - (1000 * 60)));
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		fooNode.createMultiProperty("propString", Type.STRING).setValues("string", "string2");
		fooNode.createMultiProperty("propBoolean", Type.BOOLEAN).setValues(Boolean.TRUE, Boolean.FALSE);
		fooNode.createMultiProperty("propDate", Type.DATE).setValues(date, date2);
		fooNode.createMultiProperty("propDecimal", Type.DECIMAL).setValues(new BigDecimal("3.99"), new BigDecimal("4.99"));
		fooNode.createMultiProperty("propDouble", Type.DOUBLE).setValues(3.99D, 4.99D);
		fooNode.createMultiProperty("propLong", Type.LONG).setValues(3L, 4L);
		fooNode.createMultiProperty("propBinary", Type.BINARY).setValues(new ByteArrayInputStream(testBytes), new ByteArrayInputStream(testBytes2));
		repo.flush();
		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		Assert.assertEquals(fooReloaded.getProperty("propString").getString(), "string");
		Assert.assertEquals(fooReloaded.getProperty("propString").getStrings()[0], "string");
		Assert.assertEquals(fooReloaded.getProperty("propString").getStrings()[1], "string2");
		Assert.assertEquals(fooReloaded.getProperty("propBoolean").getBoolean(), Boolean.TRUE);
		Assert.assertEquals(fooReloaded.getProperty("propBoolean").getBooleans()[0], Boolean.TRUE);
		Assert.assertEquals(fooReloaded.getProperty("propBoolean").getBooleans()[1], Boolean.FALSE);
		Assert.assertEquals(fooReloaded.getProperty("propDate").getDate(), date);
		Assert.assertEquals(fooReloaded.getProperty("propDate").getDates()[0], date);
		Assert.assertEquals(fooReloaded.getProperty("propDate").getDates()[1], date2);
		Assert.assertEquals(fooReloaded.getProperty("propDecimal").getDecimal(), new BigDecimal("3.99000"));
		Assert.assertEquals(fooReloaded.getProperty("propDecimal").getDecimals()[0], new BigDecimal("3.99000"));
		Assert.assertEquals(fooReloaded.getProperty("propDecimal").getDecimals()[1], new BigDecimal("4.99000"));
		Assert.assertEquals(fooReloaded.getProperty("propDouble").getDouble(), 3.99D);
		Assert.assertEquals(fooReloaded.getProperty("propDouble").getDoubles()[0], 3.99D);
		Assert.assertEquals(fooReloaded.getProperty("propDouble").getDoubles()[1], 4.99D);
		Assert.assertEquals(fooReloaded.getProperty("propLong").getLong().longValue(), 3L);
		Assert.assertEquals(fooReloaded.getProperty("propLong").getLongs()[0].longValue(), 3L);
		Assert.assertEquals(fooReloaded.getProperty("propLong").getLongs()[1].longValue(), 4L);
		InputStream is = fooReloaded.getProperty("propBinary").getBinary();
		Assert.assertEquals(IOUtils.read(is), testBytes);
		InputStream is2 = fooReloaded.getProperty("propBinary").getBinaries()[1];
		Assert.assertEquals(IOUtils.read(is2), testBytes2);
	}
	
	@Test
	public void testAddValue() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		Property prop = fooNode.createMultiProperty("prop", Type.STRING);
		prop.addValue("val1");
		prop.addValue("val2");
		repo.flush();

		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		prop = fooReloaded.getProperty("prop");
		String[] strings = prop.getStrings();
		Assert.assertNotNull(strings);
		Assert.assertEquals(strings.length, 2);
		Assert.assertEquals(strings[0], "val1");
		Assert.assertEquals(strings[1], "val2");
	}
	
	@Test
	public void testAddValueThenSetValues() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		Property prop = fooNode.createMultiProperty("prop", Type.STRING);
		prop.addValue("val1");
		prop.addValue("val2");
		prop.setValues("v1", "v2", "v3");
		String[] strings = prop.getStrings();
		Assert.assertNotNull(strings);
		Assert.assertEquals(strings.length, 3);
		Assert.assertEquals(strings[0], "v1");
		Assert.assertEquals(strings[1], "v2");
		Assert.assertEquals(strings[2], "v3");
		repo.flush();

		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		prop = fooReloaded.getProperty("prop");
		strings = prop.getStrings();
		Assert.assertNotNull(strings);
		Assert.assertEquals(strings.length, 3);
		Assert.assertEquals(strings[0], "v1");
		Assert.assertEquals(strings[1], "v2");
		Assert.assertEquals(strings[2], "v3");
	}
	
	@Test
	public void testReplaceProperty() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		Property prop = fooNode.createMultiProperty("prop", Type.STRING);
		prop.setValues("v1", "v2", "v3");
		repo.flush();

		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		prop = fooReloaded.getProperty("prop");
		prop.remove();
		prop = fooReloaded.createProperty("prop", Type.LONG);
		prop.setValue(1985L);
		
		prop = fooReloaded.getProperty("prop");
		Assert.assertNotNull(prop);
		Assert.assertEquals(prop.getType(), Type.LONG);
		repo.flush();

		repo = createRepositorySession();
		fooReloaded = repo.getRoot().getChild("foo");
		prop = fooReloaded.getProperty("prop");
		Assert.assertNotNull(prop);
		Assert.assertEquals(prop.getType(), Type.LONG);
		Assert.assertEquals(prop.getLong(), Long.valueOf(1985));
	}
	
	@Test
	public void testLongString() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RepositorySession repo = createRepositorySession();
		Node fooNode = repo.getRoot().createChild("foo", NodeTypes.UNSTRUCTURED);
		Property prop = fooNode.createMultiProperty("prop", Type.STRING);
		StringBuffer longString = new StringBuffer();
		for (int i = 0; i < 1024; i++) {
			longString.append("a");
		}
		prop.setValue(longString.toString());
		repo.flush();

		repo = createRepositorySession();
		Node fooReloaded = repo.getRoot().getChild("foo");
		prop = fooReloaded.getProperty("prop");
		Assert.assertNotNull(prop);
		Assert.assertEquals(prop.getType(), Type.STRING);
		Assert.assertEquals(prop.getString(), longString.toString());
	}
	
	@Test
	public void testImport() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		RecordContext ctx = accessor.buildRecordContext();
		Record unrelatedRecord = ctx.create("Unrelated");
		accessor.insert(unrelatedRecord);
		
		long start = System.currentTimeMillis();
		RepositorySessionImpl repo = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		RepositoryImporterImpl repositoryImporterImpl = new RepositoryImporterImpl();
		repositoryImporterImpl.setBase64Service(base64Service);
		repositoryImporterImpl.importRepositoryData(Paths.get("src", "test", "resources", "repository", "root.json"), repo);
		repo.flush();
		long duration = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		repo = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		Node node = repo.getNode(PathBuilder.newInstance("/").build());
		Assert.assertNotNull(node);
		Property property = node.getProperty("prop");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getType(), Property.Type.STRING);
		Assert.assertEquals(property.getString(), "this is a property on root");
		
		node = repo.getNode(PathBuilder.newInstance("/nested").build());
		Assert.assertNotNull(node);
		property = node.getProperty("prop");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getType(), Property.Type.STRING);
		Assert.assertEquals(property.getString(), "this is nested");
		
		node = repo.getNode(PathBuilder.newInstance("/nestedMulti").build());
		Assert.assertNotNull(node);

		node = repo.getNode(PathBuilder.newInstance("/nestedMulti/0").build());
		Assert.assertNotNull(node);
		property = node.getProperty("prop");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getString(), "string value");
		property = node.getProperty("propMulti");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getString(), "a");
		Assert.assertEquals(property.getStrings()[0], "a");
		Assert.assertEquals(property.getStrings()[1], "b");
		property = node.getProperty("propDecimal");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getDecimal(), new BigDecimal("3.99000"));
		property = node.getProperty("propLong");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getLong(), new Long("3"));
		property = node.getProperty("propDouble");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getDouble(), new Double("3.999999"));
		property = node.getProperty("propDate");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getDate(), new Date(1442651040000L));
		property = node.getProperty("propBoolean");
		Assert.assertNotNull(property);
		Assert.assertEquals(property.getBoolean(), Boolean.TRUE);
		property = node.getProperty("propBinary");
		Assert.assertNotNull(property);
		InputStream is = property.getBinary();
		Assert.assertNotNull(is);
		byte[] bytes = IOUtils.read(is);
		Assert.assertEquals(new String(bytes, "UTF-8"), "This is binary");
		property = node.getProperty("propEntityRef");
		Assert.assertNotNull(property);
		Record entity = property.getEntity();
		Assert.assertNotNull(entity);
		Assert.assertEquals(entity.getType().getName(), "Unrelated");
		Assert.assertEquals(entity.getId(), new Long(1));
		
		node = repo.getNode(PathBuilder.newInstance("/nestedMulti/1").build());
		Assert.assertNotNull(node);
		duration = System.currentTimeMillis() - start;
		duration = duration;
	}
	
	@Test
	public void testNodeOrder() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySessionImpl session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		Node root = session.getRoot();
		root.createChild("child-a", NodeTypes.UNSTRUCTURED);
		root.createChild("child-b", NodeTypes.UNSTRUCTURED);
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		Iterator<Node> children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-a");
		Assert.assertEquals(children.next().getName(), "child-b");
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(0);
		children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-b");
		Assert.assertEquals(children.next().getName(), "child-a");
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(1);
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(0);
		children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-b");
		Assert.assertEquals(children.next().getName(), "child-a");
		session.flush();
		children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-b");
		Assert.assertEquals(children.next().getName(), "child-a");
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(0);
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(1);
		session.getNode(PathBuilder.newInstance("/child-b").build()).moveToIndex(0);
		children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-b");
		Assert.assertEquals(children.next().getName(), "child-a");
		session.flush();
		children = root.getChildren();
		Assert.assertEquals(children.next().getName(), "child-b");
		Assert.assertEquals(children.next().getName(), "child-a");
	}

	@Test
	public void testPropertyOrder() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySessionImpl session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		Node root = session.getRoot();
		PropertyImpl prop = (PropertyImpl) root.createMultiProperty("prop", Type.STRING);
		PropertyImpl prop2 = (PropertyImpl) root.createMultiProperty("prop2", Type.STRING);
		Assert.assertEquals(prop.getRecord().getAttributeValue("parentIndex", Long.class).longValue(), 0L);
		Assert.assertEquals(prop2.getRecord().getAttributeValue("parentIndex", Long.class).longValue(), 1L);
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		Iterator<Property> properties = root.getProperties();
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop");
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop2");
		PropertyImpl prop3 = (PropertyImpl) root.createMultiProperty("prop3", Type.STRING);
		Assert.assertEquals(prop3.getRecord().getAttributeValue("parentIndex", Long.class).longValue(), 2L);
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		PropertyImpl prop4 = (PropertyImpl) root.createMultiProperty("prop4", Type.STRING);
		Assert.assertEquals(prop4.getRecord().getAttributeValue("parentIndex", Long.class).longValue(), 3L);
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		Assert.assertNotNull(root.getProperty("prop3"));
		properties = root.getProperties();
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop");
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop2");
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop3");
		Assert.assertTrue(properties.hasNext());
		Assert.assertEquals(properties.next().getName(), "prop4");
		Assert.assertEquals(prop4.getRecord().getAttributeValue("parentIndex", Long.class).longValue(), 3L);
	}

	@Test
	public void testValueOrder() throws RepositoryException {
		Accessor accessor = deployNodeSchema();
		RepositorySessionImpl session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		Node root = session.getRoot();
		PropertyImpl prop = (PropertyImpl) root.createMultiProperty("prop", Type.STRING);
		prop.setValues("A", "B", "C", "D", "E");
		// move B to D
		Value valB = prop.getValuesInternal().get(1);
		Assert.assertEquals(valB.getValue(), "B");
		Assert.assertEquals(valB.getIndex(), 1);
		valB.moveToIndex(3);
		String[] vals = prop.getStrings();
		Assert.assertEquals(vals[0], "A");
		Assert.assertEquals(vals[1], "C");
		Assert.assertEquals(vals[2], "D");
		Assert.assertEquals(vals[3], "B");
		Assert.assertEquals(vals[4], "E");
		session.flush();
		
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		prop = (PropertyImpl) root.getProperty("prop");
		String[] valsAfterFlush = prop.getStrings();
		Assert.assertEquals(valsAfterFlush[0], "A");
		Assert.assertEquals(valsAfterFlush[1], "C");
		Assert.assertEquals(valsAfterFlush[2], "D");
		Assert.assertEquals(valsAfterFlush[3], "B");
		Assert.assertEquals(valsAfterFlush[4], "E");
		
		valB = prop.getValuesInternal().get(3);
		valB.moveToIndex(1);
		String[] valsBeforeFlush = prop.getStrings();
		Assert.assertEquals(valsBeforeFlush[0], "A");
		Assert.assertEquals(valsBeforeFlush[1], "B");
		Assert.assertEquals(valsBeforeFlush[2], "C");
		Assert.assertEquals(valsBeforeFlush[3], "D");
		Assert.assertEquals(valsBeforeFlush[4], "E");
		// the moved value is not persisted, but we should get the string in ABCDE
		session.flush();
		
		// the moved value is persisted. hence we should get the strings in ABCDE
		session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		root = session.getRoot();
		prop = (PropertyImpl) root.getProperty("prop");
		valsAfterFlush = prop.getStrings();
		Assert.assertEquals(valsAfterFlush[0], "A");
		Assert.assertEquals(valsAfterFlush[1], "B");
		Assert.assertEquals(valsAfterFlush[2], "C");
		Assert.assertEquals(valsAfterFlush[3], "D");
		Assert.assertEquals(valsAfterFlush[4], "E");
	}
	
	@Test
	public void testPackageImporter() throws RepositoryException, IOException {
		Accessor accessor = deployNodeSchema();
		accessor.insert(accessor.buildRecordContext().create("Unrelated"));
		RepositorySessionImpl session = new RepositorySessionImpl(false, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		try (InputStream is = Files.newInputStream(Paths.get("src", "test", "resources", "repository.zip"), StandardOpenOption.READ)) {
			PackageImporterImpl packageImporterImpl = new PackageImporterImpl();
			RepositoryImporterImpl repositoryImporterImpl = new RepositoryImporterImpl();
			repositoryImporterImpl.setBase64Service(new Base64ServiceImpl());
			packageImporterImpl.setRepositoryImporter(repositoryImporterImpl);
			packageImporterImpl.setRepositoryExporter(repositoryImporterImpl);
			packageImporterImpl.importRepositoryData(is, session);
			session.flush();
			
			repositoryImporterImpl.exportRepositoryData(Paths.get("target", "repo-export-test"), session);
			Path exportZip = Paths.get("target","repo-export-test.zip");
			Files.deleteIfExists(exportZip);
			Files.createFile(exportZip);
			try (OutputStream os = Files.newOutputStream(exportZip, StandardOpenOption.WRITE)) {
				packageImporterImpl.exportRepositoryData(os, session);
				os.flush();
			}
		}
		session = new RepositorySessionImpl(true, engine, accessor.buildRecordContext(), Collections.EMPTY_LIST, LOCK);
		Node rootNode = session.getRoot();
		Assert.assertEquals(rootNode.getType(), NodeTypes.ROOT);
		Assert.assertEquals(rootNode.getProperty("prop").getString(), "this is a property on root");
		Assert.assertEquals(rootNode.getProperty("prop").getType(), Property.Type.STRING);
		Assert.assertEquals(rootNode.getProperty("prop").isMultiValued(), false);
		Node nestedNode = rootNode.getChild("nested");
		Assert.assertNotNull(nestedNode);
		Assert.assertEquals(nestedNode.getIndex(), 0);
		Assert.assertEquals(nestedNode.getType(), NodeTypes.UNSTRUCTURED);
		Assert.assertEquals(nestedNode.getProperty("prop").getString(), "this is nested");
		Assert.assertEquals(nestedNode.getProperty("prop").getType(), Property.Type.STRING);
		Assert.assertEquals(nestedNode.getProperty("prop").isMultiValued(), false);
		Node nestedMulti = rootNode.getChild("nestedMulti");
		Assert.assertNotNull(nestedMulti);
		Assert.assertEquals(nestedMulti.getIndex(), 1);
		Assert.assertEquals(nestedMulti.getType(), NodeTypes.ARRAY);
		Assert.assertEquals(nestedMulti.getProperties().hasNext(), false);
		Iterator<Node> children = nestedMulti.getChildren();
		Node a = children.next();
		Node b = children.next();
		final int child0Index;
		final Node child0;
		final int child1Index;
		final Node child1;
		if (a.getName().equals("0")) {
			child0Index = 0;
			child0 = a;
			child1Index = 1;
			child1 = b;
		} else {
			child0Index = 1;
			child0 = b;
			child1Index = 0;
			child1 = a;
		}
		Assert.assertEquals(child0.getType(), NodeTypes.UNSTRUCTURED);
		Assert.assertEquals(child0.getIndex(), child0Index);
		Assert.assertEquals(child0.getProperty("prop").getString(), "string value");
		Assert.assertEquals(child0.getProperty("prop").getType(), Property.Type.STRING);
		Assert.assertEquals(child0.getProperty("prop").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propMulti").getStrings()[0], "a");
		Assert.assertEquals(child0.getProperty("propMulti").getStrings()[1], "b");
		Assert.assertEquals(child0.getProperty("propMulti").getType(), Property.Type.STRING);
		Assert.assertEquals(child0.getProperty("propMulti").isMultiValued(), true);
		Assert.assertEquals(child0.getProperty("propDecimal").getDecimal(), new BigDecimal("3.99000"));
		Assert.assertEquals(child0.getProperty("propDecimal").getType(), Property.Type.DECIMAL);
		Assert.assertEquals(child0.getProperty("propDecimal").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propLong").getLong(), Long.valueOf(3));
		Assert.assertEquals(child0.getProperty("propLong").getType(), Property.Type.LONG);
		Assert.assertEquals(child0.getProperty("propLong").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propDouble").getDouble(), Double.valueOf(3.999999));
		Assert.assertEquals(child0.getProperty("propDouble").getType(), Property.Type.DOUBLE);
		Assert.assertEquals(child0.getProperty("propDouble").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propDate").getDate(), new Date(1442651040000L));
		Assert.assertEquals(child0.getProperty("propDate").getType(), Property.Type.DATE);
		Assert.assertEquals(child0.getProperty("propDate").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propBoolean").getBoolean(), Boolean.TRUE);
		Assert.assertEquals(child0.getProperty("propBoolean").getType(), Property.Type.BOOLEAN);
		Assert.assertEquals(child0.getProperty("propBoolean").isMultiValued(), false);
		Assert.assertNotNull(child0.getProperty("propBinary").getBinary());
		Assert.assertEquals(child0.getProperty("propBinary").getType(), Property.Type.BINARY);
		Assert.assertEquals(child0.getProperty("propBinary").isMultiValued(), false);
		Assert.assertEquals(child0.getProperty("propEntityRef").getEntity().getType().getName(), "Unrelated");
		Assert.assertEquals(child0.getProperty("propEntityRef").getType(), Property.Type.ENTITY);
		Assert.assertEquals(child0.getProperty("propEntityRef").isMultiValued(), false);
		Assert.assertEquals(child1.getType(), NodeTypes.UNSTRUCTURED);
		Assert.assertEquals(child1.getIndex(), child1Index);
	}
	
	private Accessor deployNodeSchema() {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.virtualMixin("Named")
				.attribute("name", StringAttribute.class)
					.mandatory()
					.nonVirtual()
				;
		sb.virtualMixin("IndexedItem")
				.attribute("parentIndex", DecimalAttribute.class)
					.attributeValue("decimalPlaces", 0)
					.mandatory()
					.nonVirtual()
				;
		sb.mixin("NodeValue")
				;
		sb.type("Unrelated")
				.mixWith("NodeValue")
				;
		sb.type("Node")
				.mixWith("Named")
				.mixWith("IndexedItem")
				.typeAttribute("parent", "Node")
				.attribute("nodeType", StringAttribute.class)
					.mandatory()
				.unique("parent", "name")
				;
		sb.type("Property")
				.mixWith("Named")
				.mixWith("IndexedItem")
				.typeAttribute("node", "Node")
					.cascadeDelete()
				.attribute("isMultiValued", BooleanAttribute.class)
					.mandatory()
				.attribute("type", StringAttribute.class)
					.mandatory()
				.unique("node", "name")
				;
		sb.type("Value")
				.mixWith("IndexedItem")
				.typeAttribute("property", "Property")
					.mandatory()
					.cascadeDelete()
				.attribute("stringValue", StringAttribute.class)
				.attribute("textValue", StringAttribute.class)
					.attributeValue("isLong", true)
				.attribute("isText", BooleanAttribute.class)
				.attribute("dateValue", DateAttribute.class)
				.attribute("decimalValue", DecimalAttribute.class)
					.attributeValue("decimalPlaces", 5)
					.attributeValue("length", 55)
				.attribute("longValue", DecimalAttribute.class)
					.attributeValue("decimalPlaces", 0)
				.attribute("doubleValue", DecimalAttribute.class)
					.attributeValue("decimalPlaces", 5)
				.attribute("booleanValue", BooleanAttribute.class)
				.attribute("binaryValue", BinaryAttribute.class)
				.mixinAttribute("entityValue", "NodeValue")
				;
		Schema schema = sb.getSchema();
//		try {
//			Path get = Paths.get("repository-schema.xml");
//			Files.deleteIfExists(get);
//			Files.createFile(get);
//			try (OutputStream os = Files.newOutputStream(get, StandardOpenOption.WRITE)) {
//				new SchemaDefinitionIOImpl().serialize(schema, os);
//				os.flush();
//			} catch (SerializingException ex) {
//			}
//		} catch(IOException e) {
//			
//		}
		deployer.deploy(schema);
		return accessor;
	}

	private RepositorySession createRepositorySession() {
		final Accessor accessor = engine.getAccessor();
		final RecordContext ctx = accessor.buildRecordContext();
		return new RepositorySessionImpl(false, engine, ctx, Collections.EMPTY_LIST, LOCK);
	}
	
}
