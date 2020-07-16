package org.bndly.schema.impl.db;

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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.DeploymentState;
import org.bndly.schema.api.db.Index;
import org.bndly.schema.api.db.Table;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class IndexImpl implements Index, PrivateHasDeploymentState {
	private DeploymentState state;
	private final String indexName;
    private final List<AttributeColumn> indexedColumns;
    private final Table indexedTable;

	public IndexImpl(String indexName, List<AttributeColumn> indexedColumns, Table indexedTable) {
		this.indexName = indexName;
		this.indexedColumns = indexedColumns;
		this.indexedTable = indexedTable;
	}

	public String getIndexName() {
		return indexName;
	}

	public List<AttributeColumn> getIndexedColumns() {
		return indexedColumns;
	}

	public Table getIndexedTable() {
		return indexedTable;
	}
	
	@Override
	public DeploymentState getState() {
		return state;
	}

	@Override
	public void setState(DeploymentState state) {
		this.state = state;
	}
	
}
