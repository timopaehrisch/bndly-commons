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
import org.bndly.schema.model.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LoggingTableIteratorListener extends TableHierarchyIterator.NoOpIterationCallback {
	private static final Logger LOG = LoggerFactory.getLogger(LoggingTableIteratorListener.class);

	@Override
	public void onJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("onJoinTable: {}", joinTable.getTableName());
	}

	@Override
	public void afterJoinTable(JoinTable joinTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("afterJoinTable: {}", joinTable.getTableName());
	}

	@Override
	public void onTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("onTypeTable: {}", typeTable.getTableName());
	}

	@Override
	public void afterTypeTable(TypeTable typeTable, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("afterTypeTable: {}", typeTable.getTableName());
	}

	@Override
	public void onJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn) {
		LOG.info("onJoinedTable: {} - {}.{}", joinedTable.getTableName(), joinTable.getTableName(), attributeColumn.getColumnName());
	}

	@Override
	public void afterJoinedTable(Table joinedTable, JoinTable joinTable, AttributeColumn attributeColumn) {
		LOG.info("afterJoinedTable: {} - {}.{}", joinedTable.getTableName(), joinTable.getTableName(), attributeColumn.getColumnName());
	}

	@Override
	public void onColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("onColumn: {}.{}", table.getTableName(), column.getColumnName());
	}

	@Override
	public void afterColumn(AttributeColumn column, Table table, boolean isPrimaryKeyColumn, Attribute[] attributeStack, Attribute[] domainAttributeStack) {
		LOG.info("afterColumn: {}.{}", table.getTableName(), column.getColumnName());
	}
	
}
