package org.bndly.schema.impl.vendor.mariadb;

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
import org.bndly.schema.vendor.def.DefaultIndexExistenceAdapter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MariaDBIndexExistenceAdapter extends DefaultIndexExistenceAdapter {
	
	@Override
	public boolean isIndexDefinedOnTableColumn(final String dbSchemaName, final String indexName, final Table table, TransactionTemplate template) {
		if (dbSchemaName == null) {
			throw new IllegalStateException("the dbSchemaName is required to determine if the index " + indexName + " is defined");
		}
		
		return template.doInTransaction(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS t WHERE t.TABLE_SCHEMA=? AND t.TABLE_NAME=? AND t.INDEX_NAME=?",
					new Object[]{dbSchemaName, table.getTableName(), indexName},
					new CountGreaterZeroToBooleanMapper()
				);
			}
		});
	}
}
