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

import org.bndly.common.service.cache.api.KeyParameter;
import org.bndly.common.service.cache.api.MethodInvocationCacheKey;
import java.io.Serializable;
import java.util.List;

public class MethodInvocationCacheKeyImpl implements MethodInvocationCacheKey, Serializable {

	private final String methodName;
	private final List<KeyParameter> parameters;
	protected final String locale;

	public MethodInvocationCacheKeyImpl(String methodName, List<KeyParameter> parameters) {
		this(methodName, parameters, null);
	}

	public MethodInvocationCacheKeyImpl(String methodName, List<KeyParameter> parameters, String locale) {
		this.methodName = methodName;
		this.parameters = parameters;
		this.locale = locale;
	}

	@Override
	public String getMethodName() {
		return methodName;
	}

	@Override
	public List<KeyParameter> getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 73 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
		hash = 73 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
		hash = 73 * hash + (this.locale != null ? this.locale.hashCode() : 0);
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
		final MethodInvocationCacheKeyImpl other = (MethodInvocationCacheKeyImpl) obj;
		if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
			return false;
		}
		if (this.parameters != other.parameters && (this.parameters == null || !this.parameters.equals(other.parameters))) {
			return false;
		}
		if ((this.locale == null) ? (other.locale != null) : !this.locale.equals(other.locale)) {
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
	public String toString() {
		return "MethodInvocationCacheKeyImpl{" + "methodName=" + methodName + ", parameters=" + parameters + ", hashCode=" + this._hashCode() + '}';
	}

}
