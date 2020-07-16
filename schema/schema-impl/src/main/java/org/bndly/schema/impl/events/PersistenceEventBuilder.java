package org.bndly.schema.impl.events;

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
import org.bndly.schema.api.Logic;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.impl.EngineImpl;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PersistenceEventBuilder {

	private final EngineImpl engine;
	private List<Logic> logic;

	public PersistenceEventBuilder(EngineImpl engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine is not allowed to be null");
		}
		this.engine = engine;
	}

	public void scheduleMergeEvent(final Record record) {
		if (logic == null) {
			logic = new ArrayList<>();
		}
		logic.add(new Logic() {

			@Override
			public void execute(Transaction transaction) {
				List<MergeListener> listeners = engine.getListeners(MergeListener.class, record.getType().getName());
				for (MergeListener listener : listeners) {
					listener.onMerge(record);
				}
			}
		});
	}

	public void schedulePersistEvent(final Record record) {
		if (logic == null) {
			logic = new ArrayList<>();
		}
		logic.add(new Logic() {

			@Override
			public void execute(Transaction transaction) {
				List<PersistListener> listeners = engine.getListeners(PersistListener.class, record.getType().getName());
				for (PersistListener listener : listeners) {
					listener.onPersist(record);
				}
			}
		});
	}

	public void scheduleDeleteEvent(final Record record) {
		if (logic == null) {
			logic = new ArrayList<>();
		}
		logic.add(new Logic() {

			@Override
			public void execute(Transaction transaction) {
				List<DeleteListener> listeners = engine.getListeners(DeleteListener.class, record.getType().getName());
				for (DeleteListener listener : listeners) {
					listener.onDelete(record);
				}
			}
		});
	}
	
	public boolean hasEventsScheduled() {
		return logic != null;
	}
	
	public void attachToTransaction(Transaction transaction) {
		transaction.afterCommit(new CommitHandler() {

			@Override
			public void didCommit(Transaction transaction) {
				for (Logic l : logic) {
					l.execute(transaction);
				}
			}
		});
	}

}
