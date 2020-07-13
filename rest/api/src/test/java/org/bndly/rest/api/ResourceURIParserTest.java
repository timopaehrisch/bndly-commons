package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ResourceURIParserTest {
	
	@Test
	public void testPathWithEscaping() {
		String input = "https://local.ebx:8443/bndly/home/bndly%40bndly%2eorg";
		ResourceURI parsed = new ResourceURIParser(new PathCoder.UTF8(), input).parse().getResourceURI();
		Assert.assertNotNull(parsed);
		ResourceURI.Path path = parsed.getPath();
		Assert.assertNotNull(path);
		List<String> elements = path.getElements();
		Assert.assertNotNull(elements);
		Assert.assertEquals(elements.size(), 3);
		Assert.assertNull(parsed.getSelectors());
		Assert.assertNull(parsed.getExtension());
	}
	
	@Test
	public void testSelectorWithEscaping() {
		String input = "https://local.ebx:8443/bndly/home/bndly.bndly%40bndly.org";
		ResourceURI parsed = new ResourceURIParser(new PathCoder.UTF8(), input).parse().getResourceURI();
		Assert.assertNotNull(parsed);
		ResourceURI.Path path = parsed.getPath();
		Assert.assertNotNull(path);
		List<String> elements = path.getElements();
		Assert.assertNotNull(elements);
		Assert.assertEquals(elements.size(), 3);
		List<ResourceURI.Selector> selectors = parsed.getSelectors();
		Assert.assertNotNull(selectors);
		Assert.assertEquals(selectors.size(), 1);
		Assert.assertEquals(selectors.get(0).getName(), "bndly@bndly");
		ResourceURI.Extension ext = parsed.getExtension();
		Assert.assertNotNull(ext);
		Assert.assertEquals(ext.getName(), "org");
	}
}
