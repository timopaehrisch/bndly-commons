package org.bndly.common.antivirus.impl;

/*-
 * #%L
 * Antivirus Impl
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
import java.io.UnsupportedEncodingException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SlowStringInputStream extends InputStream {

	private final byte[] data;
	private final int sleepTime;
	private int pos = 0;

	public SlowStringInputStream(String string, String charset, int sleepTime) throws UnsupportedEncodingException {
		this.data = string.getBytes(charset);
		this.sleepTime = sleepTime;
	}
	
	@Override
	public int read() throws IOException {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException ex) {
			throw new IOException("interruped while sleeping");
		}
		if(pos >= data.length) {
			return -1;
		}
		byte d = data[pos];
		pos++;
		return d;
	}
	
}
