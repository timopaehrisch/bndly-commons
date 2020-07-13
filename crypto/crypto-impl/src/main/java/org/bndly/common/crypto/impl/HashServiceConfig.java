package org.bndly.common.crypto.impl;

/*-
 * #%L
 * Crypto Impl
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

import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.CryptoServiceFactory;
import org.bndly.common.crypto.api.HashService;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HashServiceConfig is used to enable OSGI containers to create {@link org.bndly.common.crypto.api.HashServiceConfig} instances with the OSGI config admin.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = HashServiceConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = HashServiceConfig.Configuration.class)
public class HashServiceConfig implements org.bndly.common.crypto.api.HashServiceConfig {

	@ObjectClassDefinition(name = "Hash service configuration")
	public @interface Configuration {
		@AttributeDefinition(
				name = "Name",
				description = "The name of the hash crypto service"
		)
		String name() default "default";
		
		@AttributeDefinition(
				name = "Message Digest Algorithm",
				description = "Algorithm used for default hashing service"
		)
		String messageDigestAlgorithm() default "SHA-256";
		
		@AttributeDefinition(
				name = "Secure Random Algorithm",
				description = "Algorithm used for creating a secure random, that might be used as a hashing salt"
		)
		String secureRandomAlorithm() default "SHA1PRNG";
		
		@AttributeDefinition(
				name = "Hashing Iterations",
				description = "Iterations done for a single hash operation"
		)
		int hashingIterations() default 1000;
		
		@AttributeDefinition(
				name = "Hashing Default Salt Length",
				description = "Default length of a random salt for hashing."
		)
		int hashingDefaultSaltLength() default 8;
		
		@AttributeDefinition(
				name = "Default String Encoding",
				description = "Default encoding for strings"
		)
		String defaultInputStringEncoding() default "UTF-8";
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(HashServiceConfig.class);
	@Reference
	private CryptoServiceFactory cryptoServiceFactory;
	private DictionaryAdapter config;
	private ServiceRegistration<HashService> reg;

	/**
	 * This method loads the configuration properties from the OSGI component context and creates a hash service instance. 
	 * The service instance will be registered as an OSGI service.
	 * @param componentContext The OSGI component context of this configuration instance
	 */
	@Activate
	public void activate(ComponentContext componentContext) {
		config = new DictionaryAdapter(componentContext.getProperties());
		try {
			HashService hashService = cryptoServiceFactory.createHashService(this);
			if (hashService != null) {
				reg = ServiceRegistrationBuilder.newInstance(HashService.class, hashService)
						.pid(HashService.class.getName() + "." + getName())
						.property("name", getName())
						.register(componentContext.getBundleContext())
						;

			}
		} catch (CryptoException e) {
			LOG.error("could not activate hash service config");
		}
	}
	
	/**
	 * This deactivates the configuration and removes the hash service instance from {@link #activate(org.osgi.service.component.ComponentContext) }, if such an instance had been registered.
	 */
	@Deactivate
	public void deactivate() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}
	
	/**
	 * Gets the name property of the configuration.
	 * @return the name or 'default', if no name is explicitly defined
	 */
	public String getName() {
		return config.getString("name", "default");
	}
	
	@Override
	public String getDefaultInputStringEncoding() {
		return config.getString("defaultInputStringEncoding", "UTF-8");
	}

	@Override
	public String getMessageDigestAlgorithm() {
		return config.getString("messageDigestAlgorithm", "SHA-256");
	}

	@Override
	public String getSecureRandomAlorithm() {
		return config.getString("secureRandomAlorithm", "SHA1PRNG");
	}

	@Override
	public int getHashingDefaultSaltLength() {
		return config.getInteger("hashingDefaultSaltLength", 8);
	}

	@Override
	public int getHashingIterations() {
		return config.getInteger("hashingIterations", 1000);
	}
	
}
