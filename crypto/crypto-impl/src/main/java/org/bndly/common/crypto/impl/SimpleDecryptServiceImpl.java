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
 * The SimpleDecryptServiceImpl uses a {@link CipherProvider} and a {@link CryptoServiceFactoryImpl} to implement {@link SimpleDecryptService}.
 * The {@link CipherProvider} is used to get the cipher for decryption. 
 * The {@link CryptoServiceFactoryImpl} is used to get the default string encoding for the convenience method {@link SimpleDecryptService#decode(byte[]) }.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SimpleDecryptServiceImpl implements SimpleDecryptService {

	private final CipherProvider cipherProvider;
	private CryptoServiceFactoryImpl cryptoServiceImpl;

	/**
	 * Creates a SimpleDecryptServiceImpl with a {@link CipherProvider} and a {@link CryptoServiceFactoryImpl}.
	 * @param cipherProvider The cipherProvider provides the cipher for decryption.
	 * @param cryptoServiceImpl The cryptoServiceImpl provides the default string encoding for {@link SimpleDecryptService#decode(byte[]) }.
	 */
	public SimpleDecryptServiceImpl(CipherProvider cipherProvider, CryptoServiceFactoryImpl cryptoServiceImpl) {
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
	public String decode(byte[] bytes) throws CryptoException {
		try {
			return new String(decodeToBytes(bytes), cryptoServiceImpl.getDefaultInputStringEncoding());
		} catch (UnsupportedEncodingException ex) {
			throw new CryptoException("could not decrypt input data because string encoding was not supported: " + ex.getMessage(), ex);
		}
	}

	@Override
	public byte[] decodeToBytes(byte[] bytes) throws CryptoException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (InputStream is = new CipherInputStream(new ByteArrayInputStream(bytes), cipherProvider.getCipher().getCipher())) {
			IOUtils.copy(is, bos);
			bos.flush();
			return bos.toByteArray();
		} catch (IOException ex) {
			throw new CryptoException("could not decrypt input data: " + ex.getMessage(), ex);
		}
	}

	@Override
	public InputStream decodeStream(InputStream stream) {
		return new CipherInputStream(stream, cipherProvider.getCipher().getCipher());
	}
	
}
