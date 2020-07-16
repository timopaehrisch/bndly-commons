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

import org.bndly.ssl.api.KeyStoreProvider;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = HttpClientFactory.class, immediate = true)
public class HttpClientFactory {

	private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);
	private ComponentContext componentContext;

	private class ConfiguredHttpClient {

		private final HttpClientConfiguration configuration;
		private ServiceRegistration reg;
		private org.apache.http.client.HttpClient client;
		private org.apache.http.impl.conn.PoolingHttpClientConnectionManager connectionManager;

		public ConfiguredHttpClient(HttpClientConfiguration configuration) {
			if (configuration == null) {
				throw new IllegalArgumentException("configuration is not allowed to be null");
			}
			this.configuration = configuration;
		}
		
		private void initClient() {
			SSLConnectionSocketFactory sslConnectionFactory = null;
			KeyStoreProvider trustStoreProvider = configuration.getTrustStoreProvider();
			KeyStoreProvider keyStoreProvider = configuration.getKeyStoreProvider();
			sslConnectionFactory = Apache43ClientFactory.createSSLConnectionSocketFactory(
				keyStoreProvider, trustStoreProvider, configuration.getSecureSocketProtocolName(), configuration.getIgnoreSSLHostNames()
			);
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(configuration.getConnectionTimeout())
					.setConnectionRequestTimeout(configuration.getConnectionRequestTimeout())
					.setRedirectsEnabled(configuration.getRedirectsEnabled())
					.setSocketTimeout(configuration.getConnectionTimeout())
					.build();
			org.apache.http.impl.client.HttpClientBuilder builder = org.apache.http.impl.client.HttpClientBuilder.create()
					.setDefaultRequestConfig(requestConfig);
			connectionManager = Apache43ClientFactory.setupPoolingHttpClientConnectionManager(builder, sslConnectionFactory);
			connectionManager.setMaxTotal(configuration.getMaxConnections());
			connectionManager.setDefaultMaxPerRoute(configuration.getMaxConnectionsPerHost());
			SocketConfig socketConfig = SocketConfig.custom()
					.setSoKeepAlive(configuration.getConnectionKeepAlive())
					.setSoTimeout(configuration.getConnectionTimeout())
					.build();
			connectionManager.setDefaultSocketConfig(socketConfig);
			client = builder.build();
		}

		private void init() {
			if (componentContext == null) {
				lock.writeLock().lock();
				try {
					lazyInits.add(new Runnable() {

						@Override
						public void run() {
							init();
						}
					});
				} finally {
					lock.writeLock().unlock();
				}
			} else {
				initClient();
				Dictionary props = new Hashtable<>();
				props.put("service.pid", org.apache.http.client.HttpClient.class.getName() + "." + configuration.getName());
				reg = componentContext.getBundleContext().registerService(org.apache.http.client.HttpClient.class, client, props);
			}
		}

		private void destroy() {
			if (reg != null) {
				reg.unregister();
				reg = null;
			}
			if (client != null) {
				client = null;
			}
			if (connectionManager != null) {
				connectionManager.close();
				connectionManager = null;
			}
		}

	}

	private final List<ConfiguredHttpClient> configurations = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final List<Runnable> lazyInits = new ArrayList<>();

	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
		lock.writeLock().lock();
		try {
			for (Runnable lazyInit : lazyInits) {
				lazyInit.run();
			}
			lazyInits.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		lock.writeLock().lock();
		try {
			for (ConfiguredHttpClient configuration : configurations) {
				configuration.destroy();
			}
			configurations.clear();
			lazyInits.clear();
		} finally {
			lock.writeLock().unlock();
		}
		this.componentContext = null;
	}

	public HttpClient createClientFromConfiguration(HttpClientConfiguration configuration) {
		ConfiguredHttpClient configuredHttpClient = new ConfiguredHttpClient(configuration);
		configuredHttpClient.initClient();
		return configuredHttpClient.client;
	}
	
	@Reference(
			bind = "addHttpClientConfiguration",
			unbind = "removeHttpClientConfiguration",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = HttpClientConfiguration.class
	)
	public void addHttpClientConfiguration(HttpClientConfiguration configuration) {
		if (configuration != null) {
			ConfiguredHttpClient client = new ConfiguredHttpClient(configuration);
			lock.writeLock().lock();
			try {
				configurations.add(client);
			} finally {
				lock.writeLock().unlock();
			}
			client.init();
		}
	}

	public void removeHttpClientConfiguration(HttpClientConfiguration configuration) {
		if (configuration != null) {
			lock.writeLock().lock();
			try {
				Iterator<ConfiguredHttpClient> iterator = configurations.iterator();
				while (iterator.hasNext()) {
					ConfiguredHttpClient next = iterator.next();
					if (next.configuration == configuration) {
						next.destroy();
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
}
