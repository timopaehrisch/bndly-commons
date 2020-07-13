package org.bndly.common.data.io;

/*-
 * #%L
 * Data IO
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
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class IOUtils {
	private static final int INTERNAL_BUF_SIZE = 1024;
	private static final OutputStream NO_OP_OUTPUTSTREAM = new OutputStream() {

		@Override
		public void write(int b) throws IOException {
		}

		@Override
		public void write(byte[] b) throws IOException {
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
		
	};

	public static void consume(InputStream is) throws IOException {
		copy(is, NO_OP_OUTPUTSTREAM);
	}
	private IOUtils() {
	}
	
	public static void copy(byte[] bytes, OutputStream os) throws IOException {
		os.write(bytes);
	}
	
	public static void copy(InputStream is, OutputStream os) throws IOException, ReadIOException, WriteIOException {
		copyBuffered(is, os, INTERNAL_BUF_SIZE);
	}
	
	public static void copyBuffered(InputStream is, OutputStream os, int bufferSize) throws IOException, ReadIOException, WriteIOException {
		copy(is, os, new byte[bufferSize]);
	}
	
	public static void copy(InputStream is, OutputStream os, byte[] buffer) throws IOException, ReadIOException, WriteIOException {
		int bytesWritten = 0;
		boolean didRead = false;
		try {
			while (bytesWritten > -1) {
				bytesWritten = is.read(buffer, 0, buffer.length);
				didRead = true;
				if (bytesWritten < 0) {
					break;
				}
				os.write(buffer, 0, bytesWritten);
				didRead = false;
			}
		} catch (IOException e) {
			if (didRead) {
				throw new WriteIOException(e);
			} else {
				throw new ReadIOException(e);
			}
		}
	}

	public static void copy(InputStream is, OutputStream os, long numberOfBytes) throws IOException, ReadIOException, WriteIOException {
		copy(is, os, numberOfBytes, new byte[INTERNAL_BUF_SIZE]);
	}
	
	public static void copy(InputStream is, OutputStream os, long numberOfBytes, byte[] buffer) throws IOException, ReadIOException, WriteIOException {
		long pos = 0;
		
		int bytesWritten = 0;
		boolean didRead = false;
		try {
			while (bytesWritten > -1) {
				bytesWritten = is.read(buffer, 0, INTERNAL_BUF_SIZE);
				didRead = true;
				if (bytesWritten < 0) {
					break;
				}
				if ((pos + bytesWritten) >= numberOfBytes) {
					os.write(buffer, 0, (int) (numberOfBytes - pos));
					didRead = false;
					break;
				} else {
					os.write(buffer, 0, bytesWritten);
					didRead = false;
					pos += bytesWritten;
				}
			}
		} catch (IOException e) {
			if (didRead) {
				throw new WriteIOException(e);
			} else {
				throw new ReadIOException(e);
			}
		}
	}

	public static byte[] read(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		copy(is, bos);
		bos.flush();
		byte[] bytes = bos.toByteArray();
		return bytes;
	}
	
	public static void seek(InputStream is, long numberOfBytes) throws IOException {
		if (numberOfBytes < 0) {
			throw new IllegalArgumentException("numberOfBytes has to be a non-negative value");
		}
		is.skip(numberOfBytes);
	}
	
	public static String readToString(InputStream is, String encoding) throws IOException {
		return readToString(new InputStreamReader(is, encoding));
	}
	
	public static String readToString(Reader reader) throws IOException {
		CharArrayWriter writer = new CharArrayWriter();
		char[] buf = new char[INTERNAL_BUF_SIZE];
		int i;
		while ((i = reader.read(buf)) > -1) {
			writer.write(buf, 0, i);
		}
		writer.flush();
		return writer.toString();
	}

	public static Path writeToTempFile(InputStream is) throws IOException {
		UUID uuid = UUID.randomUUID();
		String prefix = uuid.toString();
		Path tmp = Files.createTempFile(prefix, "tmp");
		try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.WRITE)) {
			copy(is, os);
			os.flush();
		}
		return tmp;
	}
}
