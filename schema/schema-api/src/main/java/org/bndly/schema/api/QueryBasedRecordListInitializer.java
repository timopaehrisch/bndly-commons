package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.Engine;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QueryBasedRecordListInitializer implements RecordContext.RecordListInitializer {

	private final Accessor accessor;
	private final String query;
	private final RecordContext recordContext;
	private final Object[] queryArgs;

	public QueryBasedRecordListInitializer(Engine engine, String query, RecordContext recordContext, Object... queryArgs) {
		this(engine.getAccessor(), query, recordContext, queryArgs);
	}
	
	public QueryBasedRecordListInitializer(Accessor accessor, String query, RecordContext recordContext, Object... queryArgs) {
		if (accessor == null) {
			throw new IllegalArgumentException("accessor is not allowed to be null");
		}
		this.accessor = accessor;
		if (query == null) {
			throw new IllegalArgumentException("query is not allowed to be null");
		}
		this.query = query;
		if (recordContext == null) {
			throw new IllegalArgumentException("recordContext is not allowed to be null");
		}
		this.recordContext = recordContext;
		this.queryArgs = queryArgs;
	}
	
	@Override
	public Iterator<Record> initialize() {
		Iterator<Record> iter = new Iterator<Record>() {

			private Iterator<Record> wrappedIterator;

			private Iterator<Record> getIter() {
				if (wrappedIterator == null) {
					wrappedIterator = accessor.query(query, recordContext, null, queryArgs);
					if (wrappedIterator == null) {
						wrappedIterator = Collections.EMPTY_LIST.iterator();
					}
				}
				return wrappedIterator;
			}
			
			@Override
			public boolean hasNext() {
				return getIter().hasNext();
			}

			@Override
			public Record next() {
				return onIterated(getIter().next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can not remove item from query based collection");
			}
			
		};
		return iter;
	}
	
	public Record onIterated(Record iteratedRecord) {
		return iteratedRecord;
	}
	
}
