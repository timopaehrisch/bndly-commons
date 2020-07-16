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
import org.bndly.schema.vendor.IndexExistenceAdapter;
import org.bndly.schema.vendor.def.CountGreaterZeroToBooleanMapper;

/**
 * Regular indices that are not PK or UNIQUE indices, can only be retrieved from the pg_index in the pg_catalog.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresIndexExistenceAdapter implements IndexExistenceAdapter {

	@Override
	public boolean isIndexDefinedOnTableColumn(String dbSchemaName, final String indexName, Table table, TransactionTemplate template) {
		// http://www.postgresql.org/docs/9.3/static/catalog-pg-index.html
		
		// create index idx_cartitem_cart_id on _cartitem(_cart_id);
		
		/*
		this statement selects the row in the meta data index table, by the index name
		
		select 
			t.relname as table_name,
			i.relname as index_name,
			idx.indexrelid, 
			idx.indrelid 
		from 
			pg_class t,
			pg_class i,
			pg_index idx
		where
			t.oid = idx.indrelid
		AND 
			i.oid = idx.indexrelid
		AND
			i.relname = 'idx_cartitem_cart_id'
		;
		*/
		return template.doInTransaction(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus transactionStatus, Template template) {
				return template.queryForObject(
					"select count(*) from pg_catalog.pg_class i, pg_catalog.pg_index idx where i.oid = idx.indexrelid AND i.relname = ?", 
					new Object[]{indexName}, 
					new CountGreaterZeroToBooleanMapper()
				);
			}
		});
	}

	// 'PostgreSQL automatically creates a unique index when a unique constraint or primary key is defined for a table.'
	// http://www.postgresql.org/docs/9.3/static/indexes-unique.html
	
	@Override
	public boolean isPrimaryKeyIndexedAutomatically() {
		return true;
	}

	@Override
	public boolean isUniqueColumnIndexedAutomatically() {
		return true;
	}
	
}
