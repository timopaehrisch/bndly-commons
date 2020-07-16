package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
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
import org.bndly.schema.api.db.AttributeColumn;

public class ForeignKeyConstraint {

	public static enum OnDelete {
		CASCADE, SETNULL
	}
	
    private AttributeColumn referencingColumn;
    private Table referencingTable;
    private AttributeColumn referencedColumn;
    private Table referencedTable;
    private OnDelete onDelete;

	private final String name;

	public ForeignKeyConstraint(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
    public AttributeColumn getReferencingColumn() {
        return referencingColumn;
    }

    public void setReferencingColumn(AttributeColumn referencingColumn) {
        this.referencingColumn = referencingColumn;
    }

    public Table getReferencingTable() {
        return referencingTable;
    }

    public void setReferencingTable(Table referencingTable) {
        this.referencingTable = referencingTable;
    }

    public AttributeColumn getReferencedColumn() {
        return referencedColumn;
    }

    public void setReferencedColumn(AttributeColumn referencedColumn) {
        this.referencedColumn = referencedColumn;
    }

    public Table getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(Table referencedTable) {
        this.referencedTable = referencedTable;
    }

	public OnDelete getOnDelete() {
		return onDelete;
	}

	public void setOnDelete(OnDelete onDelete) {
		this.onDelete = onDelete;
	}

}
