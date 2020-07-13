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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class SmartBufferOutputStream extends OutputStream {

	protected static final int SIZE_1_KB = 1024;				// 1KB
	protected static final int SIZE_128_KB = 128 * SIZE_1_KB;	// 128KB
	protected static final int SIZE_512_KB = 4 * SIZE_128_KB;	// 512KB
	protected static final int SIZE_1_MB = 2 * SIZE_512_KB;		// 1MB
	protected static final int SIZE_2_MB = 2 * SIZE_1_MB;		// 2MB
	
	public static final int IN_MEMORY_BUF_LIMIT = SIZE_1_MB;
	
	private static final byte[] NOTHING = new byte[0];
	
	private Path fileSystemBufferPath;
	private OutputStream fileSystemBuffer;
	private ByteArrayOutputStream inMemoryBuffer;
	private long bytesBuffered;
	private boolean closed;
	private final int inMemoryBufferLimit;
	private final ReplayableInputStream createdFrom;
	
	private Object finalizer; // the finalizer will be called when the replayable input stream is getting garbage collected.

	public static SmartBufferOutputStream newInstance() {
		return newInstance(IN_MEMORY_BUF_LIMIT);
	}
	
	public static SmartBufferOutputStream newInstance(int inMemoryBufferLimit) {
		if (inMemoryBufferLimit < 0) {
			inMemoryBufferLimit = 0;
		}
		return new SmartBufferOutputStream(inMemoryBufferLimit, null);
	}
	
	protected static SmartBufferOutputStream newInstance(int inMemoryBufferLimit, ReplayableInputStream createdFrom) {
		if (inMemoryBufferLimit < 0) {
			inMemoryBufferLimit = 0;
		}
		return new SmartBufferOutputStream(inMemoryBufferLimit, createdFrom);
	}
	
	private SmartBufferOutputStream(int inMemoryBufferLimit, ReplayableInputStream createdFrom) {
		this.closed = false;
		this.inMemoryBufferLimit = inMemoryBufferLimit;
		this.createdFrom = createdFrom;
	}
	
	@Override
	public final void write(int b) throws IOException {
		bufferReadBytes(b, null, null, null);
	}

	@Override
	public final void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public final void write(byte[] b, int off, int len) throws IOException {
		bufferReadBytes(null, b, off, len);
	}
	
	private void bufferReadBytes(Integer singleByte, byte[] multipleBytes, Integer offset, Integer length) throws IOException {
		if (closed) {
			throw new IOException("smart buffer has already been closed.");
		}
		OutputStream theBufferToFill;
		// how many bytes have been read?
		int newBytes = singleByte != null ? 1 : length - offset;
		if (bytesBuffered + newBytes > inMemoryBufferLimit || fileSystemBuffer != null) {
			// more than IN_MEMORY_BUF_LIMIT?
			// -> open/create the temp file and write the inMemoryBuffer to it
			if (fileSystemBuffer == null) {
				fileSystemBufferPath = Files.createTempFile(UUID.randomUUID().toString(), ".tmp");

				/**
				 * A good reference on how and when to use finalizers:
				 * https://www.securecoding.cert.org/confluence/display/java/MET12-J.+Do+not+use+finalizers
				 *
				 * Since the replayable input stream might be used in a server
				 * application that runs pretty long, it makes sense to delete
				 * file buffers as soon as the replayable input stream is not
				 * available anymore to other objects.
				 */
				finalizer = new Object() {

					private final Path bufferPath = fileSystemBufferPath;

					@Override
					protected void finalize() throws Throwable {
						try {
							if (bufferPath != null) {
								// uncomment when debugging. do not print this statement in production.
								Files.deleteIfExists(bufferPath);
							}
						} catch (Throwable e) {
							// failed to delete the file
							System.err.println("failed to delete file " + bufferPath + ": " + e.getMessage());
						} finally {
							super.finalize();
						}
					}

				};
				fileSystemBufferPath.toFile().deleteOnExit();
				fileSystemBuffer = Files.newOutputStream(fileSystemBufferPath, StandardOpenOption.WRITE);
			}

			if (inMemoryBuffer != null) {
				fileSystemBuffer.write(inMemoryBuffer.toByteArray());

				// -> then drop the inMemoryBuffer to free the memory
				inMemoryBuffer = null;
			}

			// -> write the read bytes to the fileSystemBuffer.
			theBufferToFill = fileSystemBuffer;
		} else {
			// less than IN_MEMORY_BUF_LIMIT?
			// -> is there an inMemoryBufferYet?
			if (inMemoryBuffer == null) {
				//   -> if not, create one
				inMemoryBuffer = new ByteArrayOutputStream();
			}

			// -> write the read bytes to the inMemoryBufferYet.
			theBufferToFill = inMemoryBuffer;
		}
		if (singleByte != null) {
			theBufferToFill.write(singleByte);
		} else {
			theBufferToFill.write(multipleBytes, offset, length);
		}
		bytesBuffered += newBytes;
	}

	@Override
	public final void flush() throws IOException {
		OutputStream buf = getCurrentBuffer();
		if (buf != null) {
			buf.flush();
		}
	}

	@Override
	public final void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		OutputStream buf = getCurrentBuffer();
		if (buf != null) {
			buf.close();
		}
	}
	
	public final InputStream getBufferedDataAsStream() throws IOException {
		if (!closed) {
			flush();
			close();
		}

		InputStream in;
		if (isBufferedInMemory()) {
			if (inMemoryBuffer == null) {
				in = new ByteArrayInputStream(NOTHING);
			} else {
				in = new ByteArrayInputStream(inMemoryBuffer.toByteArray());
			}
		} else {
			in = Files.newInputStream(fileSystemBufferPath, StandardOpenOption.READ);
		}
		return in;
	}
	
	public final ReplayableInputStream getBufferedDataAsReplayableStream() throws IOException {
		return createdFrom != null
				? createdFrom
				: isBufferedInMemory()
					? ReplayableInputStream.newInstance(inMemoryBuffer == null ? NOTHING : inMemoryBuffer.toByteArray()) 
					: ReplayableInputStream.newInstance(Files.newInputStream(fileSystemBufferPath, StandardOpenOption.READ));
	}

	public final long getBytesBuffered() {
		return bytesBuffered;
	}
	
	private OutputStream getCurrentBuffer() {
		return fileSystemBuffer != null ? fileSystemBuffer : inMemoryBuffer;
	}
	
	public final boolean isBufferedInMemory() {
		return fileSystemBuffer == null;
	}

	public final Path getFileSystemBufferPath() {
		return fileSystemBufferPath;
	}
	
	public final Path getTempFilePath() {
		return fileSystemBufferPath;
	}
}
