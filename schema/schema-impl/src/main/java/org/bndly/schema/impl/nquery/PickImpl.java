package org.bndly.schema.impl.nquery;

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

import org.bndly.schema.api.nquery.Pick;
import org.bndly.schema.api.nquery.IfClause;
import org.bndly.schema.api.nquery.Ordering;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PickImpl implements Pick {
	private String attributeHolderName;
	private String attributeHolderNameAlias;
	private IfClause ifClause;
	private Long limit;
	private Long offset;
	private Ordering ordering;

	@Override
	public String getAttributeHolderName() {
		return attributeHolderName;
	}

	@Override
	public String getAttributeHolderNameAlias() {
		return attributeHolderNameAlias;
	}

	@Override
	public IfClause getIfClause() {
		return ifClause;
	}

	@Override
	public Long getOffset() {
		return offset;
	}

	@Override
	public Long getLimit() {
		return limit;
	}

	@Override
	public Ordering getOrdering() {
		return ordering;
	}

	public void setAttributeHolderName(String attributeHolderName) {
		this.attributeHolderName = attributeHolderName;
	}

	public void setAttributeHolderNameAlias(String attributeHolderNameAlias) {
		this.attributeHolderNameAlias = attributeHolderNameAlias;
	}

	public void setIfClause(IfClause ifClause) {
		this.ifClause = ifClause;
	}

	public void setLimit(Long limit) {
		this.limit = limit;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public void setOrdering(Ordering ordering) {
		this.ordering = ordering;
	}
	
}
