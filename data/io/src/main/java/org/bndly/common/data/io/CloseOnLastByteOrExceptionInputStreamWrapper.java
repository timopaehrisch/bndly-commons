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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CloseOnLastByteOrExceptionInputStreamWrapper extends FilterInputStream {

	public CloseOnLastByteOrExceptionInputStreamWrapper(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		try {
			int r = in.read();
			if (r == -1) {
				in.close();
			}
			return r;
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		try {
			int r = in.read(b);
			if (r == -1) {
				in.close();
			}
			return r;
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			int r = in.read(b, off, len);
			if (r == -1) {
				in.close();
			}
			return r;
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		try {
			long r = in.skip(n);
			// we have hit the end
			if (r < n) {
				in.close();
			}
			return r;
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		try {
			in.reset();
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}

	@Override
	public int available() throws IOException {
		try {
			return in.available();
		} catch (Exception e) {
			in.close();
			throw e;
		}
	}
	
}
