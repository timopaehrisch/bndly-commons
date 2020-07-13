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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.HashService;
import org.bndly.common.crypto.api.HashServiceConfig;
import org.bndly.common.crypto.api.SaltedHashResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HashServiceImpl implements HashService {
	
	private final HashServiceConfig hashServiceConfig;
	private final Base64Service base64Service;
	private final String defaultInputStringEncoding;
	private final String messageDigestAlgorithm;
	private final String secureRandomAlorithm;
	private final int hashingDefaultSaltLength;
	private final int hashingIterations;
	private final int hashChunkSize = 2048;

	/**
	 * This constructor creates a HashServiceImpl instance based on a configuration object.
	 * @param base64Service the Base64 service to use in order to implement {@link SaltedHashResult#getHashBase64()} and {@link SaltedHashResult#getSaltBase64()}.
	 * @param hashServiceConfig the configuration for this service instance.
	 */
	public HashServiceImpl(Base64Service base64Service, HashServiceConfig hashServiceConfig) {
		this(
			base64Service,
			hashServiceConfig,
			hashServiceConfig.getDefaultInputStringEncoding(),
			hashServiceConfig.getMessageDigestAlgorithm(),
			hashServiceConfig.getSecureRandomAlorithm(),
			hashServiceConfig.getHashingDefaultSaltLength(),
			hashServiceConfig.getHashingIterations()
		);
	}
	
	/**
	 * This constructor creates a HashServiceImpl instance based on raw configuration values.
	 * @param base64Service the Base64 service to use in order to implement {@link SaltedHashResult#getHashBase64()} and {@link SaltedHashResult#getSaltBase64()}.
	 * @param defaultInputStringEncoding the encoding for automatic string conversions
	 * @param messageDigestAlgorithm the hashing algorithm
	 * @param secureRandomAlorithm the algorithm for generating secure randoms
	 * @param hashingDefaultSaltLength the default length for automatically generated salts
	 * @param hashingIterations the number of hashing iterations to run after the first hash
	 */
	public HashServiceImpl(
			Base64Service base64Service, 
			final String defaultInputStringEncoding, 
			final String messageDigestAlgorithm, 
			final String secureRandomAlorithm, 
			final int hashingDefaultSaltLength, 
			final int hashingIterations
	) {
		this(
			base64Service,
			new HashServiceConfig() {
				@Override
				public String getDefaultInputStringEncoding() {
					return defaultInputStringEncoding;
				}

				@Override
				public String getMessageDigestAlgorithm() {
					return messageDigestAlgorithm;
				}

				@Override
				public String getSecureRandomAlorithm() {
					return secureRandomAlorithm;
				}

				@Override
				public int getHashingDefaultSaltLength() {
					return hashingDefaultSaltLength;
				}

				@Override
				public int getHashingIterations() {
					return hashingIterations;
				}
			}
		);
	}
	private HashServiceImpl(
			Base64Service base64Service, 
			HashServiceConfig hashServiceConfig,
			String defaultInputStringEncoding, 
			String messageDigestAlgorithm, 
			String secureRandomAlorithm, 
			int hashingDefaultSaltLength, 
			int hashingIterations
	) {
		if (base64Service == null) {
			throw new IllegalArgumentException("base64Service is not allowed to be null");
		}
		this.base64Service = base64Service;
		if (hashServiceConfig == null) {
			throw new IllegalArgumentException("hashServiceConfig is not allowed to be null");
		}
		this.hashServiceConfig = hashServiceConfig;
		if (defaultInputStringEncoding == null) {
			throw new IllegalArgumentException("defaultInputStringEncoding is not allowed to be null");
		}
		this.defaultInputStringEncoding = defaultInputStringEncoding;
		if (messageDigestAlgorithm == null) {
			throw new IllegalArgumentException("messageDigestAlgorithm is not allowed to be null");
		}
		this.messageDigestAlgorithm = messageDigestAlgorithm;
		if (secureRandomAlorithm == null) {
			throw new IllegalArgumentException("secureRandomAlorithm is not allowed to be null");
		}
		this.secureRandomAlorithm = secureRandomAlorithm;
		this.hashingDefaultSaltLength = hashingDefaultSaltLength;
		this.hashingIterations = hashingIterations;
	}

	@Override
	public final HashServiceConfig getConfig() {
		return hashServiceConfig;
	}

	@Override
	public SaltedHashResult hash(InputStream inputStream) throws CryptoException {
		return hash(inputStream, secureRandom(hashingDefaultSaltLength));
	}

	@Override
	public SaltedHashResult hash(InputStream inputStream, byte[] salt) throws CryptoException {
		return hash(inputStream, salt, hashingIterations);
	}
	
	@Override
	public SaltedHashResult hash(InputStream inputStream, byte[] salt, int iterations) throws CryptoException {
		byte[] digest;
		try {
			digest = getHash(iterations, inputStream, salt);
		} catch (NoSuchAlgorithmException ex) {
			throw new CryptoException("could not hash input, because configured algorithm is unknown: " + ex.getMessage(), ex);
		}
		String digest64 = base64Service.base64Encode(digest);
		String salt64 = base64Service.base64Encode(salt);
		return new SaltedHashResultImpl(salt, digest, salt64, digest64);
	}
	
	@Override
	public SaltedHashResult hash(String inputString) throws CryptoException {
		return hash(inputString, secureRandom(hashingDefaultSaltLength));
	}

	@Override
	public SaltedHashResult hash(byte[] inputData, byte[] salt) throws CryptoException {
		byte[] digest;
		try {
			digest = getHash(hashingIterations, inputData, salt);
		} catch (NoSuchAlgorithmException ex) {
			throw new CryptoException("could not hash input, because configured algorithm is unknown: " + ex.getMessage(), ex);
		}
		String digest64 = base64Service.base64Encode(digest);
		String salt64 = base64Service.base64Encode(salt);
		return new SaltedHashResultImpl(salt, digest, salt64, digest64);
	}

	@Override
	public SaltedHashResult hash(String inputString, byte[] salt) throws CryptoException {
		byte[] digest;
		try {
			digest = getHash(hashingIterations, inputString, salt);
		} catch (NoSuchAlgorithmException ex) {
			throw new CryptoException("could not hash input, because configured algorithm is unknown: " + ex.getMessage(), ex);
		}
		String digest64 = base64Service.base64Encode(digest);
		String salt64 = base64Service.base64Encode(salt);
		return new SaltedHashResultImpl(salt, digest, salt64, digest64);
	}

	private byte[] getHash(int iterationCount, String inputString, byte[] salt) throws NoSuchAlgorithmException {
		try {
			return getHash(iterationCount, inputString.getBytes(defaultInputStringEncoding), salt);
		} catch (UnsupportedEncodingException ex) {
			throw new CryptoException("could not get bytes of string: " + ex.getMessage(), ex);
		}
	}

	private byte[] getHash(int iterationCount, byte[] input, byte[] salt) throws NoSuchAlgorithmException {
		return getHash(iterationCount, new ByteArrayInputStream(input), salt);
	}
	
	private byte[] getHash(int iterationCount, InputStream inputStream, byte[] salt) throws NoSuchAlgorithmException {
		if (iterationCount < 1) {
			throw new IllegalArgumentException("hashing should happend at least once.");
		}
		MessageDigest digest = MessageDigest.getInstance(messageDigestAlgorithm);
		digest.reset();
		digest.update(salt);
		
		byte[] digestBytes;
		byte[] buffer = new byte[hashChunkSize]; // this buffer will be never read.
		try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
			int i;
			while ((i = digestInputStream.read(buffer)) > -1) {
				// read until the end
			}
			digestBytes = digest.digest();

			for (int hashinIteration = 0; hashinIteration < iterationCount; hashinIteration++) {
				digest.reset();
				digestBytes = digest.digest(digestBytes);
			}
			return digestBytes;
		} catch (IOException ex) {
			throw new CryptoException("failed to read data while creating a hash of it", ex);
		}
	}

	@Override
	public byte[] secureRandom(int length) throws CryptoException {
		if (length < 1) {
			throw new IllegalArgumentException("the secure random length has to be a positive number");
		}
		try {
			SecureRandom random = SecureRandom.getInstance(secureRandomAlorithm);
			// Salt generation 'length' bytes long
			byte[] saltedBytes = new byte[length];
			random.nextBytes(saltedBytes);
			return saltedBytes;
		} catch (NoSuchAlgorithmException ex) {
			throw new CryptoException("could not get a secure random, because the configured algorithm was unknown: " + ex.getMessage(), ex);
		}
	}
	
}
