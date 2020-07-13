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

import org.bndly.common.crypto.api.SaltedHashResult;

/**
 * This is an unmodifiable implementation of {@link SaltedHashResult}.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class SaltedHashResultImpl implements SaltedHashResult {
	private final byte[] salt;
	private final byte[] hash;
	private final String salt64;
	private final String hash64;

	/**
	 * Creates an instance with fixed salt, hash, salt in Base64 encoding and hash in Base64 encoding.
	 * @param salt the salt of the hash result
	 * @param hash the hash of the hash result
	 * @param salt64 the salt of the hash result in Base64 encoding
	 * @param hash64 the hash of the hash result in Base64 encoding
	 */
	public SaltedHashResultImpl(byte[] salt, byte[] hash, String salt64, String hash64) {
		this.salt = salt;
		this.hash = hash;
		this.salt64 = salt64;
		this.hash64 = hash64;
	}

	@Override
	public byte[] getSalt() {
		return salt;
	}

	@Override
	public byte[] getHash() {
		return hash;
	}

	@Override
	public String getSaltBase64() {
		return salt64;
	}

	@Override
	public String getHashBase64() {
		return hash64;
	}
	
}
