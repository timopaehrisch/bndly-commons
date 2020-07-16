package org.bndly.schema.impl.vendor.h2;

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
import org.bndly.schema.vendor.ConstraintExistenceAdapter;
import org.bndly.schema.vendor.def.CountGreaterZeroToBooleanMapper;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class H2ConstraintExistenceAdapter implements ConstraintExistenceAdapter {

	//	SELECT COUNT(*) FROM INFORMATION_SCHEMA.CONSTRAINTS c WHERE c.CONSTRAINT_NAME='CON_UQ_UNIQUETEST_NAMEUNIQUEA'
	@Override
	public boolean isConstraintDefinedOnTable(String dbSchemaName, String constraintName, Table table, TransactionTemplate transactionTemplate) {
		StringBuffer sb = new StringBuffer("SELECT COUNT(*) FROM INFORMATION_SCHEMA.CONSTRAINTS c WHERE c.CONSTRAINT_NAME=? AND c.TABLE_NAME=?");
		final Object[] args;
		if (dbSchemaName != null) {
			sb.append(" AND TABLE_SCHEMA=?");
			args = new Object[]{constraintName, table.getTableName(), dbSchemaName};
		} else {
			args = new Object[]{constraintName, table.getTableName()};
		}
		final String constraintSql = sb.toString();
		List<Boolean> rows = transactionTemplate.doInTransaction(new TransactionCallback<List<Boolean>>() {

			@Override
			public List<Boolean> doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.query(constraintSql, args, new CountGreaterZeroToBooleanMapper());
			}
		});
		for (Boolean b : rows) {
			if (b == false) {
				return false;
			}
		}
		if (rows.isEmpty()) {
			return false;
		}
		return true;
	}

}
