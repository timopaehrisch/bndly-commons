package org.bndly.rest.swagger.model;

/*-
 * #%L
 * REST Swagger Integration
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
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SerialzingTest {
	
	@Test
	public void testSimpleSerialization() throws IOException {
		Info info = new Info("testapi", "1.0");
		info.setDescription("This is a test swagger document.");
		License license = new License("Bndly License");
		license.setUrl("http://www.bndly.org/ebx/license.html");
		info.setLicense(license);
		Contact contact = new Contact();
		contact.setEmail("bndly@cybercon.de");
		contact.setName("Bndly");
		contact.setUrl("http://www.bndly.org/ebx");
		info.setContact(contact);
		Document document = new Document();
		document.setInfo(info);
		document.setBasePath("/bndly");
		document.setSchemes(Arrays.asList("http"));
		document.setHost("localhost");
		List<Tag> tags = new ArrayList<>();
		document.setTags(tags);
		Tag productTag = new Tag("product");
		productTag.setDescription("everything about products");
		tags.add(productTag);
		Paths paths = new Paths();
		Path path = new Path();
		Operation getOperation = new Operation();
		getOperation.setDescription("get a paginated list of carts.");
		getOperation.setProduces(Arrays.asList("application/xml", "application/json"));
		Responses responses = new Responses();
		Response response = new Response("success");
		responses.put(200, response);
		getOperation.setResponses(responses);
		path.setGet(getOperation);
		paths.put("/ebx/Cart", path);
		document.setPaths(paths);
		
		ConversionContext conversionContext = new ConversionContextBuilder().initDefaults().skipNullValues().build();
		JSValue swaggerDoc = conversionContext.serialize(Document.class, document);
		StringWriter sw = new StringWriter();
		new JSONSerializer().serialize(swaggerDoc, sw);
		sw.flush();
		String swaggerDocString = sw.toString();
		swaggerDocString = swaggerDocString;
	}
}
