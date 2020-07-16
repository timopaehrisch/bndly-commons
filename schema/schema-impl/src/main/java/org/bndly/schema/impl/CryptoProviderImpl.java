package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.crypto.api.CryptoServiceFactory;
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.MissingCryptoServiceException;
import org.bndly.schema.model.CryptoAttribute;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CryptoProviderImpl implements CryptoProvider {
	
	private final CryptoServiceFactory cryptoServiceFactory;
	private final Map<String, SimpleCryptoService> simpleCryptoServicesByName;
	private final Base64Service base64Service;

	public CryptoProviderImpl(CryptoServiceFactory cryptoServiceFactory, Map<String, SimpleCryptoService> simpleCryptoServicesByName) {
		if (cryptoServiceFactory == null) {
			throw new IllegalArgumentException("cryptoServiceFactory is not allowed to be null");
		}
		this.cryptoServiceFactory = cryptoServiceFactory;
		if (simpleCryptoServicesByName == null) {
			throw new IllegalArgumentException("simpleCryptoServicesByName is not allowed to be null");
		}
		this.simpleCryptoServicesByName = simpleCryptoServicesByName;
		this.base64Service = cryptoServiceFactory.createBase64Service();
	}
	

	public SimpleCryptoService getSimpleCryptoService(String name) throws MissingCryptoServiceException {
		SimpleCryptoService service = simpleCryptoServicesByName.get(name);
		if (service == null) {
			throw new MissingCryptoServiceException("could not find crypto service " + name);
		}
		return service;
	}

	@Override
	public InputStream createDecryptingStream(InputStream stream, CryptoAttribute attribute) throws MissingCryptoServiceException {
		SimpleCryptoService service = getSimpleCryptoService(attribute.getCryptoReference());
		InputStream decoded = service.decodeStream(stream);
		return decoded;
	}

	@Override
	public InputStream createEncryptingStream(InputStream inputStream, CryptoAttribute attribute) throws MissingCryptoServiceException {
		SimpleCryptoService service = getSimpleCryptoService(attribute.getCryptoReference());
		InputStream encoded = service.encodeStream(inputStream);
		return encoded;
	}

	@Override
	public String base64Encode(InputStream stream) {
		return base64Service.base64Encode(stream);
	}

	@Override
	public InputStream createBase64DecodingStream(String base64String) {
		byte[] bytes = base64Service.base64Decode(base64String);
		return new ByteArrayInputStream(bytes);
	}
	
}
