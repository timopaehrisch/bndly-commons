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
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.db.TypeTable;
import java.util.ArrayList;
import java.util.List;

public class SingleTableMappingBindingsProvider extends DelegatingMappingBindingsProvider {

	private TypeTable table;

	@Override
	protected List<MappingBinding> localMappingBindings() {
		MappingBinding b = localRootMappingBindings();
		if (b == null) {
			return null;
		}

		List<MappingBinding> bindings = new ArrayList<>();
		bindings.add(b);
		return bindings;
	}

	@Override
	protected MappingBinding localRootMappingBindings() {
		if (table == null) {
			return null;
		}
		AliasBinding pkAlias = new AliasBinding(table.getPrimaryKeyColumn().getColumnName(), table.getPrimaryKeyColumn(), table.getTableName());
		MappingBinding binding = new MappingBinding(table.getType(), pkAlias, table.getTableName());
		for (AttributeColumn attributeColumn : table.getColumns()) {
			AliasBinding attributeAlias = new AliasBinding(attributeColumn.getColumnName(), attributeColumn, table.getTableName());
			binding.addAlias(attributeAlias);
		}
		return binding;
	}

	public void setTable(TypeTable table) {
		this.table = table;
	}

}
