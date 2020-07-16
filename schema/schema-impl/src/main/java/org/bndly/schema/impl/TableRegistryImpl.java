package org.bndly.schema.impl;

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

import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.db.UniqueConstraintTable;
import org.bndly.schema.impl.db.AttributeColumnImpl;
import org.bndly.schema.impl.db.JoinTableImpl;
import org.bndly.schema.impl.db.TypeTableImpl;
import org.bndly.schema.impl.db.UniqueConstraintTableImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.UniqueConstraint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TableRegistryImpl implements TableRegistry, Resetable {

	private final Map<String, TypeTable> typeTables = new HashMap<>();
	private final Map<String, JoinTable> joinTables = new HashMap<>();
	private final Map<String, UniqueConstraintTable> uniqueConstraintTables = new HashMap<>();
	private final Map<String, Table> tablesByName = new HashMap<>();
	private MediatorRegistryImpl mediatorRegistry;
	private VendorConfiguration vendorConfiguration;

	@Override
	public TypeTableImpl createTypeTable(Type type, String tableName) {
		TypeTableImpl tt = new TypeTableImpl(type, tableName);
		tt.setPrimaryKeyColumn(createPrimaryKeyColumn(tt));
		typeTables.put(type.getName(), tt);
		tablesByName.put(tableName, tt);
		return tt;
	}

	@Override
	public JoinTableImpl createJoinTable(NamedAttributeHolder namedAttributeHolder, String tableName) {
		JoinTableImpl jt = new JoinTableImpl(tableName, namedAttributeHolder);
		jt.setPrimaryKeyColumn(createPrimaryKeyColumn(jt));
		joinTables.put(namedAttributeHolder.getName(), jt);
		tablesByName.put(tableName, jt);
		return jt;
	}

	@Override
	public UniqueConstraintTableImpl createUniqueConstraintTable(UniqueConstraint uniqueConstraint, String tableName) {
		UniqueConstraintTableImpl uqt = new UniqueConstraintTableImpl(uniqueConstraint, tableName);
		uniqueConstraintTables.put(tableName, uqt);
		tablesByName.put(tableName, uqt);
		return uqt;
	}

	@Override
	public JoinTable getJoinTableByNamedAttributeHolder(NamedAttributeHolder namedAttributeHolder) {
		return getJoinTableByNamedAttributeHolder(namedAttributeHolder.getName());
	}

	@Override
	public JoinTable getJoinTableByNamedAttributeHolder(String namedAttributeHolderName) {
		return joinTables.get(namedAttributeHolderName);
	}

	@Override
	public TypeTable getTypeTableByType(Type type) {
		return getTypeTableByType(type.getName());
	}

	@Override
	public TypeTable getTypeTableByType(String typeName) {
		return typeTables.get(typeName);
	}

	@Override
	public UniqueConstraintTable getUniqueConstraintTableByConstraint(UniqueConstraint uniqueConstraint) {
		for (Map.Entry<String, UniqueConstraintTable> entry : uniqueConstraintTables.entrySet()) {
			UniqueConstraintTable uniqueConstraintTable = entry.getValue();
			if (uniqueConstraintTable.getUniqueConstraint() == uniqueConstraint) {
				return uniqueConstraintTable;
			}
		}
		throw new IllegalStateException("could not find unique constraint table");
	}

	@Override
	public Collection<JoinTable> getAllJoinTables() {
		return joinTables.values();
	}

	@Override
	public Collection<TypeTable> getAllTypeTables() {
		return typeTables.values();
	}

	@Override
	public Collection<UniqueConstraintTable> getAllUniqueConstraintTables() {
		return uniqueConstraintTables.values();
	}

	@Override
	public Table getTableByName(String tableName) {
		return tablesByName.get(tableName);
	}

	private AttributeColumnImpl createPrimaryKeyColumn(Table table) {
		DecimalAttribute att = new DecimalAttribute();
		att.setName("id");
		att.setDecimalPlaces(0);
		att.setIndexed(true);
		// PK should be indexed. Most DB system automatically create the PK index.
		String colTypeName = mediatorRegistry.getMediatorForAttribute(att).columnType(att);
		boolean requiresIndex = att.isIndexed();
		if (att.isIndexed()) {
			if (vendorConfiguration.getIndexExistenceAdapter().isPrimaryKeyIndexedAutomatically()) {
				requiresIndex = false;
			}
		}
		AttributeColumnImpl attCol = new AttributeColumnImpl(att, vendorConfiguration.getIdentifierAdapter().transformColumnName("ID"), colTypeName, table, requiresIndex, true);
		return attCol;
	}

	@Override
	public void reset() {
		typeTables.clear();
		joinTables.clear();
		uniqueConstraintTables.clear();
		tablesByName.clear();
	}

	public void setMediatorRegistry(MediatorRegistryImpl mediatorRegistry) {
		this.mediatorRegistry = mediatorRegistry;
	}

	public void setVendorConfiguration(VendorConfiguration vendorConfiguration) {
		this.vendorConfiguration = vendorConfiguration;
	}

}
