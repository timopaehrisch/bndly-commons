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
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.model.NamedAttributeHolder;
import java.util.ArrayList;
import java.util.List;

public class JoinTableImpl extends TableImpl implements JoinTable {

    private final NamedAttributeHolder namedAttributeHolder;
    private final List<Table> joinedTables;

    public JoinTableImpl(String tableName, NamedAttributeHolder namedAttributeHolder) {
        super(tableName, new ArrayList<AttributeColumn>());
        this.joinedTables = new ArrayList<>();
        this.namedAttributeHolder = namedAttributeHolder;
    }

	@Override
    public NamedAttributeHolder getNamedAttributeHolder() {
        return namedAttributeHolder;
    }

	@Override
    public List<Table> getJoinedTables() {
        return joinedTables;
    }
}
