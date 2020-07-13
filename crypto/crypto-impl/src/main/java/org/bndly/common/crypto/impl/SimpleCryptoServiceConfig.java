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

import org.bndly.common.crypto.api.AsymetricEncryptionConfig;
import org.bndly.common.crypto.api.BlockBasedEncryptionConfig;
import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.CryptoServiceFactory;
import org.bndly.common.crypto.api.KeystoreConfig;
import org.bndly.common.crypto.api.KeystoreEncryptionConfig;
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.common.data.io.CloseOnLastByteOrExceptionInputStreamWrapper;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * The SimpleCryptoServiceConfig is used to enable OSGI containers to create {@link org.bndly.common.crypto.api.EncryptionConfig} instances with the OSGI config admin.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SimpleCryptoServiceConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = SimpleCryptoServiceConfig.Configuration.class)
public class SimpleCryptoServiceConfig implements KeystoreEncryptionConfig, AsymetricEncryptionConfig, KeystoreConfig, BlockBasedEncryptionConfig {
	
	@ObjectClassDefinition(name = "Simple crypto service configuration")
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "The name of the simple crypto service"
		)
		String name() default "default";

		@AttributeDefinition(
				name = "Keystore location",
				description = "Path to default keystore"
		)
		String keystoreLocation() default ".keystore";

		@AttributeDefinition(
				name = "Keystore password",
				description = "Password of the keystore",
				type = AttributeType.PASSWORD
		)
		String keystorePassword() default "changeit";

		@AttributeDefinition(
				name = "Keystore type",
				description = "Type of the keystore ('jks' for example)"
		)
		String keystoreType() default "jks";

		@AttributeDefinition(
				name = "Encryption key alias",
				description = "Alias of the key to use for default encryption"
		)
		String encryptionKeyAlias() default "defaultEncKey";

		@AttributeDefinition(
				name = "Encryption key password",
				description = "Password of the key to use for default encryption",
				type = AttributeType.PASSWORD
		)
		String encryptionKeyPassword() default "changeit";

		@AttributeDefinition(
				name = "Key algorithm",
				description = "Algorithm of the key used for the default encryption cipher"
		)
		String encryptionKeyAlgorithm() default "AES";

		@AttributeDefinition(
				name = "Private key encryption enabled",
				description = "If this value is true and the algorithm is asymetric (like RSA), then the private key will be used for encryption."
		)
		boolean privateKeyEncryption() default true;

		@AttributeDefinition(
				name = "Block size",
				description = "Number of bytes per encrypted block."
		)
		int blockSize() default -1;

		@AttributeDefinition(
				name = "Max data block size",
				description = "Number of data bytes to be contained in a block."
		)
		int maxDataBlockSize() default -1;

		@AttributeDefinition(
				name = "Default String Encoding",
				description = "Default encoding for strings"
		)
		String defaultInputStringEncoding() default "UTF-8";
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(SimpleCryptoServiceConfig.class);
	@Reference
	private CryptoServiceFactory cryptoServiceFactory;
	private DictionaryAdapter config;
	private ServiceRegistration<SimpleCryptoService> reg;
	
	/**
	 * This method loads the configuration properties from the OSGI component context and creates a crypto service instance. 
	 * The service instance will be registered as an OSGI service.
	 * @param componentContext The OSGI component context of this configuration instance
	 */
	@Activate
	public void activate(ComponentContext componentContext) {
		config = new DictionaryAdapter(componentContext.getProperties());
		try {
			SimpleCryptoService simpleCryptoService = cryptoServiceFactory.createSimpleCryptoService(this);
			if (simpleCryptoService != null) {
				reg = ServiceRegistrationBuilder.newInstance(SimpleCryptoService.class, simpleCryptoService)
						.pid(SimpleCryptoService.class.getName() + "." + getName())
						.property("name", getName())
						.register(componentContext.getBundleContext())
						;

			}
		} catch (CryptoException e) {
			LOG.error("could not activate crypto service config");
		}
	}
	
	/**
	 * This deactivates the configuration and removes the crypto service instance from {@link #activate(org.osgi.service.component.ComponentContext) }, if such an instance had been registered.
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
	public KeystoreConfig getKeystoreConfig() {
		return this;
	}

	@Override
	public String getKeyAlias() {
		return config.getString("encryptionKeyAlias", "defaultEncKey");
	}

	@Override
	public char[] getKeystoreEntryPassword() {
		String encryptionKeyPassword = config.getString("encryptionKeyPassword", "changeit");
		if (encryptionKeyPassword == null) {
			return null;
		}
		return encryptionKeyPassword.toCharArray();
	}

	@Override
	public String getCipherAlgorithm() {
		return config.getString("encryptionKeyAlgorithm", "AES");
	}

	@Override
	public InputStream getInputStream() {
		String keystoreLocation = config.getString("keystoreLocation", ".keystore");
		Path path = Paths.get(keystoreLocation);
		try {
			return new CloseOnLastByteOrExceptionInputStreamWrapper(Files.newInputStream(path, StandardOpenOption.READ));
		} catch (IOException ex) {
			LOG.error("could not open input stream for crypto service config", ex);
			return null;
		}
	}

	@Override
	public char[] getPassword() {
		String encryptionKeyPassword = config.getString("keystorePassword", "changeit");
		if (encryptionKeyPassword == null) {
			return null;
		}
		return encryptionKeyPassword.toCharArray();
	}

	@Override
	public String getType() {
		return config.getString("keystoreType", "jks");
	}

	@Override
	public boolean isPrivateKeyUsedForEncryption() {
		return config.getBoolean("privateKeyEncryption", true);
	}

	@Override
	public int getBlockSize() {
		return config.getInteger("blockSize", -1);
	}

	@Override
	public int getMaxDataBlockSize() {
		return config.getInteger("maxDataBlockSize", -1);
	}

}
