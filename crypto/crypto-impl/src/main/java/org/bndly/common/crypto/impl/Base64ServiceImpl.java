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

import org.bndly.common.crypto.impl.shared.Base64Util;
import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.crypto.api.CryptoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

/**
 * The Base64ServiceImpl is the default implementation of the Base64Service interface.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Base64ServiceImpl implements Base64Service {

	@Override
	public String base64Encode(InputStream stream) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			Base64Util.encode(stream, bos);
			bos.flush();
			return new String(bos.toByteArray(), "ASCII");
		} catch (IOException ex) {
			throw new CryptoException("failed to base64 encode byte array", ex);
		}
	}

	@Override
	public void base64EncodeStream(InputStream is, OutputStream os) throws IOException {
		// three octets are encoded as four encoded characters. this means that there might be some padding at the end.
		Base64Util.encode(is, os);
	}

	@Override
	public void base64EncodeStream(InputStream is, Writer writer) throws IOException {
		Base64Util.encode(is, writer);
	}
	
	@Override
	public void base64DecodeStream(InputStream is, OutputStream os) throws IOException {
		// three octets are encoded as four encoded characters. this means that there might be some padding at the end.
		Base64Util.decode(is, os);
	}

	@Override
	public String base64Encode(byte[] bytes) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			Base64Util.encode(new ByteArrayInputStream(bytes), bos);
			bos.flush();
			return new String(bos.toByteArray(), "ASCII");
		} catch (IOException ex) {
			throw new CryptoException("failed to base64 encode byte array", ex);
		}
	}

	// 8   7  6  5  4 3 2 1 
	// 128 64 32 16 8 4 2 1
	@Override
	public byte[] base64Decode(String base64String) {
		if (base64String.length() % 4 != 0) {
			throw new IllegalArgumentException("input string has to have a length of a multiple of 4");
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			Base64Util.decode(new ByteArrayInputStream(base64String.getBytes("ASCII")), bos);
			bos.flush();
		} catch (IOException ex) {
			throw new CryptoException("failed to decode base64 input string", ex);
		}
		return bos.toByteArray();
	}
	
}
