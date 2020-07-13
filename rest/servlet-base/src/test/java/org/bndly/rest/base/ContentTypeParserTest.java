package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.rest.api.ContentType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ContentTypeParserTest {
	
	@Test
	public void testParsing() {
		String[] inputs = new String[]{
			"application/json; charset=utf-8",
			"application/json; charset=UTF-8",
			"application/json;charset=UTF-8",
			"application/json;charset=utf-8",
			"application/json; CHARSET=utf-8",
			"application/json; CHARSET=UTF-8",
			"application/json;CHARSET=UTF-8",
			"application/json;CHARSET=utf-8",
			"application/json"
		};
		for (String input : inputs) {
			ContentType contentTypeFromString = ContentTypeParser.getContentTypeFromString(input);
			Assert.assertNotNull(contentTypeFromString);
			String charsetFromContentTypeString = ContentTypeParser.getCharsetFromContentTypeString(input);
			if ("application/json".equals(input)) {
				Assert.assertNull(charsetFromContentTypeString);
			} else {
				Assert.assertEquals(charsetFromContentTypeString.toLowerCase(), "utf-8");
			}
		}
	}
	
}
