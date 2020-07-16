package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.common.json.model.JSObject;
import org.bndly.schema.api.Record;
import java.util.Objects;

public class MarshalledRecord {
    private final String typeName;
    private final long id;
    private final JSObject jSObject;

    public MarshalledRecord(Record record, JSObject jSObject) {
        typeName = record.getType().getName();
        id = record.getId();
        this.jSObject = jSObject;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.typeName);
        hash = 73 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MarshalledRecord other = (MarshalledRecord) obj;
        if (!Objects.equals(this.typeName, other.typeName)) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public JSObject getjSObject() {
        return jSObject;
    }
    
    
}
