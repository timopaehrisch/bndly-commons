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
 * A SaltedHashResult is a model for the result of a hashing operation.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SaltedHashResult {
	/**
	 * Gets the raw bytes of the salt, that was used for hashing.
	 * @return the used salt as a byte array
	 */
	byte[] getSalt();
	
	/**
	 * Gets the raw bytes of the hash.
	 * @return the hash as a byte array
	 */
	byte[] getHash();
	
	/**
	 * Gets the salt, that was used for hashing, as a Base64 encoded string.
	 * @return the used salt as a Base64 string
	 */
	String getSaltBase64();
	
	/**
	 * Gets the bytes of the hash as a Base64 encoded string.
	 * @return the hash as a Base64 string
	 */
	String getHashBase64();
}
