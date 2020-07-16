package org.bndly.schema.impl.nquery.sqlmapper;

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
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.impl.nquery.TypedExpression;
import static org.bndly.schema.impl.nquery.sqlmapper.InRangeExpressionMapper.addAttributesToSet;
import org.bndly.schema.impl.query.ExpressionProducer;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypedExpressionMapper extends AbstractFieldCentricMapper<TypedExpression> implements RequiredAttribtuesInspector<TypedExpression> {

	public TypedExpressionMapper() {
		super(TypedExpression.class);
	}

	@Override
	protected void map(
			TypedExpression booleanStatement, 
			ExpressionProducer expressionProducer, 
			String prefix, 
			MappingBinding mappingBinding, 
			BooleanStatementSQLMapper.Context context, 
			AliasBinding aliasBinding
	) {
		Table table = aliasBinding.getAttributeColumn().getTable();
		if (!TypeTable.class.isInstance(table)) {
			throw new IllegalStateException("expected a type table for the id field");
		}
		TypeTable tt = (TypeTable) table;
		if (tt.getType().getName().equals(booleanStatement.getTypeName())) {
			String fieldName = aliasBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
			expressionProducer.produce().criteria().field(fieldName).isNotNull();
		}
	}

	@Override
	public void collectRequiredAttributesFromBooleanStatement(Set<String> set, String alias, TypedExpression booleanStatement, RequiredAttribtuesInspector.Context context) {
		addAttributesToSet(booleanStatement.getField(), set, alias);
	}

	@Override
	protected String getAttributePath(TypedExpression booleanStatement) {
		return booleanStatement.getField().getName();
	}
	
}
