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

import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.serializing.JSONSerializer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SerializingTest {

	@Test
	public void testEncoding() {
		JSObject o = new JSObject().createMemberValue("test", "€");

		StringWriter writer = new StringWriter();
		try {
			new JSONSerializer().serialize(o, writer);
		} catch (IOException ex) {
		}
		String result = writer.getBuffer().toString();
		Assert.assertEquals(result, "{\"test\":\"€\"}");
	}
	
	@Test
	public void testEncodingEmoji() throws UnsupportedEncodingException {
		JSString jsString = new JSString(new String(new byte[]{(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x81}, "UTF-8"));
		StringWriter writer = new StringWriter();
		try {
			new JSONSerializer().serialize(jsString, writer);
		} catch (IOException ex) {
		}
		String result = writer.getBuffer().toString();
		Assert.assertEquals(result, "\"\\ud83d\\ude01\"");
	}

}
