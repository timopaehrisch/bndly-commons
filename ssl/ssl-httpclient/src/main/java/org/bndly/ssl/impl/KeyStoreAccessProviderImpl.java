package org.bndly.ssl.impl;

/*-
 * #%L
 * SSL HTTPClient
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

import org.bndly.ssl.api.KeyStoreAccessProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreAccessProviderImpl implements KeyStoreAccessProvider {

	private static final Logger LOG = LoggerFactory.getLogger(KeyStoreAccessProviderImpl.class);
	
	private String keyStoreLocation;
	private String keyStorePassword;
	private String secureSocketProtocol;

	@Override
	public InputStream getKeyStoreInputStream() {
		String loc = getKeyStoreLocation();
		if (loc == null || loc.isEmpty()) {
			return null;
		}
		Path f = Paths.get(loc);
		if (Files.notExists(f) || !Files.isRegularFile(f)) {
			LOG.error("key store from location does not exist or is no file: " + loc);
		}
		try {
			InputStream is = Files.newInputStream(f, StandardOpenOption.READ);
			return is;
		} catch (IOException ex) {
			LOG.error("failed to load key store from location: " + loc, ex);
			return null;
		}
	}

	@Override
	public KeyStore getKeyStore() {
		return null;
	}

	@Override
	public String getSecureSocketProtocol() {
		return secureSocketProtocol;
	}

	public void setSecureSocketProtocol(String secureSocketProtocol) {
		this.secureSocketProtocol = secureSocketProtocol;
	}

	@Override
	public String getKeyStoreAccessPassword() {
		return getKeyStorePassword();
	}

	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}

	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}
}
