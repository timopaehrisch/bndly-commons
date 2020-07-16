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
import org.bndly.schema.api.RecordValue;
import org.bndly.schema.model.Attribute;

public final class RecordValueImpl implements RecordValue {
    private final Record record;
    private final Object value;
    private final Attribute attribute;

    public RecordValueImpl(Record record, Attribute attribute, Object value) {
        this.record = record;
        this.attribute = attribute;
        this.value = value;
    }

	@Override
    public Record getRecord() {
        return record;
    }

    public String getAttributeName() {
        return attribute.getName();
    }

	@Override
    public Attribute getAttribute() {
        return attribute;
    }

	@Override
    public Object getValue() {
        return value;
    }

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(record.toString()).append(" ").append(attribute.getName()).append(": ").append(value);
		String stringValue = sb.toString();
		return stringValue;
	}
    
}
