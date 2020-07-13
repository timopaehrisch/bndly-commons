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
 * A SignatureCreationService is used to create signatures of hashes.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SignatureCreationService {
	/**
	 * This method will create a signature of the provided hash.
	 * @param hash The hash, that shall be signed
	 * @return the signature as a SignatureResult
	 * @throws CryptoException if the signature can not be created for whatever reason.
	 */
	SignatureResult createSignatureOf(byte[] hash) throws CryptoException;
}
