package org.bndly.rest.swagger.impl;

/*-
 * #%L
 * REST Swagger Integration
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

import org.bndly.rest.swagger.model.Document;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;

/**
 * The SwaggerDocumentProvider holds a single instance of a swagger documentation document. 
 * The document can be accessed via {@link #read(org.bndly.rest.swagger.impl.SwaggerDocumentProvider.Consumer) } 
 * or {@link #write(org.bndly.rest.swagger.impl.SwaggerDocumentProvider.Consumer) }.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = SwaggerDocumentProvider.class)
public class SwaggerDocumentProvider {
	public static interface Consumer<E> {
		E consume(Document swaggerDocument);
	}
	
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Document swaggerDocument = new Document();
	
	public <E> E write(Consumer<E> consumer) {
		readWriteLock.writeLock().lock();
		try {
			return consumer.consume(swaggerDocument);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}
	
	public <E> E read(Consumer<E> consumer) {
		readWriteLock.readLock().lock();
		try {
			return consumer.consume(swaggerDocument);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
}
