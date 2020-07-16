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
import org.bndly.schema.api.query.Criteria;
import org.bndly.schema.api.query.Expression;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.InRangeExpression;
import org.bndly.schema.impl.query.ExpressionProducer;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class InRangeExpressionMapper extends AbstractFieldCentricMapper<InRangeExpression> implements RequiredAttribtuesInspector<InRangeExpression> {
	private final MediatorProvider mediatorProvider;

	public InRangeExpressionMapper(MediatorProvider mediatorProvider) {
		super(InRangeExpression.class);
		if (mediatorProvider == null) {
			throw new IllegalArgumentException("mediatorProvider is not allowed to be null");
		}
		this.mediatorProvider = mediatorProvider;
	}

	@Override
	protected void map(
			InRangeExpression booleanStatement, 
			ExpressionProducer expressionProducer, 
			String prefix, 
			MappingBinding mappingBinding, 
			BooleanStatementSQLMapper.Context context, 
			AliasBinding aliasBinding
	) {
		mapToCriteria(booleanStatement, expressionProducer, aliasBinding);
	}

	@Override
	public void collectRequiredAttributesFromBooleanStatement(Set<String> set, String alias, InRangeExpression booleanStatement, RequiredAttribtuesInspector.Context context) {
		addAttributesToSet(booleanStatement.getField(), set, alias);
		addAttributesToSet(booleanStatement.getLowerBorder(), set, alias);
		addAttributesToSet(booleanStatement.getUpperBorder(), set, alias);
	}

	static void addAttributesToSet(ContextVariable cv, Set<String> set, String alias) {
		if (!cv.isArg()) {
			Util.addAttributeToSet(set, Util.stripEntityAliasPrefix(alias, cv.getName()));
		}
	}
	
	@Override
	protected String getAttributePath(InRangeExpression booleanStatement) {
		return booleanStatement.getField().getName();
	}

	private void mapToCriteria(InRangeExpression booleanStatement, ExpressionProducer expressionProducer, AliasBinding aliasBinding) {
		String fieldName = aliasBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
		Object lower = booleanStatement.getLowerBorder().getArg();
		Object upper = booleanStatement.getUpperBorder().getArg();
		if (lower != null && upper != null) {
			PreparedStatementValueProvider lowerValueProvider = ComparisonExpressionMapper.createPreparedStatementValueProvider(
					mediatorProvider, aliasBinding.getAttributeColumn(), booleanStatement.getLowerBorder()
			);
			PreparedStatementValueProvider upperValueProvider = ComparisonExpressionMapper.createPreparedStatementValueProvider(
					mediatorProvider, aliasBinding.getAttributeColumn(), booleanStatement.getUpperBorder()
			);
			if (booleanStatement.isNegated()) {
				Expression expression = expressionProducer.produce().wrap().wrapped();
				expression.criteria().field(fieldName).lowerThan().value(lowerValueProvider);
				expression.or().criteria().field(fieldName).greaterThan().value(upperValueProvider);
			} else {
				Criteria criteria = expressionProducer.produce().criteria();
				criteria.field(fieldName).between().left(lowerValueProvider).right(upperValueProvider);
			}
		} else if (lower != null && upper == null) {
			Criteria criteria = expressionProducer.produce().criteria();
			PreparedStatementValueProvider lowerValueProvider = ComparisonExpressionMapper.createPreparedStatementValueProvider(
					mediatorProvider, aliasBinding.getAttributeColumn(), booleanStatement.getLowerBorder()
			);
			if (booleanStatement.isNegated()) {
				criteria.field(fieldName).lowerThan().value(lowerValueProvider);
			} else {
				criteria.field(fieldName).equalOrGreaterThan().value(lowerValueProvider);
			}
		} else if (lower == null && upper != null) {
			Criteria criteria = expressionProducer.produce().criteria();
			PreparedStatementValueProvider upperValueProvider = ComparisonExpressionMapper.createPreparedStatementValueProvider(
					mediatorProvider, aliasBinding.getAttributeColumn(), booleanStatement.getUpperBorder()
			);
			if (booleanStatement.isNegated()) {
				criteria.field(fieldName).greaterThan().value(upperValueProvider);
			} else {
				criteria.field(fieldName).equalOrLowerThan().value(upperValueProvider);
			}
		} else {
			throw new IllegalStateException("neither left or right border are defined for INRANGE");
		}
	}
	
}
