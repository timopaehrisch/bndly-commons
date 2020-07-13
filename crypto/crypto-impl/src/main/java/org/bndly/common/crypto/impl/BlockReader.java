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
 * A BlockReader is used to read input data in a fixed size data blocks.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class BlockReader {
	private final int blockSize;
	private final byte[] blockBuffer;

	/**
	 * Creates a BlockReader with a fixed block size.
	 * @param blockSize 
	 */
	public BlockReader(int blockSize) {
		this.blockSize = blockSize;
		this.blockBuffer = new byte[blockSize];
	}
	
	/**
	 * This method will iterate of the array of provided raw data bytes and invokes {@link #doWithBlock(byte[], int)} for each block.
	 * @param data the data that should be broken down into blocks.
	 * @throws IOException if the implementation of {@link #doWithBlock(byte[], int)} fails.
	 */
	public void read(byte[] data) throws IOException {
		int offset = 0;
		while (true) {
			int from = offset;
			int to = from + blockSize;
			if (to > data.length) {
				to = data.length;
				// last call
				int len = to - from;
				if (len == 0) {
					break;
				}
				System.arraycopy(data, from, blockBuffer, 0, len);
				doWithBlock(blockBuffer, len);
				break;
			} else {
				System.arraycopy(data, from, blockBuffer, 0, blockSize);
				doWithBlock(blockBuffer, blockSize);
				offset += blockSize;
			}
		}
	}
	
	/**
	 * This method will read the provided stream's raw data bytes and invokes {@link #doWithBlock(byte[], int)} for each block.
	 * @param is the stream of data that should be broken down into blocks.
	 * @throws IOException if the implementation of {@link #doWithBlock(byte[], int)} fails.
	 */
	public void read(InputStream is) throws IOException {
		int offset = 0;
		int i;
		while ((i = is.read(blockBuffer, offset, blockSize - offset)) > -1) {
			offset += i;
			offset = offset % blockSize;
			if (offset == 0 && i != 0) {
				// a block has been read
				doWithBlock(blockBuffer, blockSize);
			}
		}
		if (offset > 0) {
			// last block was only partially available
			doWithBlock(blockBuffer, offset);
		}
	}

	/**
	 * This method should be overwritten in order to perform logic for each block of data, that has been read by {@link #read(java.io.InputStream)} or {@link #read(byte[])}.
	 * @param blockBuffer the blocks data as a byte array
	 * @param length the length of the block data in the provided array. the last block may be incomplete.
	 * @throws IOException if the internal handling of the raw data block fails for whatever reason
	 */
	protected abstract void doWithBlock(byte[] blockBuffer, int length) throws IOException;
	
}
