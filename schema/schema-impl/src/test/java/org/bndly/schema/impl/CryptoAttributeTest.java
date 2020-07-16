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

import org.bndly.common.data.io.IOUtils;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CryptoAttributeTest extends AbstractSchemaTest {
	@Test
	public void testCryptoAttributePlainAutoDecrypted() throws IOException{
		// plain and auto decrypt means: string.toBytes() -> encrypt -> DB -> decrypt -> string.fromBytes()
		String expectedString = "testCryptoAttributePlainAutoDecrypted";
		testCryptoAttribute(true, true, expectedString, expectedString);
	}

	@Test
	public void testCryptoAttributeEncodedAutoDecrypted() throws IOException {
		// not plain and auto decrypt means: string64 -> decode -> encrypt -> DB -> decrypt -> encode -> string64
		String expectedString = "testCryptoAttributeEncodedAutoDecrypted";
		String encoded = base64Service.base64Encode(expectedString.getBytes("UTF-8"));
		testCryptoAttribute(false, true, encoded, expectedString);
	}
	
	@Test
	public void testCryptoAttributePlainEncrypted() throws IOException {
		// plain and no auto decrypt means: string -> decode -> DB -> encode -> string
		String expectedString = "this!is!a_test...123";
		byte[] demoData = expectedString.getBytes("UTF-8");
		InputStream encryptingStream = simpleCryptoService.encodeStream(new ByteArrayInputStream(demoData));
		String base64 = base64Service.base64Encode(encryptingStream);
		testCryptoAttribute(true, false, base64, expectedString);
	}

	@Test
	public void testCryptoAttributeEncodedEncrypted() throws IOException {
		// plain and no auto decrypt means: string64 -> decode -> DB -> encode -> string64
		String expectedString = "this!is!a_test...123";
		String encoded = base64Service.base64Encode(expectedString.getBytes("UTF-8"));
		InputStream encryptingStream = simpleCryptoService.encodeStream(new ByteArrayInputStream(encoded.getBytes("UTF-8")));
		String encryptedAndEncoded = base64Service.base64Encode(encryptingStream);
		testCryptoAttribute(false, false, encryptedAndEncoded, encoded);
	}
	
	public void testCryptoAttribute(boolean plainString, boolean autoDecrypted, String testString, String expectedString) throws IOException {
		Deployer deployer = engine.getDeployer();
		Accessor accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.type("Foo")
			.attribute("ca", CryptoAttribute.class)
				.attributeValue("autoDecrypted", autoDecrypted)
				.attributeValue("plainString", plainString)
		;
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		RecordContext rc = accessor.buildRecordContext();
		Record foo = rc.create("Foo");
		foo.setAttributeValue("ca", testString);
		long id = accessor.insert(foo);
		rc = accessor.buildRecordContext();
		Record fooReloaded = accessor.readById("Foo", id, rc);
		String fromDB = fooReloaded.getAttributeValue("ca", String.class);
		Assert.assertEquals(fromDB, testString);
		if(!autoDecrypted) {
			byte[] decodedButEncrypted = base64Service.base64Decode(fromDB);
			InputStream plainStream = simpleCryptoService.decodeStream(new ByteArrayInputStream(decodedButEncrypted));
			Assert.assertEquals(IOUtils.readToString(plainStream, "UTF-8"), expectedString);
		}
	}
}
