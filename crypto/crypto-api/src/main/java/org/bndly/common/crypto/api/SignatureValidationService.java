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
 * A SignatureValidationService should be used to validate a provided signature for a provided hash.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SignatureValidationService {
	/**
	 * This method tests, if the provided signature had been created for the provided hash. If the signature does not match, a {@link SignatureMismatchException} will be thrown.
	 * @param signature The signature, that should be tested
	 * @param hash The hash, for which the signature should have been created
	 * @throws SignatureMismatchException if the signature does not match for the provided hash.
	 * @throws CryptoException if the signature can not be validated for whatever reason.
	 */
	void testSignatureWithHash(byte[] signature, byte[] hash) throws SignatureMismatchException, CryptoException;

	/**
	 * This method tests, if the provided signature had been created for the provided hash. If the signature does not match, a {@link SignatureMismatchException} will be thrown.
	 * @param signatureBase64 The signature, that should be tested as a Base64 encoded string.
	 * @param hashBase64 The hash, for which the signature should have been created as a Base64 encoded string.
	 * @throws SignatureMismatchException if the signature does not match for the provided hash.
	 * @throws CryptoException if the signature can not be validated for whatever reason.
	 */
	void testSignatureWithHash(String signatureBase64, String hashBase64) throws SignatureMismatchException, CryptoException;
	
	/**
	 * This method tests, if the provided signature had been created for the provided hash. If the signature does not match, a {@link SignatureMismatchException} will be thrown.
	 * @param signatureBase64 The signature, that should be tested as a Base64 encoded string.
	 * @param hash The hash, for which the signature should have been created
	 * @throws SignatureMismatchException if the signature does not match for the provided hash.
	 * @throws CryptoException if the signature can not be validated for whatever reason.
	 */
	void testSignatureWithHash(String signatureBase64, byte[] hash) throws SignatureMismatchException, CryptoException;
	
	/**
	 * This method tests, if the provided signature had been created for the provided hash. If the signature does not match, a {@link SignatureMismatchException} will be thrown.
	 * @param signature The signature, that should be tested
	 * @param hashBase64 The hash, for which the signature should have been created as a Base64 encoded string.
	 * @throws SignatureMismatchException if the signature does not match for the provided hash.
	 * @throws CryptoException if the signature can not be validated for whatever reason.
	 */
	void testSignatureWithHash(byte[] signature, String hashBase64) throws SignatureMismatchException, CryptoException;
}
