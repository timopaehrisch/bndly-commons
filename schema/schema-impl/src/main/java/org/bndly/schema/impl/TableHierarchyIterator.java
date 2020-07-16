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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class TableHierarchyIterator {

	public static enum IterationCommand {
		CONTINUE,
		SKIP,
		ABORT
	}
	
	public static interface IterationCallback {

		/**
		 * This method is called upon start of the iteration over the graph of tables and columns
		 * @param table the table, that is the root of the iteration
		 */
		public void onStart(Table table);
		
		/**
		 * This method is called upon the end of the iteration over the graph of tables and columns
		 * @param table the table, that is the root of the iteration
		 */
		public void onEnd(Table table);
		
		/**
		 * This method is called for every column in all kinds of tables - this includes {@link TypeTable} and {@link JoinTable} instances.
		 * The method is called for columns, that exist because:
		 * <ul>
		 * <li>they are natural domain attributes</li>
		 * <li>they are the primary key of the table</li>
		 * <li>they reference the primary key of a foreign table</li>
		 * </ul>
		 * @param column the currently iterated column
		 * @param table the table, that owns the {@code column}
		 * @param isPrimaryKeyColumn {@code true}, if the column is the primary key column of the table. 
		 * {@code false}, if the column is a column for a natural attribute, which includes references to other tables.
		 * @param attributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack includes attributes, that only exist to implement primary keys.
		 * @param domainAttributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack only contains attributes, that are defined in the schema. 
		 * this stack will not contain attributes, that only exist to implement primary keys.
		 */
		public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		public void afterColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack);
		
		
		public void onAttributeForeignTable(Table foreignTable, AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		public void afterAttributeForeignTable(Table foreignTable, AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		
		public IterationCommand onCyclicColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		
		public void onJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn);

		public void afterJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn);

		
		/**
		 * This method is called for each type table within the iterated tables and columns
		 * @param typeTable the found type table
		 * @param attributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack includes attributes, that only exist to implement primary keys.
		 * @param domainAttributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack only contains attributes, that are defined in the schema. 
		 * this stack will not contain attributes, that only exist to implement primary keys.
		 */
		public void onTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		public void afterTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		/**
		 * This method is called for each join table within the iterated tables and columns.
		 * @param joinTable the found join table
		 * @param attributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack includes attributes, that only exist to implement primary keys.
		 * @param domainAttributeStack the stack of all attributes, that have currently been iterated. 
		 * this stack only contains attributes, that are defined in the schema. 
		 * this stack will not contain attributes, that only exist to implement primary keys.
		 */
		public void onJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack);

		public void afterJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack);
	}

	public static class NoOpIterationCallback implements IterationCallback {

		@Override
		public void onStart(Table table) {
		}

		@Override
		public void onEnd(Table table) {
		}

		@Override
		public void onAttributeForeignTable(Table foreignTable, AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void afterAttributeForeignTable(Table foreignTable, AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void afterColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public IterationCommand onCyclicColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
			return IterationCommand.SKIP;
		}

		@Override
		public void onJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn) {
		}

		@Override
		public void afterJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn) {
		}

		@Override
		public void onTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void afterTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void onJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}

		@Override
		public void afterJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		}
	}

	private TableHierarchyIterator() {
	}

	public static void iterateTypeHierarchyDown(NamedAttributeHolder attributeHolder, TableRegistry tableRegistry, IterationCallback callback) {
		iterateTypeHierarchyDown(attributeHolder.getName(), tableRegistry, callback);
	}

	public static void iterateTypeHierarchyDown(String attributeHoldername, TableRegistry tableRegistry, IterationCallback callback) {
		bootIterateTypeHierarchyDown(attributeHoldername, tableRegistry, false, callback);
	}

	public static void iterateTypeHierarchyDownAndFollowAttributes(String attributeHoldername, TableRegistry tableRegistry, IterationCallback callback) {
		bootIterateTypeHierarchyDown(attributeHoldername, tableRegistry, true, callback);
	}

	private static void bootIterateTypeHierarchyDown(String attributeHoldername, TableRegistry tableRegistry, boolean followAttributes, IterationCallback callback) {
		Table table = getIterationStartTable(attributeHoldername, tableRegistry);
		callback.onStart(table);
		iterateTypeHierarchyDown(table, tableRegistry, followAttributes, new Stack<Attribute>(), new Stack<Attribute>(), callback);
		callback.onEnd(table);
	}
	
	public static String buildAttributePath(Attribute[] attributeStack) {
		StringBuilder sb = null;
		for (Attribute att : attributeStack) {
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append(".");
			}
			sb.append(att.getName());
		}
		return sb == null ? "" : sb.toString();
	}
	
	private static void iterateTypeHierarchyDown(
		final Table table,
		final TableRegistry tableRegistry, 
		final boolean followAttributes, 
		final Stack<Attribute> attributeStack, 
		final Stack<Attribute> domainAttributeStack, 
		final IterationCallback callback
	) {
		final AttributeColumn primaryKeyColumn = table.getPrimaryKeyColumn();
		boolean isCycle = attributeStack.contains(primaryKeyColumn.getAttribute());
		boolean drillDown;
		if (isCycle) {
			final Attribute[] dAttributeStack = convertStackToArray(domainAttributeStack);
			IterationCommand result = callback.onCyclicColumn(primaryKeyColumn, table, true, convertStackToArray(attributeStack), dAttributeStack);
			if (result == null) {
				throw new IllegalStateException("no command after cyclic column iteration");
			}
			drillDown = result == IterationCommand.CONTINUE;
		} else {
			drillDown = true;
		}
		
		if (drillDown) {
			attributeStack.push(primaryKeyColumn.getAttribute());
			final Attribute[] attStackWithPK;
			final Attribute[] attStackWithoutPK;
			if (TypeTable.class.isInstance(table)) {
				domainAttributeStack.push(primaryKeyColumn.getAttribute());
				attStackWithPK = convertStackToArray(domainAttributeStack);
				domainAttributeStack.pop();
				attStackWithoutPK = convertStackToArray(domainAttributeStack);
			} else {
				attStackWithoutPK = convertStackToArray(domainAttributeStack);
				attStackWithPK = attStackWithoutPK;
			}
			
			if (JoinTable.class.isInstance(table)) {
				JoinTable joinTable = (JoinTable) table;
				callback.onJoinTable(joinTable, attStackWithPK, attStackWithoutPK);
				callback.onColumn(primaryKeyColumn, table, true, attStackWithPK, attStackWithoutPK);
				callback.afterColumn(primaryKeyColumn, table, true, attStackWithPK, attStackWithoutPK);
				List<AttributeColumn> columns = joinTable.getColumns();
				if (columns != null) {
					for (AttributeColumn column : columns) {
						callback.onColumn(column, table, false, attStackWithPK, attStackWithoutPK);
						NamedAttributeHolderAttribute att = (NamedAttributeHolderAttribute) column.getAttribute();
						Table joinedTable = getIterationStartTable(att.getNamedAttributeHolder(), tableRegistry);
						if (joinedTable == joinTable) {
							joinedTable = tableRegistry.getTypeTableByType(att.getNamedAttributeHolder().getName());
						}
						callback.onJoinedTable(joinedTable, joinTable, column);
						iterateTypeHierarchyDown(joinedTable, tableRegistry, followAttributes, attributeStack, domainAttributeStack, callback);
						callback.afterJoinedTable(joinedTable, joinTable, column);
						callback.afterColumn(column, table, false, attStackWithPK, attStackWithoutPK);
					}
				}
				callback.afterJoinTable(joinTable, attStackWithPK, attStackWithoutPK);
			} else if (TypeTable.class.isInstance(table)) {
				TypeTable typeTable = (TypeTable) table;
				callback.onTypeTable(typeTable, attStackWithPK, attStackWithoutPK);
				callback.onColumn(primaryKeyColumn, table, true, attStackWithPK, attStackWithoutPK);
				callback.afterColumn(primaryKeyColumn, table, true, attStackWithPK, attStackWithoutPK);
				List<AttributeColumn> cols = typeTable.getColumns();
				if (cols != null) {
					for (AttributeColumn col : cols) {
						Attribute attribute = col.getAttribute();
						attributeStack.push(attribute);
						domainAttributeStack.push(attribute);
						final Attribute[] attStackWithoutPKAndWithCurrentAttribute = convertStackToArray(domainAttributeStack);
						callback.onColumn(col, table, false, attStackWithPK, attStackWithoutPKAndWithCurrentAttribute);
						if (followAttributes) {
							if (!attribute.isVirtual() && NamedAttributeHolderAttribute.class.isInstance(attribute)) {
								NamedAttributeHolder namedAttributeHolder = ((NamedAttributeHolderAttribute) attribute).getNamedAttributeHolder();
								if (!namedAttributeHolder.isVirtual()) {
									boolean isUnusedMixin = false;
									if (Mixin.class.isInstance(namedAttributeHolder)) {
										List<Type> mixedInto = ((Mixin) namedAttributeHolder).getMixedInto();
										isUnusedMixin = (mixedInto == null || mixedInto.isEmpty());
									}
									if (!isUnusedMixin) {
										Table foreignTable = getIterationStartTable(namedAttributeHolder, tableRegistry);
										callback.onAttributeForeignTable(foreignTable, col, table, false, attStackWithPK, attStackWithoutPKAndWithCurrentAttribute);
										iterateTypeHierarchyDown(
											foreignTable, tableRegistry, followAttributes, attributeStack, domainAttributeStack, callback
										);
										callback.afterAttributeForeignTable(foreignTable, col, table, false, attStackWithPK, attStackWithoutPKAndWithCurrentAttribute);
									}
								}
							}
						}
						callback.afterColumn(col, table, false, attStackWithPK, attStackWithoutPKAndWithCurrentAttribute);
						domainAttributeStack.pop();
						attributeStack.pop();
					}
				}
				callback.afterTypeTable(typeTable, attStackWithPK, attStackWithoutPK);
			}
			attributeStack.pop();
		}
	}

	private static Table getIterationStartTable(NamedAttributeHolder namedAttributeHolder, TableRegistry tableRegistry) {
		return getIterationStartTable(namedAttributeHolder.getName(), tableRegistry);
	}

	private static Table getIterationStartTable(String attributeHoldername, TableRegistry tableRegistry) {
		Table table = tableRegistry.getJoinTableByNamedAttributeHolder(attributeHoldername);
		if (table == null) {
			table = tableRegistry.getTypeTableByType(attributeHoldername);
		}
		if (table == null) {
			throw new SchemaException("no table found for " + attributeHoldername);
		}
		return table;
	}

	private static Attribute[] convertStackToArray(Stack<Attribute> attributeStack) {
		Attribute[] arr = new Attribute[attributeStack.size()];
		int i = 0;
		for (Attribute att : attributeStack) {
			arr[i] = att;
			i++;
		}
		return arr;
	}

}
