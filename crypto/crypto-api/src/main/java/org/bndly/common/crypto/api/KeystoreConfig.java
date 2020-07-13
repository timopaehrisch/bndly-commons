package org.bndly.common.crypto.api;

/*-
 * #%L
 * Crypto API
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

/**
 * A KeystoreConfig gives access to the raw data of a keystore and also provides the password and type of the keystore.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface KeystoreConfig {
	/**
	 * Gets an input stream with the raw data of the keystore.
	 * Note: Implementations of this method should take care of closing the inputstream, when it is no longer needed.
	 * @return A stream with the data of the keystore or null, if the keystore data can not be provided for whatever reason.
	 */
	InputStream getInputStream();
	
	/**
	 * Gets the password to open the keystore.
	 * @return The password to open the keystore.
	 */
	char[] getPassword();
	
	/**
	 * Gets the type of the keystore. Typically this would be something like 'jks' or 'jceks'.
	 * @return The type of the keystore.
	 */
	String getType();
}
