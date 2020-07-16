package org.bndly.code.output;

/*-
 * #%L
 * Code Model
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

import org.bndly.code.model.XMLElement;
import java.io.IOException;
import java.io.StringWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class XMLWriterTest {

	@Test
	public void testEasy() throws IOException {
		XMLElement xmlElement = new XMLElement("easy");
		StringWriter sw = new StringWriter();
		new XMLWriter().write(xmlElement, sw);
		String xml = sw.toString();
		Assert.assertEquals(xml, "<easy/>");
	}

	@Test
	public void testSpecialCharacter() throws IOException {
		XMLElement xmlElement = new XMLElement("ä");
		xmlElement.createAttribute("ü", "&\"");
		StringWriter sw = new StringWriter();
		new XMLWriter().write(xmlElement, sw);
		String xml = sw.toString();
		Assert.assertEquals(xml, "<ä ü=\"&amp;&quot;\"/>");
	}

	@Test
	public void testEvil() throws IOException {
		XMLElement xmlElement = new XMLElement("<");
		StringWriter sw = new StringWriter();
		try {
			new XMLWriter().write(xmlElement, sw);
			Assert.fail("expected an XMLIllegalCharacterException");
		} catch (XMLIllegalCharacterException expected) {
		}
	}

	@Test
	public void testEvil2() throws IOException {
		XMLElement xmlElement = new XMLElement("a<");
		StringWriter sw = new StringWriter();
		try {
			new XMLWriter().write(xmlElement, sw);
			Assert.fail("expected an XMLIllegalCharacterException");
		} catch (XMLIllegalCharacterException expected) {
		}
	}
}
