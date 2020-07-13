package org.bndly.common.util;

/*-
 * #%L
 * Reflection
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

import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.PathWriterImpl;
import org.bndly.common.reflection.TypeHint;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeHintTest {
	public static class ChildImpl implements Child{
		private String id;
		private String name;
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
		
	}
	public static interface Child {
		String getName();
		void setName(String name);
	}
	
	public static interface Parent {
		List<Child> getChildren();
		void setChildren(List<Child> children);
	}
	
	@Test
	public void testTypeHint() {
		Parent parentImpl = InstantiationUtil.instantiateDomainModelInterface(Parent.class);
		TypeHint[] typeHints = new TypeHint[]{new TypeHint() {

			@Override
			public String getPath() {
				return "children[]";
			}

			@Override
			public boolean isCollection() {
				return true;
			}

			@Override
			public Class<?> getType() {
				return ChildImpl.class;
			}
		}};
		new PathWriterImpl().write("children[0].id", "1", parentImpl, typeHints);
		List<Child> children = parentImpl.getChildren();
		Assert.assertNotNull(children);
		Assert.assertEquals(children.size(), 1);
		Child child = children.get(0);
		Assert.assertNotNull(child);
		Assert.assertEquals(child.getClass(), ChildImpl.class);
		Assert.assertEquals(((ChildImpl)child).getId(), "1");
	}
}
