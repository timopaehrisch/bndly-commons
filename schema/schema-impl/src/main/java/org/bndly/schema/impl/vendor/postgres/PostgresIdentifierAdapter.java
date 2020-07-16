package org.bndly.schema.impl.vendor.postgres;

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

import org.bndly.schema.vendor.def.LowerCaseIdentifierAdapter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresIdentifierAdapter extends LowerCaseIdentifierAdapter {

	private static final String[] IGNORED_PREFIXES = new String[]{"UQ_", "JOIN_", "MIXIN_"};

	@Override
	public String transformColumnName(String columnName) {
		return "_" + super.transformColumnName(columnName);
	}

	@Override
	public String transformConstraintName(String constraintName) {
		return "_" + super.transformConstraintName(constraintName);
	}

	@Override
	public String transformTableName(String tableName) {
		for (String prefix : IGNORED_PREFIXES) {
			if (tableName.startsWith(prefix)) {
				return super.transformTableName(tableName);
			}
		}
		return "_" + super.transformTableName(tableName);
	}

	@Override
	public int getIdentifierMaxLength() {
		// http://www.postgresql.org/docs/current/interactive/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
		return 63; // yes, not 64!
	}

	@Override
	public String transformIndexName(String indexName) {
		return super.transformConstraintName(indexName);
	}
	
}
