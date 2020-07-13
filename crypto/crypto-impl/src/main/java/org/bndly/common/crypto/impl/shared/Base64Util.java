package org.bndly.common.crypto.impl.shared;

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

import org.bndly.common.crypto.impl.ChunkedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Base64Util {
	private static final String BASE64INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final Map<Character, Integer> BASE64REVERSEINDEX;
	private static final char BASE64PADDING = '=';
	
	static {
		BASE64REVERSEINDEX = new HashMap<>();
		for (int index = 0; index < BASE64INDEX.length(); index++) {
			char character = BASE64INDEX.charAt(index);
			BASE64REVERSEINDEX.put(character, index);
		}
	}
	/**
	 * Encodes the data from the provided input stream in Base64 and writes the result to the provided writer.
	 * @param is the input stream with the data to encode
	 * @param writer the writer to which the encoded data should be written.
	 * @throws IOException if reading the input stream or writing on the writer fails
	 */
	public static void encode(InputStream is, final Writer writer) throws IOException {
		encode(is, new WriteAdapter() {

			@Override
			public void write(byte[] buffer, char[] charBuffer) throws IOException {
				writer.write(charBuffer);
			}
		});
	}
	
	private static interface WriteAdapter {
		void write(byte[] buffer, char[] charBuffer) throws IOException;
	}
	
	/**
	 * Encodes the data from the provided input stream in Base64 and writes the result to the provided output stream.
	 * @param is the input stream with the data to encode
	 * @param os the output stream to which the encoded data should be written.
	 * @throws IOException if reading the input stream or writing on the output stream fails
	 */
	public static void encode(InputStream is, final OutputStream os) throws IOException {
		encode(is, new WriteAdapter() {

			@Override
			public void write(byte[] buffer, char[] charBuffer) throws IOException {
				os.write(buffer);
			}
		});
	}
	
	private static void encode(InputStream is, final WriteAdapter writeAdapter) throws IOException {
		final byte[] writeBuf = new byte[4];
		final char[] writeCharBuf = new char[4];
		new ChunkedReader(is, 3) {
			
			@Override
			protected void onChunk(byte[] buf) throws IOException {
				encodeWithPadding(buf, 0, writeBuf);
			}
			
			@Override
			protected void onLastChunk(byte[] buf, int filledUpToIndex) throws IOException {
				for (int i = filledUpToIndex; i < buf.length; i++) {
					buf[i] = 0;
				}
				encodeWithPadding(buf, buf.length - filledUpToIndex, writeBuf);
			}
			
			private void encodeWithPadding(byte[] buf, int padding, byte[] writeBuf) throws IOException {
				final byte byte1 = buf[0];
				final byte byte2 = buf[1];
				final byte byte3 = buf[2];
				
				int masked1 = (((int) byte1) & 0xFC) >> 2;
				int masked2 = ((((int) byte1) & 0x3) << 4) | ((((int) byte2) & 0xF0) >> 4);
				int masked3 = ((((int) byte2) & 0xF) << 2) | ((((int) byte3) & 0xC0) >> 6);
				int masked4 = (((int) byte3) & 0x3F);
				
				
				writeCharBuf[0] = BASE64INDEX.charAt(masked1);
				writeBuf[0] = (byte) writeCharBuf[0];
				writeCharBuf[1] = BASE64INDEX.charAt(masked2);
				writeBuf[1] = (byte) writeCharBuf[1];
				if (padding == 0) {
					writeCharBuf[2] = BASE64INDEX.charAt(masked3);
					writeBuf[2] = (byte) writeCharBuf[2];
					writeCharBuf[3] = BASE64INDEX.charAt(masked4);
					writeBuf[3] = (byte) writeCharBuf[3];
				} else if (padding == 1) {
					writeCharBuf[2] = BASE64INDEX.charAt(masked3);
					writeBuf[2] = (byte) writeCharBuf[2];
					writeCharBuf[3] = BASE64PADDING;
					writeBuf[3] = (byte) writeCharBuf[3];
				} else if (padding == 2) {
					writeCharBuf[2] = BASE64PADDING;
					writeBuf[2] = (byte) writeCharBuf[2];
					writeCharBuf[3] = BASE64PADDING;
					writeBuf[3] = (byte) writeCharBuf[3];
				} else {
					throw new IllegalStateException("padding is too big.");
				}
				writeAdapter.write(writeBuf, writeCharBuf);
			}
		}
		.startReading();
	}
	
	/**
	 * Encodes the data from the provided byte array to a Base64 encoded string.
	 * @param toEncode the data to encode
	 * @return a string with Base64 encoding
	 * @throws IOException if the encoding fails
	 */
	public static String encode(byte[] toEncode) throws IOException {
		StringWriter sw = new StringWriter();
		encode(new ByteArrayInputStream(toEncode), sw);
		sw.flush();
		return sw.toString();
	}
	
	/**
	 * Decodes the raw data from a Base64 encoded string.
	 * @param input a Base64 encoded string
	 * @return a byte array with the raw data decoded from the provided string
	 */
	public static byte[] decode(String input) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Base64Util.decode(new ByteArrayInputStream(input.getBytes("ASCII")), bos);
			bos.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			// screw this. we are working in-memory.
			return null;
		}
	}
	
	/**
	 * Decodes Base64 encoded data from the provided input stream and writes the decoded data to the provided output stream
	 * @param is the stream that provides the base64 encoded data
	 * @param os the stream to which the decoded data should be written.
	 * @throws IOException if reading the input stream or writing on the output stream fails
	 */
	public static void decode(InputStream is, final OutputStream os) throws IOException {
		final byte[] writeBuf = new byte[3];
		final char[] writeCharBuf = new char[3];
		new ChunkedReader(is, 4) {
			
			@Override
			protected void onChunk(byte[] buf) throws IOException {
				decode(buf);
			}
			
			@Override
			protected void onLastChunk(byte[] buf, int filledUpToIndex) throws IOException {
				for (int i = filledUpToIndex; i < buf.length; i++) {
					buf[i] = BASE64PADDING;
				}
				decode(buf);
			}
			
			private void decode(byte[] buf) throws IOException {
				final int padding;
				char tmp1 = (char) ((int) buf[3]);
				char tmp2 = (char) ((int) buf[2]);
				if (tmp1 == BASE64PADDING && tmp2 == BASE64PADDING) {
					padding = 2;
				} else if (tmp1 == BASE64PADDING) {
					padding = 1;
				} else {
					padding = 0;
				}
				final int byte1 = getReverseIndex(buf[0]);
				final int byte2 = getReverseIndex(buf[1]);
				final int byte3 = getReverseIndex(buf[2]);
				final int byte4 = getReverseIndex(buf[3]);
				
				writeBuf[0] = (byte) ((byte1 << 2) | ((byte2 & 0x30) >> 4));
				writeBuf[1] = (byte) (((byte2 & 0xF) << 4) | ((byte3 & 0x3C) >> 2));
				writeBuf[2] = (byte) (((byte3 & 0x3) << 6) | byte4);
				
				int byteCount = 3 - padding;
				os.write(writeBuf, 0, byteCount);
			}
			
			private int getReverseIndex(byte b) {
				char c = (char)((int)b);
				if (c == BASE64PADDING) {
					return 0;
				}
				return BASE64REVERSEINDEX.get(c);
			}
			
		}
		.startReading();
	}
}
