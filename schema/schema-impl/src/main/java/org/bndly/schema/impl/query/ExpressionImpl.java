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

import org.bndly.schema.api.query.Criteria;
import org.bndly.schema.api.query.Expression;
import org.bndly.schema.api.query.QueryFragmentRenderer;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.WrapperExpression;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.ArrayList;
import java.util.List;

public class ExpressionImpl extends ContextRelatedQueryComponentImpl implements Expression {

	protected List<QueryFragmentRenderer> thingsToRender = new ArrayList<>();
	private CriteriaImpl criteria;
	private WrapperExpressionImpl wrapper;
	private QueryFragmentRenderer queryFragmentRenderer;

	public ExpressionImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public Expression and() {
		thingsToRender.add(new QueryFragmentRenderer() {

			@Override
			public void renderQueryFragment(QueryRenderContext context) {
				context.getSql().append(" AND ");
			}
		});
		ExpressionImpl next = new ExpressionImpl(getQueryContext(), getVendorConfiguration());
		thingsToRender.add(next);
		return next;
	}

	@Override
	public Expression or() {
		thingsToRender.add(new QueryFragmentRenderer() {

			@Override
			public void renderQueryFragment(QueryRenderContext context) {
				context.getSql().append(" OR ");
			}
		});
		ExpressionImpl next = new ExpressionImpl(getQueryContext(), getVendorConfiguration());
		thingsToRender.add(next);
		return next;
	}

	@Override
	public Criteria criteria() {
		if (queryFragmentRenderer != null) {
			throw new IllegalStateException("the expression is already configured");
		}
		this.criteria = new CriteriaImpl(getQueryContext(), getVendorConfiguration());
		queryFragmentRenderer = criteria;
		return criteria;
	}

	@Override
	public WrapperExpression wrap() {
		if (queryFragmentRenderer != null) {
			throw new IllegalStateException("the expression is already configured");
		}
		wrapper = new WrapperExpressionImpl(getQueryContext(), getVendorConfiguration());
		queryFragmentRenderer = wrapper;
		return wrapper;
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		if (queryFragmentRenderer != null) {
			queryFragmentRenderer.renderQueryFragment(ctx);
		}
		for (QueryFragmentRenderer renderMe : thingsToRender) {
			renderMe.renderQueryFragment(ctx);
		}
	}

}
