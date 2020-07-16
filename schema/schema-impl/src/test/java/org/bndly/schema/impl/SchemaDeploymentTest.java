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

import org.bndly.schema.api.tx.PreparedStatementCreator;
import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.schema.api.LoadedAttributes;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.impl.test.beans.AbstractProduct;
import org.bndly.schema.impl.test.beans.AnotherJsonType;
import org.bndly.schema.impl.test.beans.Bar;
import org.bndly.schema.impl.test.beans.Bingo;
import org.bndly.schema.impl.test.beans.Bundle;
import org.bndly.schema.impl.test.beans.BundleItem;
import org.bndly.schema.impl.test.beans.Cart;
import org.bndly.schema.impl.test.beans.CartItem;
import org.bndly.schema.impl.test.beans.CreateAware;
import org.bndly.schema.impl.test.beans.Foo;
import org.bndly.schema.impl.test.beans.JsonType;
import org.bndly.schema.impl.test.beans.Product;
import org.bndly.schema.impl.test.beans.Purchasable;
import org.bndly.schema.impl.test.beans.SimpleType;
import org.bndly.schema.impl.test.beans.UpdateAware;
import org.bndly.schema.impl.test.beans.Variant;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SchemaDeploymentTest extends AbstractSchemaTest {

	@Test
	public void createSchemaWithMixins() {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.virtualMixin("VirtualMixin")
					.attribute("virtualAttribute", StringAttribute.class)
				.virtualType("VirtualType")
					.mixWith("VirtualMixin")
				.mixin("UniqueTest")
					.attribute("name", StringAttribute.class)
						.unique()
				.type("UniqueA")
					.mixWith("UniqueTest")
				.type("UniqueB")
					.mixWith("UniqueTest")
				.mixin("Purchasable")
					.attribute("sku", StringAttribute.class)
				.mixin("CreateAware")
					.attribute("createdOn", DateAttribute.class)
				.mixin("UpdateAware")
					.attribute("updatedOn", DateAttribute.class)
				.type("Bar")
					.abstractType()
					.mixWith("CreateAware")
					.mixWith("UpdateAware")
					.mixWith("VirtualMixin")
					.attribute("name", StringAttribute.class)
						.attributeValue("length", 255)
				.type("Bingo")
					.parentType("Bar")
					.attribute("iDontCare", StringAttribute.class)
				.type("Foo")
					.parentType("Bar")
					.attribute("numericValue", DecimalAttribute.class)
						.attributeValue("decimalPlaces", 2)
						.attributeValue("length", 10)
					.attribute("date", DateAttribute.class)
					.typeAttribute("bar", "Bar")
					.mixinAttribute("purchasable", "Purchasable");

		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		Record r = accessor.buildRecordContext().create("Foo");
		r.setAttributeValue("name", "Mett");
		r.setAttributeValue("numericValue", new BigDecimal("3.99"));
		r.setAttributeValue("date", new Date());
		r.setAttributeValue("createdOn", new Date());
		r.setAttributeValue("updatedOn", null);
		r.setAttributeValue("bar", null);
		// there is no type that is mixed with purchasable, so this value should be ignored
		r.setAttributeValue("purchasable", 1L);
		accessor.insert(r);

		Record fooRecord = accessor.readById("Foo", r.getId(), accessor.buildRecordContext());
		Assert.assertNotNull(fooRecord);
		Assert.assertNotNull(fooRecord.getType());
		Assert.assertEquals(fooRecord.getAttributeValue("name", String.class), "Mett");
		Assert.assertNotNull(fooRecord.getAttributeValue("date", Date.class));
		Assert.assertNotNull(fooRecord.getAttributeValue("createdOn", Date.class));
		Assert.assertNull(fooRecord.getAttributeValue("updatedOn", Date.class));
		Assert.assertEquals(fooRecord.getAttributeValue("numericValue", BigDecimal.class), new BigDecimal("3.99"));
		Assert.assertNull(fooRecord.getAttributeValue("bar"));
		Assert.assertNotNull(fooRecord.getAttributeValue("purchasable"));
		Assert.assertNull(fooRecord.getAttributeValue("purchasable", Record.class));

		Record mixinRecord = accessor.readById("CreateAware", 1, accessor.buildRecordContext());
		Assert.assertNotNull(mixinRecord);
		Assert.assertNotNull(mixinRecord.getType());
		Assert.assertEquals(mixinRecord.getType().getName(), "Foo");

		// insert an empty object
		Record insert = accessor.buildRecordContext().create("Foo");
		long idOfInserted = accessor.insert(insert);
		Assert.assertEquals(idOfInserted, 2);

		// insert a filled object
		Record filled = accessor.buildRecordContext().create("Foo");
		filled.setAttributeValue("name", "Hackfleisch");
		filled.setAttributeValue("numericValue", new BigDecimal("2.99"));
		filled.setAttributeValue("date", new Date());
		filled.setAttributeValue("createdOn", new Date());
		filled.setAttributeValue("updatedOn", new Date());

        // this should work too
		// filled.setAttributeValue("bar", fooRecord);
		filled.setAttributeValue("bar", 1L);

		long idOfFilled = accessor.insert(filled);
		Assert.assertEquals(idOfFilled, 3);

		Record emptyBingo = accessor.buildRecordContext().create("Bingo");
		emptyBingo.setAttributeValue("name", "i am bingo");
		Long bingoId = accessor.insert(emptyBingo);
		Assert.assertEquals(bingoId, new Long(1));

        // even we read the CreateAware record with id 4, the result is the actual object record,
		// that might have a different id
		Record bingoRecord = accessor.readById("CreateAware", 4, accessor.buildRecordContext());
		Assert.assertEquals(bingoRecord.getId(), bingoId);
		Assert.assertEquals(bingoRecord.getAttributeValue("iDontCare"), emptyBingo.getAttributeValue("iDontCare"));

		Assert.assertEquals(accessor.queryByExample("CreateAware", accessor.buildRecordContext()).count(), 4);
		Assert.assertEquals(accessor.queryByExample("UpdateAware", accessor.buildRecordContext()).count(), 4);
		Assert.assertEquals(accessor.queryByExample("Foo", accessor.buildRecordContext()).count(), 3);
		Assert.assertEquals(accessor.queryByExample("Bar", accessor.buildRecordContext()).count(), 4);
		Assert.assertEquals(accessor.queryByExample("Bingo", accessor.buildRecordContext()).count(), 1);

		// schema bean tests
		schemaBeanFactory.registerTypeBindings(Foo.class, Bar.class, Bingo.class, CreateAware.class, UpdateAware.class, Purchasable.class);

		Bingo bingo = schemaBeanFactory.getSchemaBean(Bingo.class, bingoRecord);
		Assert.assertNotNull(bingo);
		Assert.assertEquals(bingo.getName(), emptyBingo.getAttributeValue("name"));
		Assert.assertNull(bingo.getCreatedOn());
		Assert.assertNull(bingo.getUpdatedOn());
		Assert.assertNull(bingo.getIDontCare());

		fooRecord.setAttributeValue("bar", bingoRecord);
		accessor.update(fooRecord);

		RecordContext context = schemaBeanFactory.getEngine().getAccessor().buildRecordContext();
		Foo foo = schemaBeanFactory.getSchemaBean(Foo.class, context.create(Foo.class.getSimpleName(), fooRecord.getId()));
		Assert.assertNotNull(foo);
		Bar bar = foo.getBar();
		Assert.assertNotNull(bar);
		Assert.assertTrue(Bingo.class.isInstance(bar), "bar in foo was referring to bingo, but the returned object was only conforming to the bar interface.");
		foo.setBar(bar);

		Record ua = accessor.buildRecordContext().create("UniqueA");
		ua.setAttributeValue("name", "Mett");
		accessor.insert(ua);

		Record ub = accessor.buildRecordContext().create("UniqueB");
		ub.setAttributeValue("name", "Mett");
		try {
			accessor.insert(ub);
			Assert.fail("expected a unique constraint violation");
		} catch (ConstraintViolationException e) {
			// this is expected
		}
		
		Bingo bingoInstance = schemaBeanFactory.newInstance(Bingo.class, context);
		Assert.assertNotNull(bingoInstance);
		try {
			schemaBeanFactory.newInstance(ActiveRecord.class, context);
			Assert.fail("expected an exception");
		} catch (IllegalArgumentException e) {
			// this is expected
		}
	}

	@Test
	public void testWithShopExample() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.mixin("Purchasable")
				.attribute("sku", StringAttribute.class)
			.type("BinaryData")
				.attribute("name", StringAttribute.class)
				.attribute("contentType", StringAttribute.class)
				.binaryAttribute("bytes")
			.type("Cart")
				.inverseTypeAttribute("items", "CartItem", "cart")
			.type("CartItem")
				.typeAttribute("cart", "Cart")
				.mixinAttribute("itemToPurchase", "Purchasable")
					.attributeValue("mandatory", true)
					.cascadeDelete()
				.attribute("quantity", DecimalAttribute.class)
					.attributeValue("decimalPlaces", 0)
			.type("Bundle")
				.mixWith("Purchasable")
			.type("BundleItem")
				.typeAttribute("bundle", "Bundle")
			.type("AbstractProduct")
				.abstractType()
			.type("Product")
				.parentType("AbstractProduct")
				.mixWith("Purchasable")
			.type("Variant")
				.parentType("AbstractProduct")
				.mixWith("Purchasable")
				.typeAttribute("product", "Product");

		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		final String demoString = "hallo binary welt";
		final InputStream demoIS = new ByteArrayInputStream(demoString.getBytes("UTF-8"));
		transactionTemplate.doInTransaction(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
				GeneratedKeyHolder kh = new GeneratedKeyHolder();
				template.update(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement statement = connection.prepareStatement("INSERT INTO BINARYDATA (NAME,CONTENTTYPE,BYTES) VALUES(?,?,?)");
						statement.setString(1, "name");
						statement.setString(2, "text/plain");
						statement.setBinaryStream(3, demoIS);
						return statement;
					}
				}, kh);

				final long id = kh.getKey().longValue();
				Blob b = template.queryForObject("SELECT NAME, CONTENTTYPE, BYTES FROM BINARYDATA BD WHERE BD.id=?", new PreparedStatementArgumentSetter[]{new PreparedStatementArgumentSetter() {

					@Override
					public void set(int index, PreparedStatement ps) throws SQLException {
						ps.setLong(index, id);
					}
				}}, new RowMapper<Blob>() {

					@Override
					public Blob mapRow(ResultSet rs, int i) throws SQLException {
						return rs.getBlob(3);
					}
				});

				Assert.assertNotNull(b);
				try {
					InputStream bis = b.getBinaryStream();
					InputStreamReader isr = new InputStreamReader(bis, "UTF-8");
					StringWriter sw = new StringWriter();
					int i;
					while((i = isr.read()) > -1) {
						sw.write(i);
					}
					sw.flush();
					String data = sw.toString();
					Assert.assertEquals(data, demoString);
				}catch(Exception e){
					throw new IllegalStateException(e);
				}
				return null;
			}
		});
		
		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		qc.insert().into("BINARYDATA").value("NAME", new ObjectReference("test"), Types.VARCHAR);
		Query query = qc.build(engine.getAccessor().buildRecordContext());
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<Long> idRef = tx.getQueryRunner().number(query, "ID");
		tx.commit();
		Long createdId = idRef.get();
		
		qc = engine.getQueryContextFactory().buildQueryContext();
		demoIS.reset();
		qc.update().table("BINARYDATA").set("BYTES", new ObjectReference(demoIS), Types.BLOB).where().expression().criteria().field("NAME").equal().value(new ValueProvider() {

			@Override
			public Object get() {
				return "test";
			}
		});
		Query q = qc.build(engine.getAccessor().buildRecordContext());
		tx = engine.getQueryRunner().createTransaction();
		tx.getQueryRunner().uploadBlob(q);
		tx.commit();
		
		Record reloaded = engine.getAccessor().readById("BinaryData", createdId, engine.getAccessor().buildRecordContext());
		Assert.assertNotNull(reloaded);
		Object bytes = reloaded.getAttributeValue("bytes");

		Record cart = accessor.buildRecordContext().create("Cart");
		accessor.insert(cart);

		Record cart2 = accessor.buildRecordContext().create("Cart");
		accessor.insert(cart2);

		Record product = accessor.buildRecordContext().create("Product");
		product.setAttributeValue("sku", "4711");
		accessor.insert(product);

		Record abstractProduct = accessor.buildRecordContext().create("Product");
		accessor.insert(abstractProduct);

		Record variant = accessor.buildRecordContext().create("Variant");
		variant.setAttributeValue("sku", "4712");
		variant.setAttributeValue("product", abstractProduct);
		accessor.insert(variant);

		Record bundle = accessor.buildRecordContext().create("Bundle");
		bundle.setAttributeValue("sku", "4713");
		accessor.insert(bundle);

		Record bundle2 = accessor.buildRecordContext().create("Bundle");
		bundle2.setAttributeValue("sku", "4714");
		accessor.insert(bundle2);

		Record productCartItem = createCartItem(accessor, cart, product);
		Record variantCartItem = createCartItem(accessor, cart, variant);
		Record bundleCartItem = createCartItem(accessor, cart, bundle);
		List<Record> allCartItems = accessor.queryByExample("CartItem", accessor.buildRecordContext()).pagination(new Pagination() {
			@Override
			public Long getOffset() {
				return 0L;
			}

			@Override
			public Long getSize() {
				return 3L;
			}
		}).all();
		Assert.assertNotNull(allCartItems);
		Assert.assertEquals(allCartItems.size(), 3);

		Record cartWithItems = accessor.readById("Cart", cart.getId(), accessor.buildRecordContext());

		List<Record> listOfCartItems = cartWithItems.getAttributeValue("items", List.class);
		Assert.assertNotNull(listOfCartItems);
		Assert.assertEquals(listOfCartItems.size(), allCartItems.size());

		List<Record> listOfCart2Items = cart2.getAttributeValue("items", List.class);
		if (listOfCart2Items != null) {
			Assert.assertEquals(listOfCart2Items.size(), 0);
			Assert.assertTrue(listOfCart2Items.isEmpty());
		}

		List<Record> allPurchasableObjects = accessor.queryByExample("Purchasable", accessor.buildRecordContext()).pagination(new Pagination() {
			@Override
			public Long getOffset() {
				return null;
			}

			@Override
			public Long getSize() {
				return null;
			}
		}).all();
		Assert.assertNotNull(allPurchasableObjects);
		Assert.assertEquals(allPurchasableObjects.size(), 5);

		Record shouldBeBundle = accessor.queryByExample("Purchasable", accessor.buildRecordContext()).attribute("sku", "4713").single();
		Assert.assertNotNull(shouldBeBundle);
		Assert.assertTrue(shouldBeBundle.getType() == bundle.getType());
		Assert.assertEquals(shouldBeBundle.getId(), bundle.getId());

		Record bundle2CartItem = createCartItem(accessor, cart, bundle2);

		bundle2.setAttributeValue("sku", "4715");
		accessor.update(bundle2);

		Record shouldBeBundleCartItem = accessor.queryByExample("CartItem", accessor.buildRecordContext()).eager().attribute("itemToPurchase", bundle).single();
		Assert.assertNotNull(shouldBeBundleCartItem);
		Assert.assertEquals(shouldBeBundleCartItem.getType(), bundleCartItem.getType());
		Assert.assertEquals(shouldBeBundleCartItem.getId(), bundleCartItem.getId());

		Record shouldBeBundle2CartItem = accessor.queryByExample("CartItem", accessor.buildRecordContext()).eager().attribute("itemToPurchase", bundle2).single();
		Assert.assertNotNull(shouldBeBundle2CartItem);
		Assert.assertEquals(shouldBeBundle2CartItem.getType(), bundle2CartItem.getType());
		Assert.assertEquals(shouldBeBundle2CartItem.getId(), bundle2CartItem.getId());

		Record shouldBeBundle2 = accessor.queryByExample("Purchasable", accessor.buildRecordContext()).attribute("sku", "4715").single();
		Assert.assertNotNull(shouldBeBundle2);
		Assert.assertTrue(shouldBeBundle2.getType() == bundle2.getType());
		Assert.assertEquals(shouldBeBundle2.getId(), bundle2.getId());

		tx = engine.getQueryRunner().createTransaction();
		accessor.delete(shouldBeBundle2,tx);
		tx.commit();

		Record emptyData = accessor.buildRecordContext().create("BinaryData");
		String testValue = "hello binary world.";
		ByteArrayInputStream bis = new ByteArrayInputStream(testValue.getBytes());
		emptyData.setAttributeValue("bytes", bis);
		accessor.insert(emptyData);

		Record dataRead = accessor.readById(emptyData.getType().getName(), emptyData.getId(), accessor.buildRecordContext());
		InputStream is = dataRead.getAttributeValue("bytes", InputStream.class);
		InputStreamReader isr = new InputStreamReader(is);
		StringBuilder stringBuilder = new StringBuilder();
		int intChar;
		try {
			while ((intChar = isr.read()) > -1) {
				stringBuilder.append((char) intChar);
			}
		} catch (IOException ex) {
		} finally {
			try {
				isr.close();
			} catch (IOException ex) {
			}
		}
		Assert.assertEquals(stringBuilder.toString(), testValue);

		schemaBeanFactory.registerTypeBindings(Purchasable.class, Cart.class, CartItem.class, Bundle.class, BundleItem.class, AbstractProduct.class, Product.class, Variant.class);
		Cart cartBean = schemaBeanFactory.getSchemaBean(Cart.class, cartWithItems);
		List<CartItem> items = cartBean.getItems();
		Assert.assertNotNull(items);
		Assert.assertEquals(items.size(), listOfCartItems.size());
		for (CartItem cartItem : items) {
			// just iterate
		}

		List<Record> purchasableItems = accessor.queryByExample("Purchasable", accessor.buildRecordContext()).orderBy("sku").asc().all();
		List<Purchasable> piASC = schemaBeanFactory.getSchemaBeans(purchasableItems, Purchasable.class);
		purchasableItems = accessor.queryByExample("Purchasable", accessor.buildRecordContext()).orderBy("sku").desc().all();
		List<Purchasable> piDESC = schemaBeanFactory.getSchemaBeans(purchasableItems, Purchasable.class);
		for (int jndex = 0; jndex < piDESC.size(); jndex++) {
			Purchasable purchasable = piDESC.get(jndex);
			int indexInASC = piDESC.size() - 1 - jndex;
			Purchasable purchasableASC = piASC.get(indexInASC);
			Assert.assertEquals(purchasable.getSku(), purchasableASC.getSku());
			Assert.assertEquals(((ActiveRecord) purchasable).getId(), ((ActiveRecord) purchasableASC).getId());
		}
		List<Record> productsBySku = accessor.queryByExample("Product", accessor.buildRecordContext()).orderBy("sku").all();
