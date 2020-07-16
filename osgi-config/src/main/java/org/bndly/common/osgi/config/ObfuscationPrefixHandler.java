package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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

import org.bndly.common.osgi.config.spi.PrefixHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The ObfuscationPrefixHandler is using an XOR to obfuscate string values. The mask is specified in the constructor.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ObfuscationPrefixHandler implements PrefixHandler {

	private final String encoding;
	private final byte mask;

	public ObfuscationPrefixHandler() {
		this(
			System.getProperty("org.bndly.common.osgi.config.ObfuscationPrefixHandler.encoding", "UTF-8"),
			Byte.valueOf(System.getProperty("org.bndly.common.osgi.config.ObfuscationPrefixHandler.mask", "85"))
		);
	}

	public ObfuscationPrefixHandler(String encoding, byte mask) {
		if (encoding == null) {
			throw new IllegalArgumentException("encoding is not allowed to be null");
		}
		this.encoding = encoding;
		this.mask = mask;
	}
	
	@Override
	public String getPrefix() {
		return "OBF";
	}

	@Override
	public String get(String rawStringValue) {
		if (rawStringValue == null) {
			return null;
		}
		try {
			char[] chars = rawStringValue.toCharArray();
			byte[] target = new byte[chars.length / 2];
			for (int i = 0; i < chars.length; i = i + 2) {
				int hi = HEX_CHAR_TO_INT.get(chars[i]);
				int lo = HEX_CHAR_TO_INT.get(chars[i + 1]);
				target[i / 2] = (byte) (((byte) ((hi << 4) | lo)) ^ mask);
			}
			String deobfuscated = new String(target, "UTF-8");
			return deobfuscated;
		} catch (IOException e) {
			throw new IllegalStateException("could not deobfuscate", e);
		}
	}

	@Override
	public String set(String rawStringValue) {
		if (rawStringValue == null) {
			return null;
		}
		try {
			byte[] bytes = rawStringValue.getBytes(encoding);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			for (byte b : bytes) {
				bos.write(b ^ mask);
			}
			bos.flush();
			byte[] bytesObfuscated = bos.toByteArray();
			// print the bytes as HEX strings, because this way we do not created conflicts with properties files
			return bytesToHex(bytesObfuscated);
		} catch (IOException e) {
			throw new IllegalStateException("could not obfuscate", e);
		}
	}

	private static final char[] HEX_CHAR_ARRAY = "0123456789ABCDEF".toCharArray();
	private static final Map<Character, Integer> HEX_CHAR_TO_INT;
	static {
		HEX_CHAR_TO_INT = new HashMap<>();
		for (int i = 0; i < HEX_CHAR_ARRAY.length; i++) {
			char c = HEX_CHAR_ARRAY[i];
			HEX_CHAR_TO_INT.put(c, i);
		}
	}

	public static String bytesToHex(byte[] bytes) {
		// 1 byte requires 2 chars in HEX
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			// int in order to perform an upcast to access the entire byte
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_CHAR_ARRAY[v >>> 4]; // shift 4 bits to get the 4 higher order bits. as an int, they point to the right position in our static HEX char array
			hexChars[j * 2 + 1] = HEX_CHAR_ARRAY[v & 0x0F]; // mask the 4 lower order bits
		}
		return new String(hexChars);
	}

}
