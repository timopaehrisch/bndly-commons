package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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

import org.bndly.rest.controller.api.ExceptionMapper;
import org.bndly.rest.controller.api.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DelegatingExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

	private final List<ExceptionMapper<T>> mappers = new ArrayList<>();
	private final ReadWriteLock mappserLock = new ReentrantReadWriteLock();
	
	@Override
	public Response toResponse(T throwable) {
		Response r = null;
		mappserLock.readLock().lock();
		try {
			for (ExceptionMapper<T> exceptionMapper : mappers) {
				try {
					r = exceptionMapper.toResponse(throwable);
					if (r != null) {
						break;
					}
				} catch (ClassCastException ex) {
					// no handling. hence return an empty response
				}
			}
			return r;
		} finally {
			mappserLock.readLock().unlock();
		}
	}
	
	public void clear() {
		mappserLock.writeLock().lock();
		try {
			mappers.clear();
		} finally {
			mappserLock.writeLock().unlock();
		}
	}
	
	public void registerMapper(ExceptionMapper<T> mapper) {
		mappserLock.writeLock().lock();
		try {
			mappers.add(0, mapper);
		} finally {
			mappserLock.writeLock().unlock();
		}
	}
	
	public void unregisterMapper(ExceptionMapper<T> mapper) {
		mappserLock.writeLock().lock();
		try {
			mappers.remove(mapper);
		} finally {
			mappserLock.writeLock().unlock();
		}
	}
	
}
