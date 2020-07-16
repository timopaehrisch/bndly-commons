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
 * An IndexedItem is an item or entity, that exists in a sorted container. The item knows its index and can be moved around in the container.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface IndexedItem {
	/**
	 * Returns the current index of the item in the container
	 * @return current index
	 */
	long getIndex();
	/**
	 * Moves the item to the provided index in the container
	 * @param index the target index
	 * @throws org.bndly.schema.api.repository.RepositoryException if the item can not be moved, an exception will be thrown
	 */
	void moveToIndex(long index) throws RepositoryException;
}
