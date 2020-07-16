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
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.QueryValueProvider;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.api.query.Where;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateImpl extends ContextRelatedQueryComponentImpl implements Update {

	private String tableName;
	private final List<KeyValue> values = new ArrayList<>();
	private Where where;

	public UpdateImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public Update table(String tableName) {
		this.tableName = tableName;
		return this;
	}

	@Override
	public Update set(String columnName, ValueProvider value) {
		return _set(columnName, value, null, false);
	}

	@Override
	public Update set(String columnName, ValueProvider value, int sqlType) {
		return _set(columnName, value, sqlType, false);
	}

	@Override
	public Update setNull(String columnName, int sqlType) {
		return _set(columnName, ValueProvider.NULL, sqlType, true);
	}

	private Update _set(String columnName, ValueProvider value, Integer sqlType, boolean allowNull) {
		if (value != null || allowNull) {
			final PreparedStatementArgumentSetter argumentSetter;
			if (PreparedStatementArgumentSetter.class.isInstance(value)) {
				argumentSetter = (PreparedStatementArgumentSetter) value;
			} else {
				argumentSetter = createFallbackPreparedStatementArgumentSetter(value, sqlType);
			}
			values.add(new KeyValue(columnName, value, sqlType, argumentSetter));
		}
		return this;
	}

	@Override
	public Where where() {
		where = new WhereImpl(getQueryContext(), getVendorConfiguration());
		return where;
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		ctx.getSql().append("UPDATE ");
		ctx.getSql().append(tableName);
		ctx.getSql().append(" SET ");
		boolean first = true;
		for (final KeyValue kv : values) {
			if (!first) {
				ctx.getSql().append(',');
			}
			first = false;
			ctx.getSql().append(kv.getKey());
			ctx.getSql().append('=');
			ValueProvider valueProvider = kv.getValue();
			if (QueryValueProvider.class.isInstance(valueProvider)) {
				QueryValueProvider qvp = (QueryValueProvider) valueProvider;
				Query nestedQuery = qvp.get();
				ctx.getSql()
						.append('(')
						.append(nestedQuery.getSql())
						.append(')');
				ctx.getArgs().addAll(Arrays.asList(nestedQuery.getArgs()));
				ctx.getArgumentSetters().addAll(Arrays.asList(nestedQuery.getArgumentSetters()));
			} else if (PreparedStatementValueProvider.class.isInstance(valueProvider)) {
				PreparedStatementValueProvider psvp = (PreparedStatementValueProvider) valueProvider;
				ctx.getSql().append('?');
				ValueUtil.appendToArgs(ctx, null); // null, because this might be a lazy value
				ctx.getArgumentSetters().add(psvp);
			} else {
				Object value = valueProvider.get();
				ctx.getSql().append('?');
				ValueUtil.appendToArgs(ctx, value);
				ctx.getArgumentSetters().add(kv.getArgumentSetter());
			}
		}
		if (where != null) {
			where.renderQueryFragment(ctx);
		}
	}

}
