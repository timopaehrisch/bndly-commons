package org.bndly.common.graph;

/*-
 * #%L
 * Graph Impl
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

import org.bndly.common.graph.CompiledBeanIterator.CompiledBeanIteratorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CompiledBeanIteratorProviderImpl implements CompiledBeanIteratorProvider {

	private final Map<Class, CompiledBeanIterator> iteratorsByType = new HashMap<>();
	private final ReadWriteLock iteratorsByTypeLock = new ReentrantReadWriteLock();
	private TypeBasedReferenceDetector referenceDetector;
	private EntityCollectionDetector entityCollectionDetector;
	
	public void clear() {
		iteratorsByTypeLock.writeLock().lock();
		try {
			iteratorsByType.clear();
		} finally {
			iteratorsByTypeLock.writeLock().unlock();
		}
	}
	
	@Override
	public <F> CompiledBeanIterator<F> getCompiledBeanIteratorForType(Class<F> inspectedType) {
		// first try to just read
		iteratorsByTypeLock.readLock().lock();
		try {
			CompiledBeanIterator iterator = iteratorsByType.get(inspectedType);
			if (iterator != null) {
				return iterator;
			}
		} finally {
			iteratorsByTypeLock.readLock().unlock();
		}
		
		// if reading fails, try to create a new instance
		
		iteratorsByTypeLock.writeLock().lock();
		try {
			CompiledBeanIterator iterator = iteratorsByType.get(inspectedType);
			// meanwhile someone else might have created the iterator
			if (iterator != null) {
				return iterator;
			}
			iterator = new CompiledBeanIterator(this, inspectedType, referenceDetector, entityCollectionDetector);
			iteratorsByType.put(inspectedType, iterator);
			return iterator;
		} finally {
			iteratorsByTypeLock.writeLock().unlock();
		}
		
	}

	public void setEntityCollectionDetector(EntityCollectionDetector entityCollectionDetector) {
		this.entityCollectionDetector = entityCollectionDetector;
	}

	public void setReferenceDetector(TypeBasedReferenceDetector referenceDetector) {
		this.referenceDetector = referenceDetector;
	}
	
}
