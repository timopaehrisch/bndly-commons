package org.bndly.schema.vendor.def;

/*-
 * #%L
 * Schema Vendor
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultTableExistenceAdapter implements TableExistenceAdapter {

	@Override
	public boolean isTableDefined(final String dbSchemaName, final String tableName, TransactionTemplate transactionTemplate, final AntiSQLInject antiSQLInject) {
		return transactionTemplate.doInTransaction(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				try {
					template.execute("SELECT 1 FROM " + antiSQLInject.filterCharactersForSQLIdentifier(tableName) + " LIMIT 1");
					return true;
				} catch (Exception ex) {
					return false;
				}
			}
		});

	}

}
