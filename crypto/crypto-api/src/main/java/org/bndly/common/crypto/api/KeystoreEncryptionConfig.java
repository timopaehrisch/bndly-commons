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

/**
 * A KeystoreEncryptionConfig is a EncryptionConfig, that relies on a keystore, that will provide secrets or keypairs for encryption/decryption.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface KeystoreEncryptionConfig extends EncryptionConfig {
	
	/**
	 * Gets the configuration, to gain access to the keystore.
	 * @return a keystore configuration. never null.
	 */
	KeystoreConfig getKeystoreConfig();
	
	/**
	 * Gets the name of the keystore entry, that should be used for encryption/decryption
	 * @return a keystore entry name. never null.
	 */
	String getKeyAlias();
	
	/**
	 * Gets the password to access the keystore entry, that should be used for encryption/decryption
	 * @return an array with the password characters or null, if no password is required.
	 */
	char[] getKeystoreEntryPassword();
}
