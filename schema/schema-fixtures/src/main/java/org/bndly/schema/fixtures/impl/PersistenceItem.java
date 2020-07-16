package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.beans.ActiveRecord;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PersistenceItem implements FixtureDeployerImpl.TransactionAppender {
	private final ActiveRecord activeRecord;
	private final Record record;
	private final String key;
	private boolean persistenceScheduled;
	private List<PersistenceItem> dependsOn;
	private List<DependencyTracker.OneToOneBinding> oneToOneBindings;

	public PersistenceItem(String key, ActiveRecord activeRecord, Record record) {
		if (activeRecord == null) {
			throw new IllegalArgumentException("activeRecord is not allowed to be null");
		}
		this.activeRecord = activeRecord;
		if (key == null) {
			throw new IllegalArgumentException("key is not allowed to be null");
		}
		this.key = key;
		if (record == null) {
			throw new IllegalArgumentException("record is not allowed to be null");
		}
		this.record = record;
	}

	public String getKey() {
		return key;
	}

	public ActiveRecord getActiveRecord() {
		return activeRecord;
	}
	
	public void dependsOn(PersistenceItem item) {
		if (dependsOn == null) {
			dependsOn = new ArrayList<>();
		}
		dependsOn.add(item);
	}

	public void skipPersistence() {
		persistenceScheduled = true;
	}
	
	@Override
	public void appendToTransaction(Transaction transaction) {
		if (persistenceScheduled) {
			return;
		}
		persistenceScheduled = true;
		if (dependsOn != null) {
			for (PersistenceItem dependency : dependsOn) {
				dependency.appendToTransaction(transaction);
			}
		}
		if (record.getType().isVirtual()) {
			return;
		}
		if (activeRecord.getId() == null) {
			activeRecord.persist(transaction);
		} else {
			activeRecord.update(transaction);
		}
		// for one to one relations we need one extra DML statement, because we might need to express a cycle
		if (oneToOneBindings != null) {
			for (DependencyTracker.OneToOneBinding oneToOneBinding : oneToOneBindings) {
				PersistenceItem target = oneToOneBinding.getTarget();
				target.record.setAttributeValue(oneToOneBinding.getAttribute().getToOneAttribute(), record);
				target.activeRecord.updatePostPersist(transaction);
			}
		}
	}

	void oneToOneBindings(List<DependencyTracker.OneToOneBinding> oneToOneBindings) {
		if (oneToOneBindings == null || oneToOneBindings.isEmpty()) {
			this.oneToOneBindings = null;
		} else {
			this.oneToOneBindings = oneToOneBindings;
		}
	}
	
}
