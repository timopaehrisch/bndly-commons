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

import org.bndly.rest.api.DataRange;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RangeHeaderParserTest {
	
	@Test
	public void testEmptyHeader() {
		String header = "";
		DataRange range = RangeHeadersUtil.parseRangeHeader(header);
		Assert.assertNotNull(range);
		
		range = RangeHeadersUtil.parseRangeHeader(null);
		Assert.assertNull(range);
	}
	
	@Test
	public void testOpenEndHeader() {
		String header = "bytes=0-";
		DataRange range = RangeHeadersUtil.parseRangeHeader(header);
		Assert.assertNotNull(range);
		Assert.assertEquals(range.getUnit(), DataRange.Unit.BYTES);
		Assert.assertEquals(range.getStart(), Long.valueOf(0));
		Assert.assertNull(range.getEnd());
		Assert.assertNull(range.getTotal());
	}
	
	@Test
	public void testRangeHeader() {
		String header = "bytes=64312833-64657026";
		DataRange range = RangeHeadersUtil.parseRangeHeader(header);
		Assert.assertNotNull(range);
		Assert.assertEquals(range.getUnit(), DataRange.Unit.BYTES);
		Assert.assertEquals(range.getStart(), Long.valueOf(64312833));
		Assert.assertEquals(range.getEnd(), Long.valueOf(64657026));
		Assert.assertNull(range.getTotal());
	}
	
	@Test
	public void testFormatContentRangeHeader() {
		String headerValue = RangeHeadersUtil.formatContentRangeHeader(new DataRange() {
			
			@Override
			public Long getStart() {
				return 0L;
			}

			@Override
			public Long getEnd() {
				return 87L;
			}

			@Override
			public Long getTotal() {
				return 88L;
			}

			@Override
			public DataRange.Unit getUnit() {
				return Unit.BYTES;
			}
		});
		Assert.assertNotNull(headerValue);
		Assert.assertEquals(headerValue, "bytes 0-87/88");
	}
}
