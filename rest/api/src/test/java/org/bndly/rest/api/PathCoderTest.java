package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PathCoderTest {
	@Test
	public void testEncoding() {
		String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMOPQRSTUVWXYZ0123456789-_~";
		String decoded = PathCoder.decode(input);
		Assert.assertEquals(decoded, input);
		String encoded = PathCoder.encode(input);
		Assert.assertEquals(encoded, input);
	}
	
	@Test
	public void testEncodingDiaresis() throws CharacterCodingException {
		char c = 'ä';
		CharBuffer charbuffer = CharBuffer.allocate(1);
		charbuffer.put(c);
		charbuffer.position(0);
		ByteBuffer encodedBuffer = Charset.forName("UTF-8").newEncoder().encode(charbuffer);
		Assert.assertEquals(encodedBuffer.position(), 0);
		Assert.assertEquals(encodedBuffer.remaining(), 2);
		byte[] array = encodedBuffer.array();
		Assert.assertEquals(encodeByteToHexString(array[0]), "c3");
		Assert.assertEquals(encodeByteToHexString(array[1]), "a4");
		
		charbuffer.position(0);
		encodedBuffer = Charset.forName("ISO-8859-1").newEncoder().encode(charbuffer);
		Assert.assertEquals(encodedBuffer.position(), 0);
		Assert.assertEquals(encodedBuffer.remaining(), 1);
		array = encodedBuffer.array();
		Assert.assertEquals(encodeByteToHexString(array[0]), "e4");
		
		String encoded = PathCoder.encode("" + c);
		Assert.assertEquals(encoded, "%c3%a4");
		
		Assert.assertEquals(new PathCoder.UTF8().encodeString("ä"), "%c3%a4");
		Assert.assertEquals(new PathCoder.ISO88591().encodeString("ä"), "%e4");
		Assert.assertEquals(new PathCoder.UTF8().encodeString("äü"), "%c3%a4%c3%bc");
		Assert.assertEquals(new PathCoder.ISO88591().encodeString("äü"), "%e4%fc");
	}
	
	@Test
	public void testEmoji() throws IOException {
		// emoji in utf-8 bytes
		byte[] a = new byte[]{(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x81};
		String emojiString = new String(a, "UTF-8");
		Assert.assertEquals(emojiString.length(), 2); // the emoji is a single code point, but it is stored as two characters
		Assert.assertEquals(emojiString.codePointCount(0, emojiString.length()), 1);
		String decoded = new PathCoder.UTF8().decodeString("%f0%9f%98%81");
		Assert.assertEquals(decoded, emojiString);
		String encoded = new PathCoder.UTF8().encodeString(decoded);
		Assert.assertEquals(encoded, "%f0%9f%98%81");
	}
	
	@Test
	public void testDecodingDiaresis() {
		Assert.assertEquals(new PathCoder.UTF8().decodeString("%c3%a4"), "ä");
		Assert.assertEquals(new PathCoder.ISO88591().decodeString("%e4"), "ä");
		Assert.assertEquals(new PathCoder.UTF8().decodeString("%c3%a4%c3%bc"), "äü");
		Assert.assertEquals(new PathCoder.ISO88591().decodeString("%e4%fc"), "äü");
	}
	
	@Test
	public void testDecodingSpaces() {
		Assert.assertEquals(new PathCoder.UTF8().decodeString("a+b"), "a b");
		Assert.assertEquals(new PathCoder.UTF8().decodeString("a%20b"), "a b");
		Assert.assertEquals(new PathCoder.ISO88591().decodeString("a+b"), "a b");
		Assert.assertEquals(new PathCoder.ISO88591().decodeString("a%20b"), "a b");
	}
	
	@Test
	public void testDecodingAndEncoding() throws UnsupportedEncodingException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int i = 0; i < 0xFF; i++) {
			bos.write(i);
		}
		String string = new String(bos.toByteArray(), "ISO-8859-1");
		String encoded = new PathCoder.ISO88591().encodeString(string);
		String decoded = new PathCoder.ISO88591().decodeString(encoded);
		Assert.assertEquals(decoded, string);
		
		bos = new ByteArrayOutputStream();
		for (int i = 0; i < 0x1FFFFF; i++) {
			bos.write(i);
		}
		string = new String(bos.toByteArray(), "UTF-8");
		encoded = new PathCoder.UTF8().encodeString(string);
		decoded = new PathCoder.UTF8().decodeString(encoded);
		Assert.assertEquals(decoded, string);
	}
	
	private static String encodeByteToHexString(byte b) {
		StringBuffer stringBuffer = new StringBuffer();
		encodeByteToHex(b, stringBuffer);
		return stringBuffer.toString();
	}
	
	private static void encodeByteToHex(byte b, StringBuffer stringBuffer) {
		stringBuffer.append(Character.forDigit(((b >> 4) & 0xF), 16)); // first half
		stringBuffer.append(Character.forDigit((b & 0xF), 16)); // last half
	}
}
