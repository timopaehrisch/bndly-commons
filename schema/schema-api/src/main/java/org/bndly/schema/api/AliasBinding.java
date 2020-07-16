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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.model.Attribute;

public class AliasBinding {
    private final String aliasName;
    private final String tableAlias;
    private final Attribute attribute;
    private final AttributeColumn attributeColumn;

    public AliasBinding(String alias, AttributeColumn attributeColumn, String tableAlias) {
        this.aliasName = alias;
        this.tableAlias = tableAlias;
        this.attribute = attributeColumn.getAttribute();
        this.attributeColumn = attributeColumn;
    }

    public String getAlias() {
        return aliasName;
    }

	public String getTableAlias() {
		return tableAlias;
	}

    public Attribute getAttribute() {
        return attribute;
    }

    public AttributeColumn getAttributeColumn() {
        return attributeColumn;
    }
    
}
