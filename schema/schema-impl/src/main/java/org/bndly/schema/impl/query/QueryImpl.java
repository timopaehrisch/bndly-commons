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
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.mapper.RowMapper;

public final class QueryImpl implements Query<RowMapper<Record>> {

	private final String sql;
	private final Object[] args;
	private final RowMapper<Record> mapper;
	private final PreparedStatementArgumentSetter[] argumentSetters;
	private final boolean asUpdate;

	public QueryImpl(String sql, Object[] args, RowMapper<Record> mapper, PreparedStatementArgumentSetter[] argumentSetters, boolean asUpdate) {
		this.sql = sql;
		this.args = args;
		this.mapper = mapper;
		this.argumentSetters = argumentSetters;
		this.asUpdate = asUpdate;
	}

	@Override
	public String getSql() {
		return sql;
	}

	@Override
	public PreparedStatementArgumentSetter[] getArgumentSetters() {
		return argumentSetters;
	}

	@Override
	public Object[] getArgs() {
		return args;
	}

	@Override
	public RowMapper<Record> getMapper() {
		return mapper;
	}

	@Override
	public boolean asUpdate() {
		return asUpdate;
	}
}
