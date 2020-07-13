package org.bndly.common.lang;

/*-
 * #%L
 * Lang
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
import java.io.StringWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class StringUtilTest {
	
	@Test
	public void testHex() throws IOException {
		int i = 16;
		StringWriter sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw);
		Assert.assertEquals(sw.toString(), "00000010");
		i = 15;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw);
		Assert.assertEquals(sw.toString(), "0000000f");
		i = 31;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw);
		Assert.assertEquals(sw.toString(), "0000001f");
		i = 0;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw);
		Assert.assertEquals(sw.toString(), "00000000");
		
		i = 16;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw, false);
		Assert.assertEquals(sw.toString(), "10");
		i = 15;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw, false);
		Assert.assertEquals(sw.toString(), "f");
		i = 31;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw, false);
		Assert.assertEquals(sw.toString(), "1f");
		i = 0;
		sw = new StringWriter();
		StringUtil.appendIntAsHex(i, sw, false);
		Assert.assertEquals(sw.toString(), "0");
	}
}
