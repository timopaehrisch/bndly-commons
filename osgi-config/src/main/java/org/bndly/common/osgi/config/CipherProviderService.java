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

import java.util.Iterator;
import java.util.ServiceLoader;
import org.bndly.common.osgi.config.spi.CipherProvider;
import javax.crypto.Cipher;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CipherProviderService implements CipherProvider {

	private static CipherProviderService service;

	private CipherProviderService() {
	}

	public static synchronized CipherProviderService getInstance() {
		if (service == null) {
			service = new CipherProviderService();
		}
		return service;
	}

	@Override
	public Cipher restoreDecryptionCipher(String alias, String initVectorBase64) {
		if (alias == null) {
			return null;
		}
		Cipher cipher = null;
		ServiceLoader<CipherProvider> loader = ServiceLoader.load(CipherProvider.class);
		Iterator<CipherProvider> iter = loader.iterator();
		while (cipher == null && iter.hasNext()) {
			CipherProvider cipherProvider = iter.next();
			cipher = cipherProvider.restoreDecryptionCipher(alias, initVectorBase64);
		}
		return cipher;
	}

	@Override
	public Cipher restoreEncryptionCipher(String alias) {
		if (alias == null) {
			return null;
		}
		Cipher cipher = null;
		ServiceLoader<CipherProvider> loader = ServiceLoader.load(CipherProvider.class);
		Iterator<CipherProvider> iter = loader.iterator();
		while (cipher == null && iter.hasNext()) {
			CipherProvider cipherProvider = iter.next();
			cipher = cipherProvider.restoreEncryptionCipher(alias);
		}
		return cipher;
	}

	@Override
	public String getAlias() {
		throw new UnsupportedOperationException("getAlias should not be invoked on " + getClass().getName());
	}
	
	public CipherProvider getCipherProviderByAlias(String alias) {
		ServiceLoader<CipherProvider> loader = ServiceLoader.load(CipherProvider.class);
		Iterator<CipherProvider> iter = loader.iterator();
		while (iter.hasNext()) {
			CipherProvider cipherProvider = iter.next();
			if (cipherProvider.getAlias().equals(alias)) {
				return cipherProvider;
			}
		}
		return null;
	}
}
