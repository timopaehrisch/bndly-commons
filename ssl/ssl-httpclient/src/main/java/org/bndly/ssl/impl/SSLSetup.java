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

import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SSLSetup {

	private static final Logger LOG = LoggerFactory.getLogger(SSLSetup.class);
	
	private SSLSetup() {
	}

	public static void setupLegacy(HttpClient httpClient, KeyStore keyStore, String secureSocketProtocolName, boolean developmentMode) {
		if (keyStore != null && secureSocketProtocolName != null) {
			try {
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
				tmf.init(keyStore);
				SSLContext context = SSLContext.getInstance(secureSocketProtocolName);
				context.init(null, tmf.getTrustManagers(), null);

				SSLSocketFactory ssf;
				if (developmentMode) {
					ssf = new SSLSocketFactory(context, new DevelopmentX509HostnameVerifier());
				} else {
					ssf = new SSLSocketFactory(context);
				}

				ClientConnectionManager ccm = httpClient.getConnectionManager();
				SchemeRegistry sr = ccm.getSchemeRegistry();
				sr.register(new Scheme("https", 443, ssf));
			} catch (Exception ex) {
				throw new RuntimeException("could not setup SSL: " + ex.getMessage(), ex);
			}
		} else {
			LOG.info("http client will use default SSL context!");
		}
	}

}
