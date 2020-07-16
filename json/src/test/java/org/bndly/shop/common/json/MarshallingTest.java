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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.marshalling.Marshaller;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MarshallingTest {

	public static class MapItem {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
	}
	
	public static class MapHolder {
		private Map<String, MapItem> mapOfItems;

		public Map<String, MapItem> getMapOfItems() {
			return mapOfItems;
		}

		public void setMapOfItems(Map<String, MapItem> mapOfItems) {
			this.mapOfItems = mapOfItems;
		}
		
	}
	
	public static class TestDomainModel {

		private String textValue;
		private Long id;
		private Date date;
		private Boolean enabled;
		private TestDomainModel nested;

		public String getTextValue() {
			return textValue;
		}

		public void setTextValue(String textValue) {
			this.textValue = textValue;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public TestDomainModel getNested() {
			return nested;
		}

		public void setNested(TestDomainModel nested) {
			this.nested = nested;
		}

	}

	public static class TestCollectionModel {

		private Map<String, TestDomainModel> map;
		private List<TestDomainModel> list;
		private TestDomainModel[] array;

		public Map<String, TestDomainModel> getMap() {
			return map;
		}

		public void setMap(Map<String, TestDomainModel> map) {
			this.map = map;
		}

		public List<TestDomainModel> getList() {
			return list;
		}

		public void setList(List<TestDomainModel> list) {
			this.list = list;
		}

		public TestDomainModel[] getArray() {
			return array;
		}

		public void setArray(TestDomainModel[] array) {
			this.array = array;
		}
	}

	@Test
	public void marshallSimpleObject() {
		TestDomainModel testObject = new TestDomainModel();
		TestDomainModel nestedObject = new TestDomainModel();
		testObject.setNested(nestedObject);
		testObject.setDate(new Date());
		testObject.setEnabled(true);
		testObject.setId(1L);
		testObject.setTextValue("hallo welt");
		JSObject jsDoc = (JSObject) new Marshaller().marshall(testObject);
		Assert.assertNotNull(jsDoc);
		Set<JSMember> members = jsDoc.getMembers();
		Assert.assertNotNull(members);
		Assert.assertEquals(members.size(), 5);
	}

	@Test
	public void marshallCollectionObject() {
		TestCollectionModel c = new TestCollectionModel();

		TestDomainModel testObject = new TestDomainModel();
		testObject.setDate(new Date());
		testObject.setEnabled(true);
		testObject.setId(1L);
		testObject.setTextValue("hallo welt");

		Map<String, TestDomainModel> map = new HashMap<String, TestDomainModel>();
		map.put(testObject.toString(), testObject);

		List<TestDomainModel> list = new ArrayList<TestDomainModel>();
		list.add(testObject);

		TestDomainModel[] array = new TestDomainModel[]{testObject};
		c.setMap(map);
		c.setArray(array);
		c.setList(list);

		JSObject jsDoc = (JSObject) new Marshaller().marshall(c);
		Assert.assertNotNull(jsDoc);
		Set<JSMember> members = jsDoc.getMembers();
		Assert.assertNotNull(members);
		Assert.assertEquals(members.size(), 3);

		for (JSMember jSMember : members) {
			String memberName = jSMember.getName().getValue();
			JSValue memberValue = jSMember.getValue();
			if ("map".equals(memberName)) {
				Assert.assertEquals(JSObject.class, memberValue.getClass());

			} else if ("list".equals(memberName)) {
				Assert.assertEquals(JSArray.class, memberValue.getClass());

			} else if ("array".equals(memberName)) {
				Assert.assertEquals(JSArray.class, memberValue.getClass());

			}
		}
		StringWriter sw = new StringWriter();
		try {
			new JSONSerializer().serialize(jsDoc, sw);
		} catch (IOException ex) {
		}
		System.out.println(sw.getBuffer().toString());
	}
	
	@Test
	public void testMarshallingMap() throws IOException {
		MapHolder mapHolder = new MapHolder();
		mapHolder.setMapOfItems(new HashMap<String, MarshallingTest.MapItem>());
		MapItem item = new MapItem();
		item.setValue("def");
		mapHolder.getMapOfItems().put("abc", item);
		
		JSValue marshalled = new Marshaller().marshall(mapHolder);
		StringWriter sw = new StringWriter();
		new JSONSerializer().serialize(marshalled, sw);
		sw.flush();
		String out = sw.toString();
		Assert.assertEquals(out, "{\"mapOfItems\":{\"abc\":{\"value\":\"def\"}}}");
	}
	
	@Test
	public void testSkipNullMarshalling() throws IOException {
		MapHolder mapHolder = new MapHolder();
		mapHolder.setMapOfItems(new HashMap<String, MarshallingTest.MapItem>());
		MapItem item = new MapItem();
		item.setValue("def");
		mapHolder.getMapOfItems().put("abc", item);
		
		mapHolder.getMapOfItems().put("xyz", null);
		
		ConversionContext ctx = new ConversionContextBuilder().initDefaults().skipNullValues().build();
		JSValue marshalled = ctx.serialize(MapHolder.class, mapHolder);
		StringWriter sw = new StringWriter();
		new JSONSerializer().serialize(marshalled, sw);
		sw.flush();
		String out = sw.toString();
		Assert.assertEquals(out, "{\"mapOfItems\":{\"abc\":{\"value\":\"def\"}}}");
	}
}
