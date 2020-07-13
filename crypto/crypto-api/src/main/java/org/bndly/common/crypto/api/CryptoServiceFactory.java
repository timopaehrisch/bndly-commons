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
 * A CryptoServiceFactory creates various crypto service instances based on provided configuration objects.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface CryptoServiceFactory {
	/**
	 * Creates a SimpleCryptoService instance, that supports encryption and decryption.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SimpleCryptoService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SimpleCryptoService createSimpleCryptoService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Creates a SimpleDecryptService instance, that supports only decryption.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SimpleDecryptService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SimpleDecryptService createSimpleDecryptService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Creates a SimpleEncryptService instance, that supports only encryption.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SimpleEncryptService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SimpleEncryptService createSimpleEncryptService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Creates a SignatureService, that can be used to create and validate signatures.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SignatureService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SignatureService createSignatureService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Creates a SignatureService, that can only be used to validate signatures.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SignatureValidationService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SignatureValidationService createSignatureValidationService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Creates a SignatureCreationService, that can only be used to create signatures.
	 * @param encryptionConfig The configuration, that defines the algorithms and secrets to construct the service instance.
	 * @return A SignatureCreationService based on the provided config. Never null.
	 * @throws CryptoException if the service instance can not be constructed.
	 */
	SignatureCreationService createSignatureCreationService(EncryptionConfig encryptionConfig) throws CryptoException;
	
	/**
	 * Create an instance of a Base64Service.
	 * @return A Base64Service. Never null.
	 */
	Base64Service createBase64Service();

	/**
	 * Creates a HashService, that can be used to generate hashes of data.
	 * @param hashServiceConfig The configuration, that defines the algorithms and parameters for hashing.
	 * @return A HashService. Never null.
	 * @throws java.lang.IllegalArgumentException if the configuration is incomplete.
	 */
	HashService createHashService(HashServiceConfig hashServiceConfig);
}
