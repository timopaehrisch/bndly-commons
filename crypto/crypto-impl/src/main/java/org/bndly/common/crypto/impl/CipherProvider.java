package org.bndly.common.crypto.impl;

/*-
 * #%L
 * Crypto Impl
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
 * A CipherProvider is able to provide a cipher object for usage in encryption/decryption. The caller does not have to know, how and where the cipher is created.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface CipherProvider {
	/**
	 * The CipherInfo bundles a cipher and a block size information, that is supported when using the cipher for encryption/decryption.
	 */
	public interface CipherInfo {
		/**
		 * Gets the current cipher.
		 * @return the current cipher
		 */
		Cipher getCipher();
		
		/**
		 * Gets the block size, that can be encrypted/decrypted with the current cipher.
		 * @return the block size. -1 for unlimited block size.
		 */
		int getBlockSize();
	}
	/**
	 * Gets a wrapper object for the cipher.
	 * @return the cipher information
	 */
	CipherInfo getCipher();
}
