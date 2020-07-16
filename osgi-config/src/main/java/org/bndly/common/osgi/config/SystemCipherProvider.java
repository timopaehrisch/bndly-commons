package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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

import org.bndly.common.crypto.impl.shared.Base64Util;
import org.bndly.common.osgi.config.spi.CipherProvider;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemCipherProvider implements CipherProvider {

	private static final Logger LOG = LoggerFactory.getLogger(SystemCipherProvider.class);
	public static final String ALIAS = "SYS";
	
	public static final String SYS_PROP_PW = "org.bndly.common.osgi.config.SystemCipherProvider.password";
	public static final String SYS_PROP_SALT = "org.bndly.common.osgi.config.SystemCipherProvider.salt";
	public static final String SYS_PROP_PBKDF_ALG = "org.bndly.common.osgi.config.SystemCipherProvider.pbkdfalgorithm";
	public static final String SYS_PROP_ALG = "org.bndly.common.osgi.config.SystemCipherProvider.algorithm";
	public static final String SYS_PROP_CIPHER_ALG = "org.bndly.common.osgi.config.SystemCipherProvider.cipheralgorithm";
	public static final String SYS_PROP_ITERATION_COUNT = "org.bndly.common.osgi.config.SystemCipherProvider.iterationcount";
	public static final String SYS_PROP_KEYLENGTH = "org.bndly.common.osgi.config.SystemCipherProvider.keylength";
	
	private static final String PASSWORD;
	private static final byte[] SALT;
	private static final String PBKDF_ALGORITHM;
	private static final String ALGORITHM;
	private static final String CIPHER_ALGORITHM;
	private static final int ITERATION_COUNT;
	private static final int KEYLENGTH;
	private static SecretKeySpec SECRET;

	static {
		try {
			PASSWORD = System.getProperty(SYS_PROP_PW);
			String tmp = System.getProperty(SYS_PROP_SALT);
			if (tmp != null) {
				SALT = Base64Util.decode(tmp);
			} else {
				SALT = null;
			}
			
			PBKDF_ALGORITHM = System.getProperty(SYS_PROP_PBKDF_ALG, "PBKDF2WithHmacSHA256");
			ALGORITHM = System.getProperty(SYS_PROP_ALG, "AES");
			CIPHER_ALGORITHM = System.getProperty(SYS_PROP_CIPHER_ALG, "AES/CBC/PKCS5Padding");
			ITERATION_COUNT = Integer.valueOf(System.getProperty(SYS_PROP_ITERATION_COUNT, "65536"));
			KEYLENGTH = Integer.valueOf(System.getProperty(SYS_PROP_KEYLENGTH, "128"));
			
			if (PASSWORD != null && SALT != null) {
				try {
					SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
					KeySpec spec = new PBEKeySpec(PASSWORD.toCharArray(), SALT, ITERATION_COUNT, KEYLENGTH);
					SecretKey tmp2 = factory.generateSecret(spec);
					SECRET = new SecretKeySpec(tmp2.getEncoded(), ALGORITHM);
				} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
					LOG.error("could not set up secret", e);
				}
			}
		} finally {
			// once the class is loaded, remove the password and salt from the system properties.
			// note: the password and salt might still be available in the start command.
			Properties properties = System.getProperties();
			if (properties != null) {
				properties.remove(SYS_PROP_PW);
				properties.remove(SYS_PROP_SALT);
				properties.remove(SYS_PROP_PBKDF_ALG);
				properties.remove(SYS_PROP_ALG);
				properties.remove(SYS_PROP_CIPHER_ALG);
				properties.remove(SYS_PROP_ITERATION_COUNT);
				properties.remove(SYS_PROP_KEYLENGTH);
			}
		}
	}

	@Override
	public final String getAlias() {
		return ALIAS;
	}

	@Override
	public Cipher restoreDecryptionCipher(String alias, String initVectorBase64) {
		if (!ALIAS.equals(alias)) {
			return null;
		}
		if (SECRET != null) {
			try {
				Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
				if (initVectorBase64 != null) {
					cipher.init(Cipher.DECRYPT_MODE, SECRET, new IvParameterSpec(Base64Util.decode(initVectorBase64)));
				} else {
					cipher.init(Cipher.DECRYPT_MODE, SECRET);
				}
				return cipher;
			} catch (
					NoSuchAlgorithmException 
					| NoSuchPaddingException 
					| InvalidAlgorithmParameterException 
					| InvalidKeyException ex
			) {
				LOG.error("could not set up cipher", ex);
			}
		}
		return null;
	}

	@Override
	public Cipher restoreEncryptionCipher(String alias) {
		if (!ALIAS.equals(alias)) {
			return null;
		}
		if (SECRET != null) {
			try {
				Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
				cipher.init(Cipher.ENCRYPT_MODE, SECRET);
				return cipher;
			} catch (
					NoSuchAlgorithmException 
					| NoSuchPaddingException 
					| InvalidKeyException ex
			) {
				LOG.error("could not set up cipher", ex);
			}
		}
		return null;
	}

}
