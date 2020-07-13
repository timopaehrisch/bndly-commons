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
 * A HashService can be used to easily generate hashes of provided data.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface HashService {
	/**
	 * Gets the configuration, that was used to build the HashService instance.
	 * @return The service configuration. Never null.
	 */
	HashServiceConfig getConfig();
	
	/**
	 * Generates a secure random of the provided length. The algorithm for the secure random is taken from {@link HashServiceConfig#getSecureRandomAlorithm()}.
	 * @param length A value &gt;= 1
	 * @return an array with random data, that has the provided length.
	 * @throws java.lang.IllegalArgumentException if the length parameter is &lt;=0
	 */
	byte[] secureRandom(int length);
	
	/**
	 * Hashes the data from the provided input stream and returns a result of the hashing.
	 * The salt is automatically generated using {@link #secureRandom(int)}.
	 * The hashing iterations are taken from {@link HashServiceConfig#getHashingIterations()}.
	 * @param inputStream a stream, that holds the data, that should be hashed.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(InputStream inputStream) throws CryptoException;
	
	/**
	 * Hashes the data from the provided input stream and returns a result of the hashing.
	 * The hashing iterations are taken from {@link HashServiceConfig#getHashingIterations()}.
	 * @param inputStream a stream, that holds the data, that should be hashed.
	 * @param salt an array of salt data, that should be used during hashing. This array is not allowed to be null.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(InputStream inputStream, byte[] salt) throws CryptoException;
	
	/**
	 * Hashes the data from the provided input stream and returns a result of the hashing.
	 * @param inputStream a stream, that holds the data, that should be hashed.
	 * @param salt an array of salt data, that should be used during hashing. This array is not allowed to be null.
	 * @param iterations amount of hashing iterations to run on the first hash. If this value is bigger than 0, then the hash result will be repeatedly re-hashed.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(InputStream inputStream, byte[] salt, int iterations) throws CryptoException;
	
	/**
	 * This method is a convenience to hash a string value. The bytes of the string are obtained by calling 
	 * {@link String#getBytes(java.lang.String)} with the encoding taken from {@link HashServiceConfig#getDefaultInputStringEncoding()}.
	 * @param inputString The string, that should be hashed. This string is not allowed to be null.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(String inputString) throws CryptoException;
	
	/**
	 * This method is a convenience to hash a string value. The bytes of the string are obtained by calling 
	 * {@link String#getBytes(java.lang.String)} with the encoding taken from {@link HashServiceConfig#getDefaultInputStringEncoding()}.
	 * @param inputString The string, that should be hashed. This string is not allowed to be null.
	 * @param salt an array of salt data, that should be used during hashing. This array is not allowed to be null.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(String inputString, byte[] salt) throws CryptoException;
	/**
	 * This method is a convenience to hash a byte array of input data with a provided salt.
	 * @param inputData The data, that should be hashed. This array is not allowed to be null.
	 * @param salt an array of salt data, that should be used during hashing. This array is not allowed to be null.
	 * @return a result of the hashing. never null.
	 * @throws CryptoException if the hash can not be determined for whatever reason.
	 */
	SaltedHashResult hash(byte[] inputData, byte[] salt) throws CryptoException;
}
