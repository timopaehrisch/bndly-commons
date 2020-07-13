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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NullRemovingIteratorTest {

	@Test
	public void testFullList() {
		final List<String> list = Arrays.asList("foo", "bar");
		Iterator<String> nullRemovingIterator = new NullRemovingIterator<>(list.iterator());
		Assert.assertEquals(nullRemovingIterator.next(), "foo");
		Assert.assertEquals(nullRemovingIterator.next(), "bar");
		Assert.assertNull(nullRemovingIterator.next());

		nullRemovingIterator = new NullRemovingIterator<>(list.iterator());
		Assert.assertTrue(nullRemovingIterator.hasNext());
		Assert.assertEquals(nullRemovingIterator.next(), "foo");
		Assert.assertTrue(nullRemovingIterator.hasNext());
		Assert.assertEquals(nullRemovingIterator.next(), "bar");
		Assert.assertFalse(nullRemovingIterator.hasNext());
		Assert.assertNull(nullRemovingIterator.next());
	}
	
	@Test
	public void testListWithNullValues() {
		final List<String> list = Arrays.asList(null, null, "foo", null, "bar", null);
		Iterator<String> nullRemovingIterator = new NullRemovingIterator<>(list.iterator());
		Assert.assertEquals(nullRemovingIterator.next(), "foo");
		Assert.assertEquals(nullRemovingIterator.next(), "bar");
		Assert.assertNull(nullRemovingIterator.next());

		nullRemovingIterator = new NullRemovingIterator<>(list.iterator());
		Assert.assertTrue(nullRemovingIterator.hasNext());
		Assert.assertEquals(nullRemovingIterator.next(), "foo");
		Assert.assertTrue(nullRemovingIterator.hasNext());
		Assert.assertEquals(nullRemovingIterator.next(), "bar");
		Assert.assertFalse(nullRemovingIterator.hasNext());
		Assert.assertNull(nullRemovingIterator.next());
	}
}
