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

import org.bndly.common.lang.StringUtil;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 *
 * The PathCoder is a utility to encode path elements in an URI properly. This
 * can NOT be done with the {@link URLEncoder}, because the URLEncoder is used
 * to encode request parameters of forms.
 *
 * This coder will encode ' ' to %20 for example. Internally
 * {@link Integer#toHexString(int)} and
 * {@link Integer#parseInt(java.lang.String, int)} will be used to encode
 * special characters to and from hex representations.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 * @see <a href="http://stackoverflow.com/a/4605816">http://stackoverflow.com/a/4605816</a> 
 */
public abstract class PathCoder {
	protected final CharacterTester unsafeCharacterTester;
	protected final Charset charset;
	protected final int maxBytesPerChar;
	protected final ByteBuffer byteBuffer;
	protected final CharBuffer charBuffer;
	protected final CharsetEncoder encoder;
	protected final CharsetDecoder decoder;
	protected final CharBuffer hexBuffer;
	
	public static interface CharacterTester {
		boolean applies(int codePoint);
	}

	public static final CharacterTester RESERVED_CHARS = new CharacterTester() {

		@Override
		public boolean applies(int codePoint) {
			return ":/?#[]@*+,;=".indexOf(codePoint) >= 0;
		}
	};
	
	public static final CharacterTester UNRESERVED_CHARS = new CharacterTester() {

		@Override
		public boolean applies(int codePoint) {
			return 
					('A' <= codePoint && 'Z' >= codePoint) 
					|| ('a' <= codePoint && 'z' >= codePoint) 
					|| ('0' <= codePoint && '9' >= codePoint) 
					|| '-' == codePoint 
					|| '.' == codePoint 
					|| '_' == codePoint 
					|| '~' == codePoint
			;
		}
	};
	
	public static final CharacterTester RESOURCE_URI_RESERVED_CHARS = new CharacterTester() {

		@Override
		public boolean applies(int codePoint) {
			if ('.' == codePoint) {
				return true;
			}
			if (!UNRESERVED_CHARS.applies(codePoint)) {
				if ('{' == codePoint || '}' == codePoint) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
	};
	
	public static class UTF8 extends PathCoder {
		public static final int SINGLE_BYTE_MASK = 0x7F;
		public static final int ONE_MORE_BYTE_MASK = 0xC0;
		public static final int TWO_MORE_BYTES_MASK = 0xE0;
		public static final int THREE_MORE_BYTES_MASK = 0xF0;

		public UTF8(CharacterTester characterTester) {
			super(Charset.forName("UTF-8"), 4, characterTester);
		}
		
		public UTF8() {
			super(Charset.forName("UTF-8"), 4, RESOURCE_URI_RESERVED_CHARS);
		}

		@Override
		protected boolean isExpectingMoreByteForNextChar() {
			int pos = byteBuffer.position();
			byte b = byteBuffer.array()[0];
			if ((b & THREE_MORE_BYTES_MASK) == THREE_MORE_BYTES_MASK) {
				return pos <= 3;
			} else if ((b & TWO_MORE_BYTES_MASK) == TWO_MORE_BYTES_MASK) {
				return pos <= 2;
			} else if ((b & ONE_MORE_BYTE_MASK) == ONE_MORE_BYTE_MASK) {
				return pos <= 1;
			} else if ((b | SINGLE_BYTE_MASK) <= SINGLE_BYTE_MASK) {
				return pos <= 0;
			} else {
				return true;
			}
		}
		
	}
	
	public static class ISO88591 extends PathCoder {

		public ISO88591(CharacterTester characterTester) {
			super(Charset.forName("ISO-8859-1"), 1, characterTester);
		}
		
		public ISO88591() {
			super(Charset.forName("ISO-8859-1"), 1, RESOURCE_URI_RESERVED_CHARS);
		}

		@Override
		protected boolean isExpectingMoreByteForNextChar() {
			return false; // because this is a single byte encoding
		}
		
	}
	
	private PathCoder(Charset charset, int maxBytesPerChar, CharacterTester unsafeCharacterTester) {
		if (charset == null) {
			throw new IllegalArgumentException("charset is not allowed to be null");
		}
		this.charset = charset;
		if (unsafeCharacterTester == null) {
			throw new IllegalArgumentException("unsafeCharacterTester is not allowed to be null");
		}
		this.unsafeCharacterTester = unsafeCharacterTester;
		this.maxBytesPerChar = maxBytesPerChar;
		this.byteBuffer = ByteBuffer.allocate(maxBytesPerChar);
		// the char buffer should be bigger than one char, because some characters in UTF-8 for example take more than one java char to be stored (emojis for example)
		this.charBuffer = CharBuffer.allocate(4);
		this.hexBuffer = CharBuffer.allocate(2);
		this.encoder = charset.newEncoder();
		this.decoder = charset.newDecoder();
	}
	
	public void encodeString(String input, Appendable appendable) throws IOException {
		((Buffer) byteBuffer).clear();
		((Buffer) charBuffer).clear();
		for (int codePoint : StringUtil.codePoints(input)) {
			if (unsafeCharacterTester.applies(codePoint)) {
				int result = Character.toChars(codePoint,charBuffer.array(),0);
				if (result <= 0 || result > 2) {
					throw new IOException("could not convert codepoint to chars");
				}
				charBuffer.position(0);
				charBuffer.limit(result);
				encoder.encode(charBuffer, byteBuffer, true);
				((Buffer) byteBuffer).flip();
				while (byteBuffer.hasRemaining()) {
					byte b = byteBuffer.get();
					appendable.append('%');
					encodeByteToHex(b, appendable);
				}
				((Buffer) byteBuffer).clear();
				((Buffer) charBuffer).clear();
			} else {
				appendable.append((char)codePoint);
			}
		}
	}
	
	private static void encodeByteToHex(byte b, Appendable appendable) throws IOException {
		appendable.append(Character.forDigit(((b >> 4) & 0xF), 16)); // first half
		appendable.append(Character.forDigit((b & 0xF), 16)); // last half
	}
	
	public String encodeString(String input) {
		StringBuilder resultStr = new StringBuilder();
		try {
			encodeString(input, resultStr);
			return resultStr.toString();
		} catch (IOException ex) {
			throw new IllegalStateException("failed to encode string: " + ex.getMessage(), ex);
		}
	}
	
	public String decodeString(String input) {
		StringBuilder resultStr = new StringBuilder();
		try {
			decodeString(input, resultStr);
			return resultStr.toString();
		} catch (IOException ex) {
			throw new IllegalStateException("failed to decode string: " + ex.getMessage(), ex);
		}
	}
	
	public void decodeString(String input, Appendable appendable) throws IOException {
		boolean inHex = false;
		boolean expectMoreHex = false;
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if ('%' == ch) {
				if (!inHex) {
					expectMoreHex = false;
					hexBuffer.clear();
					inHex = true;
				} else {
					throw new IllegalArgumentException("can not decode string");
				}
			} else {
				if (expectMoreHex) {
					throw new IllegalArgumentException("expected hex value");
				}
				if (inHex) {
					hexBuffer.append(ch);
					if (hexBuffer.position() == 2) {
						char[] array = hexBuffer.array();
						byte b = hexCharsToByte(array[0], array[1]);
						byteBuffer.put(b);
						// depending on the encoding we have to figure out, if we expect any more bytes for the next character
						if (isExpectingMoreByteForNextChar()) {
							expectMoreHex = true;
						} else {
							((Buffer) byteBuffer).flip();
							decoder.decode(byteBuffer, charBuffer, true);
							((Buffer) charBuffer).flip();
							char[] arr = charBuffer.array();
							for (int j = charBuffer.position(); j < charBuffer.limit(); j++) {
								appendable.append(arr[j]);
							}
							((Buffer) charBuffer).clear();
							((Buffer) byteBuffer).clear();
							expectMoreHex = false;
						}
						hexBuffer.clear();
						inHex = false;
					}
				} else {
					if (ch == '+') {
						appendable.append(' ');
					} else {
						appendable.append(ch);
					}
				}
			}
		}
	}
	
	protected abstract boolean isExpectingMoreByteForNextChar();
	
	private byte hexCharsToByte(char c0, char c1) {
		return (byte) ((hexCharToByte(c0) << 4) | hexCharToByte(c1));
	}
	
	private byte hexCharToByte(char hexChar) {
		if ('0' == hexChar) {
			return 0x00;
		} else if ('1' == hexChar) {
			return 0x01;
		} else if ('2' == hexChar) {
			return 0x02;
		} else if ('3' == hexChar) {
			return 0x03;
		} else if ('4' == hexChar) {
			return 0x04;
		} else if ('5' == hexChar) {
			return 0x05;
		} else if ('6' == hexChar) {
			return 0x06;
		} else if ('7' == hexChar) {
			return 0x07;
		} else if ('8' == hexChar) {
			return 0x08;
		} else if ('9' == hexChar) {
			return 0x09;
		} else if ('a' == hexChar || 'A' == hexChar) {
			return 0x0a;
		} else if ('b' == hexChar || 'B' == hexChar) {
			return 0x0b;
		} else if ('c' == hexChar || 'C' == hexChar) {
			return 0x0c;
		} else if ('d' == hexChar || 'D' == hexChar) {
			return 0x0d;
		} else if ('e' == hexChar || 'E' == hexChar) {
			return 0x0e;
		} else if ('f' == hexChar || 'F' == hexChar) {
			return 0x0f;
		} else {
			throw new IllegalArgumentException("unsupported character for hex representation: " + hexChar);
		}
	}

	public static String encode(String input) {
		return new UTF8().encodeString(input);
	}

	public static String decode(String input) {
		return new UTF8().decodeString(input);
	}

	public static int decodeHexToInt(String hexString) {
		return Integer.parseInt(hexString, 16);
	}

	public static boolean isUnreserved(char ch) {
		return UNRESERVED_CHARS.applies(ch);
	}
	
	public static boolean isReserved(char ch) {
		return RESERVED_CHARS.applies(ch);
	}
	
	private static boolean isUnsafe(char ch) {
		return RESOURCE_URI_RESERVED_CHARS.applies(ch);
	}

}
