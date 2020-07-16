package org.bndly.rest.cache.impl;

/*-
 * #%L
 * REST Cache
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

import org.bndly.rest.cache.api.CacheEventListener;
import org.bndly.rest.cache.api.CacheTransaction;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NoOpCacheTransaction implements CacheTransaction {

	public static final NoOpCacheTransaction INSTANCE = new NoOpCacheTransaction();
	
	private NoOpCacheTransaction() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void flush(String path) {
	}

	@Override
	public void flushRecursive(String path) {
	}

	@Override
	public void replayEvents(CacheEventListener cacheEventListener) {
	}

	@Override
	public void close() {
	}
	
}
