package org.bndly.common.app.provisioning;

/*-
 * #%L
 * App Provisioning
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

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LaunchFrameworkTest {
	
	@Ignore
	@Test
	public void launchFelixTest() throws BundleException, InterruptedException {
		Map config = new HashMap();
		Felix felix = new Felix(config);
		felix.init(new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				String name = event.getBundle().getSymbolicName();
			}
		});
		FrameworkWiring wiring = felix.adapt(FrameworkWiring.class);
		FrameworkStartLevel startLevel = felix.adapt(FrameworkStartLevel.class);
		BundleContext framworkBundleContext = felix.adapt(BundleContext.class);
		framworkBundleContext.addFrameworkListener(new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.STARTED) {
					System.out.println("framework started");
					Bundle[] bundles = event.getBundle().getBundleContext().getBundles();
					for (Bundle bundle : bundles) {
						BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
						System.out.println("bundle " + bundle.getSymbolicName() + " [" + bsl.getStartLevel() + "]");
					}
				}
			}
		});
		framworkBundleContext.addBundleListener(new BundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				System.out.println(event.getBundle().getSymbolicName() + " " + event.getType());
			}
		});
		String location = Paths.get("/Users/thomas/.m2/repository/org.bndly/common/org.bndly.common.osgi/2.0.30-SNAPSHOT/org.bndly.common.osgi-2.0.30-SNAPSHOT.jar").toUri().toString();
		Bundle installedBundle = framworkBundleContext.installBundle(location);
		BundleStartLevel bundleStartLevel = installedBundle.adapt(BundleStartLevel.class);
		bundleStartLevel.setStartLevel(3);
		String symbolicName = installedBundle.getSymbolicName();
		Assert.assertEquals("org.bndly.common.osgi", symbolicName);
		//Thread.sleep(1000);
		Assert.assertEquals(Bundle.INSTALLED, installedBundle.getState());
		startLevel.setStartLevel(3);
		Thread.sleep(1000);
		//installedBundle.start();
		Assert.assertEquals(Bundle.ACTIVE, installedBundle.getState());
		felix.start();
		felix.stop();
	}
}
