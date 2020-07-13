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
import org.bndly.common.data.io.SmartBufferOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.crypto.CipherOutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BlockBasedEncryptServiceImpl implements SimpleEncryptService {
	private final CipherProvider cipherProvider;
	private final CryptoServiceFactoryImpl cryptoServiceFactoryImpl;

	/**
	 * This constructor takes a CipherProvider and a CryptoServiceFactoryImpl.
	 * The CipherProvider is used to get the encryption cipher.
	 * The CryptoServiceFactoryImpl is used to get the default string encoding in order to implement {@link #encode(java.lang.String) }.
	 * @param cipherProvider the provider for the encryption cipher.
	 * @param cryptoServiceFactoryImpl the crypto service factory, that provides the default string encoding.
	 */
	public BlockBasedEncryptServiceImpl(CipherProvider cipherProvider, CryptoServiceFactoryImpl cryptoServiceFactoryImpl) {
		if (cipherProvider == null) {
			throw new IllegalArgumentException("cipherProvider is not allowed to be null");
		}
		this.cipherProvider = cipherProvider;
		if (cryptoServiceFactoryImpl == null) {
			throw new IllegalArgumentException("cryptoServiceImpl is not allowed to be null");
		}
		this.cryptoServiceFactoryImpl = cryptoServiceFactoryImpl;
	}

	@Override
	public byte[] encode(String inputString) throws CryptoException {
		try {
			return encode(inputString.getBytes(cryptoServiceFactoryImpl.getDefaultInputStringEncoding()));
		} catch (UnsupportedEncodingException ex) {
			throw new CryptoException("could not encrypt string data because the encoding was not supported: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] encode(byte[] inputDataToBeEncrypted) throws CryptoException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			final CipherProvider.CipherInfo cipherInfo = cipherProvider.getCipher();
			new BlockReader(cipherInfo.getBlockSize()) {
				@Override
				protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
					try (CipherOutputStream os = new CipherOutputStream(bos, cipherInfo.getCipher())) {
						os.write(blockBuffer, 0, length);
						os.flush();
					}
					bos.flush();
				}
			}.read(inputDataToBeEncrypted);
		} catch (IOException e) {
			throw new CryptoException("failed to read data to be encoded: " + e.getMessage(), e);
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
	public void encodeStream(InputStream inputStream, final OutputStream outputStream) throws CryptoException {
		try {
			final CipherProvider.CipherInfo cipherInfo = cipherProvider.getCipher();
			new BlockReader(cipherInfo.getBlockSize()) {
				@Override
				protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
					try (CipherOutputStream os = new CipherOutputStream(outputStream, cipherInfo.getCipher())) {
						os.write(blockBuffer, 0, length);
						os.flush();
					}
				}
			}.read(inputStream);
		} catch (IOException e) {
			throw new CryptoException("failed to encode data from input stream: " + e.getMessage(), e);
		}
	}
}
