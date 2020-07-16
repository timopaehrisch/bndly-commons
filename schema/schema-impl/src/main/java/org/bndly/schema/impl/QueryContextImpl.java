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

import org.bndly.schema.api.AliasBinding;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.MappingBindingsProvider;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.query.Delete;
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.QueryFragmentRenderer;
import org.bndly.schema.api.query.QueryRenderContext;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.query.Update;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.impl.query.DeleteImpl;
import org.bndly.schema.impl.query.InsertImpl;
import org.bndly.schema.impl.query.QueryImpl;
import org.bndly.schema.impl.query.SelectImpl;
import org.bndly.schema.impl.query.UpdateImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryContextImpl implements QueryContext {
	private static final Logger LOG = LoggerFactory.getLogger(QueryContextImpl.class);
	private final Accessor accessor;
	private final TableRegistry tableRegistry;
	private final List<String> usedTableNames = new ArrayList<>();
	private final Map<String, Table> tablesByAlias = new HashMap<>();
	private final VendorConfiguration vendorConfiguration;
	private QueryFragmentRenderer command;
	private final MediatorRegistryImpl mediatorRegistry;
	private MappingBindingsProvider externalMappingBindingsProvider;
	private final ConfigurableMappingBindingsProvider configurableMappingBindingsProvider = new ConfigurableMappingBindingsProvider();

	public QueryContextImpl(TableRegistry tableRegistry, MediatorRegistryImpl mediatorRegistry, Accessor accessor, VendorConfiguration vendorConfiguration) {
		this.tableRegistry = tableRegistry;
		this.mediatorRegistry = mediatorRegistry;
		this.accessor = accessor;
		this.vendorConfiguration = vendorConfiguration;
	}

	@Override
	public void setExternalMappingBindingsProvider(MappingBindingsProvider externalMappingBindingsProvider) {
		this.externalMappingBindingsProvider = externalMappingBindingsProvider;
	}

	@Override
	public Select select() {
		SelectImpl select = new SelectImpl(this, vendorConfiguration);
		command = select;
		return select;
	}

	@Override
	public Insert insert() {
		InsertImpl insert = new InsertImpl(this, vendorConfiguration);
		command = insert;
		return insert;
	}

	@Override
	public Update update() {
		UpdateImpl update = new UpdateImpl(this, vendorConfiguration);
		command = update;
		return update;
	}

	@Override
	public Delete delete() {
		DeleteImpl delete = new DeleteImpl(this, vendorConfiguration);
		command = delete;
		return delete;
	}

	@Override
	public void useTable(String tableName, String alias) {
		if (alias != null) {
			Table table = tableRegistry.getTableByName(tableName);
			tablesByAlias.put(alias, table);
		}
		usedTableNames.add(tableName);
	}

	@Override
	public void registerSelectAlias(String tableNameOrTableAlias, String fieldName, String alias) {
		Table table = tablesByAlias.get(tableNameOrTableAlias);
		if (table == null) {
			table = tableRegistry.getTableByName(tableNameOrTableAlias);
		}
		AttributeColumn column = null;
		List<AttributeColumn> cols = table.getColumns();
		for (AttributeColumn attributeColumn : cols) {
			if (attributeColumn.getColumnName().equals(fieldName)) {
				column = attributeColumn;
				break;
			}
		}
		if (TypeTable.class.isInstance(table) && column != null) {
			TypeTable tt = (TypeTable) table;
			MappingBinding mappingBinding = configurableMappingBindingsProvider.getMappingBindingFor(tt.getType());
			if (mappingBinding != null) {
				AliasBinding aliasBinding = new AliasBinding(alias, column, tableNameOrTableAlias);
				mappingBinding.addAlias(aliasBinding);
			} else {
				throw new IllegalStateException("creating a select alias before mapping binding for type exists.");
			}
		}
		boolean isPkFieldName = false;
		AttributeColumn pkCol = table.getPrimaryKeyColumn();
		if (pkCol != null) {
			isPkFieldName = pkCol.getColumnName().equals(fieldName);
		}
		if (isPkFieldName) {
			if (TypeTable.class.isInstance(table)) {
				TypeTable tt = (TypeTable) table;

				AliasBinding pkAlias = new AliasBinding(alias, tt.getPrimaryKeyColumn(), tableNameOrTableAlias);
				MappingBinding mappingBinding = configurableMappingBindingsProvider.getMappingBindingFor(tt.getType());
				if (mappingBinding == null) {
					configurableMappingBindingsProvider.createMappingBinding(tt.getType(), pkAlias, tableNameOrTableAlias);
				}
			}
		}
	}

	private QueryRenderContext buildNewQueryRenderContext() {
		return new QueryRenderContext() {
			final StringBuilder sb = new StringBuilder();
			final ArrayList<Object> args = new ArrayList<>();
			final ArrayList<PreparedStatementArgumentSetter> argumentSetters = new ArrayList<>();

			@Override
			public StringBuilder getSql() {
				return sb;
			}

			@Override
			public List<Object> getArgs() {
				return args;
			}

			@Override
			public List<PreparedStatementArgumentSetter> getArgumentSetters() {
				return argumentSetters;
			}
		};
	}

	@Override
	public QueryImpl build(RecordContext recordContext) {
		QueryRenderContext ctx = buildNewQueryRenderContext();
		command.renderQueryFragment(ctx);

		RowMapper<Record> mapper = buildMapper(recordContext);
		PreparedStatementArgumentSetter[] setters = new PreparedStatementArgumentSetter[ctx.getArgumentSetters().size()];
		ctx.getArgumentSetters().toArray(setters);
		boolean asUpdate = !Select.class.isInstance(command);
		QueryImpl query = new QueryImpl(ctx.getSql().toString(), ctx.getArgs().toArray(), mapper, setters, asUpdate);
		LOG.trace("query with sql: {}", query.getSql());
		return query;
	}

	private RowMapper<Record> buildMapper(RecordContext recordContext) {
		configurableMappingBindingsProvider.setParent(externalMappingBindingsProvider);

		TypeTable table = null; // find this table somehow
		if (usedTableNames.size() == 1) {
			Table t = tableRegistry.getTableByName(usedTableNames.get(0));
			if (TypeTable.class.isInstance(t)) {
				table = (TypeTable) t;
			}
		}
		SingleTableMappingBindingsProvider st = new SingleTableMappingBindingsProvider();
		st.setTable(table);
		st.setParent(configurableMappingBindingsProvider);

		MappingBindingsProvider mappingBindingsProvider = st;

		return new AliasedRecordRowMapper(mappingBindingsProvider, mediatorRegistry, accessor, recordContext);
	}
}
