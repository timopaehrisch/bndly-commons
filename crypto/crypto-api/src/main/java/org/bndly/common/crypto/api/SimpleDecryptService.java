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
 * A SimpleDecryptService is used to decrypt information without having to worry about the used algorithms.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SimpleDecryptService {
	/**
	 * This method will decrypt an array of bytes to a string. 
	 * The encoding for the string construction is taken from the {@link CryptoServiceFactory}, that created this service instance.
	 *
	 * @param bytes the data to decrypt and convert to a string
	 * @return the decrypted data as a string. never null.
	 * @throws CryptoException if the provided data can not be decrypted for whatever reason.
	 */
	String decode(byte[] bytes) throws CryptoException;
	
	/**
	 * This method will decrypt an array of bytes to a separate array of plain bytes. 
	 *
	 * @param bytes the data to decrypt
	 * @return the decrypted data as a separate byte array. never null.
	 * @throws CryptoException if the provided data can not be decrypted for whatever reason.
	 */
	byte[] decodeToBytes(byte[] bytes) throws CryptoException;
	
	/**
	 * This method will create an input stream, that will provide the decrypted data from the originally provided stream.
	 *
	 * @param stream a stream, that holds the data, that should be decrypted
	 * @return a stream, that provides the decrypted data. never null.
	 */
	InputStream decodeStream(InputStream stream);
}
