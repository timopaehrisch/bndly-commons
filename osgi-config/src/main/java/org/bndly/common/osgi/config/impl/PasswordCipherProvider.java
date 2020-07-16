package org.bndly.common.osgi.config.impl;

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
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(immediate = true)
public class PasswordCipherProvider implements CipherProvider {

	private static final String ALIAS = "PWD";

	private SecretKeySpec secret;
	private String cipherTransformation;
	private ServiceRegistration<CipherProvider> reg;

	@Activate
	public void activate(BundleContext bundleContext) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String prefix = "org.bndly.common.osgi.config.impl.PasswordCipherProvider.";
		String password = getString(bundleContext, prefix + "password", null);
		if (password == null) {
			return;
		}
		String salt64 = getString(bundleContext, prefix + "salt", null);
		if (salt64 == null) {
			return;
		}

		byte[] salt = Base64Util.decode(salt64);

		String algorithm = getString(bundleContext, prefix + "keyFactoryAlgorithm", "PBKDF2WithHmacSHA256");

		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);

		int iterations = getInt(bundleContext, prefix + "iterations", 65536);
		int length = getInt(bundleContext, prefix + "keySize", 128);

		KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, length);
		SecretKey tmp = factory.generateSecret(spec);
		String secretKeyAlgorithm = getString(bundleContext, prefix + "secretKeyAlgorithm", "AES");
		secret = new SecretKeySpec(tmp.getEncoded(), secretKeyAlgorithm);
		cipherTransformation = getString(bundleContext, prefix + "cipherTransformation", "AES/CBC/PKCS5Padding");
		if (restoreEncryptionCipher(ALIAS) != null) {
			// seems to work, so we register the component as a service
			reg = ServiceRegistrationBuilder.newInstance(CipherProvider.class, this)
					.pid(getClass().getName())
					.register(bundleContext);
		}
	}

	@Deactivate
	public void deactivate(BundleContext bundleContext) {
		if (reg != null) {
			reg.unregister();
		}
	}

	private String getString(BundleContext bundleContext, String key, String defaultValue) {
		String property = bundleContext.getProperty(key);
		if (property == null) {
			return defaultValue;
		}
		return property;
	}

	private int getInt(BundleContext bundleContext, String key, int defaultValue) {
		String property = bundleContext.getProperty(key);
		if (property == null) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(property);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public String getAlias() {
		return ALIAS;
	}

	@Override
	public Cipher restoreDecryptionCipher(String alias, String initVectorBase64) {
		if (!ALIAS.equals(alias)) {
			return null;
		}
		try {
			byte[] iv = Base64Util.decode(initVectorBase64);
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			return cipher;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			return null;
		}
	}

	@Override
	public Cipher restoreEncryptionCipher(String alias) {
		if (!ALIAS.equals(alias)) {
			return null;
		}
		try {
			Cipher cipher = Cipher.getInstance(cipherTransformation);
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			return cipher;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			return null;
		}
	}

}
