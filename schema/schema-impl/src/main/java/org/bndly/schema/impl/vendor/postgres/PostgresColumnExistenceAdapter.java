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

import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.vendor.ColumnExistenceAdapter;
import org.bndly.schema.vendor.def.CountGreaterZeroToBooleanMapper;
import org.bndly.schema.vendor.def.SingleStringValueMapper;
import java.util.ArrayList;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresColumnExistenceAdapter implements ColumnExistenceAdapter {

	@Override
	public boolean isColumnDefinedOnTable(String dbSchemaName, String columnName, String tableName, TransactionTemplate template) {
		ArrayList<Object> a = new ArrayList<>();
		a.add(columnName);
		a.add(tableName);
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME=? AND TABLE_NAME=?");
		if (dbSchemaName != null) {
			sb.append(" AND TABLE_SCHEMA=?");
			a.add(dbSchemaName);
		}
		String currentDataBase = getCurrentDatabase(template);
		if (currentDataBase != null) {
			sb.append(" AND TABLE_CATALOG=?");
			a.add(currentDataBase);
		}

		final Object[] args = a.toArray();
		return template.doInTransaction(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject(sb.toString(), args, new CountGreaterZeroToBooleanMapper());
			}
		});
	}

	protected static String getCurrentDatabase(TransactionTemplate template) {
		String currentDataBase = template.doInTransaction(new TransactionCallback<String>() {
			
			@Override
			public String doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject("SELECT current_database();", null, new SingleStringValueMapper());
			}
		});
		return currentDataBase;
	}
	
}
