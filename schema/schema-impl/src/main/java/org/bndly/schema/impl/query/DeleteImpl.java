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

import org.bndly.schema.api.query.Delete;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.Where;
import org.bndly.schema.impl.QueryContextImpl;
import org.bndly.schema.vendor.VendorConfiguration;

public class DeleteImpl extends ContextRelatedQueryComponentImpl implements Delete {

	private String tableName;
	private Where where;

	public DeleteImpl(QueryContextImpl queryContext, VendorConfiguration vendorConfiguration) {
		super(queryContext, vendorConfiguration);
	}

	@Override
	public Delete from(String tableName) {
		this.tableName = tableName;
		return this;
	}

	@Override
	public Where where() {
		where = new WhereImpl(getQueryContext(), getVendorConfiguration());
		return where;
	}

	@Override
	public void renderQueryFragment(QueryRenderContext ctx) {
		ctx.getSql().append("DELETE FROM ");
		ctx.getSql().append(tableName);
		if (where != null) {
			where.renderQueryFragment(ctx);;
		}
	}

}
