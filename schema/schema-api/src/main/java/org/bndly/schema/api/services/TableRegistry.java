package org.bndly.schema.api.services;

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

import org.bndly.schema.api.db.UniqueConstraintTable;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.UniqueConstraint;
import java.util.Collection;

public interface TableRegistry {
    TypeTable createTypeTable(Type type, String tableName);
    JoinTable createJoinTable(NamedAttributeHolder namedAttributeHolder, String tableName);
    UniqueConstraintTable createUniqueConstraintTable(UniqueConstraint uniqueConstraint, String tableName);
    
    JoinTable getJoinTableByNamedAttributeHolder(NamedAttributeHolder namedAttributeHolder);
    JoinTable getJoinTableByNamedAttributeHolder(String namedAttributeHolderName);
    TypeTable getTypeTableByType(Type type);
    TypeTable getTypeTableByType(String typeName);
    UniqueConstraintTable getUniqueConstraintTableByConstraint(UniqueConstraint uniqueConstraint);
    
    Collection<JoinTable> getAllJoinTables();
    Collection<TypeTable> getAllTypeTables();
    Collection<UniqueConstraintTable> getAllUniqueConstraintTables();

    Table getTableByName(String tableName);

}
