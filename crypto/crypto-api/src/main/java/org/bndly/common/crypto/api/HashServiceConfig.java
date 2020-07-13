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
 * The HashServiceConfig defines the configuration parameters for setting up a {@link HashService} instance.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface HashServiceConfig {
	/**
	 * Gets the encoding, that should be used to convert input strings to byte arrays in {@link HashService#hash(java.lang.String)} and {@link HashService#hash(java.lang.String, byte[])}.
	 * @return A name of a character encoding.
	 */
	String getDefaultInputStringEncoding();
	
	/**
	 * Gets the name of the algorithm, that should be used for hashing.
	 * @return The hashing algorithm name.
	 */
	String getMessageDigestAlgorithm();
	
	/**
	 * Gets the name of the algorithm to generate secure randoms.
	 * Secure randoms will be used as salts in 
	 * {@link HashService#hash(java.io.InputStream)}, 
	 * {@link HashService#hash(java.lang.String)} and 
	 * {@link HashService#secureRandom(int)}.
	 * @return The secure random algorithm name.
	 */
	String getSecureRandomAlorithm();
	
	/**
	 * Gets the default length of an automatically generated salt in bytes.
	 * @return The length of automatically generated salts
	 */
	int getHashingDefaultSaltLength();
	
	/**
	 * Gets the amount of hashing iterations, that should be applied on the first hash result.
	 * A value of 0 would mean, that the input data would be hashed and the result hash would be returned.
	 * A value of 1 would mean, that the input data would be hashed and the resulting hash would be also hashed. The hashed hash would be returned.
	 * @return The number of hashing iterations to perform on a first hash of data.
	 */
	int getHashingIterations();
}
