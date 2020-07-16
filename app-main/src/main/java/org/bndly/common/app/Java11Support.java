package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import java.net.URL;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class Java11Support extends FelixMainListener {

	private static Felix FELIX;
	private static Map FELIX_CONFIG;

	@Override
	public void beforeBundleInstallation(Felix felix) {
		FELIX = felix;
		String supportJava11Jaxb = felix.getBundle().getBundleContext().getProperty(SharedConstants.SYSTEM_PROPERTY_JAXB_JAVA11_SUPPORT_ENABLED);
		if ("true".equals(supportJava11Jaxb)) {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if (contextClassLoader != null) {
				URL url = contextClassLoader.getResource("fragment/jaxbfragment.jar");
				if (url != null) {
					BundleContext framworkBundleContext = felix.adapt(BundleContext.class);
					try {
						framworkBundleContext.installBundle(url.toString());
					} catch (BundleException e) {
						throw new IllegalStateException("failed to install jaxbfragment.jar");
					}
				}
			}
		}
	}

	@Override
	public void beforeConfig(Map felixConfig) {
		FELIX_CONFIG = felixConfig;
	}
	
	public static Felix getCurrentFelix() {
		return FELIX;
	}
	
	public static Map getCurrentFelixConfig() {
		return FELIX_CONFIG;
	}
}
