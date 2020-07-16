package org.bndly.common.json.model;

/*-
 * #%L
 * JSON
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

public class JSMember {
    private JSString name;
    private JSValue value;

    public JSString getName() {
	return name;
    }

    public void setName(JSString name) {
	this.name = name;
    }

    public JSValue getValue() {
	return value;
    }

    public <E extends JSValue> E setValue(E value) {
	this.value = value;
	return value;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
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
	final JSMember other = (JSMember) obj;
	if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
	    return false;
	}
	if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
	    return false;
	}
	return true;
    }
    
}
