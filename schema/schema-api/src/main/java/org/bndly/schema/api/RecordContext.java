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

import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.Type;
import java.util.Iterator;

public interface RecordContext {
	public static interface RecordReference {
		Record getReferencedBy();
		Attribute getReferencedAs();
	}
	public static interface RecordListInitializer {
		Iterator<Record> initialize();
	}

    Record get(Type type, long id);
    Record get(String typeName, long id);
    void persisted(Record record);
    Record attach(Record record);
    void detach(Record record);
    Record create(Type type);
    Record create(String typeName);
    Record create(Type type, long id);
    Record create(String typeName, long id);
	long size();
	long unpersistedEntriesSize();
	long persistedEntriesSize();
	Iterator<RecordReference> listReferencesToRecord(Record record);
	Iterator<Record> listPersistedRecordsOfType(String typeName);
	Iterator<Record> listPersistedRecordsOfType(Type type);
	Iterator<Record> listPersistedRecords();
	boolean isAttached(Record record);
	RecordList createList(Record owner, String inverseAttributeName);
	RecordList createList(Record owner, InverseAttribute inverseAttribute);
	RecordList createList(RecordListInitializer initializer, Record owner, String inverseAttributeName);
	RecordList createList(RecordListInitializer initializer, Record owner, InverseAttribute inverseAttribute);
}
