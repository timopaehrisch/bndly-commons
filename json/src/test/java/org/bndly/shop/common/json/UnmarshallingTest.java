package org.bndly.shop.common.json;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.unmarshalling.Unmarshaller;
import org.bndly.common.reflection.InstantiationUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UnmarshallingTest {

	public interface FakeDomainModelType {

		String getText();

		void setText(String text);
	}

	public static class Cart {

		private String identifier;
		private List<CartItem> items;

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public List<CartItem> getItems() {
			return items;
		}

		public void setItems(List<CartItem> items) {
			this.items = items;
		}
	}

	public static class CartItem {

		private String productName;
		private BigDecimal price;
		private Long wishListId;

		public String getProductName() {
			return productName;
		}

		public void setProductName(String productName) {
			this.productName = productName;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public Long getWishListId() {
			return wishListId;
		}

		public void setWishListId(Long wishListId) {
			this.wishListId = wishListId;
		}
	}

	public static class CartWithMaps {

		private Map<String, CartItem> mapAsArray;
		private Map<String, CartItem> mapAsObject;

		public Map<String, CartItem> getMapAsArray() {
			return mapAsArray;
		}

		public void setMapAsArray(Map<String, CartItem> mapAsArray) {
			this.mapAsArray = mapAsArray;
		}

		public Map<String, CartItem> getMapAsObject() {
			return mapAsObject;
		}

		public void setMapAsObject(Map<String, CartItem> mapAsObject) {
			this.mapAsObject = mapAsObject;
		}

	}

	private String[] stringArrayField;
	
	@Test
	public void testArrayDeserialization() throws NoSuchFieldException {
		Field field = getClass().getDeclaredField("stringArrayField");
		Type type = field.getGenericType();
		JSArray jsarray = new JSArray();
		jsarray.add(new JSString("a"));
		jsarray.add(new JSString("b"));
		jsarray.add(JSNull.INSTANCE);
		jsarray.add(new JSBoolean(true));
		Object out = new ConversionContextBuilder().initDefaults().build().deserialize(type, jsarray);
		Assert.assertNotNull(out);
		String[] sout = (String[]) out;
		Assert.assertEquals(sout.length, 4);
		Assert.assertEquals(sout[0], "a");
		Assert.assertEquals(sout[1], "b");
		Assert.assertEquals(sout[2], null);
		Assert.assertEquals(sout[3], null);
	}
	
	@Test
	public void testInterfaceInstantiation() {
		FakeDomainModelType fakeType = InstantiationUtil.instantiateDomainModelInterface(FakeDomainModelType.class);
		Assert.assertNotNull(fakeType);
		fakeType.setText("hallo welt");
		Assert.assertEquals(fakeType.getText(), "hallo welt");

		System.out.println(fakeType.toString());

		Assert.assertTrue(fakeType.equals(fakeType), "proxy did not equal itself.");
		FakeDomainModelType emptyFakeType = InstantiationUtil.instantiateDomainModelInterface(FakeDomainModelType.class);
		Assert.assertTrue(!fakeType.equals(emptyFakeType), "filled proxy did equal an empty proxy.");
		FakeDomainModelType anotherEmptyFakeType = InstantiationUtil.instantiateDomainModelInterface(FakeDomainModelType.class);
		Assert.assertTrue(emptyFakeType.equals(anotherEmptyFakeType), "empty proxy did not equal another empty proxy.");
		FakeDomainModelType anotherFilledFakeType = InstantiationUtil.instantiateDomainModelInterface(FakeDomainModelType.class);
		anotherFilledFakeType.setText("hello world");
		Assert.assertTrue(!fakeType.equals(anotherFilledFakeType), "filled proxy did equal a filled proxy with different value.");

		Assert.assertNotEquals(fakeType.hashCode(), anotherFilledFakeType.hashCode());

		// making things equal
		anotherFilledFakeType.setText(fakeType.getText());
		Assert.assertTrue(fakeType.equals(anotherFilledFakeType), "filled proxy did not equal a filled proxy with same value.");

		Assert.assertEquals(fakeType.hashCode(), anotherFilledFakeType.hashCode());
	}

	@Test
	public void testSimpleUnmarshalling() {
		InputStream is;
		try {
			is = new FileInputStream(new File("src/test/resources/testData.json"));
			JSObject jsonDoc = (JSObject) new JSONParser().parse(is, "UTF-8");
			FakeDomainModelType docAsJava = new Unmarshaller().unmarshall(jsonDoc, FakeDomainModelType.class);
			Assert.assertNotNull(docAsJava);
			Assert.assertNotNull(docAsJava.getText());
			Assert.assertEquals(docAsJava.getText(), "hallo welt");
		} catch (FileNotFoundException ex) {
			Assert.fail("could not find required test resource", ex);
		}
	}

	@Test
	public void testComplexUnmarshalling() {
		InputStream is;
		try {
			is = new FileInputStream(new File("src/test/resources/cartData.json"));
			JSObject jsonDoc = (JSObject) new JSONParser().parse(is, "UTF-8");
			Cart cart = new Unmarshaller().unmarshall(jsonDoc, Cart.class);
			Assert.assertNotNull(cart);
			Assert.assertNotNull(cart.getIdentifier());
			Assert.assertNotNull(cart.getItems());
			Assert.assertEquals(cart.getItems().size(), 2);
		} catch (FileNotFoundException ex) {
			Assert.fail("could not find required test resource", ex);
		}
	}

	@Test
	public void testMapUnmarshalling() {
		InputStream is;
		try {
			is = new FileInputStream(new File("src/test/resources/mapData.json"));
			JSObject jsonDoc = (JSObject) new JSONParser().parse(is, "UTF-8");
			CartWithMaps cart = new Unmarshaller().unmarshall(jsonDoc, CartWithMaps.class);
			Assert.assertNotNull(cart);

			Assert.assertNotNull(cart.getMapAsArray());
			Assert.assertEquals(cart.getMapAsArray().size(), 1);

			Assert.assertNotNull(cart.getMapAsObject());
			Assert.assertEquals(cart.getMapAsObject().size(), 2);
		} catch (FileNotFoundException ex) {
			Assert.fail("could not find required test resource", ex);
		}
	}
}
