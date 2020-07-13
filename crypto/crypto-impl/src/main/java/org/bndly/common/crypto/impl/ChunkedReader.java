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

import java.io.IOException;
import java.io.InputStream;

/**
 * The ChunkedReader reads an input stream in chunks of fixed size.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ChunkedReader {
	private final InputStream is;
	private final byte[] inBuf;
	private final int bufSize;

	/**
	 * Create a ChunkedReader for a specific stream with a fixed chunk size.
	 * @param is the stream to read
	 * @param bufSize the size of the chunks
	 */
	public ChunkedReader(InputStream is, int bufSize) {
		this.is = is;
		this.inBuf = new byte[bufSize];
		this.bufSize = bufSize;
	}
	
	/**
	 * This method starts reading the wrapped input stream in chunks.
	 * @return the same chunked reader instance
	 * @throws IOException if reading the stream fails or if either {@link #onChunk(byte[])} or {@link #onLastChunk(byte[], int)} fail
	 */
	public final ChunkedReader startReading() throws IOException {
		int bytesRead = 0;
		int bytesOfChunkRead = 0;
		while ((bytesRead = is.read(inBuf, bytesOfChunkRead, inBuf.length - bytesOfChunkRead)) > -1) {
			bytesOfChunkRead += bytesRead;
			if (bytesOfChunkRead < bufSize) {
				continue;
			}
			onChunk(inBuf);
			bytesOfChunkRead = 0;
		}
		if (bytesOfChunkRead > 0) {
			onLastChunk(inBuf, bytesOfChunkRead);
		}
		return this;
	}
	
	protected abstract void onChunk(byte[] buf) throws IOException;
	
	protected abstract void onLastChunk(byte[] buf, int filledUpToIndex) throws IOException;
	
}
