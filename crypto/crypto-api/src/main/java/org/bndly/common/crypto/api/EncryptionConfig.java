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
 * An EncryptionConfig is used to describe the algorithms for encryption and decryption of data.
 * In many cases the {@link KeystoreEncryptionConfig} is used as a more specific configuration.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface EncryptionConfig {
	/**
	 * Gets the name of the algorithm, that will be used for encryption/decryption.
	 * @return algorithm name for encryption/decryption. never null.
	 */
	String getCipherAlgorithm();
}
