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

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.ssl.X509HostnameVerifier;

public class DevelopmentX509HostnameVerifier implements X509HostnameVerifier {

    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {
    }

    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {
    }

    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
    }

    @Override
    public boolean verify(String string, SSLSession ssls) {
	return true;
    }
    
}
