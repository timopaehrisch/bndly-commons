package org.bndly.schema.api.tx;

/*-
 * #%L
 * Schema API
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
import org.bndly.schema.api.mapper.RowMapper;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Template {
	// query methods are for selecting rows
	<E> List<E> query(String sql, Object[] arguments, RowMapper<E> mapper);
	<E> List<E> query(String sql, PreparedStatementArgumentSetter[] arguments, RowMapper<E> mapper);
	<E> List<E> query(String sql, RowMapper<E> mapper);
	<E> E queryForObject(String sql, Object[] arguments, RowMapper<E> mapper);
	<E> E queryForObject(String sql, Object[] arguments, Class<E> type);
	<E> E queryForObject(String sql, PreparedStatementArgumentSetter[] arguments, RowMapper<E> mapper);
	<E> E queryForObject(String sql, PreparedStatementArgumentSetter[] arguments, Class<E> type);
	
	// execute can be anything
	void execute(String sql);
	void execute(String sql, PreparedStatementCallback callback);
	
	// update is for insert, update, delete statements
	// the key holder might hold the ids of created rows
	void update(PreparedStatementCreator statementCreator, KeyHolder keyHolder);
	void update(PreparedStatementCreator statementCreator);
}
