package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.io.SmartBufferOutputStream;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.Context;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CacheWritingContextWrapper extends ContextWrapperImpl {
	private static final Logger LOG = LoggerFactory.getLogger(CacheWritingContextWrapper.class);
	public CacheWritingContextWrapper(Context wrapped) {
		super(wrapped);
	}

	private OutputStream wrappedOS;
	private SmartBufferOutputStream smartBufferOutputStream;

	@Override
	public OutputStream getOutputStream() throws IOException {
		CacheContext cacheContext = getWrapped().getCacheContext();
		if (cacheContext != null && cacheContext.isCachingPrevented()) {
			return getWrapped().getOutputStream();
		}

		if (wrappedOS == null) {
			smartBufferOutputStream = SmartBufferOutputStream.newInstance();
			wrappedOS = new FilterOutputStream(smartBufferOutputStream) {
				boolean didSaveInCache = false;
				ReplayableInputStream replayableInputStream = null;

				@Override
				public void write(byte[] b) throws IOException {
					out.write(b);
				}

				@Override
				public void write(int b) throws IOException {
					out.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					out.write(b, off, len);
				}
				
				private ReplayableInputStream getInputStream() throws IOException {
					if (replayableInputStream == null) {
						replayableInputStream = smartBufferOutputStream.getBufferedDataAsReplayableStream();
					}
					return replayableInputStream;
				}

				private void save() throws IOException {
					CacheContext cacheContext = getWrapped().getCacheContext();
					if (cacheContext != null && cacheContext.isCachingPrevented()) {
						return;
					}
					if (!didSaveInCache) {
						didSaveInCache = true;
						ReplayableInputStream is = getInputStream();
						if (canBeCached()) {
							long start = System.currentTimeMillis();
							try {
								getWrapped().saveInCache(is);
							} finally {
								long end = System.currentTimeMillis();
								LOG.trace("saving cache entry took {}ms", (end - start));
							}
						}
						long start = System.currentTimeMillis();
						try {
							OutputStream os = getWrapped().getOutputStream();
							IOUtils.copy(is, os);
							os.flush();
						} finally {
							long end = System.currentTimeMillis();
							LOG.trace("send data {}ms", (end - start));
						}
					}
				}
				
				@Override
				public void flush() throws IOException {
					super.flush();
					save();
				}

				@Override
				public void close() throws IOException {
					super.close();
					save();
				}
			};
		}
		return wrappedOS;
	}
}
