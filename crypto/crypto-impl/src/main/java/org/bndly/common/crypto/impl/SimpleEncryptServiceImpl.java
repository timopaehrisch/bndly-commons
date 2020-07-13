package org.bndly.common.crypto.impl;

/*-
 * #%L
 * Crypto Impl
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

import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.SimpleEncryptService;
import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.SmartBufferOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.crypto.CipherOutputStream;

/**
 * The SimpleEncryptServiceImpl uses a {@link CipherProvider} and a {@link CryptoServiceFactoryImpl} to implement {@link SimpleEncryptService}.
 * The {@link CipherProvider} is used to get the cipher for encryption. 
 * The {@link CryptoServiceFactoryImpl} is used to get the default string encoding for the convenience method {@link SimpleEncryptService#encode(java.lang.String)}.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SimpleEncryptServiceImpl implements SimpleEncryptService {

	private final CipherProvider cipherProvider;
	private final CryptoServiceFactoryImpl cryptoServiceImpl;

	/**
	 * Creates a SimpleEncryptServiceImpl with a {@link CipherProvider} and a {@link CryptoServiceFactoryImpl}.
	 * @param cipherProvider The cipherProvider provides the cipher for encryption.
	 * @param cryptoServiceImpl The cryptoServiceImpl provides the default string encoding for {@link SimpleEncryptService#encode(java.lang.String)}.
	 */
	public SimpleEncryptServiceImpl(CipherProvider cipherProvider, CryptoServiceFactoryImpl cryptoServiceImpl) {
		if (cipherProvider == null) {
			throw new IllegalArgumentException("cipherProvider is not allowed to be null");
		}
		this.cipherProvider = cipherProvider;
		if (cryptoServiceImpl == null) {
			throw new IllegalArgumentException("cryptoServiceImpl is not allowed to be null");
		}
		this.cryptoServiceImpl = cryptoServiceImpl;
	}

	@Override
	public byte[] encode(String inputString) throws CryptoException {
		try {
			return encode(inputString.getBytes(cryptoServiceImpl.getDefaultInputStringEncoding()));
		} catch (UnsupportedEncodingException ex) {
			throw new CryptoException("could not encrypt string data because the encoding was not supported: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] encode(byte[] inputDataToBeEncrypted) throws CryptoException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (CipherOutputStream os = new CipherOutputStream(bos, cipherProvider.getCipher().getCipher())) {
			os.write(inputDataToBeEncrypted);
			os.flush();
		} catch (IOException ex) {
			throw new CryptoException("could not encrypt input data: " + ex.getMessage(), ex);
		}
		return bos.toByteArray();
	}

	@Override
	public InputStream encodeStream(InputStream inputStream) throws CryptoException {
		SmartBufferOutputStream buffer = SmartBufferOutputStream.newInstance();
		encodeStream(inputStream, buffer);
		try {
			return buffer.getBufferedDataAsStream();
		} catch (IOException e) {
			throw new CryptoException("failed to get input stream with encoded data: " + e.getMessage(), e);
		}
	}

	@Override
	public void encodeStream(InputStream inputStream, OutputStream outputStream) throws CryptoException {
		try (CipherOutputStream cos = new CipherOutputStream(outputStream, cipherProvider.getCipher().getCipher())) {
			IOUtils.copy(inputStream, cos);
			cos.flush();
		} catch (IOException e) {
			throw new CryptoException("failed to encode data from input stream: " + e.getMessage(), e);
		}
	}
	
}
