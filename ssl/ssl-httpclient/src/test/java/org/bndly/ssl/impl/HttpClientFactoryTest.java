package org.bndly.ssl.impl;

/*-
 * #%L
 * SSL HTTPClient
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.http.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HttpClientFactoryTest {
	@Test
	public void testClientCertificate() {
		HttpClientFactory httpClientFactory = new HttpClientFactory();
		HttpClientConfiguration configuration = new HttpClientConfiguration();
		Dictionary<String, Object> props = new Hashtable<>();
		Path resources = Paths.get("src", "test", "resources");
		props.put("keyStoreLocation", resources.resolve("private.jks").toString());
		props.put("keyStorePassword", "changeit");
		props.put("trustStoreLocation", resources.resolve("public.jks").toString());
		props.put("trustStorePassword", "changeit");
		configuration.loadFromProperties(props);
		HttpClient client = httpClientFactory.createClientFromConfiguration(configuration);
		Assert.assertNotNull(client);
	}
}
