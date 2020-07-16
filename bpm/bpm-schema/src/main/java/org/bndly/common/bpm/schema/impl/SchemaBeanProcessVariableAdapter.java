package org.bndly.common.bpm.schema.impl;

/*-
 * #%L
 * BPM Schema
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

import org.bndly.common.bpm.api.ProcessVariable;
import org.bndly.common.bpm.api.ProcessVariableAdapter;
import org.bndly.schema.api.Record;
import org.bndly.schema.beans.SchemaBeanFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = ProcessVariableAdapter.class, immediate = true)
public class SchemaBeanProcessVariableAdapter implements ProcessVariableAdapter {

	private final List<SchemaBeanFactory> schemaBeanFactories = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Reference(
			bind = "addSchemaBeanFactory",
			unbind = "removeSchemaBeanFactory",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = SchemaBeanFactory.class
	)
	public void addSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory != null) {
			lock.writeLock().lock();
			try {
				schemaBeanFactories.add(schemaBeanFactory);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory != null) {
			lock.writeLock().lock();
			try {
				Iterator<SchemaBeanFactory> iterator = schemaBeanFactories.iterator();
				while (iterator.hasNext()) {
					SchemaBeanFactory next = iterator.next();
					if (next == schemaBeanFactory) {
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public boolean doesSupport(ProcessVariable variable) {
		if (variable.getValue() == null) {
			return false;
		}
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactories) {
				if (schemaBeanFactory.isSchemaBean(variable.getValue())) {
					return true;
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return false;
	}

	@Override
	public String getType(ProcessVariable variable) {
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactories) {
				Record record = schemaBeanFactory.getRecordFromSchemaBean(variable.getValue());
				if (record != null) {
					return record.getType().getName();
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return null;
	}

}
