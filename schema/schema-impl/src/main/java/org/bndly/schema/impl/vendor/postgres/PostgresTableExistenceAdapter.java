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
import org.bndly.schema.vendor.AntiSQLInject;
import org.bndly.schema.vendor.TableExistenceAdapter;
import org.bndly.schema.vendor.def.CountGreaterZeroToBooleanMapper;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresTableExistenceAdapter implements TableExistenceAdapter {

	@Override
	public boolean isTableDefined(final String dbSchemaName, final String tableName, TransactionTemplate template, AntiSQLInject antiSQLInject) {
		return template.doInTransaction(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?;", 
					new Object[]{dbSchemaName, tableName}, 
					new CountGreaterZeroToBooleanMapper()
				);
			}
		});
	}
	
}
