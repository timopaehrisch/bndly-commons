package org.bndly.rest.descriptor;

/*-
 * #%L
 * REST API Descriptor
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

import org.bndly.rest.descriptor.model.LinkDescriptor;
import org.bndly.rest.descriptor.model.TypeDescriptor;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CommunicationDescriptionProviderImplTest {

	private TestAtomLinkDescriptionImpl childAtomLinkDescription;
	private TestAtomLinkDescriptionImpl parentAtomLinkDescription;
	private TestJAXBMessageClassProvider childMessageClassProvider;
	private TestJAXBMessageClassProvider parentMessageClassProvider;
	private TestAtomLinkDescriptionImpl parentChildAtomLinkDescription;

	@XmlRootElement(name = "parent")
	@XmlAccessorType(XmlAccessType.NONE)
	private static class ParentBean {

	}

	@XmlRootElement(name = "child")
	@XmlAccessorType(XmlAccessType.NONE)
	private static class ChildBean extends ParentBean {

	}

	@BeforeTest
	public void before() {
		this.childAtomLinkDescription = new TestAtomLinkDescriptionImpl(ChildBean.class, "self", ChildBean.class);
		this.parentAtomLinkDescription = new TestAtomLinkDescriptionImpl(ParentBean.class, "self", ParentBean.class);
		this.parentChildAtomLinkDescription = new TestAtomLinkDescriptionImpl(ChildBean.class, "parent", ParentBean.class);
		this.childMessageClassProvider = new TestJAXBMessageClassProvider(ChildBean.class);
		this.parentMessageClassProvider = new TestJAXBMessageClassProvider(ParentBean.class);
	}

	@Test
	public void testOrderedSetupLinkBeforeMessageClassProvider() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentAtomLinkDescription, null);
		Assert.assertEquals(desc.size(), 0);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
		communicationDescriptionProviderImpl.removedAtomLink(childAtomLinkDescription, null);
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 0);
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
	}
	
	@Test
	public void testOrderedSetupMessageClassProviderBeforeLink() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentAtomLinkDescription, null);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
		communicationDescriptionProviderImpl.removedAtomLink(childAtomLinkDescription, null);
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 0);
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
	}
	
	@Test
	public void testCleanUpInLinkDescriptor() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentChildAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentAtomLinkDescription, null);
		Assert.assertNotNull(desc.get(ChildBean.class));
		List<LinkDescriptor> links = desc.get(ChildBean.class).getLinks();
		Assert.assertEquals(links.size(), 2);
		Assert.assertEquals(links.get(0).getRel(), childAtomLinkDescription.getRel());
		Assert.assertEquals(links.get(1).getRel(), parentChildAtomLinkDescription.getRel());
		Assert.assertNotNull(links.get(1).getReturns());
		Assert.assertEquals(links.get(1).getReturns(), desc.get(ParentBean.class));
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertNull(links.get(1).getReturns());
	}
	
	@Test
	public void testLazyLinkDescriptorConsumesReturns() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentChildAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentAtomLinkDescription, null);
		Assert.assertNotNull(desc.get(ChildBean.class));
		List<LinkDescriptor> links = desc.get(ChildBean.class).getLinks();
		Assert.assertEquals(links.size(), 2);
		Assert.assertEquals(links.get(0).getRel(), childAtomLinkDescription.getRel());
		Assert.assertEquals(links.get(1).getRel(), parentChildAtomLinkDescription.getRel());
		Assert.assertNull(links.get(1).getReturns());
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertNotNull(links.get(1).getReturns());
		Assert.assertEquals(links.get(1).getReturns(), desc.get(ParentBean.class));
	}
	
	@Test
	public void testOrderedSetupLinkWithMessageClassProviderComeAndGo() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		communicationDescriptionProviderImpl.addedAtomLink(childAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addedAtomLink(parentAtomLinkDescription, null);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(childMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.get(ChildBean.class).getLinks().size(), 1);
	}
	
	@Test
	public void testOrderedSetupMessageClassProviderBeforeLinkChildBeforeParent() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		Assert.assertEquals(desc.size(), 2);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNotNull(desc.get(ChildBean.class).getParent());
		Assert.assertEquals(desc.get(ChildBean.class).getParent(), desc.get(ParentBean.class));
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class).getParent());
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 0);
		Assert.assertNull(desc.get(ChildBean.class));
		Assert.assertNull(desc.get(ParentBean.class));
	}
	
	@Test
	public void testOrderedSetupMessageClassProviderBeforeLinkChildBeforeParent2() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		Assert.assertEquals(desc.size(), 2);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNotNull(desc.get(ChildBean.class).getParent());
		Assert.assertEquals(desc.get(ChildBean.class).getParent(), desc.get(ParentBean.class));
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class));
		Assert.assertTrue(desc.get(ParentBean.class).getSubs().isEmpty());
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertEquals(desc.size(), 0);
		Assert.assertNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class));
	}
	
	@Test
	public void testOrderedSetupMessageClassProviderBeforeLinkParentBeforeChild() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		Assert.assertEquals(desc.size(), 2);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNotNull(desc.get(ChildBean.class).getParent());
		Assert.assertEquals(desc.get(ChildBean.class).getParent(), desc.get(ParentBean.class));
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class).getParent());
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 0);
		Assert.assertNull(desc.get(ChildBean.class));
		Assert.assertNull(desc.get(ParentBean.class));
	}
	
	@Test
	public void testOrderedSetupMessageClassProviderBeforeLinkParentBeforeChild2() {
		CommunicationDescriptionProviderImpl communicationDescriptionProviderImpl = new CommunicationDescriptionProviderImpl();
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(parentMessageClassProvider);
		communicationDescriptionProviderImpl.addJAXBMessageClassProvider(childMessageClassProvider);
		Map<Class<?>, TypeDescriptor> desc = communicationDescriptionProviderImpl.getTypeDescriptors();
		Assert.assertEquals(desc.size(), 2);
		Assert.assertNotNull(desc.get(ChildBean.class));
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNotNull(desc.get(ChildBean.class).getParent());
		Assert.assertEquals(desc.get(ChildBean.class).getParent(), desc.get(ParentBean.class));
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(childMessageClassProvider);
		Assert.assertEquals(desc.size(), 1);
		Assert.assertNotNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class));
		Assert.assertTrue(desc.get(ParentBean.class).getSubs().isEmpty());
		
		communicationDescriptionProviderImpl.removeJAXBMessageClassProvider(parentMessageClassProvider);
		Assert.assertEquals(desc.size(), 0);
		Assert.assertNull(desc.get(ParentBean.class));
		Assert.assertNull(desc.get(ChildBean.class));
	}
}
