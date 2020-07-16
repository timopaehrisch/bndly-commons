package org.bndly.common.service.cache.keys;

/*-
 * #%L
 * Service Cache
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

import org.bndly.common.service.cache.api.EntityCacheKey;
import org.bndly.common.service.model.api.Identity;
import java.io.Serializable;

public class EntityCacheKeyImpl implements EntityCacheKey<Identity>, Serializable {

	private final String entityTypeName;
	private final Identity identity;
	protected final String locale;

	public EntityCacheKeyImpl(String entityTypeName, Identity identity) {
		this(entityTypeName, identity, null);
	}

	public EntityCacheKeyImpl(String entityTypeName, Identity identity, String locale) {
		this.entityTypeName = entityTypeName;
		this.identity = identity;
		this.locale = locale;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 97 * hash + (this.entityTypeName != null ? this.entityTypeName.hashCode() : 0);
		hash = 97 * hash + (this.identity != null ? this.identity.hashCode() : 0);
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
		final EntityCacheKeyImpl other = (EntityCacheKeyImpl) obj;
		if ((this.entityTypeName == null) ? (other.entityTypeName != null) : !this.entityTypeName.equals(other.entityTypeName)) {
			return false;
		}
		if (this.identity != other.identity && (this.identity == null || !this.identity.equals(other.identity))) {
			return false;
		}
		return true;
	}

	@Override
	public int _hashCode() {
		return hashCode();
	}

	@Override
	public boolean _equals(Object o) {
		return equals(o);
	}

	@Override
	public String getEntityTypeName() {
		return entityTypeName;
	}

	@Override
	public Identity getIdentity() {
		return identity;
	}

	@Override
	public String toString() {
		return "EntityCacheKeyImpl{" + "entityTypeName=" + entityTypeName + ", identity=" + identity + ", hashCode=" + this._hashCode() + '}';
	}

}
