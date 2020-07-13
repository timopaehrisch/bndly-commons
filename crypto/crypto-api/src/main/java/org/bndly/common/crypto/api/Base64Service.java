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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

/**
 * The Base64Service encodes to and decodes from Base64 data.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Base64Service {

	/**
	 * This method reads all data from the provided input stream and writes the Base64 encoded result directly to the provided output stream.
	 *
	 * @param is A stream, that provides the data to encode to Base64
	 * @param os A stream, to which the encoding result should be written.
	 * @throws IOException if reading or writing on the provided streams fails
	 */
	void base64EncodeStream(InputStream is, OutputStream os) throws IOException;

	/**
	 * This method reads all data from the provided input stream and writes the Base64 encoded result directly to the provided writer.
	 *
	 * @param is A stream, that provides the data to encode to Base64
	 * @param writer A writer, to which the encoding result should be written.
	 * @throws IOException if reading or writing on the provided streams fails
	 */
	void base64EncodeStream(InputStream is, Writer writer) throws IOException;

	/**
	 * This method decods the base64 data from the provided input stream and writes the decoded data to the provided outputstream.
	 *
	 * @param is A stream, from which Base64 data should be read.
	 * @param os A stream, to which the decoded Base64 data should be written.
	 * @throws IOException if reading or writing on the provided streams fails
	 */
	void base64DecodeStream(InputStream is, OutputStream os) throws IOException;

	/**
	 * This method is a convenience to convert an array of bytes to a Base64 encoded string.
	 *
	 * @param bytes The data, that should be converted to Base64. This array is not allowed to be null.
	 * @return A string with Base64 encoded data.
	 */
	String base64Encode(byte[] bytes);

	/**
	 * This method is a convenience to convert the data read from an input stream to a Base64 encoded string. 
	 * A {@link org.bndly.common.crypto.api.CryptoException} may be thrown, if an IOException  occurs during 
	 * the encoding.
	 *
	 * @param stream The stream, that should be read to obtain the data to be encoded to Base64. This stream is not allowed to be null.
	 * @return A string with Base64 encoded data.
	 * @throws CryptoException if reading from provided streams fails with an IOException.
	 */
	String base64Encode(InputStream stream) throws CryptoException;

	/**
	 * This method is a convenience to convert the data read from a Base64 encoded string to a byte array.
	 *
	 * @param base64String A string, that holds Base64 encoded data. This string is not allowed to be null. 
	 * The length of the string should be a multiple of 4, because Base64 converts three raw bytes of data to 4 bytes of encoded data.
	 * @return A string with Base64 encoded data.
	 * @throws java.lang.IllegalArgumentException If the length of the Base64 encoded data has an invalid length.
	 */
	byte[] base64Decode(String base64String);
}
