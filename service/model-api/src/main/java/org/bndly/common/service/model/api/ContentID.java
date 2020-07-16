package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import java.io.Serializable;

public final class ContentID implements Serializable {
    private String value;

    public ContentID() {
    }

    public ContentID(ContentID cid) {
        this.value = cid.value;
    }
    
    public ContentID(int i) {
        this(Integer.toString(i));
    }
    
    public ContentID(long i) {
        this(Long.toString(i));
    }
    
    public ContentID(String value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.value != null ? this.value.hashCode() : 0);
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
        final ContentID other = (ContentID) obj;
        if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    
    
}
