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

import org.bndly.common.crypto.api.HashServiceConfig;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class HashServiceConfigImpl implements HashServiceConfig {

	private final String inputStringEncoding;
	private final String messageDigestAlgorithm;
	private final String secureRandomAlgorithm;
	private final int saltLength;
	private final int iterations;

	/**
	 * Creates an unmodifiable {@link HashServiceConfig} instance.
	 * @param inputStringEncoding the default encoding for string to byte array conversions
	 * @param messageDigestAlgorithm the algorithm for hashing
	 * @param secureRandomAlgorithm the algorithm for generating secure randoms, that will be used as salts
	 * @param saltLength the default length of automatically generated salts
	 * @param iterations the number of hashing iterations to apply on the first hash
	 */
	public HashServiceConfigImpl(String inputStringEncoding, String messageDigestAlgorithm, String secureRandomAlgorithm, int saltLength, int iterations) {
		if (inputStringEncoding == null) {
			throw new IllegalArgumentException("inputStringEncoding is not allowed to be null");
		}
		this.inputStringEncoding = inputStringEncoding;
		if (messageDigestAlgorithm == null) {
			throw new IllegalArgumentException("messageDigestAlgorithm is not allowed to be null");
		}
		this.messageDigestAlgorithm = messageDigestAlgorithm;
		if (secureRandomAlgorithm == null) {
			throw new IllegalArgumentException("secureRandomAlgorithm is not allowed to be null");
		}
		this.secureRandomAlgorithm = secureRandomAlgorithm;
		this.saltLength = saltLength;
		this.iterations = iterations;
	}

	@Override
	public String getDefaultInputStringEncoding() {
		return inputStringEncoding;
	}

	@Override
	public String getMessageDigestAlgorithm() {
		return messageDigestAlgorithm;
	}

	@Override
	public String getSecureRandomAlorithm() {
		return secureRandomAlgorithm;
	}

	@Override
	public int getHashingDefaultSaltLength() {
		return saltLength;
	}

	@Override
	public int getHashingIterations() {
		return iterations;
	}
	
}
