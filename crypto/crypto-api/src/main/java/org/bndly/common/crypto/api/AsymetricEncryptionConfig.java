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
 * An AsymetricEncryptionConfig can be used to define configuration parameters, that only apply to asymetric enrcyption algorithms.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface AsymetricEncryptionConfig extends EncryptionConfig {
	/**
	 * This method defines if the private key of an asymetric algorithm should be used for encryption. 
	 * Encryption with a private key is used to validate the author of the encryption result.
	 * Encryption with a public key is used to only allow the holder of the private key to read the encryption result.
	 * @return true, if the private key should be used for encryption.
	 */
	boolean isPrivateKeyUsedForEncryption();
}
