package org.bndly.schema.api.repository;

/*-
 * #%L
 * Schema Repository
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface RepositorySession extends AutoCloseable {

	boolean isReadOnly();
	
	Node getRoot() throws RepositoryException;

	Node getNode(Path path) throws NodeNotFoundException, RepositoryException;
	
	RepositorySession flush() throws RepositoryException;
	
	EntityReference createEntityReference(String typeName, long id) throws RepositoryException;

	/**
	 * Close closes the session and releases any used resources. Close does not mean, that any intermediate state is being flushed.
	 *
	 * @throws org.bndly.schema.api.repository.RepositoryException If closing the session fails, a repository exception will be thrown
	 */
	@Override
	public void close() throws RepositoryException;
	
}
