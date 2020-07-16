package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MultiValuedStringGrammarTest {
	
	@Test
	public void testSplitEmptyValues() {
		Iterator<String> result = MultiValuedStringGrammar.split(",").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,a").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
	}

	@Test
	public void testSplitEmptyValuesEscaped() {
		Iterator<String> result = MultiValuedStringGrammar.split("\"\",\"\"").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,\"\"").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,,a").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("a,\"\",a").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "a");
		Assert.assertFalse(result.hasNext());
	}

	@Test
	public void testEscapedSeparator() {
		Iterator<String> result = MultiValuedStringGrammar.split("\"\"\"\"").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "\"");
		Assert.assertFalse(result.hasNext());
	}
	
	@Test
	public void testSpecialSequences() {
		Iterator<String> result = MultiValuedStringGrammar.split("\",\",").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), ",");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
		
		result = MultiValuedStringGrammar.split("\",\",\"\"").iterator();
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), ",");
		Assert.assertTrue(result.hasNext());
		Assert.assertEquals(result.next(), "");
		Assert.assertFalse(result.hasNext());
	}
	
	@Test
	public void testContact() {
		Assert.assertEquals("", MultiValuedStringGrammar.concat(Arrays.asList("")));
		Assert.assertEquals("", MultiValuedStringGrammar.concat(Collections.EMPTY_LIST));
		Assert.assertEquals("a", MultiValuedStringGrammar.concat(Arrays.asList("a")));
		Assert.assertEquals("\"a,\"", MultiValuedStringGrammar.concat(Arrays.asList("a,")));
		Assert.assertEquals("a,a", MultiValuedStringGrammar.concat(Arrays.asList("a","a")));
		Assert.assertEquals("\",\"", MultiValuedStringGrammar.concat(Arrays.asList(",")));
		Assert.assertEquals(",,", MultiValuedStringGrammar.concat(Arrays.asList("","","")));
		Assert.assertEquals("\"\"\"\"", MultiValuedStringGrammar.concat(Arrays.asList("\"")));
		Assert.assertEquals("\"\"\"\",a", MultiValuedStringGrammar.concat(Arrays.asList("\"", "a")));
		Assert.assertEquals("\"\"\"a\"\"\"", MultiValuedStringGrammar.concat(Arrays.asList("\"a\"")));
	}
}
