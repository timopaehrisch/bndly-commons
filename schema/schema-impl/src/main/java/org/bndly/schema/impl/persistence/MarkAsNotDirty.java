package org.bndly.schema.impl.persistence;

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

import org.bndly.schema.api.CommitHandler;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.impl.EngineImpl;
import org.bndly.schema.impl.RecordImpl;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MarkAsNotDirty implements TransactionAppender {

	public static final MarkAsNotDirty INSTANCE = new MarkAsNotDirty();

	@Override
	public void append(Transaction tx, final Record record, EngineImpl engine) {
		tx.afterCommit(new CommitHandler() {

			@Override
			public void didCommit(Transaction transaction) {
				markRecordAsNotDirty(record);
			}
		});
	}

	private void markRecordAsNotDirty(Record record) {
		if (RecordImpl.class.isInstance(record)) {
			((RecordImpl) record).setIsDirty(false);
		}
	}
}
