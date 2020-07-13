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
import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.crypto.api.BlockBasedEncryptionConfig;
import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.EncryptionConfig;
import org.bndly.common.crypto.api.HashService;
import org.bndly.common.crypto.api.KeystoreConfig;
import org.bndly.common.crypto.api.KeystoreEncryptionConfig;
import org.bndly.common.crypto.api.ProvidedKeyEncryptionConfig;
import org.bndly.common.crypto.api.ProvidedKeyPairEncryptionConfig;
import org.bndly.common.crypto.api.SignatureCreationService;
import org.bndly.common.crypto.api.SignatureMismatchException;
import org.bndly.common.crypto.api.SignatureResult;
import org.bndly.common.crypto.api.SignatureService;
import org.bndly.common.crypto.api.SignatureValidationService;
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.common.crypto.api.SimpleDecryptService;
import org.bndly.common.crypto.api.SimpleEncryptService;
import org.bndly.common.osgi.util.DictionaryAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.osgi.service.component.ComponentContext;
import org.bndly.common.crypto.api.CryptoServiceFactory;
import org.bndly.common.crypto.api.HashServiceConfig;
import org.bndly.common.data.io.CloseOnLastByteOrExceptionInputStreamWrapper;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.security.interfaces.RSAKey;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The CryptoServiceFactoryImpl creates various pre-configured services for encryption, decryption, signatures and hashes.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = CryptoServiceFactory.class,
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = CryptoServiceFactoryImpl.Configuration.class)
public class CryptoServiceFactoryImpl implements CryptoServiceFactory {
	
	@ObjectClassDefinition(
		name = "Crypto Service Factory",
		description = "This factory is used to create and register crypto services. If can also be used to create global default instances of crypto services."
	)
	public @interface Configuration {
		
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
				name = "Encryption key alias",
				description = "Alias of the private key used for signature creation"
		)
		String signatureKeyAlias() default "defaultSignKey";
		
		@AttributeDefinition(
				name = "Encryption key password",
				description = "Password of the private key used for signature creation",
				type = AttributeType.PASSWORD
		)
		String signatureKeyPassword() default "changeit";
		
