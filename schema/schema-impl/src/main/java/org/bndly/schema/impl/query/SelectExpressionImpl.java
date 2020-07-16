package org.bndly.schema.impl.query;

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

import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.SelectExpression;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;

public class SelectExpressionImpl extends ContextRelatedQueryComponentImpl implements SelectExpression {

	private String fieldName;
	private String alias;
	private String tableAlias;
	private String countedFieldOrAlias;

	public SelectExpressionImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public SelectExpression table(String tableAlias) {
		this.tableAlias = tableAlias;
		return this;
	}

	@Override
	public SelectExpression field(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	@Override
	public SelectExpression as(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public SelectExpression count(String countedFieldOrAlias) {
		this.countedFieldOrAlias = countedFieldOrAlias;
		return this;
	}

	@Override
	public SelectExpression count() {
		return count("*");
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		if (countedFieldOrAlias != null) {
			ctx.getSql().append("COUNT(");
			ctx.getSql().append(countedFieldOrAlias);
			ctx.getSql().append(')');
		} else {
			if (tableAlias != null) {
				ctx.getSql().append(tableAlias);
				ctx.getSql().append('.');
			}
			ctx.getSql().append(fieldName);
		}
		if (alias != null) {
			ctx.getSql().append(" AS ");
			ctx.getSql().append(alias);
		}

		// this is used to help the context in providing a mapping to the schema model
		if (tableAlias != null && alias != null) {
			getQueryContext().registerSelectAlias(tableAlias, fieldName, alias);
		}
	}

}
