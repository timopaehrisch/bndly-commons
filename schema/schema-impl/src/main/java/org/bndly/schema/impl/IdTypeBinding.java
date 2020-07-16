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
import org.bndly.schema.model.Type;
import java.util.Objects;

public class IdTypeBinding {

	private final String typeName;
	private final long id;

	public IdTypeBinding(Record r) {
		if (r == null) {
			throw new IllegalArgumentException("can't create " + getClass().getSimpleName() + " without a Record parameter");
		}
		if (r.getId() == null) {
			throw new IllegalArgumentException("can't create " + getClass().getSimpleName() + " when the record id is null");
		}
		this.typeName = r.getType().getName();
		this.id = r.getId();
	}

	public IdTypeBinding(Type type, long id) {
		this(type.getName(), id);
	}

	public IdTypeBinding(String typeName, long id) {
		this.typeName = typeName;
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getTypeName() {
		return typeName;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 43 * hash + Objects.hashCode(this.typeName);
		hash = 43 * hash + (int) (this.id ^ (this.id >>> 32));
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
		final IdTypeBinding other = (IdTypeBinding) obj;
		if (!Objects.equals(this.typeName, other.typeName)) {
			return false;
		}
		if (this.id != other.id) {
			return false;
		}
		return true;
	}

}
