package org.bndly.common.osgi.util;

/*-
 * #%L
 * OSGI Utilities
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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ServicePidUtilTest {
	
	@Test
	public void testNonFactoryPID() {
		ServicePidUtil.PID pid = ServicePidUtil.parseFileName("com.acme.Foo.cfg");
		Assert.assertNotNull(pid);
		Assert.assertEquals(pid.getPid(), "com.acme.Foo");
		Assert.assertNull(pid.getFactoryPid());
	}
	
	@Test
	public void testFactoryPID() {
		ServicePidUtil.PID pid = ServicePidUtil.parseFileName("com.acme.Foo-4711.cfg");
		Assert.assertNotNull(pid);
		Assert.assertEquals(pid.getPid(), "com.acme.Foo");
		Assert.assertEquals(pid.getFactoryPid(), "4711");
	}
}
