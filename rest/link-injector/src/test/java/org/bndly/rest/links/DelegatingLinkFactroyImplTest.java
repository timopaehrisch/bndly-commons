package org.bndly.rest.links;

/*-
 * #%L
 * REST Link Injector
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

import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.LinkFactory;
import org.bndly.rest.atomlink.impl.DelegatingLinkFactroyImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingLinkFactroyImplTest {

	private static class AtomLinkBeanImpl implements AtomLinkBean {
		private String rel;
		private String href;
		private String method;
		@Override
		public String getRel() {
			return rel;
		}

		@Override
		public void setRel(String rel) {
			this.rel = rel;
		}

		@Override
		public String getHref() {
			return href;
		}

		@Override
		public void setHref(String href) {
			this.href = href;
		}

		@Override
		public String getMethod() {
			return method;
		}

		@Override
		public void setMethod(String method) {
			this.method = method;
		}
		
	}
	
	private final List<AtomLinkBean> linkItems = new ArrayList<>();
	
	private void initOneLink() {
		AtomLinkBeanImpl atomLinkBeanImpl = new AtomLinkBeanImpl();
		atomLinkBeanImpl.setHref("http://www.cybercon.de");
		atomLinkBeanImpl.setMethod("GET");
		atomLinkBeanImpl.setRel("foo");
		linkItems.add(atomLinkBeanImpl);
	}
	
	private LinkFactory initLinkFactory() {
		return new LinkFactory() {

			@Override
			public Class getTargetType() {
				return Object.class;
			}
			
			@Override
			public Iterator<AtomLinkBean> buildLinks(Object targetBean, boolean isMessageRoot) {
				return linkItems.iterator();
			}
			
			@Override
			public Iterator buildLinks() {
				return buildLinks(null, true);
			}
		};
	}
	
	@BeforeMethod
	public void cleanup(){
		linkItems.clear();
	}
	
	@Test
	public void testEmptyDelegates() {
		DelegatingLinkFactroyImpl impl = new DelegatingLinkFactroyImpl(Object.class);
		Iterator<AtomLinkBean> iter = impl.buildLinks(new Object(), true);
		Assert.assertFalse(iter.hasNext());
		
		impl.registerLinkFactory(new LinkFactory() {

			@Override
			public Class getTargetType() {
				return Object.class;
			}
			
			@Override
			public Iterator<AtomLinkBean> buildLinks(Object targetBean, boolean isMessageRoot) {
				return Collections.EMPTY_LIST.iterator();
			}
			
			@Override
			public Iterator buildLinks() {
				return buildLinks(null, true);
			}
		});
		iter = impl.buildLinks(new Object(), true);
		Assert.assertFalse(iter.hasNext());
	}
	
	@Test
	public void testSingleDelegate() {
		initOneLink();
		DelegatingLinkFactroyImpl impl = new DelegatingLinkFactroyImpl(Object.class);
		impl.registerLinkFactory(initLinkFactory());
		Iterator<AtomLinkBean> iter = impl.buildLinks(new Object(), true);
		Assert.assertTrue(iter.hasNext());
		AtomLinkBean item = iter.next();
		Assert.assertTrue(item == linkItems.get(0));
		Assert.assertFalse(iter.hasNext());
	}
	
	@Test
	public void testMultipleDelegates() {
		initOneLink();
		DelegatingLinkFactroyImpl impl = new DelegatingLinkFactroyImpl(Object.class);
		impl.registerLinkFactory(initLinkFactory());
		impl.registerLinkFactory(initLinkFactory());
		Iterator<AtomLinkBean> iter = impl.buildLinks(new Object(), true);
		Assert.assertTrue(iter.hasNext());
		AtomLinkBean item = iter.next();
		Assert.assertTrue(item == linkItems.get(0));
		Assert.assertTrue(iter.hasNext());
		item = iter.next();
		Assert.assertTrue(item == linkItems.get(0));
		Assert.assertFalse(iter.hasNext());
	}
	
	@Test
	public void testMultipleDelegatesMultipleLinks() {
		initOneLink();
		initOneLink();
		DelegatingLinkFactroyImpl impl = new DelegatingLinkFactroyImpl(Object.class);
		impl.registerLinkFactory(initLinkFactory());
		impl.registerLinkFactory(initLinkFactory());
		Iterator<AtomLinkBean> iter = impl.buildLinks(new Object(), true);
		Assert.assertTrue(iter.hasNext());
		AtomLinkBean item = iter.next();
		Assert.assertTrue(item == linkItems.get(0));
		Assert.assertTrue(iter.hasNext());
		item = iter.next();
		Assert.assertTrue(item == linkItems.get(1));
		Assert.assertTrue(iter.hasNext());
		item = iter.next();
		Assert.assertTrue(item == linkItems.get(0));
		Assert.assertTrue(iter.hasNext());
		item = iter.next();
		Assert.assertTrue(item == linkItems.get(1));
		Assert.assertFalse(iter.hasNext());
	}
}
