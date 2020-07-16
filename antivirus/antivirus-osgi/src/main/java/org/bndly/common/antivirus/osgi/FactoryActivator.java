package org.bndly.common.antivirus.osgi;

/*-
 * #%L
 * Antivirus OSGI
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

import org.bndly.common.antivirus.api.AVServiceFactory;
import org.bndly.common.antivirus.impl.AVServiceFactoryImpl;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = FactoryActivator.class, immediate = true)
@Designate(ocd = FactoryActivator.Configuration.class)
public class FactoryActivator {

	@ObjectClassDefinition
	public @interface Configuration {
		@AttributeDefinition(
				name = "Character Set",
				description = "Character set for exchanged socket data"
		)
		String charset() default "UTF-8";
		
		@AttributeDefinition(
				name = "Connect Timeout",
				description = "Timeout for creating a socket"
		)
		int connectTimeout() default 10000;
		
		@AttributeDefinition(
				name = "Socket Timeout",
				description = "Timeout for interaction on a socket"
		)
		int socketTimeout() default 30000;
		
		@AttributeDefinition(
				name = "Max Connections",
				description = "Maximum amount of connections per host"
		)
		int maxConnectionsPerInstance() default 10;
		
		@AttributeDefinition(
				name = "Pool timeout",
				description = "Timout for getting a connection from the connection pool in milliseconds"
		)
		long timeoutMillis() default -1;
		
		@AttributeDefinition(
				name = "Chunk Size",
				description = "Size of a data chunk when sending data"
		)
		int defaultChunkSize() default 10;
	}

	private AVServiceFactoryImpl serviceFactory;
	private ServiceRegistration<AVServiceFactory> reg;

	@Activate
	public void activate(Configuration configuration, BundleContext bundleContext) {
		serviceFactory = new AVServiceFactoryImpl();
		String charset = configuration.charset();
		serviceFactory.setCharset(charset);
		int connectTimeout = configuration.connectTimeout();
		serviceFactory.setConnectTimeout(connectTimeout);
		int defaultChunkSize = configuration.defaultChunkSize();
		serviceFactory.setDefaultChunkSize(defaultChunkSize);
		int socketTimeout = configuration.socketTimeout();
		serviceFactory.setSocketTimeout(socketTimeout);
		int maxConnectionsPerInstance = configuration.maxConnectionsPerInstance();
		serviceFactory.setMaxConnectionsPerInstance(maxConnectionsPerInstance);
		serviceFactory.setDefaultTimeoutMillis(configuration.timeoutMillis());
		serviceFactory.activate();
		Dictionary<String, Object> serviceFactoryProps = new Hashtable<>();
		String pid = AVServiceFactory.class.getName();
		serviceFactoryProps.put(Constants.SERVICE_PID, pid);
		reg = bundleContext.registerService(AVServiceFactory.class, serviceFactory, serviceFactoryProps);
	}

	@Deactivate
	public void deactivate() {
		if (reg != null) {
			reg.unregister();
		}
		reg = null;
		if (serviceFactory != null) {
			serviceFactory.deactivate();
		}
		serviceFactory = null;
	}
}
