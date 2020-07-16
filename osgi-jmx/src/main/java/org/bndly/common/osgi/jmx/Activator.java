package org.bndly.common.osgi.jmx;

/*-
 * #%L
 * OSGI JMX
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

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Activator.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = Activator.Configuration.class)
public class Activator {

	@ObjectClassDefinition(
			name = "JMX Platform MBean Server Registration",
			description = "Gets the platform MBean server and registers it as an MBeanServer OSGI service"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Enabled",
				description = "If enabled, the platform MBeanServer will be registered as an OSGI service."
		)
		boolean enabled() default true;
	}
	
	private ServiceRegistration<?> reg;

	@Activate
	public void activate(Configuration configuration, BundleContext bundleContext) {
		if (configuration.enabled()) {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			reg = bundleContext.registerService(MBeanServer.class.getName(), mbs, null);
		}
	}

	@Deactivate
	public void deactivate() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}
}
