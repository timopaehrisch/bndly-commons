package org.bndly.schema.impl.nquery.expression;

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

import org.bndly.schema.api.nquery.Expression;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.impl.nquery.ComparisonExpression;
import org.bndly.schema.impl.nquery.ComparisonExpressionImpl;
import org.bndly.schema.impl.nquery.ContextVariable;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ComparisonExpressionStatementHandler extends AbstractSimpleExpressionStatementHandler {

	private final ComparisonExpression.Type type;
	private final boolean allowParameterNull;
	
	public ComparisonExpressionStatementHandler(String keyword, ComparisonExpression.Type type, boolean allowParameterNull) {
		super(keyword);
		this.type = type;
		this.allowParameterNull = allowParameterNull;
	}

	@Override
	protected Expression createExpression(ContextVariable l, ContextVariable r, Boolean negated, Context context, Parser parser, String statement) throws QueryParsingException {
		ComparisonExpressionImpl comparisonExpressionImpl = new ComparisonExpressionImpl(statement, type);
		comparisonExpressionImpl.setNegated(negated);
		comparisonExpressionImpl.setLeft(l);
		comparisonExpressionImpl.setRight(r);
		comparisonExpressionImpl.setNextOperator(context.nextOperator());
		if (!allowParameterNull && l.isArg()) {
			if (l.getArg() == null) {
				throw new QueryParsingException("parameter was null but should have been a non-null value");
			}
		}
		if (!allowParameterNull && r.isArg()) {
			if (r.getArg() == null) {
				throw new QueryParsingException("parameter was null but should have been a non-null value");
			}
		}
		return comparisonExpressionImpl;
	}
	
}
