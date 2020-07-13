package org.bndly.rest.descriptor;

/*-
 * #%L
 * REST API Descriptor
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

import org.bndly.rest.atomlink.api.annotation.QueryParameter;
import org.bndly.rest.controller.api.QueryParam;
import java.util.Objects;
/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QueryParameterImpl implements QueryParameter {

	private final String name;
	private final boolean asSelector;
	private final String description;

	public QueryParameterImpl(QueryParam qp, String description) {
		if (qp == null) {
			throw new IllegalArgumentException("QueryParam is not allowed to be null");
		}
		this.name = qp.value();
		this.asSelector = qp.asSelector();
		this.description = description;
	}

	public QueryParameterImpl(String name, boolean asSelector, String description) {
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		this.name = name;
		this.asSelector = asSelector;
		this.description = description;
	}

	@Override
	public boolean isAllowedAsSelector() {
		return asSelector;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHumanReadableDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + Objects.hashCode(this.name);
		hash = 47 * hash + (this.asSelector ? 1 : 0);
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
		final QueryParameterImpl other = (QueryParameterImpl) obj;
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (this.asSelector != other.asSelector) {
			return false;
		}
		return true;
	}
	
}
