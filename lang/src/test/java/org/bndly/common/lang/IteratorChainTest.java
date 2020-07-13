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

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class IteratorChainTest {

	@Test
	public void testChain() {
		List<String> arrayList = new ArrayList<>();
		arrayList.add("a");
		List<String> arrayList2 = new ArrayList<>();
		arrayList2.add("b");
		List<String> arrayList3 = new ArrayList<>();
		IteratorChain<String> iteratorChain = new IteratorChain<String>(arrayList.iterator(), arrayList3.iterator(), arrayList2.iterator());
		int i = 0;
		while (iteratorChain.hasNext()) {
			String next = iteratorChain.next();
			if (i == 0) {
				Assert.assertEquals(next, "a");
			} else if (i == 1) {
				Assert.assertEquals(next, "b");
			}
			i++;
		}
		Assert.assertEquals(i, 2);
	}

	@Test
	public void testFilteredIterator() {
		List<Integer> arrayList = new ArrayList<>();
		arrayList.add(0);
		arrayList.add(1);
		arrayList.add(2);
		arrayList.add(3);
		arrayList.add(4);
		FilteringIterator<Integer> filteredIterator = new FilteringIterator<Integer>(arrayList.iterator()) {
			@Override
			protected boolean isAccepted(Integer toCheck) {
				return toCheck % 2 == 0;
			}

		};
		Assert.assertEquals(filteredIterator.next().intValue(), 0);
		Assert.assertEquals(filteredIterator.next().intValue(), 2);
		Assert.assertEquals(filteredIterator.next().intValue(), 4);
		Assert.assertFalse(filteredIterator.hasNext());
		
		filteredIterator = new FilteringIterator<Integer>(arrayList.iterator()) {
			@Override
			protected boolean isAccepted(Integer toCheck) {
				return toCheck % 2 == 0;
			}

		};
		int sum = 0;
		while (filteredIterator.hasNext()) {
			sum += filteredIterator.next();
		}
		Assert.assertEquals(sum, 6);
	}
}
