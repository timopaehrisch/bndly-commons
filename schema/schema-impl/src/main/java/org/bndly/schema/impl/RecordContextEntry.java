package org.bndly.schema.impl;

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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext.RecordReference;
import org.bndly.schema.model.Attribute;
import java.util.ArrayList;
import java.util.List;

public final class RecordContextEntry {

	public static class RecordReferenceImpl implements RecordReference {

		private final Record referencedBy;
		private final Attribute referencedAs;

		public RecordReferenceImpl(Record referencedBy, Attribute referencedAs) {
			this.referencedBy = referencedBy;
			this.referencedAs = referencedAs;
		}

		@Override
		public Attribute getReferencedAs() {
			return referencedAs;
		}

		@Override
		public Record getReferencedBy() {
			return referencedBy;
		}

	}

	private final Record record;
	private final RecordContextImpl recordContext;
	private final List<RecordReference> references = new ArrayList<>();

	public RecordContextEntry(Record record, RecordContextImpl recordContext) {
		if (record == null) {
			throw new IllegalArgumentException("record context entry can not be created for null record");
		}
		if (recordContext == null) {
			throw new IllegalArgumentException("record context entry can not be created for null recordContext");
		}
		this.record = record;
		this.recordContext = recordContext;
	}

	public final RecordContextImpl getRecordContext() {
		return recordContext;
	}

		public final Record getRecord() {
		return record;
	}

	public final List<RecordReference> getReferences() {
		return references;
	}

	public final long referenceCount() {
		return references.size();
	}
}
