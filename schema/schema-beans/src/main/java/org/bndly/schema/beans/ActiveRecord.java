package org.bndly.schema.beans;

/*-
 * #%L
 * Schema Beans
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

import org.bndly.schema.api.Transaction;

public interface ActiveRecord {
	void persist(Transaction transaction);

	void persistCascaded(Transaction transaction);

	void update(Transaction transaction);

	void updateCascaded(Transaction transaction);
	
	/**
	 * Calling this method will prevent null checks on the record id, because the persist statement is assumed to be handled in the same transaction. The built query would be the same as in {@link #update(org.bndly.schema.api.Transaction)}.
	 * @param transaction The transaction to which the update statements should be appended.
	 */
	void updatePostPersist(Transaction transaction);

	void delete(Transaction transaction);

	void persist();

	void persistCascaded();

	void update();

	void updateCascaded();

	void delete();

	void reload();

	Long getId();

	boolean isReference();
}
