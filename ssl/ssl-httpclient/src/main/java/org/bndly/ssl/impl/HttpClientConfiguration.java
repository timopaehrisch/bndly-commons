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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.ssl.api.KeyStoreProvider;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.util.Dictionary;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = HttpClientConfiguration.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = HttpClientConfiguration.Configuration.class)
public class HttpClientConfiguration {
	
	@ObjectClassDefinition(
		name = "Http Client Configuration",
		description = "The Http Client configuration is used to set up an Apache Http Client with a predefined configuration."
	)
	public @interface Configuration {
		@AttributeDefinition(
			name = "Name of this config",
			description = "The name under which the client shall be registered in the OSGI container"
		)
		String name() default "default";
		
		@AttributeDefinition(
			name = "Max connections per host",
			description = "The maximum number of connections that will be created to a single host. The total number of connections can be bigger, if different hosts are called."
		)
		int maxConnectionsPerHost() default 100;
		
		@AttributeDefinition(
			name = "Max connections",
			description = "The maximum number of total connections. This number of connections will never exceeded by the configured client. "
					+ "If multiple clients are defined via separate configurations, more connections might be established."
		)
		int maxConnections() default 200;
		
		@AttributeDefinition(
			name = "Keep connections alive",
			description = "Set this property to true in order to re-use opened Http connections. This will most likely improve performance."
		)
		boolean connectionKeepAlive() default true;
		
		@AttributeDefinition(
			name = "Enable redirects",
			description = "Set this property to true in order to automatically follow redirect responses."
		)
		boolean redirectsEnabled() default true;
		
		@AttributeDefinition(
			name = "Connection request timeout (ms)",
			description = "This is the timeout to wait for a response after a connection has been established."
		)
		int connectionRequestTimeout() default 30000;
		
		@AttributeDefinition(
			name = "Connection timeout (ms)",
			description = "This is the timeout to wait for establishing a connection to a host."
		)
		int connectionTimeout() default 30000;
		
		@AttributeDefinition(
			name = "Secure Socket Protocol",
			description = "If custom truststores and keystores are used for HTTPS, then this property defines the preferred protocol for secure sockets."
		)
		String secureSocketProtocolName() default "TLS";
		
		@AttributeDefinition(
			name = "Ignore Hostname Verification",
			description = "If this property is set, the host name verification of server side SSL certificates will be ignored. This should only be set to true during development."
		)
		boolean ignoreSSLHostNames() default false;
		
		@AttributeDefinition(
			name = "Path to the TrustStore", 
			description = "Path to the TrustStore to use for SSL connections. If empty, the default JVM TrustStore will be used."
		)
		String trustStoreLocation() default "";
		
		@AttributeDefinition(
			name = "Password of the TrustStore", 
			description = "The password to open the TrustStore configured above.",
			type = AttributeType.PASSWORD
		)
		String trustStorePassword() default "";
		
		@AttributeDefinition(
			name = "Path to the KeyStore", 
			description = "Path to the KeyStore to use for SSL connections. If empty, no keystore will be used."
		)
		String keyStoreLocation() default "";
		
		@AttributeDefinition(
			name = "Password of the KeyStore", 
			description = "The password to open the KeyStore configured above.",
			type = AttributeType.PASSWORD
		)
		String keyStorePassword() default "";
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfiguration.class);
	private String name;
	private int maxConnectionsPerHost;
	private int maxConnections;
	private boolean connectionKeepAlive;
	private boolean redirectsEnabled;
	private int connectionRequestTimeout;
	private int connectionTimeout;
	private String secureSocketProtocolName;
	private boolean ignoreSSLHostNames;
	private KeyStoreProvider keyStoreProvider;
	private KeyStoreProvider trustStoreProvider;

	@Activate
	public void activate(ComponentContext componentContext) {
		loadFromProperties(componentContext.getProperties());
	}
	
	public void loadFromProperties(Dictionary<String, Object> properties) {
		DictionaryAdapter adapter = new DictionaryAdapter(properties);
		DictionaryAdapter nullAdapter = adapter.emptyStringAsNull();
		name = adapter.getString("name", "default");
		maxConnectionsPerHost = adapter.getInteger("maxConnectionsPerHost", 100);
		maxConnections = adapter.getInteger("maxConnections", 200);
		connectionKeepAlive = adapter.getBoolean("connectionKeepAlive", true);
		redirectsEnabled = adapter.getBoolean("redirectsEnabled", true);
		connectionRequestTimeout = adapter.getInteger("connectionRequestTimeout", 30000);
		connectionTimeout = adapter.getInteger("connectionTimeout", 30000);
		secureSocketProtocolName = adapter.getString("secureSocketProtocolName", "TLS");
		ignoreSSLHostNames = adapter.getBoolean("ignoreSSLHostNames", false);
		keyStoreProvider = initKeyStoreProvider(nullAdapter.getString("keyStoreLocation"), nullAdapter.getString("keyStorePassword"));
		trustStoreProvider = initKeyStoreProvider(nullAdapter.getString("trustStoreLocation"), nullAdapter.getString("trustStorePassword"));
	}
	
	private KeyStoreProvider initKeyStoreProvider(String location, String password) {
		if (location != null) {
			try (InputStream instream = Files.newInputStream(Paths.get(location), StandardOpenOption.READ)) {
				final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				final String pw = password == null ? "" : password;
				keyStore.load(instream, pw.toCharArray());
				KeyStoreProvider provider = new KeyStoreProvider() {

					@Override
					public String getKeyStoreAccessPassword() {
						return pw;
					}

					@Override
					public KeyStore getKeyStore() {
						return keyStore;
					}
				};
				return provider;
			} catch (Exception ex) {
				LOG.error("failed to load trust/keystore: " + ex.getMessage(), ex);
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}

	public int getMaxConnectionsPerHost() {
		return maxConnectionsPerHost;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public boolean getConnectionKeepAlive() {
		return connectionKeepAlive;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public int getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	public boolean getRedirectsEnabled() {
		return redirectsEnabled;
	}

	public String getSecureSocketProtocolName() {
		return secureSocketProtocolName;
	}

	public boolean getIgnoreSSLHostNames() {
		return ignoreSSLHostNames;
	}

	public KeyStoreProvider getTrustStoreProvider() {
		return trustStoreProvider;
	}

	public KeyStoreProvider getKeyStoreProvider() {
		return keyStoreProvider;
	}

}
