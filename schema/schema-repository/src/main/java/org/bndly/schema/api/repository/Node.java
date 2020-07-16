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

import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Node extends BelongsToRepositorySession, IndexedItem {
	
	Node getParent() throws RepositoryException;

	String getName();
	
	String getType();

	Path getPath();

	Iterator<Node> getChildren() throws RepositoryException;

	Node getChild(String name) throws RepositoryException;

	Node createChild(String name, String type) throws RepositoryException;

	boolean isTransient();

	Iterator<Property> getProperties() throws RepositoryException;

	Property getProperty(String name) throws RepositoryException;
	
	boolean isHavingProperty(String name) throws RepositoryException;
	
	Property createProperty(String name, Property.Type type) throws RepositoryException;
	
	Property createMultiProperty(String name, Property.Type type) throws RepositoryException;

	void remove() throws RepositoryException;
}
