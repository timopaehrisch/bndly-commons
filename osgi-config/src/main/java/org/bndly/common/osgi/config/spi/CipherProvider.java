package org.bndly.common.osgi.config.spi;

/*-
 * #%L
 * OSGI Config
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

import javax.crypto.Cipher;

/**
 * The CipherProvider is able to restore ciphers for de- and encryption of configuration files.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface CipherProvider {
	/**
	 * Returns the alias of the provider
	 * @return the alias, never null
	 */
	String getAlias();
	
	/**
	 * Restores the cipher for decryption of properties
	 * @param alias the alias determines if the cipher is known to the provider
	 * @param initVectorBase64 optional init vector of the cipher
	 * @return a restored cipher or null
	 */
	Cipher restoreDecryptionCipher(String alias, String initVectorBase64);
	
	/**
	 * Restores the cipher for encryption of properties
	 * @param alias the alias determines if the cipher is known to the provider
	 * @return a restored cipher or null
	 */
	Cipher restoreEncryptionCipher(String alias);
}
