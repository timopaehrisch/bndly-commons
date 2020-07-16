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
import org.bndly.schema.api.LoadedAttributes;
import org.bndly.schema.api.LoadedAttributes.Strategy;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.query.Join;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.MappingBindingsProvider;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.query.TableExpression;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * The LoadingIterator iterates over the available attributes and related entities from a relational database perspective and generates the basic SELECT object for SQL rendering. The required
 * attributes/entities are determined by a LoadedAttributes instance.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LoadingIterator implements MappingBindingsProvider {

	private final LoadedAttributes loadedAttributes;
	private final TableRegistry tableRegistry;
	private final Set<String> requiredAttributes;
	private final Select select;
	private int joinCounter;
	private int attributeCounter;

	private final Stack<TableExpression> joinStack = new Stack<>();
	private final Stack<AliasedTable> tableStack = new Stack<>();
	private final Stack<AttributeColumn> columnStack = new Stack<>();
	private final Stack<AttributeColumn> domainColumnStack = new Stack<>();
	private final Stack<MappingBinding> mappingBindingStack = new Stack<>();
	private final Stack<Strategy> loadingStrategyStack = new Stack<>();

	private final List<MappingBinding> mappingBindingList = new ArrayList<>();
	private MappingBinding rootMappingBinding;

	private static class AliasedTable {

		private final Table table;
		private final String alias;
		private final String attributePath;

		public AliasedTable(Table table, String alias, String attributePath) {
			this.table = table;
			this.alias = alias;
			this.attributePath = attributePath;
		}

		public String getAlias() {
			return alias;
		}

		public Table getTable() {
			return table;
		}

		public String getAttributePath() {
			return attributePath;
		}
		
	}

	public LoadingIterator(TableRegistry tableRegistry, Select select, LoadedAttributes loadedAttributes, Set<String> requiredAttributes) {
		this.select = select;
		this.tableRegistry = tableRegistry;
		this.loadedAttributes = loadedAttributes;
		this.requiredAttributes = requiredAttributes;
	}

	@Override
	public List<MappingBinding> getMappingBindings() {
		return mappingBindingList;
	}

	@Override
	public MappingBinding getRootMappingBinding() {
		return rootMappingBinding;
	}

	@Deprecated
	public List<MappingBinding> getMappingBindingList() {
		return mappingBindingList;
	}

	@Deprecated
	public final void iterate(Table table) {
		NamedAttributeHolder holder = getNamedAttributeHolderOfTable(table);
		iterate(holder.getName());
	}

	private NamedAttributeHolder getNamedAttributeHolderOfTable(Table table) {
		NamedAttributeHolder holder;
		if (TypeTable.class.isInstance(table)) {
			holder = TypeTable.class.cast(table).getType();
		} else if (JoinTable.class.isInstance(table)) {
			holder = JoinTable.class.cast(table).getNamedAttributeHolder();
		} else {
			throw new IllegalStateException("unsupported table: " + table.getClass().getSimpleName());
		}
		return holder;
	}

	public final void iterate(String attributeHolderName) {
		iterate(attributeHolderName, false);
	}

	public final void iterate(String attributeHolderName, final boolean skipColumnSelection) {
		TableHierarchyIterator.iterateTypeHierarchyDownAndFollowAttributes(attributeHolderName, tableRegistry, new TableHierarchyIterator.NoOpIterationCallback() {
			private AliasedTable rootAliasedTable;

			@Override
			public void onStart(Table table) {
				AliasedTable at = new AliasedTable(table, pullTableAlias(), "");
				initRootMapping(at);
				rootAliasedTable = at;
				tableStack.push(rootAliasedTable);
			}

			@Override
			public void onEnd(Table table) {
				tableStack.pop();
			}

			@Override
			public void onTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				onTable(typeTable, true, TableHierarchyIterator.buildAttributePath(domainAttributeStack));
			}

			@Override
			public void afterTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				afterTable(typeTable, true);
			}

			@Override
			public void onJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				onTable(joinTable, false, TableHierarchyIterator.buildAttributePath(domainAttributeStack));
			}

			@Override
			public void afterJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				afterTable(joinTable, false);
			}

			@Override
			public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				columnStack.push(column);
				boolean isTypeTable = TypeTable.class.isInstance(table);
				if (!isPrimaryKeyColumn) {
					if (isTypeTable) {
						domainColumnStack.push(column);

						Strategy peekLoadingStrategy = getCurrentLoadingStrategy();
						Strategy loadingStrategy;
						if (peekLoadingStrategy == Strategy.LAZY_LOADED || peekLoadingStrategy == Strategy.NOT_LOADED) {
							// if the parent is lazy or not loaded at all, then subsequent attributes should not be loaded.
							loadingStrategy = Strategy.NOT_LOADED;
						} else {
							loadingStrategy = loadedAttributes.isLoaded(column.getAttribute(), TableHierarchyIterator.buildAttributePath(isPrimaryKeyColumn ? attributeStack : domainAttributeStack));
						}
						loadingStrategyStack.push(loadingStrategy);

						if (loadingStrategy == Strategy.LAZY_LOADED || loadingStrategy == Strategy.LOADED) {
							createAliasBindingInCurrentMapping(column);
						}
					}
				}
			}

			@Override
			public void afterColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				boolean isTypeTable = TypeTable.class.isInstance(table);
				if (!isPrimaryKeyColumn) {
					if (isTypeTable) {
						loadingStrategyStack.pop();
						domainColumnStack.pop();
					}
				}
				columnStack.pop();
			}

			@Override
			public TableHierarchyIterator.IterationCommand onCyclicColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
				String attributePath = TableHierarchyIterator.buildAttributePath(domainAttributeStack);
				if (!attributePath.isEmpty()) {
					String attributePathPrefix = attributePath + ".";
					String idAttributePath = attributePathPrefix + "id";
					for (String requiredAttribute : requiredAttributes) {
						if (requiredAttribute.startsWith(attributePathPrefix) && !requiredAttribute.equals(idAttributePath)) {
							return TableHierarchyIterator.IterationCommand.CONTINUE;
						}
					}
				}
				return super.onCyclicColumn(column, table, isPrimaryKeyColumn, attributeStack, domainAttributeStack);
			}
			
			private void onTable(Table table, boolean isTypeTable, String attributePath) {
				AliasedTable at;
				if (rootAliasedTable == tableStack.peek() && tableStack.peek().getTable() == table && attributePath.isEmpty()) {
					at = rootAliasedTable;
				} else {
					String alias = pullTableAlias();
					at = new AliasedTable(table, alias, attributePath);
				}

				AttributeColumn col = !columnStack.isEmpty() ? columnStack.peek() : null;

				Strategy strategy = getCurrentLoadingStrategy();
				if (col != null) {
					// strategy will be null when loading a record via a JoinTable
					if (strategy == Strategy.LOADED || strategy == null) {
						insertTableJoin(at, col);
					}
				}

				tableStack.push(at);

				AttributeColumn column = table.getPrimaryKeyColumn();
				if (isTypeTable) {
					AliasBinding pkAlias;
					boolean didAppend;
					if (isTopLevelDomainAttributeColumn() && column == rootMappingBinding.getPrimaryKeyAlias().getAttributeColumn()) {
						pkAlias = rootMappingBinding.getPrimaryKeyAlias();
						didAppend = false;
					} else {
						pkAlias = createAliasBinding(column);
						didAppend = true;
					}
					final MappingBinding binding;
					if (at == rootAliasedTable) {
						binding = rootMappingBinding;
					} else {
						binding = new MappingBinding(((TypeTable) table).getType(), pkAlias, currentTableAlias());
					}
					if (isTopLevelDomainAttributeColumn()) {
						mappingBindingList.add(binding);
					} else {
						if (strategy == Strategy.LOADED) {
							mappingBindingStack.peek().addSubBinding(binding, domainColumnStack.peek().getAttribute());
						}
					}
					mappingBindingStack.push(binding);
					// primary keys are not real attributes. they only exist because we are using a relational database.
					if (!didAppend) {
						appendCurrentAttributeColumnToSelect(pkAlias, false);
					}
				}
			}

			private void afterTable(Table table, boolean isTypeTable) {
				AttributeColumn col = !columnStack.isEmpty() ? columnStack.peek() : null;

				if (col != null) {
					Strategy strategy = getCurrentLoadingStrategy();
					if (strategy == Strategy.LOADED || strategy == null) {
						joinStack.pop();
					}
				}

				if (isTypeTable) {
					mappingBindingStack.pop();
				}
				tableStack.pop();
			}

			private void initRootMapping(AliasedTable at) {
				if (rootMappingBinding == null) {
					String pkAliasName = pullColumnAlias();
					AttributeColumn pkAttributeColumn = at.getTable().getPrimaryKeyColumn();
					AliasBinding pkAlias = new AliasBinding(pkAliasName, pkAttributeColumn, at.getAlias());
					NamedAttributeHolder holder = getNamedAttributeHolderOfTable(at.getTable());
					rootMappingBinding = new MappingBinding(holder, pkAlias, at.getAlias());
					TableExpression t = select.from().table(at.getTable().getTableName(), at.getAlias());
					joinStack.push(t);
				}
			}

			private boolean isTopLevelDomainAttributeColumn() {
				return domainColumnStack.isEmpty();
			}

			private void insertTableJoin(AliasedTable aliasedTable, AttributeColumn column) {
				TableExpression peek = joinStack.peek();
				Join j = peek.join(aliasedTable.getTable().getTableName(), aliasedTable.getAlias()).left();
				String leftField = peek.alias() + "." + column.getColumnName();
				String rightField = aliasedTable.getAlias() + "." + aliasedTable.getTable().getPrimaryKeyColumn().getColumnName();
				j.on().criteria().field(leftField).equal().field(rightField);
				joinStack.push(j);
			}

			private String pullTableAlias() {
				joinCounter++;
				String alias = "j" + joinCounter;
				return alias;
			}

			private String currentTableAlias() {
				return tableStack.peek().getAlias();
			}

			private String pullColumnAlias() {
				attributeCounter++;
				return "c" + attributeCounter;
			}

			private AliasBinding createAliasBindingInCurrentMapping(AttributeColumn attributeColumn) {
				return appendCurrentAttributeColumnToSelect(attributeColumn, true);
			}

			private AliasBinding createAliasBinding(AttributeColumn attributeColumn) {
				return appendCurrentAttributeColumnToSelect(attributeColumn, false);
			}

			private AliasBinding appendCurrentAttributeColumnToSelect(AttributeColumn attributeColumn, boolean appendToCurrentMappingBinding) {
				String colAlias = pullColumnAlias();
				return appendCurrentAttributeColumnToSelect(attributeColumn, colAlias, appendToCurrentMappingBinding);
			}

			private AliasBinding appendCurrentAttributeColumnToSelect(AttributeColumn attributeColumn, String colAlias, boolean appendToCurrentMappingBinding) {
				String tableAlias = joinStack.peek().alias();
				AliasBinding aliasBinding = new AliasBinding(colAlias, attributeColumn, tableAlias);
				return appendCurrentAttributeColumnToSelect(aliasBinding, appendToCurrentMappingBinding);
			}

			private AliasBinding appendCurrentAttributeColumnToSelect(AliasBinding aliasBinding, boolean appendToCurrentMappingBinding) {
				String alias = joinStack.peek().alias();
				if (!skipColumnSelection) {
					select.expression().table(alias).field(aliasBinding.getAttributeColumn().getColumnName()).as(aliasBinding.getAlias());
				}

				if (appendToCurrentMappingBinding) {
					MappingBinding binding = mappingBindingStack.peek();
					binding.addAlias(aliasBinding);
				}
				return aliasBinding;
			}
		});
	}

	private Strategy getCurrentLoadingStrategy() {
		return loadingStrategyStack.isEmpty() ? null : loadingStrategyStack.peek();
	}

}
