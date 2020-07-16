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

import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.vendor.IndexExistenceAdapter;

/**
 * This adapter does not check for the existence of an index. It will always assume, that any index already exists.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultIndexExistenceAdapter implements IndexExistenceAdapter {

	@Override
	public boolean isIndexDefinedOnTableColumn(String dbSchemaName, final String indexName, Table table, TransactionTemplate template) {
		return true;
	}

	@Override
	public boolean isPrimaryKeyIndexedAutomatically() {
		return true;
	}

	@Override
	public boolean isUniqueColumnIndexedAutomatically() {
		return true;
	}
	
	
}
