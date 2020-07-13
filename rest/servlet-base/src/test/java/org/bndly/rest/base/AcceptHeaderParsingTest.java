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

import org.bndly.rest.api.QuantifiedContentType;
import static java.lang.Math.abs;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AcceptHeaderParsingTest {
	@Test
	public void testParsing() {
		String header = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
		List<QuantifiedContentType> parsed = QuantifiedHeaderParser.parseQuantifiedHeader(header, new QuantifiedContentTypeFactory());
		Assert.assertNotNull(parsed);
		Assert.assertEquals(parsed.size(), 5);
		Assert.assertEquals(parsed.get(0).getName(), "text/html");
		Assert.assertEquals(parsed.get(1).getName(), "application/xhtml+xml");
		Assert.assertEquals(parsed.get(2).getName(), "application/xml");
		Assert.assertEquals(parsed.get(3).getName(), "image/webp");
		Assert.assertEquals(parsed.get(4).getName(), "*/*");
		
		Assert.assertTrue(abs(parsed.get(0).getQ() - 0.9) < 0.001);
		Assert.assertTrue(abs(parsed.get(1).getQ() - 0.9) < 0.001);
		Assert.assertTrue(abs(parsed.get(2).getQ() - 0.9) < 0.001);
		Assert.assertTrue(abs(parsed.get(3).getQ() - 0.8) < 0.001);
		Assert.assertTrue(abs(parsed.get(4).getQ() - 0.8) < 0.001);
	}
	
	@Test
	public void testParsingSimple() {
		String header = "text/html";
		List<QuantifiedContentType> parsed = QuantifiedHeaderParser.parseQuantifiedHeader(header, new QuantifiedContentTypeFactory());
		Assert.assertNotNull(parsed);
		Assert.assertEquals(parsed.size(), 1);
		Assert.assertEquals(parsed.get(0).getName(), "text/html");
		
		Assert.assertTrue(abs(parsed.get(0).getQ() - 1.0) < 0.001);
	}
}