//        productsBySku = productsBySku;
	}

    @Test
	public void testReadFromJoinTableWithNestedJoinTables() {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.mixin("Foo")
				.attribute("fooAttribute", StringAttribute.class)
				.type("AbstractBar")
				.abstractType()
				.mixWith("Foo")
				.mixinAttribute("fooReferenced", "Foo")
				.type("Bar")
				.parentType("AbstractBar")
				.attribute("barrr", StringAttribute.class)
				.type("Baz")
				.parentType("AbstractBar")
				.attribute("bazzz", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Select select = qc.select();
		JoinTable jt = engine.getTableRegistry().getJoinTableByNamedAttributeHolder("AbstractBar");
		LoadingIterator el = new LoadingIterator(engine.getTableRegistry(), select, new LoadedAttributes() {

			@Override
			public LoadedAttributes.Strategy isLoaded(Attribute attribute, String attributePath) {
				return Strategy.LOADED;
			}


		}, Collections.EMPTY_SET);
		el.iterate(jt);
		Query query = qc.build(accessor.buildRecordContext());
		List<MappingBinding> mappingBindingList = el.getMappingBindingList();
		Assert.assertEquals(mappingBindingList.size(), 2);

		Type bazType;
		{
			Record barRec = accessor.buildRecordContext().create("Bar");
			barRec.setAttributeValue("barrr", "i am bar");
			barRec.setAttributeValue("fooAttribute", "foo bar");
			accessor.insert(barRec);
		}
		{
			Record bazRec = accessor.buildRecordContext().create("Baz");
			bazRec.setAttributeValue("bazzz", "i am baz");
			bazRec.setAttributeValue("fooAttribute", "foo baz");
			accessor.insert(bazRec);
			bazType = bazRec.getType();
		}

		Record shouldBeBaz = accessor.readById("Foo", 2L, accessor.buildRecordContext());
		Assert.assertNotNull(shouldBeBaz);
		Assert.assertTrue(shouldBeBaz.getType() == bazType);
		Assert.assertEquals(shouldBeBaz.getId(), new Long(1));
		Assert.assertEquals(shouldBeBaz.getAttributeValue("bazzz", String.class), "i am baz");
		Assert.assertEquals(shouldBeBaz.getAttributeValue("fooAttribute", String.class), "foo baz");

	}

    @Test
	public void createSchemaWithJSONAttribute() throws IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.virtualType("AnotherJsonType")
				.virtualType("JsonType")
				.attribute("stringValue", StringAttribute.class)
				.attribute("decimalValue", DecimalAttribute.class)
				.attribute("booleanValue", BooleanAttribute.class)
				.attribute("dateValue", DateAttribute.class)
				.typeAttribute("complexValue", "AnotherJsonType")
				.type("SimpleType")
				.jsonTypeAttribute("json", "JsonType");
		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		JSObject jsObject = new JSObject();
		createStringMember(jsObject, "_type", "JsonType");
		createStringMember(jsObject, "stringValue", "this is a test string.");
		createNumberMember(jsObject, "decimalValue", new BigDecimal("3.99"));
		createBooleanMember(jsObject, "booleanValue", true);
		createDateMember(jsObject, "dateValue", new Date());

		JSObject nestedJsObject = new JSObject();
		createStringMember(nestedJsObject, "_type", "AnotherJsonType");
		createJSObjectMember(jsObject, "complexValue", nestedJsObject);

		schemaBeanFactory.registerTypeBinding(SimpleType.class);
		schemaBeanFactory.registerTypeBinding(JsonType.class);
		schemaBeanFactory.registerTypeBinding(AnotherJsonType.class);
		jsonSchemaBeanFactory.registerTypeBinding(SimpleType.class);
		jsonSchemaBeanFactory.registerTypeBinding(JsonType.class);
		jsonSchemaBeanFactory.registerTypeBinding(AnotherJsonType.class);

		for (int index = 0; index < 10; index++) {
			RecordContext ctx = schemaBeanFactory.getEngine().getAccessor().buildRecordContext();
			SimpleType st = schemaBeanFactory.getSchemaBean(SimpleType.class, ctx.create(SimpleType.class.getSimpleName()));
			st.setJson(jsonSchemaBeanFactory.getSchemaBean(JsonType.class));
			String expectedValue = "remember me";
			st.getJson().setStringValue(expectedValue);
			((ActiveRecord) st).persistCascaded();
			((ActiveRecord) st).reload();
			JsonType json = st.getJson();
			Assert.assertNotNull(json);
			Assert.assertNotNull(json.getStringValue());
			Assert.assertEquals(json.getStringValue(), expectedValue);
		}
	}
	
	@Test
	public void testTruncationOfTooLongString() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.type("SomeType")
					.attribute("note", StringAttribute.class)
				;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record rec = ctx.create("SomeType");
		String shortValue = "short enough";
		rec.setAttributeValue("note", shortValue);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(rec, tx);
		tx.commit();
		Record recReloaded = engine.getAccessor().readById("SomeType", rec.getId(), engine.getAccessor().buildRecordContext());
		Assert.assertFalse(recReloaded == rec);
		String longValue250 = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
		String longValue255 = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234";
		String longValue256 = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
		String longValue257 = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456";
		Record rec250 = persistRecordWithLongString(longValue250, ctx);
		Assert.assertNotNull(rec250);
		Assert.assertEquals(rec250.getAttributeValue("note",String.class), longValue250);
		Record rec255 = persistRecordWithLongString(longValue255, ctx);
		Assert.assertEquals(rec255.getAttributeValue("note",String.class), longValue255);
		Record rec256 = persistRecordWithLongString(longValue256, ctx);
		Assert.assertEquals(rec256.getAttributeValue("note",String.class), longValue255);
		Record rec257 = persistRecordWithLongString(longValue257, ctx);
		Assert.assertEquals(rec257.getAttributeValue("note",String.class), longValue255);
	}
	
	private Record persistRecordWithLongString(String longValue250, RecordContext ctx) {
		Record rec = ctx.create("SomeType");
		rec.setAttributeValue("note", longValue250);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(rec, tx);
		tx.commit();
		Record recReloaded = engine.getAccessor().readById("SomeType", rec.getId(), engine.getAccessor().buildRecordContext());
		return recReloaded;
	}
	
	@Test
	public void testTransactionalPersistence() {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
				.type("BinaryData")
				.type("Image")
					.typeAttribute("data", "BinaryData")
				.type("Capture")
					.typeAttribute("original", "Image")
					.typeAttribute("crop", "Image")
					.typeAttribute("film", "Film")
				.type("Film");
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record film = ctx.create("Film");
		Record capture = ctx.create("Capture");
		Record image = ctx.create("Image");
		Record binary = ctx.create("BinaryData");
		image.setAttributeValue("data", binary);
		capture.setAttributeValue("original", image);
		capture.setAttributeValue("film", film);
		
		// only inserts
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(binary, tx);
		engine.getAccessor().buildInsertQuery(image, tx);
		engine.getAccessor().buildInsertQuery(film, tx);
		engine.getAccessor().buildInsertQuery(capture, tx);
		tx.commit();

		Assert.assertNotNull(capture.getId());

		Record reloadedCapture = engine.getAccessor().readById("Capture", capture.getId(), engine.getAccessor().buildRecordContext());
		Assert.assertNotNull(reloadedCapture);
		Record reloadedImage = reloadedCapture.getAttributeValue("original", Record.class);
		Record reloadedBinary = reloadedImage.getAttributeValue("data", Record.class);
		Record reloadedFilm = reloadedCapture.getAttributeValue("film", Record.class);
		Assert.assertEquals(capture.getId(), reloadedCapture.getId());
		Assert.assertEquals(image.getId(), reloadedImage.getId());
		Assert.assertEquals(binary.getId(), reloadedBinary.getId());
		Assert.assertEquals(film.getId(), reloadedFilm.getId());
		
		
		// test an update and insert in the same transaction
		Record cropImage = ctx.create("Image");
		capture.setAttributeValue("crop", cropImage);
		// create a new transaction
		tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(cropImage, tx);
		engine.getAccessor().buildUpdateQuery(capture, tx);
		tx.commit();
		
		reloadedCapture = engine.getAccessor().readById("Capture", capture.getId(), engine.getAccessor().buildRecordContext());
		reloadedImage = reloadedCapture.getAttributeValue("original", Record.class);
		Record reloadedCropImage = reloadedCapture.getAttributeValue("crop", Record.class);
		Assert.assertEquals(capture.getId(), reloadedCapture.getId());
		Assert.assertEquals(image.getId(), reloadedImage.getId());
		Assert.assertEquals(cropImage.getId(), reloadedCropImage.getId());
	}

	@Test
	public void testWithBadIdentifiers() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("; DROP USER;")
				.attribute("%#cryptic ", StringAttribute.class);

		Schema schema = sb.getSchema();
		try {
			deployer.deploy(schema);
			Assert.fail("deployment should have failed, because the type name is bad");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals(e.getMessage(), "found unallowed character for identifier name: ;");
		}
	}

	@Test
	public void testUpdateWithNullBlob() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("TestEntity")
				.binaryAttribute("data");
		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record entity = ctx.create("TestEntity");
		entity.setAttributeValue("data", null); // this way the attribtue is present and explicitly null
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertCascadedQuery(entity, tx);
		tx.commit();
		// this should run and not throw any exceptions
	}
	
	@Test
	public void testCascadeDelete() throws UnsupportedEncodingException, IOException {
		Deployer deployer = engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
			.type("CartItem")
				.typeAttribute("cart", "Cart")
			.type("Cart")
				.inverseTypeAttribute("items", "CartItem", "cart")
					.deleteOrphans()
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);

		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record cart = ctx.create("Cart");
		Record item = ctx.create("CartItem");
		Record item2 = ctx.create("CartItem");
		item.setAttributeValue("cart", cart);
		item2.setAttributeValue("cart", cart);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().buildInsertQuery(cart, tx);
		engine.getAccessor().buildInsertQuery(item, tx);
		engine.getAccessor().buildInsertQuery(item2, tx);
		tx.commit();
		
		Assert.assertNotNull(cart.getId());
		Assert.assertNotNull(item.getId());
		Assert.assertNotNull(item2.getId());
		
		Record cartReloaded = engine.getAccessor().readById("Cart", cart.getId(), engine.getAccessor().buildRecordContext());
		Assert.assertNotNull(cartReloaded);
		List<Record> items = (List<Record>) cartReloaded.getAttributeValue("items");
		Assert.assertNotNull(items);
		Assert.assertEquals(items.size(), 2);
		
		Transaction deleteTx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().delete(item2, deleteTx);
		deleteTx.commit();
		
		Record cartReloadedAgain = engine.getAccessor().readById("Cart", cart.getId(), engine.getAccessor().buildRecordContext());
		Assert.assertNotNull(cartReloadedAgain);
		List<Record> itemsReloadedAgain = (List<Record>) cartReloadedAgain.getAttributeValue("items");
		Assert.assertNotNull(itemsReloadedAgain);
		Assert.assertEquals(itemsReloadedAgain.size(), 1);
		
		Transaction deleteCartTx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().delete(cart, deleteCartTx);
		deleteCartTx.commit();
		
		Record cartItemShouldNotExist = engine.getAccessor().readById("CartItem", item.getId(), engine.getAccessor().buildRecordContext());
		Assert.assertNull(cartItemShouldNotExist);
	}
	
	private Record createCartItem(Accessor accessor, Record cart, Record purchasable) {
		Record cartItem = accessor.buildRecordContext().create("CartItem");
		cartItem.setAttributeValue("cart", cart);
		cartItem.setAttributeValue("quantity", 88L);
		cartItem.setAttributeValue("itemToPurchase", purchasable);
		accessor.insert(cartItem);
		return cartItem;
	}

	private void createDateMember(JSObject jsObject, String memberName, Date dateValue) {
		JSNumber jsValue = new JSNumber();
		jsValue.setValue(new BigDecimal(dateValue.getTime()));
		setJSMember(jsObject, memberName, jsValue);
	}

	private void createBooleanMember(JSObject jsObject, String memberName, boolean booleanValue) {
		JSBoolean jsValue = new JSBoolean();
		jsValue.setValue(booleanValue);
		setJSMember(jsObject, memberName, jsValue);
	}

	private void createNumberMember(JSObject jsObject, String memberName, BigDecimal numberValue) {
		JSNumber jsValue = new JSNumber();
		jsValue.setValue(numberValue);
		setJSMember(jsObject, memberName,jsValue);
	}

	private void createStringMember(JSObject jsObject, String memberName, String stringValue) {
		setJSMember(jsObject, memberName, new JSString(stringValue));
	}

	private void setJSMember(JSObject jsObject, String memberName, JSValue jsValue) {
		jsObject.createMember(memberName).setValue(jsValue);
	}

	private void createJSObjectMember(JSObject jsObject, String memberName, JSObject nestedJsObject) {
		setJSMember(jsObject, memberName, nestedJsObject);
	}
}
