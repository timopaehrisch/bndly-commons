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
import org.bndly.common.crypto.api.BlockBasedEncryptionConfig;
import org.bndly.common.crypto.api.HashService;
import org.bndly.common.crypto.api.KeystoreConfig;
import org.bndly.common.crypto.api.KeystoreEncryptionConfig;
import org.bndly.common.crypto.api.SaltedHashResult;
import org.bndly.common.crypto.api.SignatureMismatchException;
import org.bndly.common.crypto.api.SignatureResult;
import org.bndly.common.crypto.api.SignatureService;
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.common.data.io.CloseOnLastByteOrExceptionInputStreamWrapper;
import org.bndly.common.data.io.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CryptoServiceTest {
	
	@Test
	public void testSecureRandomGenerationForDefaultAlgorithm() {
		HashService hashService = new CryptoServiceFactoryImpl().createHashService();
		byte[] byteArrayOf64Bits = hashService.secureRandom(8);
		Assert.assertNotNull(byteArrayOf64Bits);
		Assert.assertEquals(byteArrayOf64Bits.length, 8);
		byte[] otherByteArrayOf64Bits = hashService.secureRandom(8);
		boolean isEqual = true;
		for (int index = 0; index < otherByteArrayOf64Bits.length; index++) {
			byte otherByteArrayOf64Bit = otherByteArrayOf64Bits[index];
			byte byteArrayOf64Bit = byteArrayOf64Bits[index];
			isEqual = isEqual && (otherByteArrayOf64Bit == byteArrayOf64Bit);
		}
		Assert.assertFalse(isEqual, "expected secure randoms to be not equal");
	}
	
	@Test
	public void testBase64Encoding() throws IOException {
		Base64Service base64Service = new CryptoServiceFactoryImpl().createBase64Service();
		String encoded = base64Service.base64Encode(new byte[]{77,97,110});
		Assert.assertEquals(encoded, "TWFu");
		
		String encodedWithPadding1 = base64Service.base64Encode("any carnal pleasure.".getBytes("ASCII"));
		Assert.assertEquals(encodedWithPadding1, "YW55IGNhcm5hbCBwbGVhc3VyZS4=");
		String encodedWithPadding2 = base64Service.base64Encode("any carnal pleasure".getBytes("ASCII"));
		Assert.assertEquals(encodedWithPadding2, "YW55IGNhcm5hbCBwbGVhc3VyZQ==");
		
		StringWriter sw = new StringWriter();
		base64Service.base64EncodeStream(new ByteArrayInputStream("any carnal pleasure.".getBytes("ASCII")), sw);
		sw.flush();
		encodedWithPadding1 = sw.toString();
		Assert.assertEquals(encodedWithPadding1, "YW55IGNhcm5hbCBwbGVhc3VyZS4=");
		sw = new StringWriter();
		base64Service.base64EncodeStream(new ByteArrayInputStream("any carnal pleasure".getBytes("ASCII")), sw);
		sw.flush();
		encodedWithPadding2 = sw.toString();
		Assert.assertEquals(encodedWithPadding2, "YW55IGNhcm5hbCBwbGVhc3VyZQ==");
	}
	
	@Test
	public void testBase64Decoding() throws UnsupportedEncodingException {
		Base64Service base64Service = new CryptoServiceFactoryImpl().createBase64Service();
		byte[] decoded = base64Service.base64Decode("TWFu");
		Assert.assertEquals(decoded.length, 3);
		Assert.assertEquals(decoded[0], 77);
		Assert.assertEquals(decoded[1], 97);
		Assert.assertEquals(decoded[2], 110);
		
		byte[] decodedPadding1 = base64Service.base64Decode("YW55IGNhcm5hbCBwbGVhc3VyZS4=");
		Assert.assertEquals(new String(decodedPadding1, "ASCII"), "any carnal pleasure.");
		byte[] decodedPadding2 = base64Service.base64Decode("YW55IGNhcm5hbCBwbGVhc3VyZQ==");
		Assert.assertEquals(new String(decodedPadding2, "ASCII"), "any carnal pleasure");
	}
	
	@Test
	public void testHashing() throws UnsupportedEncodingException {
		HashService hashService = new CryptoServiceFactoryImpl().createHashService();
		String input = "my secret";
		SaltedHashResult firstHash = hashService.hash(input);
		byte[] salt = firstHash.getSalt();
		SaltedHashResult secondHash = hashService.hash(input, salt);
		Assert.assertEquals(firstHash.getHashBase64(), secondHash.getHashBase64());
	}
	
	@Test
	public void testHashingIterations() throws UnsupportedEncodingException {
		CryptoServiceFactoryImpl cryptoServiceImpl = new CryptoServiceFactoryImpl();
		cryptoServiceImpl.setHashingIterations(1);
		HashService hashService = cryptoServiceImpl.createHashService();
		String input = "my secret";
		SaltedHashResult firstHash = hashService.hash(input);
		byte[] salt = firstHash.getSalt();
		SaltedHashResult secondHash = hashService.hash(input, salt);
		Assert.assertEquals(firstHash.getHashBase64(), secondHash.getHashBase64());
		
		cryptoServiceImpl.setHashingIterations(2);
		hashService = cryptoServiceImpl.createHashService();
		SaltedHashResult thirdHash = hashService.hash(input, salt);
		Assert.assertNotEquals(thirdHash.getHashBase64(), secondHash.getHashBase64());
	}
	
	@Test
	public void testEncryption() throws IOException {
		CryptoServiceFactoryImpl cryptoServiceImpl = new CryptoServiceFactoryImpl();
		Path path = Paths.get("src","test","resources","demo.jceks");
		cryptoServiceImpl.setKeystoreLocation(path.toString());
		cryptoServiceImpl.setKeystorePassword("changeit");
		cryptoServiceImpl.setKeystoreType("jceks");
		cryptoServiceImpl.setEncryptionKeyAlias("symkey");
		cryptoServiceImpl.setEncryptionKeyAlgorithm("AES");
		cryptoServiceImpl.setEncryptionKeyPassword("changeit");
		cryptoServiceImpl.setSignatureKeyAlias("demokey");
		cryptoServiceImpl.setSignatureKeyAlgorithm("RSA");
		cryptoServiceImpl.setSignatureKeyPassword("changeit");
		cryptoServiceImpl.initDefaultConfigs();
		
		SimpleCryptoService cs = cryptoServiceImpl.createSimpleCryptoService();
		String secret = "my secret, that should be encrypted";
		byte[] encrypted = cs.encode(secret);
		String decrypted = cs.decode(encrypted);
		Assert.assertEquals(decrypted, secret);
		
		InputStream encodingStream = cs.encodeStream(new ByteArrayInputStream(secret.getBytes("UTF-8")));
		encrypted = IOUtils.read(encodingStream);
		String encoded = new String(encrypted, "UTF-8");
		Assert.assertNotEquals(encoded, secret);
		InputStream decryptingStream = cs.decodeStream(new ByteArrayInputStream(encrypted));
		byte[] decryptedBytes = IOUtils.read(decryptingStream);
		decrypted = new String(decryptedBytes, "UTF-8");
		Assert.assertEquals(decrypted, secret);
	}
	
	@Test
	public void testReadStreamInFixedChunkSize() throws UnsupportedEncodingException, IOException {
		testEncoding("any carnal pleasure.", "YW55IGNhcm5hbCBwbGVhc3VyZS4=");
		testEncoding("any carnal pleasure", "YW55IGNhcm5hbCBwbGVhc3VyZQ==");
		testDecoding("YW55IGNhcm5hbCBwbGVhc3VyZS4=", "any carnal pleasure.");
		testDecoding("YW55IGNhcm5hbCBwbGVhc3VyZQ==", "any carnal pleasure");
		
		// the base64util also supports incomplete base64 input strings, where the padding is missing.
		testDecoding("YW55IGNhcm5hbCBwbGVhc3VyZS4", "any carnal pleasure.");
		testDecoding("YW55IGNhcm5hbCBwbGVhc3VyZQ=", "any carnal pleasure");
		testDecoding("YW55IGNhcm5hbCBwbGVhc3VyZQ", "any carnal pleasure");
	}
	
	private void testEncoding(String input, String output) throws UnsupportedEncodingException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Base64Util.encode(new ByteArrayInputStream(input.getBytes("ASCII")), bos);
		bos.flush();
		Assert.assertEquals(new String(bos.toByteArray(), "ASCII"), output);
	}
	private void testDecoding(String input, String output) throws UnsupportedEncodingException, IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Base64Util.decode(new ByteArrayInputStream(input.getBytes("ASCII")), bos);
		bos.flush();
		Assert.assertEquals(new String(bos.toByteArray(), "ASCII"), output);
	}
	
	@Test
	public void testEncryptionWithCipher() throws IOException {
		CryptoServiceFactoryImpl cryptoServiceImpl = new CryptoServiceFactoryImpl();
		Path path = Paths.get("src","test","resources","demo.jceks");
		cryptoServiceImpl.setKeystoreLocation(path.toString());
		cryptoServiceImpl.setKeystorePassword("changeit");
		cryptoServiceImpl.setKeystoreType("jceks");
		cryptoServiceImpl.setEncryptionKeyAlias("symkey");
		cryptoServiceImpl.setEncryptionKeyAlgorithm("AES");
		cryptoServiceImpl.setEncryptionKeyPassword("changeit");
		cryptoServiceImpl.setSignatureKeyAlias("demokey");
		cryptoServiceImpl.setSignatureKeyAlgorithm("RSA");
		cryptoServiceImpl.setSignatureKeyPassword("changeit");
		cryptoServiceImpl.initDefaultConfigs();
		byte[] randomData = new byte[1024 * 4];
		for (int i = 0; i < randomData.length; i++) {
			double rand = Math.random() * 256 - 128;
			byte b = (byte) rand; // down cast
			randomData[i] = b;
		}
		SimpleCryptoService cs = cryptoServiceImpl.createSimpleCryptoService();
//		String secret = "my secret, that should be encrypted";
		InputStream encodeStream = cs.encodeStream(new ByteArrayInputStream(randomData));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		IOUtils.copy(encodeStream, bos);
		bos.flush();
		byte[] encrypted = bos.toByteArray();
		Assert.assertNotEquals(encrypted.length, 0, "encrypted data byte array should have at least a single byte");
		InputStream decodingStream = cs.decodeStream(new ByteArrayInputStream(encrypted));
		bos = new ByteArrayOutputStream();
		IOUtils.copy(decodingStream, bos);
		bos.flush();
		byte[] shouldBeTheRandomData = bos.toByteArray();
		Assert.assertEquals(shouldBeTheRandomData.length, randomData.length);
	}
	
	@Test
	public void testSignatureCreation() throws IOException {
		CryptoServiceFactoryImpl cryptoServiceImpl = new CryptoServiceFactoryImpl();
		Path path = Paths.get("src","test","resources","demo.jceks");
		cryptoServiceImpl.setKeystoreLocation(path.toString());
		cryptoServiceImpl.setKeystorePassword("changeit");
		cryptoServiceImpl.setKeystoreType("jceks");
		cryptoServiceImpl.setEncryptionKeyAlias("symkey");
		cryptoServiceImpl.setEncryptionKeyAlgorithm("AES");
		cryptoServiceImpl.setEncryptionKeyPassword("changeit");
		cryptoServiceImpl.setSignatureKeyAlias("demokey");
		cryptoServiceImpl.setSignatureKeyAlgorithm("RSA");
		cryptoServiceImpl.setSignatureKeyPassword("changeit");
		cryptoServiceImpl.initDefaultConfigs();
		SimpleCryptoService cs = cryptoServiceImpl.createSimpleCryptoService();
		HashService hs = cryptoServiceImpl.createHashService();
		SignatureService ss = cryptoServiceImpl.createSignatureService();
		
		byte[] randomData = new byte[1024 * 4];
		for (int i = 0; i < randomData.length; i++) {
			double rand = Math.random() * 256 - 128;
			byte b = (byte) rand; // down cast
			randomData[i] = b;
		}
		byte[] encoded = cs.encode(randomData);
		SaltedHashResult hashed = hs.hash(new ByteArrayInputStream(encoded));
		// create a signature of the hash
		SignatureResult signature = ss.createSignatureOf(hashed.getHash());
		// now test the signature
		Assert.assertNotNull(signature.getSignature());
		Assert.assertTrue(signature.getSignature().length > 0);
		Assert.assertNotNull(signature.getSignatureBase64());
		Assert.assertTrue(signature.getSignatureBase64().length() > 0);
		
		ss.testSignatureWithHash(signature.getSignatureBase64(), hashed.getHashBase64());
		ss.testSignatureWithHash(signature.getSignatureBase64(), hashed.getHash());
		ss.testSignatureWithHash(signature.getSignature(), hashed.getHashBase64());
		ss.testSignatureWithHash(signature.getSignature(), hashed.getHash());
		
		byte[] wrongHash = Arrays.copyOf(hashed.getHash(), hashed.getHash().length);
		for (int i = 0; i < wrongHash.length; i++) {
			byte b = wrongHash[i];
			if (b == Byte.MAX_VALUE) {
				b = (byte) (b - 1);
			} else if (b == Byte.MIN_VALUE) {
				b = (byte) (b + 1);
			} else {
				b = (byte) (b + 1);
			}
			wrongHash[i] = b;
		}
		String wrongHash64 = cryptoServiceImpl.createBase64Service().base64Encode(wrongHash);
		
		try {
			ss.testSignatureWithHash(signature.getSignature(), wrongHash);
			Assert.fail("expected SignatureMismatchException");
		} catch(SignatureMismatchException e) { }
		try {
			ss.testSignatureWithHash(signature.getSignature(), wrongHash64);
			Assert.fail("expected SignatureMismatchException");
		} catch(SignatureMismatchException e) { }
		try {
			ss.testSignatureWithHash(signature.getSignatureBase64(), wrongHash);
			Assert.fail("expected SignatureMismatchException");
		} catch(SignatureMismatchException e) { }
		try {
			ss.testSignatureWithHash(signature.getSignatureBase64(), wrongHash64);
			Assert.fail("expected SignatureMismatchException");
		} catch(SignatureMismatchException e) { }
	}
	
	@Test
	public void testAsymetricEncryptionForLongStrings() throws UnsupportedEncodingException {
		CryptoServiceFactoryImpl cryptoServiceImpl = new CryptoServiceFactoryImpl();
		Path path = Paths.get("src","test","resources","demo.jceks");
		SimpleCryptoService service = cryptoServiceImpl.createSimpleCryptoService(new TestRSAEncryptionConfig());
		Assert.assertNotNull(service);
		String shortInput = "hello";
		byte[] encoded = service.encode(shortInput);
		Assert.assertNotEquals(encoded.length, 0);
		Assert.assertNotEquals(new String(encoded, "UTF-8"), shortInput);
		Assert.assertEquals(service.decode(encoded), shortInput);
		
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < (256 / 8); i++) {
			stringBuilder.append("abcdefgh");
		}
		String longInput = stringBuilder.toString();
		encoded = service.encode(longInput);
		Assert.assertNotEquals(encoded.length, 0);
		Assert.assertNotEquals(new String(encoded, "UTF-8"), longInput);
		String decoded = service.decode(encoded);
		Assert.assertEquals(decoded, longInput);
		
	}
	
	private class TestRSAEncryptionConfig implements KeystoreEncryptionConfig, BlockBasedEncryptionConfig {

		private final KeystoreConfig keystoreConfig = new KeystoreConfig() {
			@Override
			public InputStream getInputStream() {
				try {
					return new CloseOnLastByteOrExceptionInputStreamWrapper(Files.newInputStream(Paths.get("src","test","resources","demo.jceks"), StandardOpenOption.READ));
				} catch (IOException ex) {
					return null;
				}
			}

			@Override
			public char[] getPassword() {
				return "changeit".toCharArray();
			}

			@Override
			public String getType() {
				return "jceks";
			}
		};
		@Override
		public KeystoreConfig getKeystoreConfig() {
			return keystoreConfig;
		}

		@Override
		public String getKeyAlias() {
			return "demokey";
		}

		@Override
		public char[] getKeystoreEntryPassword() {
			return "changeit".toCharArray();
		}

		@Override
		public String getCipherAlgorithm() {
			return "RSA/ECB/PKCS1Padding";
			//return "RSA";
		}

		@Override
		public int getBlockSize() {
			return 256;
		}

		@Override
		public int getMaxDataBlockSize() {
			return getBlockSize() - 11; // 11 bytes are reserved for the padding
		}
		
	}
}
