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
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.common.crypto.api.SimpleDecryptService;
import org.bndly.common.crypto.api.SimpleEncryptService;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The SimpleCryptoServiceImpl is a delegation to separated instances of {@link SimpleDecryptService} and {@link SimpleEncryptService} in order to implement {@link SimpleCryptoService}.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SimpleCryptoServiceImpl implements SimpleCryptoService {
	
	private final SimpleDecryptService decryptService;
	private final SimpleEncryptService encryptService;

	/**
	 * Creates a SimpleCryptoServiceImpl instance with a separate instances for decryption and encryption.
	 * @param decryptService the service for decryption
	 * @param encryptService the service for encryption
	 */
	public SimpleCryptoServiceImpl(SimpleDecryptService decryptService, SimpleEncryptService encryptService) {
		this.decryptService = decryptService;
		this.encryptService = encryptService;
	}

	@Override
	public byte[] encode(String inputString) throws CryptoException {
		return encryptService.encode(inputString);
	}

	@Override
	public byte[] encode(byte[] inputDataToBeEncrypted) throws CryptoException {
		return encryptService.encode(inputDataToBeEncrypted);
	}
	
	@Override
	public InputStream encodeStream(InputStream inputStream) {
		return encryptService.encodeStream(inputStream);
	}

	@Override
	public void encodeStream(InputStream inputStream, OutputStream outputStream) throws CryptoException {
		encryptService.encodeStream(inputStream, outputStream);
	}

	@Override
	public String decode(byte[] bytes) throws CryptoException {
		return decryptService.decode(bytes);
	}

	@Override
	public byte[] decodeToBytes(byte[] bytes) throws CryptoException {
		return decryptService.decodeToBytes(bytes);
	}

	@Override
	public InputStream decodeStream(InputStream stream) {
		return decryptService.decodeStream(stream);
	}

}
