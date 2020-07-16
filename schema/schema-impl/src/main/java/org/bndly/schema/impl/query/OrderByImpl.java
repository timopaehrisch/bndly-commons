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

import org.bndly.schema.api.query.OrderBy;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.ArrayList;
import java.util.List;

public class OrderByImpl extends ContextRelatedQueryComponentImpl implements OrderBy {

	private final List<String> aliases = new ArrayList<>();
	private String direction;

	public OrderByImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public OrderBy columnAlias(String alias) {
		if (alias == null) {
			return this;
		}
		aliases.add(alias);
		return this;
	}

	@Override
	public OrderBy direction(String direction) {
		this.direction = direction;
		return this;
	}

	@Override
	public void renderQueryFragment(QueryRenderContext context) {
		if (aliases.isEmpty()) {
			return;
		}
		StringBuilder sb = context.getSql();
		sb.append(" ORDER BY ");
		if (aliases.size() == 1) {
			sb.append(aliases.get(0));
		} else {
			appendIFNULL(sb, 0);
		}
		if (direction != null) {
			sb.append(" ");
			sb.append(direction);
		}
	}

	private void appendIFNULL(StringBuilder sb, int start) {
		sb.append("IFNULL(");
		String leftAlias = aliases.get(start);
		sb.append(leftAlias);
		sb.append(",");
		if (start + 1 == aliases.size() - 1) {
			String rightAlias = aliases.get(start + 1);
			sb.append(rightAlias);
		} else {
			appendIFNULL(sb, start + 1);
		}
		sb.append(")");
	}
}
