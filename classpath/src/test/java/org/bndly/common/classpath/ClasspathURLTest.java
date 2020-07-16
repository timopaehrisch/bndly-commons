package org.bndly.common.classpath;

/*-
 * #%L
 * Classpath
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ClasspathURLTest {

	@Test
	public void testLoadClasspathURL() throws IOException {
		Handler.init();
		String demoURL = "classpath:someresource";
		URL url = new URL(demoURL);
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();
		StringWriter sw = new StringWriter();
		int byteInfo;
		while ((byteInfo = is.read()) > -1) {
			sw.write(byteInfo);
		}
		sw.flush();
		String result = sw.getBuffer().toString();
		Assert.assertEquals("hello world", result);
	}
}
