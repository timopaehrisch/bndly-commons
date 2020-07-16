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

import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSString;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParsingTest {

	@Test
	public void parseSimple() {
		String input = "{\"attribute\":\"wert\",\"array\":[\"string im array\",3,0.3,-3,-0.3,1E+3,null,true,false,{}],\"numeric\":1E+3,\"nestedObject\":{}}";
		JSONParser parser = new JSONParser();
		StringReader reader = new StringReader(input);
		JSObject object = (JSObject) parser.parse(reader);
		Assert.assertNotNull(object);

		Set<JSMember> members = object.getMembers();
		Assert.assertEquals(members.size(), 4);

		JSMember attributeJSMember = null;
		JSMember arrayJSMember = null;
		JSMember numericJSMember = null;
		for (JSMember jSMember : members) {
			String memberName = jSMember.getName().getValue();
			if ("attribute".equals(memberName)) {
				attributeJSMember = jSMember;
			} else if ("numeric".equals(memberName)) {
				numericJSMember = jSMember;
			} else if ("array".equals(memberName)) {
				arrayJSMember = jSMember;
			}
		}

		Assert.assertNotNull(attributeJSMember);
		Assert.assertNotNull(arrayJSMember);
		Assert.assertNotNull(numericJSMember);
		BigDecimal numericDecimal = ((JSNumber) numericJSMember.getValue()).getValue();
		Assert.assertEquals(numericDecimal.longValue(), 1000);

		List<JSValue> arrayItems = ((JSArray) arrayJSMember.getValue()).getItems();
		Assert.assertEquals(arrayItems.size(), 10);
	}

	@Test
	public void parseWithWhiteSpaces() {
		String input = "  {  \"attribute\"  :\t\"wert\" \n ,\"array\":[\"string im array\"\r,\f  3,  0.3,-3,-0.3, 1E+3   ,  null   ,true, false   ,{  }],\"numeric\"\n\r :1E+3,\"nestedObject\"\t: {}}";
		JSONParser parser = new JSONParser();
		StringReader reader = new StringReader(input);
		JSObject object = (JSObject) parser.parse(reader);
		Assert.assertNotNull(object);

		Set<JSMember> members = object.getMembers();
		Assert.assertEquals(members.size(), 4);

		JSMember attributeJSMember = null;
		JSMember arrayJSMember = null;
		JSMember numericJSMember = null;
		for (JSMember jSMember : members) {
			String memberName = jSMember.getName().getValue();
			if ("attribute".equals(memberName)) {
				attributeJSMember = jSMember;
			} else if ("numeric".equals(memberName)) {
				numericJSMember = jSMember;
			} else if ("array".equals(memberName)) {
				arrayJSMember = jSMember;
			}
		}

		Assert.assertNotNull(attributeJSMember);
		Assert.assertNotNull(arrayJSMember);
		Assert.assertNotNull(numericJSMember);
		BigDecimal numericDecimal = ((JSNumber) numericJSMember.getValue()).getValue();
		Assert.assertEquals(numericDecimal.longValue(), 1000);

		List<JSValue> arrayItems = ((JSArray) arrayJSMember.getValue()).getItems();
		Assert.assertEquals(arrayItems.size(), 10);
	}

	@Test
	public void testParseEmoji() throws UnsupportedEncodingException {
		String input = new String(new byte[]{(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x81}, "UTF-8");
		JSValue parsed = new JSONParser().parse(new StringReader("\"" + input + "\""));
		Assert.assertEquals(parsed.getClass(), JSString.class);
		Assert.assertEquals(((JSString)parsed).getValue(), input);
		parsed = new JSONParser().parse(new StringReader("\"\\ud83d\\ude01\""));
		Assert.assertEquals(parsed.getClass(), JSString.class);
		Assert.assertEquals(((JSString)parsed).getValue(), input);
	}
	
	@Test
	public void testParseNumber() {
		String input = "{\"serviceName\":\"createPriceAlarm\",\"parameters\":{\"cid\":\"3296\",\"sku\":\"3000128\",\"p\":3.88,\"e\":\"gold@cybercon.de\"}}";
		JSONParser parser = new JSONParser();
		StringReader reader = new StringReader(input);
		JSObject object = (JSObject) parser.parse(reader);
		JSMember parameters = getMember(object, "parameters");
		JSObject parametersObject = (JSObject) parameters.getValue();
		JSMember p = getMember(parametersObject, "p");
		JSValue pValue = p.getValue();
		Assert.assertNotNull(pValue);
		JSNumber pNumber = (JSNumber) pValue;
		BigDecimal bd = pNumber.getValue();
		Assert.assertNotNull(bd);
		Assert.assertEquals(bd, new BigDecimal("3.88"));
	}
	
	@Test
	public void testHexaString() {
		String input = "{\"name\":\"Bndly \\u00df\"}";
		JSONParser parser = new JSONParser();
		StringReader reader = new StringReader(input);
		JSObject object = (JSObject) parser.parse(reader);
		String name = object.getMemberStringValue("name");
		Assert.assertEquals(name, "Bndly ß");
	}

	private JSMember getMember(JSObject object, String member) {
		for (JSMember jSMember : object.getMembers()) {
			if (jSMember.getName().getValue().equals(member)) {
				return jSMember;
			}
		}
		return null;
	}
}
