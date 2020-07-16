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
import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.query.Criteria;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.ComparisonExpression;
import org.bndly.schema.impl.query.ExpressionProducer;
import org.bndly.schema.model.Attribute;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ComparisonExpressionMapper 
	extends AbstractFieldCentricMapper<ComparisonExpression> 
	implements BooleanStatementSQLMapper<ComparisonExpression>, RequiredAttribtuesInspector<ComparisonExpression> {

	private final MediatorProvider mediatorProvider;
	
	public ComparisonExpressionMapper(MediatorProvider mediatorProvider) {
		super(ComparisonExpression.class);
		if (mediatorProvider == null) {
			throw new IllegalArgumentException("mediatorProvider is not allowed to be null");
		}
		this.mediatorProvider = mediatorProvider;
	}

	@Override
	protected void map(
			ComparisonExpression booleanStatement, 
			ExpressionProducer expressionProducer, 
			String prefix, 
			MappingBinding mappingBinding, 
			BooleanStatementSQLMapper.Context context, 
			AliasBinding aliasBinding
	) {
		Criteria criteria = expressionProducer.produce().criteria();
		mapComparisonExpressionToCriteria(booleanStatement, criteria, aliasBinding);
	}

	@Override
	protected String getAttributePath(ComparisonExpression booleanStatement) {
		return booleanStatement.getLeft().getName();
	}

	@Override
	public void collectRequiredAttributesFromBooleanStatement(Set<String> set, String alias, ComparisonExpression booleanStatement, RequiredAttribtuesInspector.Context context) {
		ContextVariable left = booleanStatement.getLeft();
		ContextVariable right = booleanStatement.getRight();
		if (!left.isArg()) {
			Util.addAttributeToSet(set, Util.stripEntityAliasPrefix(alias, left.getName()));
		}
		if (!right.isArg()) {
			Util.addAttributeToSet(set, Util.stripEntityAliasPrefix(alias, right.getName()));
		}
	}

	private void mapComparisonExpressionToCriteria(ComparisonExpression ee, Criteria criteria, AliasBinding alias) {
		String fieldName = alias.getTableAlias() + "." + alias.getAttributeColumn().getColumnName();
		if (ee.getComparisonType() == ComparisonExpression.Type.EQUAL) {
			mapEquals(ee, criteria, fieldName, alias.getAttributeColumn());
		} else if (ee.getComparisonType() == ComparisonExpression.Type.GREATER) {
			mapGreater(ee, criteria, fieldName, alias.getAttributeColumn());
		} else if (ee.getComparisonType() == ComparisonExpression.Type.LOWER) {
			mapLower(ee, criteria, fieldName, alias.getAttributeColumn());
		} else if (ee.getComparisonType() == ComparisonExpression.Type.GREATER_EQUAL) {
			mapGreaterEqual(ee, criteria, fieldName, alias.getAttributeColumn());
		} else if (ee.getComparisonType() == ComparisonExpression.Type.LOWER_EQUAL) {
			mapLowerEqual(ee, criteria, fieldName, alias.getAttributeColumn());
		} else {
			throw new IllegalStateException("unsupported comparison type");
		}
	}

	private void mapEquals(ComparisonExpression ee, Criteria criteria, String fieldName, AttributeColumn attributeColumn) {
		if (ee.getRight().getArg() == null) {
			if (ee.isNegated()) {
				criteria
						.field(fieldName)
						.isNotNull();
			} else {
				criteria
						.field(fieldName)
						.isNull();
			}
		} else {
			if (ee.isNegated()) {
				criteria
						.field(fieldName)
						.notEqual()
						.value(createPreparedStatementValueProvider(attributeColumn, ee.getRight()));
			} else {
				criteria
						.field(fieldName)
						.equal()
						.value(createPreparedStatementValueProvider(attributeColumn, ee.getRight()));
			}
		}
	}
	
	private PreparedStatementValueProvider createPreparedStatementValueProvider(final AttributeColumn attributeColumn, final ContextVariable contextVariable) {
		return createPreparedStatementValueProvider(mediatorProvider, attributeColumn, contextVariable);
	}
	
	static PreparedStatementValueProvider createPreparedStatementValueProvider(MediatorProvider mediatorProvider, final AttributeColumn attributeColumn, final ContextVariable contextVariable) {
		final Attribute attribute = attributeColumn.getAttribute();
		final AttributeMediator mediator = mediatorProvider.getMediatorForAttribute(attribute);
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				return contextVariable.getArg();
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				mediator.setRawParameterInPreparedStatement(index, ps, attribute, get());
			}
		};
	}

	private void mapGreater(ComparisonExpression ee, Criteria criteria, String fieldName, AttributeColumn attributeColumn) {
		criteria.field(fieldName);
		ContextVariable contextVariable = ee.getRight().isArg() ? ee.getRight() : ee.getLeft();
		PreparedStatementValueProvider vp = createPreparedStatementValueProvider(attributeColumn, contextVariable);
		if (ee.isNegated()) {
			criteria.equalOrLowerThan().value(vp);
		} else {
			criteria.greaterThan().value(vp);
		}
	}
	
	private void mapLower(ComparisonExpression ee, Criteria criteria, String fieldName, AttributeColumn attributeColumn) {
		criteria.field(fieldName);
		ContextVariable contextVariable = ee.getRight().isArg() ? ee.getRight() : ee.getLeft();
		PreparedStatementValueProvider vp = createPreparedStatementValueProvider(attributeColumn, contextVariable);
		if (ee.isNegated()) {
			criteria.equalOrGreaterThan().value(vp);
		} else {
			criteria.lowerThan().value(vp);
		}
	}
	
	private void mapLowerEqual(ComparisonExpression ee, Criteria criteria, String fieldName, AttributeColumn attributeColumn) {
		criteria.field(fieldName);
		ContextVariable contextVariable = ee.getRight().isArg() ? ee.getRight() : ee.getLeft();
		PreparedStatementValueProvider vp = createPreparedStatementValueProvider(attributeColumn, contextVariable);
		if (ee.isNegated()) {
			criteria.greaterThan().value(vp);
		} else {
			criteria.equalOrLowerThan().value(vp);
		}
	}
	
	private void mapGreaterEqual(ComparisonExpression ee, Criteria criteria, String fieldName, AttributeColumn attributeColumn) {
		criteria.field(fieldName);
		ContextVariable contextVariable = ee.getRight().isArg() ? ee.getRight() : ee.getLeft();
		PreparedStatementValueProvider vp = createPreparedStatementValueProvider(attributeColumn, contextVariable);
		if (ee.isNegated()) {
			criteria.lowerThan().value(vp);
		} else {
			criteria.equalOrGreaterThan().value(vp);
		}
	}

}
