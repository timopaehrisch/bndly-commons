package org.bndly.schema.impl.repository;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.repository.RepositoryException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class IndexManager {
	
	private Long nextChildIndex;
	private final IndexContext indexContext;

	public static interface IndexContext {
		boolean isTransient();
		long countChildren() throws RepositoryException;
	}

	public IndexManager(IndexContext indexContext) {
		if (indexContext == null) {
			throw new IllegalArgumentException("indexContext is not allowed to be null");
		}
		this.indexContext = indexContext;
	}

	public long pullNextChildIndex() throws RepositoryException {
		if (nextChildIndex == null) {
			if (indexContext.isTransient()) {
				nextChildIndex = 1L;
				return 0;
			} else {
				nextChildIndex = indexContext.countChildren() + 1;
				return nextChildIndex - 1;
			}
		} else {
			long index = nextChildIndex;
			nextChildIndex++;
			return index;
		}
	}
}
