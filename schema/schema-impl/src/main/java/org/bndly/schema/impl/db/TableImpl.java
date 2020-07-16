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
import org.bndly.schema.api.db.Table;
import java.util.List;

public class TableImpl implements Table, PrivateHasDeploymentState {
    private DeploymentState state;
    private AttributeColumn primaryKeyColumn;
    private final String tableName;
    private final List<AttributeColumn> columns;

    public TableImpl(String tableName, List<AttributeColumn> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

	@Override
    public final AttributeColumn getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

	public final void setPrimaryKeyColumn(AttributeColumn primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }
    
	@Override
    public final String getTableName() {
        return tableName;
    }

	@Override
    public final List<AttributeColumn> getColumns() {
        return columns;
    }

	@Override
    public final DeploymentState getState() {
        return state;
    }

	@Override
    public final void setState(DeploymentState state) {
        this.state = state;
    }
    
}
