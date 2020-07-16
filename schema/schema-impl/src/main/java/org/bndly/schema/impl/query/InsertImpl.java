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
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.QueryValueProvider;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InsertImpl extends ContextRelatedQueryComponentImpl implements Insert {

	private String tableName;
	private final List<KeyValue> values = new ArrayList<>();

	public InsertImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public Insert into(String tableName) {
		this.tableName = tableName;
		return this;
	}

	@Override
	public Insert value(String columnName, QueryValueProvider value) {
		return _value(columnName, value, null);
	}

	@Override
	public Insert value(String columnName, ValueProvider value) {
		// we don't know the sql type. hence we depend on the standard sql java mappings
		return _value(columnName, value, null);
	}

	@Override
	public Insert value(String columnName, ValueProvider value, int sqlType) {
		return _value(columnName, value, sqlType);
	}

	private Insert _value(String columnName, ValueProvider value, Integer sqlType) {
		if (value != null) {
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
	public void renderQueryFragment(QueryRenderContext ctx) {
		ctx.getSql().append("INSERT INTO ");
		ctx.getSql().append(tableName);
		if (values.isEmpty()) {
			ctx.getSql().append(getVendorConfiguration().getQueryRenderingAdapter().createEmptyRowInsertForTable(tableName));
		} else {
			StringBuilder k = new StringBuilder("(");
			StringBuilder v = new StringBuilder("(");
			boolean first = true;
			for (final KeyValue kv : values) {
				if (!first) {
					k.append(',');
					v.append(',');
				}
				first = false;
				k.append(kv.getKey());
				ValueProvider valueProvider = kv.getValue();
				if (QueryValueProvider.class.isInstance(valueProvider)) {
					QueryValueProvider qvp = (QueryValueProvider) valueProvider;
					Query nestedQuery = qvp.get();
					v.append('(');
					v.append(nestedQuery.getSql());
					v.append(')');
					ctx.getArgs().addAll(Arrays.asList(nestedQuery.getArgs()));
					ctx.getArgumentSetters().addAll(Arrays.asList(nestedQuery.getArgumentSetters()));
				} else if (PreparedStatementValueProvider.class.isInstance(valueProvider)) {
					PreparedStatementValueProvider psvp = (PreparedStatementValueProvider) valueProvider;
					v.append('?');
					ValueUtil.appendToArgs(ctx, null); // null, because this might be a lazy value
					ctx.getArgumentSetters().add(psvp);
				} else {
					Object value = valueProvider.get();
					v.append('?');
					ValueUtil.appendToArgs(ctx, value);
					ctx.getArgumentSetters().add(kv.getArgumentSetter());
				}
			}

			k.append(')');
			v.append(')');
			ctx.getSql().append(k);
			ctx.getSql().append(" VALUES ");
			ctx.getSql().append(v);
		}
	}
}
