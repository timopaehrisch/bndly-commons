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
import org.bndly.common.crypto.api.SimpleDecryptService;
import org.bndly.common.data.io.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.crypto.CipherInputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BlockBasedDecryptServiceImpl implements SimpleDecryptService {
	private final CipherProvider cipherProvider;
	private CryptoServiceFactoryImpl cryptoServiceFactoryImpl;

	/**
	 * This constructor takes a CipherProvider and a CryptoServiceFactoryImpl.
	 * The CipherProvider is used to get the decryption cipher.
	 * The CryptoServiceFactoryImpl is used to get the default string encoding in order to implement {@link #decode(byte[])}.
	 * @param cipherProvider the provider for the decryption cipher.
	 * @param cryptoServiceFactoryImpl the crypto service factory, that provides the default string encoding.
	 */
	public BlockBasedDecryptServiceImpl(CipherProvider cipherProvider, CryptoServiceFactoryImpl cryptoServiceFactoryImpl) {
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
	public String decode(byte[] bytes) throws CryptoException {
		try {
			return new String(decodeToBytes(bytes), cryptoServiceFactoryImpl.getDefaultInputStringEncoding());
		} catch (UnsupportedEncodingException ex) {
			throw new CryptoException("could not decrypt input data because string encoding was not supported: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] decodeToBytes(byte[] bytes) throws CryptoException {
		try {
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final CipherProvider.CipherInfo cipherInfo = cipherProvider.getCipher();
			new BlockReader(cipherInfo.getBlockSize()) {
				@Override
				protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
					try (InputStream is = new CipherInputStream(new ByteArrayInputStream(blockBuffer, 0, length), cipherInfo.getCipher())) {
						IOUtils.copy(is, bos);
						bos.flush();
					}
				}
			}.read(bytes);
			return bos.toByteArray();
		} catch (IOException ex) {
			throw new CryptoException("could not decrypt input data: " + ex.getMessage(), ex);
		}
	}

	@Override
	public InputStream decodeStream(InputStream stream) {
		final CipherProvider.CipherInfo cipherInfo = cipherProvider.getCipher();
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			new BlockReader(cipherInfo.getBlockSize()) {
				@Override
				protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
					try (InputStream is = new CipherInputStream(new ByteArrayInputStream(blockBuffer, 0, length), cipherInfo.getCipher())) {
						IOUtils.copy(is, bos);
					}
				}
			}.read(stream);
			bos.flush();
		} catch (IOException ex) {
			throw new CryptoException("could not decrypt input data: " + ex.getMessage(), ex);
		}
		// we could do better here, because we are putting everything in memory
		return new ByteArrayInputStream(bos.toByteArray());
	}
}
