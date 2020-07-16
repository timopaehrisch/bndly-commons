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
import java.io.InputStream;
import java.security.KeyStore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// a great post on SSL usage during development: http://javaskeleton.blogspot.de/2010/07/avoiding-peer-not-authenticated-with.html
public final class SSLHttpClientFactory {
	private static final Logger LOG = LoggerFactory.getLogger(SSLHttpClientFactory.class);

	public HttpClient build(KeyStoreAccessProvider keyStoreAccessProvider) {
		String prop = System.getProperty("ebp.client.ssl.developmentMode");
		boolean developmentMode = false;
		if (prop != null) {
			try {
				developmentMode = Boolean.valueOf(prop);
			} catch (Exception e) {
				// ignore this
			}
		}
		return build(keyStoreAccessProvider, developmentMode);
	}

	public HttpClient build(KeyStoreAccessProvider keyStoreAccessProvider, boolean developmentMode) {
		try {
			Class<?> cls = getClass().getClassLoader().loadClass("org.apache.http.impl.client.HttpClients");
			return new Apache43ClientFactory().build(keyStoreAccessProvider, developmentMode);
		} catch (ClassNotFoundException ex) {
			return buildLegacy(keyStoreAccessProvider, developmentMode);
		}
	}

	public HttpClient buildLegacy(KeyStoreAccessProvider keyStoreAccessProvider, boolean developmentMode) {
		HttpClient httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager());
		
		KeyStore keystore = keyStoreAccessProvider.getKeyStore();
		if (keystore == null) {
			InputStream tmpInstream = keyStoreAccessProvider.getKeyStoreInputStream();
			if (tmpInstream != null) {
				try (InputStream instream = tmpInstream) {
					keystore = KeyStore.getInstance(KeyStore.getDefaultType());
					keystore.load(instream, keyStoreAccessProvider.getKeyStoreAccessPassword().toCharArray());
				} catch (Exception ex) {
					LOG.error("failed to load keystore from provided inputstream: " + ex.getMessage(), ex);
					keystore = null;
				}
			}
		}
		SSLSetup.setupLegacy(httpclient, keystore, keyStoreAccessProvider.getSecureSocketProtocol(), developmentMode);
		return httpclient;
	}
}