		@AttributeDefinition(
				name = "Key algorithm",
				description = "Algorithm of the private key used for signature creation"
		)
		String signatureKeyAlgorithm() default "RSA/ECB/PKCS1Padding";
		
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
				name = "Default String Encoding",
				description = "Default encoding for strings"
		)
		String defaultInputStringEncoding() default "UTF-8";
	}

	private String defaultInputStringEncoding = "UTF-8";
	private String messageDigestAlgorithm = "SHA-256";
	private String secureRandomAlorithm = "SHA1PRNG";
	private int hashingIterations = 1000;
	private int hashingDefaultSaltLength = 8;
	private static final String BASE64INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final Map<Character, Integer> BASE64REVERSEINDEX;
	private static final AlgorithmTester RSA_TESTER = new AlgorithmTester("RSA");
	private static final AlgorithmTester AES_TESTER = new AlgorithmTester("AES");

	private KeystoreConfig defaultKeystoreConfig;
	private KeystoreEncryptionConfig defaultEncryptionConfig;
	private KeystoreEncryptionConfig signatureEncryptionConfig;

	private String keystoreLocation;
	private String keystorePassword;
	private String keystoreType;
	private String encryptionKeyAlias;
	private String encryptionKeyPassword;
	private String encryptionKeyAlgorithm;
	private String signatureKeyAlias;
	private String signatureKeyPassword;
	private String signatureKeyAlgorithm;

	static {
		BASE64REVERSEINDEX = new HashMap<>();
		for (int index = 0; index < BASE64INDEX.length(); index++) {
			char character = BASE64INDEX.charAt(index);
			BASE64REVERSEINDEX.put(character, index);
		}
	}
	private final Base64Service base64Service = new Base64ServiceImpl();
	private ServiceRegistration<Base64Service> base64ServiceReg;
	private HashService hashService;
	private ServiceRegistration<HashService> hashServiceReg;

	/**
	 * Activates the service factory and automatically registers default crypto services, if default configurations are available.
	 * @param componentContext the OSGI component context of this factory
	 */
	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter da = new DictionaryAdapter(componentContext.getProperties(), true);
		keystoreLocation = da.getString("keystoreLocation");
		keystorePassword = da.getString("keystorePassword");
		keystoreType = da.getString("keystoreType");

		encryptionKeyAlias = da.getString("encryptionKeyAlias");
		encryptionKeyPassword = da.getString("encryptionKeyPassword");
		encryptionKeyAlgorithm = da.getString("encryptionKeyAlgorithm");

		signatureKeyAlias = da.getString("signatureKeyAlias");
		signatureKeyPassword = da.getString("signatureKeyPassword");
		signatureKeyAlgorithm = da.getString("signatureKeyAlgorithm");
		
		hashingIterations = da.getInteger("hashingIterations", 1000);
		hashingDefaultSaltLength = da.getInteger("hashingDefaultSaltLength", 8);
		defaultInputStringEncoding = da.getString("defaultInputStringEncoding", "UTF-8");
		messageDigestAlgorithm = da.getString("messageDigestAlgorithm", "SHA-256");
		secureRandomAlorithm = da.getString("secureRandomAlorithm", "SHA1PRNG");
		initDefaultConfigs();
		base64ServiceReg = ServiceRegistrationBuilder.newInstance(Base64Service.class, base64Service)
				.pid(Base64Service.class.getName())
				.property("name", "default")
				.register(componentContext.getBundleContext());
		hashService = createHashService();
		hashServiceReg = ServiceRegistrationBuilder.newInstance(HashService.class, hashService)
				.pid(HashService.class.getName())
				.property("name", "default")
				.register(componentContext.getBundleContext());
	}
	
	/**
	 * Deactivates the service factory and unregisters manually registered OSGI services.
	 */
	@Deactivate
	public void deactivate() {
		base64ServiceReg.unregister();
		base64ServiceReg = null;
		
		hashServiceReg.unregister();
		hashServiceReg = null;
		hashServiceReg = null;
	}

	/**
	 * Initializes the service factory in order to create default crypto service instances.
	 * If keystoreLocation, keystorePassword and keystoreType are defined, a default {@link KeystoreConfig} is created.
	 * 
	 * If encryptionKeyAlias, encryptionKeyPassword and encryptionKeyAlgorithm are defined, a default {@link KeystoreEncryptionConfig} 
	 * for a {@link SimpleCryptoService} is created, that uses the mentioned {@link KeystoreConfig}.
	 * 
	 * If signatureKeyAlias, signatureKeyPassword and signatureKeyAlgorithm are defined, a default {@link KeystoreEncryptionConfig} 
	 * for a {@link SignatureService} is created, that uses the mentioned {@link KeystoreConfig}.
	 */
	public final void initDefaultConfigs() {
		if (keystoreLocation != null && keystorePassword != null && keystoreType != null) {
			final Path path = Paths.get(keystoreLocation);
			if (Files.exists(path)) {
				final char[] passwd = keystorePassword.toCharArray();
				defaultKeystoreConfig = new KeystoreConfig() {

					@Override
					public InputStream getInputStream() {
						try {
							return new CloseOnLastByteOrExceptionInputStreamWrapper(Files.newInputStream(path, StandardOpenOption.READ));
						} catch (IOException ex) {
							throw new CryptoException("could not open keystore: " + ex.getMessage(), ex);
						}
					}

					@Override
					public char[] getPassword() {
						return passwd;
					}

					@Override
					public String getType() {
						return keystoreType;
					}
				};
			}
		}
		if (encryptionKeyAlias != null && encryptionKeyPassword != null && encryptionKeyAlgorithm != null) {
			final char[] passwd = encryptionKeyPassword.toCharArray();
			defaultEncryptionConfig = new KeystoreEncryptionConfig() {

				@Override
				public KeystoreConfig getKeystoreConfig() {
					return defaultKeystoreConfig;
				}

				@Override
				public String getKeyAlias() {
					return encryptionKeyAlias;
				}

				@Override
				public char[] getKeystoreEntryPassword() {
					return passwd;
				}

				@Override
				public String getCipherAlgorithm() {
					return encryptionKeyAlgorithm;
				}
			};
		}
		if (signatureKeyAlias != null && signatureKeyPassword != null && signatureKeyAlgorithm != null) {
			final char[] passwd = signatureKeyPassword.toCharArray();
			signatureEncryptionConfig = new KeystoreEncryptionConfig() {

				@Override
				public KeystoreConfig getKeystoreConfig() {
					return defaultKeystoreConfig;
				}

				@Override
				public String getKeyAlias() {
					return signatureKeyAlias;
				}

				@Override
				public char[] getKeystoreEntryPassword() {
					return passwd;
				}

				@Override
				public String getCipherAlgorithm() {
					return signatureKeyAlgorithm;
				}
			};
		}
	}

	@Override
	public Base64Service createBase64Service() {
		return base64Service;
	}
	
	/**
	 * Creates a hash service with the default configuration values of this factory. 
	 * @return a hash service instance
	 */
	public HashService createHashService() {
		return createHashService(new HashServiceConfigImpl(defaultInputStringEncoding, messageDigestAlgorithm, secureRandomAlorithm, hashingDefaultSaltLength, hashingIterations));
	}

	@Override
	public HashService createHashService(HashServiceConfig hashServiceConfig) {
		return new HashServiceImpl(createBase64Service(), hashServiceConfig);
	}

	/**
	 * Creates a crypto service with the default configuration values of this factory. 
	 * @return a crypto service instance
	 * @throws CryptoException if the default {@link SimpleCryptoService} can not be created
	 */
	public SimpleCryptoService createSimpleCryptoService() throws CryptoException {
		KeyStore ks = null;
		if (KeystoreEncryptionConfig.class.isInstance(defaultEncryptionConfig)) {
			KeystoreEncryptionConfig cfg = ((KeystoreEncryptionConfig) defaultEncryptionConfig);
			ks = loadKeystoreWithConfig(cfg);
		}
		SimpleDecryptService ds = createSimpleDecryptService(defaultEncryptionConfig, ks, getKeySelectorByAlgorithm(defaultEncryptionConfig, secretKeySelector, false));
		SimpleEncryptService es = createSimpleEncryptService(defaultEncryptionConfig, ks, getKeySelectorByAlgorithm(defaultEncryptionConfig, secretKeySelector, true));
		SimpleCryptoServiceImpl simpleCryptoService = new SimpleCryptoServiceImpl(ds, es);
		return simpleCryptoService;
	}
	
	@Override
	public SimpleCryptoService createSimpleCryptoService(EncryptionConfig encryptionConfig) throws CryptoException {
		KeyStore ks = null;
		if (KeystoreEncryptionConfig.class.isInstance(encryptionConfig)) {
			KeystoreEncryptionConfig cfg = ((KeystoreEncryptionConfig) encryptionConfig);
			ks = loadKeystoreWithConfig(cfg);
		}
		SimpleDecryptService ds = createSimpleDecryptService(encryptionConfig, ks, getKeySelectorByAlgorithm(encryptionConfig, secretKeySelector, false));
		SimpleEncryptService es = createSimpleEncryptService(encryptionConfig, ks, getKeySelectorByAlgorithm(encryptionConfig, secretKeySelector, true));
		SimpleCryptoServiceImpl simpleCryptoService = new SimpleCryptoServiceImpl(ds, es);
		return simpleCryptoService;
	}

	private KeyStore loadKeystoreWithConfig(KeystoreEncryptionConfig cfg) throws CryptoException {
		KeystoreConfig keystoreConfig = cfg.getKeystoreConfig();
		KeyStore ks;
		try {
			ks = KeyStore.getInstance(keystoreConfig.getType());
			InputStream stream = keystoreConfig.getInputStream();
			if (stream == null) {
				throw new CryptoException("KeystoreEncryptionConfig did not provide a stream with raw keystore data.");
			}
			ks.load(stream, keystoreConfig.getPassword());
		} catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException ex) {
			throw new CryptoException("failed creating a keystore: " + ex.getMessage(), ex);
		}
		return ks;
	}

	private Key getPublicKeyFromKeystore(KeyStore keystore, KeystoreEncryptionConfig encryptionConfig) {
		PublicKey publicKey;
		try {
			boolean isCertificate = keystore.isCertificateEntry(encryptionConfig.getKeyAlias());
			boolean isKey = keystore.isKeyEntry(encryptionConfig.getKeyAlias());
			if (isKey) {
				KeyStore.ProtectionParameter protection = createProtectionParameterFromConfig(encryptionConfig);
				KeyStore.Entry entry = keystore.getEntry(encryptionConfig.getKeyAlias(), protection);
				if (PrivateKeyEntry.class.isInstance(entry)) {
					PrivateKeyEntry pk = (PrivateKeyEntry) entry;
					publicKey = pk.getCertificate().getPublicKey();
				} else {
					throw new CryptoException("keystore entry was not supported to look up a public key");
				}
			} else if (isCertificate) {
				Certificate cert = keystore.getCertificate(encryptionConfig.getKeyAlias());
				publicKey = cert.getPublicKey();
			} else {
				throw new CryptoException("keystore entry is not a key and not a certificate");
			}
		} catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
			throw new CryptoException("failed to get entry from keystore: " + ex.getMessage(), ex);
		}
		return publicKey;
	}

	private KeyStore.ProtectionParameter createProtectionParameterFromConfig(KeystoreEncryptionConfig encryptionConfig) {
		KeyStore.ProtectionParameter protection;
		char[] entryPassword = encryptionConfig.getKeystoreEntryPassword();
		if (entryPassword != null) {
			protection = new KeyStore.PasswordProtection(entryPassword);
		} else {
			protection = null;
		}
		return protection;
	}

	private Key getPrivateKeyFromKeystore(KeyStore keystore, KeystoreEncryptionConfig encryptionConfig) {
		PrivateKey privateKey;
		try {
			KeyStore.ProtectionParameter protection = createProtectionParameterFromConfig(encryptionConfig);
			KeyStore.Entry entry = keystore.getEntry(encryptionConfig.getKeyAlias(), protection);
			if (PrivateKeyEntry.class.isInstance(entry)) {
				PrivateKeyEntry pk = (PrivateKeyEntry) entry;
				privateKey = pk.getPrivateKey();
			} else {
				throw new CryptoException("keystore entry was not supported to look up a private key");
			}
		} catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
			throw new CryptoException("failed to get entry from keystore: " + ex.getMessage(), ex);
		}
		return privateKey;
	}
	
	private Key getSecretKeyFromKeystore(KeyStore keystore, KeystoreEncryptionConfig encryptionConfig) {
		SecretKey secretKey;
		try {
			KeyStore.ProtectionParameter protection = createProtectionParameterFromConfig(encryptionConfig);
			KeyStore.Entry entry = keystore.getEntry(encryptionConfig.getKeyAlias(), protection);
			if (KeyStore.SecretKeyEntry.class.isInstance(entry)) {
				KeyStore.SecretKeyEntry sk = (KeyStore.SecretKeyEntry) entry;
				secretKey = sk.getSecretKey();
			} else {
				throw new CryptoException("keystore entry was not supported to look up a secret key");
			}
		} catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
			throw new CryptoException("failed to get entry from keystore: " + ex.getMessage(), ex);
		}
		return secretKey;
	}

	@Override
	public SignatureCreationService createSignatureCreationService(EncryptionConfig encryptionConfig) throws CryptoException {
		KeyStore ks = null;
		if (KeystoreEncryptionConfig.class.isInstance(encryptionConfig)) {
			KeystoreEncryptionConfig cfg = ((KeystoreEncryptionConfig) encryptionConfig);
			ks = loadKeystoreWithConfig(cfg);
		}
		final SimpleEncryptService es = createSimpleEncryptService(encryptionConfig, ks, getKeySelectorByAlgorithm(encryptionConfig, privateKeySelector, true));
		final Base64Service base64Service = createBase64Service();
		return new SignatureCreationService() {
			@Override
			public SignatureResult createSignatureOf(byte[] hash) throws CryptoException {
				return createSignatureResult(base64Service, es.encode(hash));
			}
		};
	}

	@Override
	public SignatureValidationService createSignatureValidationService(EncryptionConfig encryptionConfig) throws CryptoException {
		KeyStore ks = null;
		if (KeystoreEncryptionConfig.class.isInstance(encryptionConfig)) {
			KeystoreEncryptionConfig cfg = ((KeystoreEncryptionConfig) encryptionConfig);
			ks = loadKeystoreWithConfig(cfg);
		}
		final SimpleDecryptService ds = createSimpleDecryptService(encryptionConfig, ks, publicKeySelector);
		final Base64Service base64Service = createBase64Service();
		return new SignatureValidationService() {
			@Override
			public void testSignatureWithHash(String signatureBase64, String hashBase64) throws SignatureMismatchException, CryptoException {
				testSignatureWithHash(base64Service.base64Decode(signatureBase64), base64Service.base64Decode(hashBase64));
			}

			@Override
			public void testSignatureWithHash(String signatureBase64, byte[] hash) throws SignatureMismatchException, CryptoException {
				testSignatureWithHash(base64Service.base64Decode(signatureBase64), hash);
			}

			@Override
			public void testSignatureWithHash(byte[] signature, String hashBase64) throws SignatureMismatchException, CryptoException {
				testSignatureWithHash(signature, base64Service.base64Decode(hashBase64));
			}

			@Override
			public void testSignatureWithHash(byte[] signature, byte[] hash) throws SignatureMismatchException, CryptoException {
				byte[] decoded = ds.decodeToBytes(signature);
				if (decoded.length != hash.length) {
					throw new SignatureMismatchException("wrong length");
				}
				for (int i = 0; i < decoded.length; i++) {
					if (decoded[i] != hash[i]) {
						throw new SignatureMismatchException("mismatch at byte " + i);
					}
				}
			}
		};
	}

	private SignatureResult createSignatureResult(final Base64Service base64Service, final byte[] encoded) throws CryptoException {
		return new SignatureResult() {
			private String base64ed;

			@Override
			public byte[] getSignature() {
				return encoded;
			}

			@Override
			public String getSignatureBase64() {
				if (base64ed == null) {
					base64ed = base64Service.base64Encode(getSignature());
				}
				return base64ed;
			}
		};
	}

	/**
	 * Creates a signature service with the default configuration values of this factory.
	 * @return a signature service instance
	 */
	public SignatureService createSignatureService() {
		return createSignatureService(signatureEncryptionConfig);
	}
	
	@Override
	public SignatureService createSignatureService(EncryptionConfig encryptionConfig) {
		final SignatureCreationService signatureCreationService = createSignatureCreationService(encryptionConfig);
		final SignatureValidationService signatureValidationService = createSignatureValidationService(encryptionConfig);
		return new SignatureService() {
			@Override
			public SignatureResult createSignatureOf(byte[] bytes) throws CryptoException {
				return signatureCreationService.createSignatureOf(bytes);
			}

			@Override
			public void testSignatureWithHash(String signatureBase64, String hashBase64) throws SignatureMismatchException, CryptoException {
				signatureValidationService.testSignatureWithHash(signatureBase64, hashBase64);
			}

			@Override
			public void testSignatureWithHash(String signatureBase64, byte[] hash) throws SignatureMismatchException, CryptoException {
				signatureValidationService.testSignatureWithHash(signatureBase64, hash);
			}

			@Override
			public void testSignatureWithHash(byte[] signature, String hashBase64) throws SignatureMismatchException, CryptoException {
				signatureValidationService.testSignatureWithHash(signature, hashBase64);
			}

			@Override
			public void testSignatureWithHash(byte[] signature, byte[] hash) throws SignatureMismatchException, CryptoException {
				signatureValidationService.testSignatureWithHash(signature, hash);
			}
			
		};
	}
	
	@Override
	public SimpleDecryptService createSimpleDecryptService(EncryptionConfig encryptionConfig) throws CryptoException {
		return createSimpleDecryptService(encryptionConfig, null, secretKeySelector);
	}

	private SimpleDecryptService createSimpleDecryptService(
			final EncryptionConfig encryptionConfig,
			final KeyStore keyStore,
			final KeyStoreKeySelector keyStoreKeySelector
	) throws CryptoException {
		CipherProvider cipherProvider = new CipherProvider() {
			
			@Override
			public CipherInfo getCipher() {
				final Cipher cipher = createCipherStub(encryptionConfig);
				Key secretKey = dealWithEncryptionConfig(encryptionConfig, new EncryptionConfigHandler<Key>() {

					@Override
					public Key doWith(KeystoreEncryptionConfig keystoreEncryptionConfig) {
						KeyStore ks = keyStore;
						if (ks == null) {
							ks = loadKeystoreWithConfig(keystoreEncryptionConfig);
						}
						return keyStoreKeySelector.selectKey(ks, keystoreEncryptionConfig);
					}

					@Override
					public Key doWith(ProvidedKeyEncryptionConfig providedKeyEncryptionConfig) {
						return providedKeyEncryptionConfig.getKey();
					}

					@Override
					public Key doWith(ProvidedKeyPairEncryptionConfig providedKeyPairEncryptionConfig) {
						return providedKeyPairEncryptionConfig.getPublicKey();
					}
				});
				try {
					cipher.init(Cipher.DECRYPT_MODE, secretKey);
				} catch (InvalidKeyException ex) {
					throw new CryptoException("provided key is bad: " + ex.getMessage(), ex);
				}
				final int blockSize;
				if (BlockBasedEncryptionConfig.class.isInstance(encryptionConfig)) {
					blockSize = ((BlockBasedEncryptionConfig)encryptionConfig).getBlockSize();
				} else {
					if (RSAKey.class.isInstance(secretKey)) {
						blockSize = ((RSAKey) secretKey).getModulus().bitLength() / 8;
					} else {
						blockSize = -1;
					}
				}
				return new CipherInfo() {
					@Override
					public Cipher getCipher() {
						return cipher;
					}

					@Override
					public int getBlockSize() {
						return blockSize;
					}
				};
			}
		};
		String alg = encryptionConfig.getCipherAlgorithm();
		if (RSA_TESTER.matches(alg)) {
			return new BlockBasedDecryptServiceImpl(cipherProvider, this);
		} else {
			return new SimpleDecryptServiceImpl(cipherProvider, this);
		}
	}

	@Override
	public SimpleEncryptService createSimpleEncryptService(EncryptionConfig encryptionConfig) throws CryptoException {
		return createSimpleEncryptService(encryptionConfig, null, getKeySelectorByAlgorithm(encryptionConfig, secretKeySelector, true));
	}
	
	private static interface KeyStoreKeySelector {
		Key selectKey(KeyStore ks, KeystoreEncryptionConfig keystoreEncryptionConfig);
	}

	private final KeyStoreKeySelector secretKeySelector = new KeyStoreKeySelector() {
		@Override
		public Key selectKey(KeyStore ks, KeystoreEncryptionConfig keystoreEncryptionConfig) {
			return getSecretKeyFromKeystore(ks, keystoreEncryptionConfig);
		}
	};
	
	private final KeyStoreKeySelector privateKeySelector = new KeyStoreKeySelector() {
		@Override
		public Key selectKey(KeyStore ks, KeystoreEncryptionConfig keystoreEncryptionConfig) {
			return getPrivateKeyFromKeystore(ks, keystoreEncryptionConfig);
		}
	};
	
	private final KeyStoreKeySelector publicKeySelector = new KeyStoreKeySelector() {
		@Override
		public Key selectKey(KeyStore ks, KeystoreEncryptionConfig keystoreEncryptionConfig) {
			return getPublicKeyFromKeystore(ks, keystoreEncryptionConfig);
		}
	};
	
	private SimpleEncryptService createSimpleEncryptService(
			final EncryptionConfig encryptionConfig,
			final KeyStore keyStore,
			final KeyStoreKeySelector keyStoreKeySelector
	) throws CryptoException {
		CipherProvider cipherProvider = new CipherProvider() {

			@Override
			public CipherInfo getCipher() {
				final Cipher cipher = createCipherStub(encryptionConfig);
				Key secretKey = dealWithEncryptionConfig(encryptionConfig, new EncryptionConfigHandler<Key>() {

					@Override
					public Key doWith(KeystoreEncryptionConfig keystoreEncryptionConfig) {
						KeyStore ks = keyStore;
						if (ks == null) {
							ks = loadKeystoreWithConfig(keystoreEncryptionConfig);
						}
						return keyStoreKeySelector.selectKey(ks, keystoreEncryptionConfig);
					}

					@Override
					public Key doWith(ProvidedKeyEncryptionConfig providedKeyEncryptionConfig) {
						return providedKeyEncryptionConfig.getKey();
					}

					@Override
					public Key doWith(ProvidedKeyPairEncryptionConfig providedKeyPairEncryptionConfig) {
						return providedKeyPairEncryptionConfig.getPrivateKey();
					}
				});
				try {
					cipher.init(Cipher.ENCRYPT_MODE, secretKey);
				} catch (InvalidKeyException ex) {
					throw new CryptoException("provided key is bad: " + ex.getMessage(), ex);
				}
				final int blockSize;
				if (BlockBasedEncryptionConfig.class.isInstance(encryptionConfig)) {
					blockSize = ((BlockBasedEncryptionConfig)encryptionConfig).getMaxDataBlockSize();
				} else {
					if (RSAKey.class.isInstance(secretKey)) {
						blockSize = ((RSAKey) secretKey).getModulus().bitLength() / 8;
					} else {
						blockSize = -1;
					}
				}
				return new CipherInfo() {
					@Override
					public Cipher getCipher() {
						return cipher;
					}

					@Override
					public int getBlockSize() {
						return blockSize;
					}
				};
			}
		};
		String alg = encryptionConfig.getCipherAlgorithm();
		if (RSA_TESTER.matches(alg)) {
			return new BlockBasedEncryptServiceImpl(cipherProvider, this);
		} else {
			return new SimpleEncryptServiceImpl(cipherProvider, this);
		}
	}

	private Cipher createCipherStub(EncryptionConfig encryptionConfig) {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(encryptionConfig.getCipherAlgorithm());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
			throw new CryptoException("could not create cipher: " + ex.getMessage(), ex);
		}
		return cipher;
	}

	private <E> E dealWithEncryptionConfig(EncryptionConfig encryptionConfig, EncryptionConfigHandler<E> handler) {
		if (KeystoreEncryptionConfig.class.isInstance(encryptionConfig)) {
			return handler.doWith((KeystoreEncryptionConfig) encryptionConfig);
		} else if (ProvidedKeyEncryptionConfig.class.isInstance(encryptionConfig)) {
			return handler.doWith((ProvidedKeyEncryptionConfig) encryptionConfig);
		} else if (ProvidedKeyPairEncryptionConfig.class.isInstance(encryptionConfig)) {
			return handler.doWith((ProvidedKeyPairEncryptionConfig) encryptionConfig);
		} else {
			throw new IllegalArgumentException("unsupported encryption config");
		}
	}
	
	private KeyStoreKeySelector getKeySelectorByAlgorithm(EncryptionConfig encryptionConfig, KeyStoreKeySelector defaultKeyStoreKeySelector, boolean isEncryption) {
		String algorithm = encryptionConfig.getCipherAlgorithm();
		boolean usePrivateKeyForEncryption = true;
		if (AsymetricEncryptionConfig.class.isInstance(encryptionConfig)) {
			usePrivateKeyForEncryption = ((AsymetricEncryptionConfig)encryptionConfig).isPrivateKeyUsedForEncryption();
		}
		if (RSA_TESTER.matches(algorithm)) {
			if (isEncryption) {
				if (usePrivateKeyForEncryption) {
					return privateKeySelector;
				} else {
					return publicKeySelector;
				}
			} else {
				if (usePrivateKeyForEncryption) {
					return publicKeySelector;
				} else {
					return privateKeySelector;
				}
			}
		} else if (AES_TESTER.matches(algorithm)) {
			return secretKeySelector;
		} else {
			return defaultKeyStoreKeySelector;
		}
	}

	/**
	 * Gets the configuration value for the default string encoding, when strings should be converted to byte arrays or when byte arrays should be converted to strings.
	 * @return a character encoding
	 */
	public String getDefaultInputStringEncoding() {
		return defaultInputStringEncoding;
	}

	/**
	 * Sets the algorithm for creating secure randoms, that will be used as salts for hashing.
	 * @param secureRandomAlorithm an algorithm for creating secure randoms
	 */
	public void setSecureRandomAlorithm(String secureRandomAlorithm) {
		this.secureRandomAlorithm = secureRandomAlorithm;
	}

	/**
	 * Sets the hashing algorithm.
	 * @param messageDigestAlgorithm an algorithm for hashing
	 */
	public void setMessageDigestAlgorithm(String messageDigestAlgorithm) {
		this.messageDigestAlgorithm = messageDigestAlgorithm;
	}

	/**
	 * Sets the configuration value for the default string encoding, when strings should be converted to byte arrays or when byte arrays should be converted to strings.
	 * @param defaultInputStringEncoding a character encoding
	 */
	public void setDefaultInputStringEncoding(String defaultInputStringEncoding) {
		this.defaultInputStringEncoding = defaultInputStringEncoding;
	}

	/**
	 * Sets the number for hashing iterations, that should be applied on the first hash.
	 * @param hashingIterations a value &gt;= 0
	 */
	public void setHashingIterations(int hashingIterations) {
		this.hashingIterations = hashingIterations;
	}

	/**
	 * Sets the length for automatically created salts. The value should be bigger than 0.
	 * @param defaultSaltLength the length of automatically created salts
	 */
	public void setDefaultSaltLength(int defaultSaltLength) {
		this.hashingDefaultSaltLength = defaultSaltLength;
	}

	/**
	 * Sets the type of the keystore, that contains keys for encryption, decryption and signature creation/validation.
	 * @param keystoreType the keystore type
	 */
	public void setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
	}

	/**
	 * Sets the password, to access the keystore, that contains keys for encryption, decryption and signature creation/validation.
	 * @param keystorePassword the keystore password
	 */
	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	/**
	 * Sets the location to the keystore, that contains keys for encryption, decryption and signature creation/validation.
	 * @param keystoreLocation file system path to a keystore
	 */
	public void setKeystoreLocation(String keystoreLocation) {
		this.keystoreLocation = keystoreLocation;
	}

	/**
	 * Sets the password to access the encryption/decryption key in the keystore.
	 * @param encryptionKeyPassword  the password to access the encryption/decryption key
	 */
	public void setEncryptionKeyPassword(String encryptionKeyPassword) {
		this.encryptionKeyPassword = encryptionKeyPassword;
	}

	/**
	 * Sets the key name of the encyption/decryption key in the keystore.
	 * @param encryptionKeyAlias the key name of the encyption/decryption key
	 */
	public void setEncryptionKeyAlias(String encryptionKeyAlias) {
		this.encryptionKeyAlias = encryptionKeyAlias;
	}

	/**
	 * Sets the algorithm of the key in the keystore, that is used for encryption/decryption.
	 * @param encryptionKeyAlgorithm encryption/decryption algorithm
	 */
	public void setEncryptionKeyAlgorithm(String encryptionKeyAlgorithm) {
		this.encryptionKeyAlgorithm = encryptionKeyAlgorithm;
	}

	/**
	 * Sets the password to access the signature creation/validation key in the keystore.
	 * @param signatureKeyPassword the password of the signature creation/validation key
	 */
	public void setSignatureKeyPassword(String signatureKeyPassword) {
		this.signatureKeyPassword = signatureKeyPassword;
	}

	/**
	 * Sets the key name of the signature creation/validation key in the keystore.
	 * @param signatureKeyAlias the key name of the signature creation/validation key
	 */
	public void setSignatureKeyAlias(String signatureKeyAlias) {
		this.signatureKeyAlias = signatureKeyAlias;
	}

	/**
	 * Sets the algorithm of the signature creation/validation key in the keystore.
	 * @param signatureKeyAlgorithm the algorithm of the signature creation/validation key
	 */
	public void setSignatureKeyAlgorithm(String signatureKeyAlgorithm) {
		this.signatureKeyAlgorithm = signatureKeyAlgorithm;
	}

}
