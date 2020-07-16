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

import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.vendor.def.CountGreaterZeroToBooleanMapper;
import static org.bndly.schema.impl.vendor.postgres.PostgresColumnExistenceAdapter.getCurrentDatabase;
import org.bndly.schema.vendor.ConstraintExistenceAdapter;
import java.util.ArrayList;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresConstraintExistenceAdapter implements ConstraintExistenceAdapter {

	@Override
	public boolean isConstraintDefinedOnTable(String dbSchemaName, String constraintName, Table table, TransactionTemplate template) {
		ArrayList<Object> a = new ArrayList<>();
		StringBuffer sb = new StringBuffer("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS c WHERE");
		sb.append(" c.CONSTRAINT_NAME=? AND c.TABLE_NAME=?");
		a.add(constraintName);
		a.add(table.getTableName());
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
		final String constraintSql = sb.toString();
		Boolean res = template.doInTransaction(new TransactionCallback<Boolean>() {
			
			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject(constraintSql, args, new CountGreaterZeroToBooleanMapper());
			}
		});
		return res;
	}
	
}
