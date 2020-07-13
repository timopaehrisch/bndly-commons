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
import java.io.OutputStream;

/**
 * A SimpleEncryptService is used to encrypt information without having to worry about the used algorithms.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SimpleEncryptService {
	/**
	 * This method will encrypt the provided plain data and returns a byte array with the encrypted data.
	 * @param inputDataToBeEncrypted the data, that shall be encrypted
	 * @return a byte array with the encrypted data
	 * @throws CryptoException if the provided data can not be encrypted for whatever reason.
	 */
	byte[] encode(byte[] inputDataToBeEncrypted) throws CryptoException;
	
	/**
	 * This method will encrypt the provided plain string and returns a byte array with the encrypted data.
	 * The encoding for the string to byte conversion is taken from the {@link CryptoServiceFactory}, that created this service instance.
	 * 
	 * @param inputString the plain string data, that shall be encrypted
	 * @return a byte array with the encrypted data
	 * @throws CryptoException if the provided data can not be encrypted for whatever reason.
	 */
	byte[] encode(String inputString) throws CryptoException;
	
	/**
	 * This method will create an input stream, that will provide the encrypted data from the plain data, that has been read from the originally provided input stream.
	 * @param inputStream a stream, that provides the plain data to encrypt.
	 * @return a stream, that provides encrypted data
	 * @throws CryptoException if the encrypting input stream can not be created for whatever reason.
	 */
	InputStream encodeStream(InputStream inputStream) throws CryptoException;
	
	/**
	 * Takes an input stream with data and encrypts it into the output stream.
	 * @param inputStream the data to encrypt
	 * @param outputStream the target for the encrypted data
	 * @throws CryptoException if the data can not be fully encrypted
	 */
	void encodeStream(InputStream inputStream, OutputStream outputStream) throws CryptoException;
}
