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
import org.bndly.ssl.api.KeyStoreProvider;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Apache43ClientFactory {

	private static final Logger LOG = LoggerFactory.getLogger(Apache43ClientFactory.class);
	
	public HttpClient build(final KeyStoreAccessProvider keyStoreAccessProvider, boolean developmentMode) {
		org.apache.http.impl.client.HttpClientBuilder builder = org.apache.http.impl.client.HttpClientBuilder.create();
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
		final KeyStore finalKeystore = keystore;
		KeyStoreProvider provider = new KeyStoreProvider() {
			
			@Override
			public String getKeyStoreAccessPassword() {
				return keyStoreAccessProvider.getKeyStoreAccessPassword();
			}

			@Override
			public KeyStore getKeyStore() {
				return finalKeystore;
			}
		};
		SSLConnectionSocketFactory sslsf = createSSLConnectionSocketFactory(provider, provider, keyStoreAccessProvider.getSecureSocketProtocol(), developmentMode);
		setupPoolingHttpClientConnectionManager(builder, sslsf);
		org.apache.http.impl.client.CloseableHttpClient client = builder.build();
		return client;
	}
	
	public static KeyManagerFactory createKeyManagerFactory(KeyStoreProvider keyManagerKeyStoreProvider) {
		if (keyManagerKeyStoreProvider == null) {
			return null;
		}
		KeyStore keyStore = keyManagerKeyStoreProvider.getKeyStore();
		if (keyStore == null) {
			return null;
		}
		try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			char[] password;
			if (keyManagerKeyStoreProvider.getKeyStoreAccessPassword() == null) {
				password = null;
			} else {
				password = keyManagerKeyStoreProvider.getKeyStoreAccessPassword().toCharArray();
			}
			kmf.init(keyStore, password);
			return kmf;
		} catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException ex) {
			throw new RuntimeException("could not set up key manager factory: " + ex.getMessage(), ex);
		}
	}
	
	public static TrustManagerFactory createX509TrustManagerFactory(KeyStoreProvider trustManagerKeyStoreProvider) {
		if (trustManagerKeyStoreProvider == null) {
			return null;
		}
		KeyStore keyStore = trustManagerKeyStoreProvider.getKeyStore();
		if (keyStore == null) {
			return null;
		}
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(keyStore);
			return tmf;
		} catch (NoSuchAlgorithmException | KeyStoreException ex) {
			throw new RuntimeException("could not set up trust manager factory: " + ex.getMessage(), ex);
		}
	}
	
	public static SSLConnectionSocketFactory createSSLConnectionSocketFactory(
			KeyStoreProvider keyStoreProvider, KeyStoreProvider trustStoreProvider, String secureSocketProtocolName, boolean developmentMode
	) {
		return createSSLConnectionSocketFactory(createKeyManagerFactory(keyStoreProvider), createX509TrustManagerFactory(trustStoreProvider), secureSocketProtocolName, developmentMode);
	}

	public static SSLConnectionSocketFactory createSSLConnectionSocketFactory(
			KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory, String secureSocketProtocolName, boolean developmentMode
	) {
		if (trustManagerFactory == null || secureSocketProtocolName == null) {
			return null;
		}
		try {
			SSLContext context = SSLContext.getInstance(secureSocketProtocolName);
			context.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			SSLConnectionSocketFactory sslsf;
			if (developmentMode) {
				org.apache.http.conn.ssl.X509HostnameVerifier hnv = new DevelopmentX509HostnameVerifier();
				sslsf = new SSLConnectionSocketFactory(context, hnv);
			} else {
				sslsf = new SSLConnectionSocketFactory(context);
			}
			return sslsf;
		} catch (Exception ex) {
			throw new RuntimeException("could not setup SSL: " + ex.getMessage(), ex);
		}
	}

	public static void setupPoolingHttpClientConnectionManager(org.apache.http.impl.client.HttpClientBuilder builder) {
		setupPoolingHttpClientConnectionManager(builder, null);
	}
	
	public static PoolingHttpClientConnectionManager setupPoolingHttpClientConnectionManager(org.apache.http.impl.client.HttpClientBuilder builder, SSLConnectionSocketFactory sslsf) {
		PoolingHttpClientConnectionManager connectionManager;
		if (sslsf != null) {
			SchemePortResolver portResolver = new DefaultSchemePortResolver() {
				@Override
				public int resolve(HttpHost host) throws UnsupportedSchemeException {
					if ("https".equals(host.getSchemeName())) {
						return 443;
					} else {
						return super.resolve(host);
					}
				}
			};
			builder.setSchemePortResolver(portResolver);
			
			builder.setSSLSocketFactory(sslsf);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslsf)
					.register("http", new PlainConnectionSocketFactory())
					.build();
			connectionManager = new org.apache.http.impl.conn.PoolingHttpClientConnectionManager(socketFactoryRegistry);
		} else {
			connectionManager = new org.apache.http.impl.conn.PoolingHttpClientConnectionManager();
		}
		builder.setConnectionManager(connectionManager);
		return connectionManager;
	}
}
