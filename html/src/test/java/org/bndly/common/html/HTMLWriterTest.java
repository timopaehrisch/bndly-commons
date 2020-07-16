package org.bndly.common.html;

/*-
 * #%L
 * HTML
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
 * A test for the HTMLWriter.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HTMLWriterTest {

	@Test
	public void testDelayedElementClose() throws IOException {
		StringWriter sw = new StringWriter();
		HTMLWriter w = new HTMLWriter(sw);
		w.createElement("div").setAttribute("class", "foo").flush();
		String bufAsString = sw.getBuffer().toString();
		Assert.assertEquals(bufAsString, "<div class=\"foo\">");
		w.closeElement().flush();
		bufAsString = sw.getBuffer().toString();
		Assert.assertEquals(bufAsString, "<div class=\"foo\"></div>");
	}
	
	@Test
	public void testRenderingWithAdditionalChildElementAfterFlush() throws IOException {
		StringWriter sw = new StringWriter();
		HTMLWriter w = new HTMLWriter(sw);
		w.createElement("div").setAttribute("class", "foo").flush();
		w.createElement("a").closeElement().flush();
		String bufAsString = sw.getBuffer().toString();
		Assert.assertEquals(bufAsString, "<div class=\"foo\"><a></a>");
		w.closeElement().flush();
		bufAsString = sw.getBuffer().toString();
		Assert.assertEquals(bufAsString, "<div class=\"foo\"><a></a></div>");
	}
}
