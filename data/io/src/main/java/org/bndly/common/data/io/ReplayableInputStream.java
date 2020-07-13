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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ReplayableInputStream extends FilterInputStream {

	public static final int IN_MEMORY_BUF_LIMIT = SmartBufferOutputStream.SIZE_1_MB;
	private static final byte[] NOTHING = new byte[0];
	private static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(NOTHING);

	private final byte[] bytes;
	private final long readLimit;
	private final boolean closeOriginalOnReplay;
	
	private long length;
	private long bytesRead;
	private long bytesPos;
	private boolean hasBeenWrittenToBuffer;
	private boolean hasStartedReading;
	private boolean noOp;
	private SmartBufferOutputStream bufferOutputStream;

	public static ReplayableInputStream newInstance(byte[] bytes) {
		return new ReplayableInputStream(bytes);
	}

	public static ReplayableInputStream newInstance(InputStream in) throws IOException {
		return newInstance(in, true);
	}

	public static ReplayableInputStream newInstance(InputStream in, boolean closeOriginalOnReplay) throws IOException {
		return newInstance(in, -1, closeOriginalOnReplay);
	}

	public static ReplayableInputStream newInstance(InputStream in, long length) throws IOException {
		return newInstance(in, length, true);
	}

	public static ReplayableInputStream newInstance(InputStream in, long length, boolean closeOriginalOnReplay) throws IOException {
		if (in == null) {
			throw new IllegalArgumentException("wrapped inputstream is not allowed to be null");
		}
		return new ReplayableInputStream(in, length, closeOriginalOnReplay);
	}
	
	protected static ReplayableInputStream newInstance(SmartBufferOutputStream bufferOutputStream) throws IOException {
		if (bufferOutputStream == null) {
			throw new IllegalArgumentException(
				"when creating a replayable inputstream from an already existing smart buffer, then the buffer is not allowed to be null. use an inputstream wrapping factory method."
			);
		}
		bufferOutputStream.flush();
		bufferOutputStream.close();
		return new ReplayableInputStream(bufferOutputStream);
	}

	private ReplayableInputStream(SmartBufferOutputStream bufferOutputStream) {
		super(EMPTY_STREAM);
		bytes = null;
		readLimit = bufferOutputStream.getBytesBuffered();
		length = readLimit;
		closeOriginalOnReplay = true;
		hasBeenWrittenToBuffer = true;
		hasStartedReading = true;
		this.bufferOutputStream = bufferOutputStream;
	}
	
	private ReplayableInputStream(byte[] bytes) {
		super(new ByteArrayInputStream(bytes));
		length = bytes.length;
		readLimit = -1;
		closeOriginalOnReplay = true;
		this.bytes = bytes;
		noOp = false;
	}

	private ReplayableInputStream(InputStream in, long limit, boolean closeOriginalOnReplay) throws IOException {
		super(in);
		hasStartedReading = false;
		this.length = -1;
		this.readLimit = limit;
		bytesRead = 0;
		bytesPos = 0;
		this.closeOriginalOnReplay = closeOriginalOnReplay;
		this.bytes = null;
		noOp = false;
	}

	public final Path getTempFilePath() {
		return getFileSystemBufferPath();
	}

	public static final <E extends InputStream> E replayIfPossible(E is) throws IOException {
		if (is != null && ReplayableInputStream.class.isInstance(is)) {
			((ReplayableInputStream) is).replay();
		}
		return is;
	}

	/**
	 * Converts the replayable input stream to a no-op instance. This invocation
	 * can not be undone. Any subsequent replay will do nothing. If the doReplay
	 * method is invoked and not data has been read yet, then the wrapped stream
	 * will be read completely. No data of any read method will be buffered.
	 *
	 * @return the same ReplayableInputStream instance
	 */
	public final ReplayableInputStream noOp() {
		noOp = true;
		return this;
	}
	
	public final ReplayableInputStream replay() throws IOException {
		if (noOp) {
			return this;
		}
		if (!hasStartedReading) {
			return this;
		}
		if (closeOriginalOnReplay) {
			in.close();
		}
		finishOutputStream();
		if (bytes != null) {
			in = new ByteArrayInputStream(bytes);
		} else {
			in = bufferOutputStream.getBufferedDataAsStream();
		}
		bytesRead = 0; // reset this to 0 in case we re-read the stream
		bytesPos = 0;
		return this;
	}

	/**
	 * This method reads the entire provided inputstream in order to store it in
	 * the buffer. If the ReplayableInputStream is then read again the data will
	 * be served from a buffer.
	 *
	 * If already some bytes have been read from the stream, then only the
	 * already read data will be available in the replay.
	 *
	 * @return the current ReplayableInputStream instance
	 * @throws IOException
	 */
	public final ReplayableInputStream doReplay() throws IOException {
		if (!hasStartedReading) {
			// read this stream first
			IOUtils.consume(this);
		}
		// just call replay
		return replay();
	}

	public final long getLength() {
		return length;
	}

	public final long getBytesRead() {
		return bytesRead;
	}

	public final long getBytesPos() {
		return bytesPos;
	}

	@Override
	public final int read() throws IOException {
		int readData;
		if (readLimit >= 0 && bytesRead >= readLimit) {
			readData = -1;
		} else {
			readData = super.read();
		}
		hasStartedReading = true;
		if (readData == -1) {
			finishOutputStream();
		} else if (!hasBeenWrittenToBuffer) {
			bytesRead++;
			bytesPos++;
			bufferReadBytes(readData, null, null, null);
		}
		return readData;
	}

	@Override
	public final int read(byte []byteArray, int off, int len) throws IOException {
		// check the length with the limit
		if (readLimit > -1) {
			if (bytesRead + len > readLimit) {
				len = (int) (readLimit - bytesRead);
			}
		}
		if (len == 0) {
			// we are done
			finishOutputStream();
			hasStartedReading = true;
			return -1;
		}
		int actuallyRead = super.read(byteArray, off, len);
		hasStartedReading = true;
		if (actuallyRead == -1) {
			finishOutputStream();
		} else {
			if (!hasBeenWrittenToBuffer) {
				bytesRead += actuallyRead;
				bytesPos += actuallyRead;
				bufferReadBytes(null, byteArray, off, actuallyRead);
			}
		}
		return actuallyRead;
	}

	private void bufferReadBytes(Integer singleByte, byte[] multipleBytes, Integer offset, Integer length) throws IOException {
		if (noOp) {
			// this replayable input stream has been set to no-op mode. this 
			// means it has become a plain old filterinputstream again.
			return ;
		}
		
		// was the stream created with a static byte array?
		if (bytes != null) {
			// -> if yes, we don't need to buffer. everything is already in memory
			return;
		}

		if (hasBeenWrittenToBuffer) {
			// we have already buffered everything.
			return;
		}

		if (bufferOutputStream == null) {
			bufferOutputStream = SmartBufferOutputStream.newInstance(IN_MEMORY_BUF_LIMIT, this);
		}
		if (singleByte != null) {
			bufferOutputStream.write(singleByte);
		} else {
			bufferOutputStream.write(multipleBytes, offset, length);
		}
	}

	@Override
	public final long skip(long n) throws IOException {
		// read the bytes.
		byte[] buf = new byte[2048];
		long skipped = 0;
		while (skipped < n) {
			// read
			int readLen;
			if (buf.length < n - skipped) {
				readLen = buf.length;
			} else {
				readLen = (int) (n - skipped);
			}
			int actuallyRead = read(buf, 0, readLen);
			if (actuallyRead < 0) {
				// end
				break;
			}
			skipped += actuallyRead;
		}
		return skipped;
	}
	
	public final long skipUnreplayable(long n) throws IOException {
		long actuallySkipped = super.skip(n);
		if (actuallySkipped > -1) {
			bytesPos += actuallySkipped;
		}
		return actuallySkipped;
	}

	@Override
	public final void close() throws IOException {
		finishOutputStream();
		if (closeOriginalOnReplay) {
			super.close();
		}
	}

	private void finishOutputStream() throws IOException {
		if (!hasBeenWrittenToBuffer) {
			length = bytesPos; // of course this means that we didn't overflow Long.MAX_VALUE
			if (bufferOutputStream != null) {
				bufferOutputStream.flush();
			}
			hasBeenWrittenToBuffer = true;
			if (bufferOutputStream != null) {
				bufferOutputStream.close();
			}
		}
	}

	public final SmartBufferOutputStream getBufferOutputStream() {
		return bufferOutputStream;
	}

	public final boolean isBufferedInMemory() {
		return bufferOutputStream == null ? true : bufferOutputStream.isBufferedInMemory();
	}

	public final Path getFileSystemBufferPath() {
		return bufferOutputStream == null ? null : bufferOutputStream.getFileSystemBufferPath();
	}

}
