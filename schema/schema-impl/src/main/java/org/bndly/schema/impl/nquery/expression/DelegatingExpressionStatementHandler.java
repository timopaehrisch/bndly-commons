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

import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.nquery.BooleanOperator;
import org.bndly.schema.api.nquery.Expression;
import org.bndly.schema.api.nquery.ExpressionStatementHandler;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.ParsingState;
import org.bndly.schema.api.nquery.QueryParsingException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingExpressionStatementHandler implements ExpressionStatementHandler {

	private final List<ExpressionStatementHandler> handlers = new ArrayList<>();
	
	@Override
	public void handleStatement(String statement, Parser parser, final Context context) throws QueryParsingException {
		final ObjectReference<Expression> exp = new ObjectReference<>();
		Context ctx = new Context() {
			
			@Override
			public BooleanOperator nextOperator() {
				return context.nextOperator();
			}

			@Override
			public void expressionCreated(Expression expression) throws QueryParsingException {
				context.expressionCreated(expression);
				exp.set(expression);
			}
		};
		parser.markQueryArgPosition();
		ParsingState peekBefore = parser.peek();
		for (ExpressionStatementHandler handler : handlers) {
			ParsingState localPeekBefore = parser.peek();
			try {
				parser.resetQueryArgPosition();
				handler.handleStatement(statement, parser, ctx);
			} catch (QueryParsingException e) {
				// any handler is allowed to fail, but since the first might 
				// fail and the second might succeed, we have to catch the 
				// exceptions here.
			}
			ParsingState localPeekAfter = parser.peek();
			if (localPeekBefore != localPeekAfter) {
				throw new QueryParsingException("expression statement handler did alter the peek parsing state stack");
			}
			if (exp.get() != null) {
				break;
			}
		}
		ParsingState peekAfter = parser.peek();
		if (peekBefore != peekAfter) {
			throw new QueryParsingException("peek parsing states have changed while parsing an expression statement");
		}
		if (exp.get() == null) {
			throw new QueryParsingException("statement could not be converted to an expression");
		}
	}

	public void addExpressionStatementHandler(ExpressionStatementHandler handler) {
		if (handler != null) {
			handlers.add(handler);
		}
	}

	public void removeExpressionStatementHandler(ExpressionStatementHandler handler) {
		if (handler != null) {
			handlers.remove(handler);
		}
	}

	public void clear() {
		handlers.clear();
	}
	
}
