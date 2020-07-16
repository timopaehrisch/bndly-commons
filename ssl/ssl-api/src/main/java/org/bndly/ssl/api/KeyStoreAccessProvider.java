package org.bndly.ssl.api;

/*-
 * #%L
 * SSL API
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

import java.io.InputStream;
import java.security.KeyStore;

/**
 * The KeyStoreAccessProvider interface allows the eBX Client to use a custom 
 * KeyStore while creating SSL connections to the eBX Core.
 */
public interface KeyStoreAccessProvider extends KeyStoreProvider {
    String NAME = "keyStoreAccessProvider";
    /**
     * Opens an InputStream to the Resource that holds the KeyStore's content.
     * In most cases this will be a file or classpath resource.
     * @return an InputStream object, that can be read by the eBX Client
     */
    InputStream getKeyStoreInputStream();
    
	/**
	 * Returns the keystore to use or null, if the keystore should be loaded via
	 * the inputstream/password provided by the current keystoreaccessprovider.
	 * @return 
	 */
	KeyStore getKeyStore();
	
	/**
     * Returns the name of the secure socket protocol used to invoke {@link javax.net.ssl.SSLContext#getInstance(java.lang.String) }.
     * @return secure socket protocol name. for example "TLS".
     */
	String getSecureSocketProtocol();
    
}
