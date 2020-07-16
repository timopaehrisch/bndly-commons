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

import org.bndly.schema.api.ValueUtil;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.query.Criteria;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CriteriaImpl extends ContextRelatedQueryComponentImpl implements Criteria {

	private final List<String> fieldNames = new ArrayList<>();
	private String operand;
	private Boolean isNull;
	private ValueProvider parameterValue;
	private BetweenImpl between;

	public CriteriaImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public Criteria between() {
		between = new BetweenImpl();
		return this;
	}

	@Override
	public Criteria left(ValueProvider reference) {
		between.setLeft(reference);
		return this;
	}

	@Override
	public Criteria right(ValueProvider reference) {
		between.setRight(reference);
		return this;
	}

	@Override
	public Criteria field(String fieldName) {
		this.fieldNames.add(fieldName);
		return this;
	}

	@Override
	public Criteria equal() {
		operand = "=";
		return this;
	}

	@Override
	public Criteria notEqual() {
		operand = "!=";
		return this;
	}

	@Override
	public Criteria lowerThan() {
		operand = "<";
		return this;
	}

	@Override
	public Criteria equalOrLowerThan() {
		operand = "<=";
		return this;
	}

	@Override
	public Criteria greaterThan() {
		operand = ">";
		return this;
	}

	@Override
	public Criteria equalOrGreaterThan() {
		operand = ">=";
		return this;
	}

	@Override
	public Criteria isNotNull() {
		isNull = false;
		return this;
	}

	@Override
	public Criteria isNull() {
		isNull = true;
		return this;
	}

	@Override
	public Criteria value(ValueProvider reference) {
		parameterValue = reference;
		return this;
	}

	private void appendValueProvider(QueryRenderContext ctx, final ValueProvider valueProvider) {
		if (PreparedStatementValueProvider.class.isInstance(valueProvider)) {
			ValueUtil.appendToArgs(ctx, null);
			ctx.getArgumentSetters().add((PreparedStatementValueProvider) valueProvider);
		} else {
			ValueUtil.appendToArgs(ctx, valueProvider.get());
			ctx.getArgumentSetters().add(new PreparedStatementArgumentSetter() {
				@Override
				public void set(int index, PreparedStatement ps) throws SQLException {
					ps.setObject(index, ValueUtil.unwrapValue(valueProvider));
				}
			});
		}
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		if (!fieldNames.isEmpty()) {
			if (between != null && fieldNames.size() == 1) {
				String fieldName = fieldNames.get(0);
				ctx.getSql().append(fieldName);
				ctx.getSql().append(" BETWEEN ");
				ctx.getSql().append("? AND ?");
				appendValueProvider(ctx, between.getLeft());
				appendValueProvider(ctx, between.getRight());
				return;
			}
			if (fieldNames.size() == 1 && isNull != null) {
				ctx.getSql().append(fieldNames.get(0));
				if (isNull) {
					ctx.getSql().append(" IS NULL");
				} else {
					ctx.getSql().append(" IS NOT NULL");
				}
				return;
			} else {
				if (operand != null) {
					if (fieldNames.size() > 1) {
						String p1 = fieldNames.get(0);
						String p2 = fieldNames.get(1);
						ctx.getSql().append(p1);
						ctx.getSql().append(operand);
						ctx.getSql().append(p2);
						return;
					} else if (fieldNames.size() == 1 && parameterValue != null) {
						ctx.getSql().append(fieldNames.get(0));
						ctx.getSql().append(operand);
						ctx.getSql().append('?');
						appendValueProvider(ctx, parameterValue);
						return;
					}
				}
			}
		}
		throw new IllegalStateException("failed rendering criteria.");
	}

}
