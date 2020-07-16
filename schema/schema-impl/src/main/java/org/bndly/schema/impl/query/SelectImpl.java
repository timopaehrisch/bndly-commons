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

import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.query.From;
import org.bndly.schema.api.query.OrderBy;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.query.SelectExpression;
import org.bndly.schema.api.query.Where;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SelectImpl extends ContextRelatedQueryComponentImpl implements Select {

	private boolean selectAll = true;
	private From from;
	private Where where;
	private OrderBy orderBy;
	private final List<SelectExpression> selectExpressions = new ArrayList<>();
	private Long limit;
	private Long offset;

	public SelectImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		ctx.getSql().append("SELECT");
		if (selectAll) {
			ctx.getSql().append(" * ");
		} else {
			boolean first = true;
			for (SelectExpression selectExpression : selectExpressions) {
				if (!first) {
					ctx.getSql().append(',');
				}
				first = false;

				ctx.getSql().append(' ');
				selectExpression.renderQueryFragment(ctx);
			}
		}
		from.renderQueryFragment(ctx);
		if (where != null) {
			where.renderQueryFragment(ctx);
		}
		if (orderBy != null) {
			orderBy.renderQueryFragment(ctx);
		}
		if (limit != null) {
			ctx.getSql().append(" LIMIT ?");
			ctx.getArgs().add(limit);
			ctx.getArgumentSetters().add(new PreparedStatementArgumentSetter() {
				@Override
				public void set(int index, PreparedStatement ps) throws SQLException {
					ps.setLong(index, limit);
				}
			});
			if (offset != null) {
				ctx.getSql().append(" OFFSET ?");
				ctx.getArgs().add(offset);
				ctx.getArgumentSetters().add(new PreparedStatementArgumentSetter() {

					@Override
					public void set(int index, PreparedStatement ps) throws SQLException {
						ps.setLong(index, offset);
					}
				});
			}
		}
	}

	@Override
	public SelectExpression expression() {
		selectAll = false;
		SelectExpressionImpl expr = new SelectExpressionImpl(getQueryContext(), getVendorConfiguration());
		selectExpressions.add(expr);
		return expr;
	}

	@Override
	public Select all() {
		selectAll = true;
		selectExpressions.clear();
		return this;
	}

	@Override
	public Select count() {
		selectAll = false;
		selectExpressions.clear();
		expression().count();
		return this;
	}

	@Override
	public From from() {
		from = new FromImpl(getQueryContext(), getVendorConfiguration());
		return from;
	}

	@Override
	public Where where() {
		where = new WhereImpl(getQueryContext(), getVendorConfiguration());
		return where;
	}

	@Override
	public OrderBy orderBy() {
		orderBy = new OrderByImpl(getQueryContext(), getVendorConfiguration());
		return orderBy;
	}

	@Override
	public Select offset(Long offset) {
		this.offset = offset;
		return this;
	}

	@Override
	public Select limit(Long limit) {
		this.limit = limit;
		return this;
	}

}
