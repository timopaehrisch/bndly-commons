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

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TeeOutputStream extends OutputStream {
	private final OutputStream left;
	private final OutputStream right;
	
	private boolean failOnLeftFailure;
	private boolean failOnRightFailure;
	
	private boolean leftFailure;
	private boolean rightFailure;
	

	public TeeOutputStream(OutputStream left, OutputStream right) {
		if (left == null) {
			throw new IllegalArgumentException("left outputstream is not allowed to be null");
		}
		this.left = left;
		if (right == null) {
			throw new IllegalArgumentException("right outputstream is not allowed to be null");
		}
		this.right = right;
	}
	
	public TeeOutputStream failOnSingleFailure() {
		return failOnLeftFailure().failOnRightFailure();
	}
	
	public TeeOutputStream failOnLeftFailure() {
		failOnLeftFailure = true;
		return this;
	}
	public TeeOutputStream failOnRightFailure() {
		failOnRightFailure = true;
		return this;
	}
	
	@Override
	public void write(int b) throws IOException {
		try {
			left.write(b);
		} catch (IOException e) {
			if (failOnLeftFailure || rightFailure) {
				throw e;
			}
			leftFailure = true;
		}
		try {
			right.write(b);
		} catch (IOException e) {
			if (failOnRightFailure || leftFailure) {
				throw e;
			}
			rightFailure = true;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			left.write(b, off, len);
		} catch (IOException e) {
			if (failOnLeftFailure || rightFailure) {
				throw e;
			}
			leftFailure = true;
		}
		try {
			right.write(b, off, len);
		} catch (IOException e) {
			if (failOnRightFailure || leftFailure) {
				throw e;
			}
			rightFailure = true;
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			left.flush();
		} catch (IOException e) {
			if (failOnLeftFailure || rightFailure) {
				throw e;
			}
			leftFailure = true;
		}
		try {
			right.flush();
		} catch (IOException e) {
			if (failOnRightFailure || leftFailure) {
				throw e;
			}
			rightFailure = true;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			left.close();
		} catch (IOException e) {
			if (failOnLeftFailure || rightFailure) {
				throw e;
			}
			leftFailure = true;
		} finally {
			try {
				right.close();
			} catch (IOException e) {
				if (failOnRightFailure || leftFailure) {
					throw e;
				}
				rightFailure = true;
			}
		}
	}

	public OutputStream getLeft() {
		return left;
	}

	public OutputStream getRight() {
		return right;
	}
	
}
