package org.bndly.schema.impl.nquery.states;

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
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.api.nquery.BooleanOperator;
import org.bndly.schema.api.nquery.BooleanStatement;
import org.bndly.schema.api.nquery.Expression;
import org.bndly.schema.api.nquery.ExpressionStatementHandler;
import org.bndly.schema.impl.nquery.ExpressionImpl;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.WrapperBooleanStatement;
import org.bndly.schema.impl.nquery.WrapperBooleanStatementImpl;
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class BooleanStatementState extends ConsumeWhiteSpacesState {
	
	private final Stack<BooleanStatement> statementStack = new Stack<>();
	private BooleanStatement first;
	
	@Override
	protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
		parser.push(createReadAheadState());
		doContinue(character, parser);
	}

	private ReadAheadState createReadAheadState() {
		return new ReadAheadState(
				ReservedKeywords.GROUP_START,
				ReservedKeywords.GROUP_END,
				ReservedKeywords.BOOLEAN_OPERATOR_AND,
				ReservedKeywords.BOOLEAN_OPERATOR_OR,
				ReservedKeywords.ORDERBY,
				ReservedKeywords.LIMIT,
				ReservedKeywords.OFFSET
		) {

			@Override
			protected void onStopWord(String bufferedBeforeStopWord, String stopWord, char character, Parser parser) throws QueryParsingException {
				parser.pop();
				addStatement(bufferedBeforeStopWord, stopWord, parser);
				if (ReservedKeywords.GROUP_START.equals(stopWord)
						|| ReservedKeywords.GROUP_END.equals(stopWord)
						|| ReservedKeywords.BOOLEAN_OPERATOR_AND.equals(stopWord)
						|| ReservedKeywords.BOOLEAN_OPERATOR_OR.equals(stopWord)
						) {
					parser.push(createReadAheadState());
				} else {
					onEndOfBooleanStatement(stopWord, parser);
				}
			}

			@Override
			public void onEnd(Parser parser) throws QueryParsingException {
				// the buffered string should be parsed separatly
				parser.pop();
				addStatement(getBuffered(), null, parser);
				boolean isPeek = true;
				while (!statementStack.isEmpty()) {
					BooleanStatement top = statementStack.pop();
					if (WrapperBooleanStatement.class.isInstance(top)) {
						BooleanStatement wrapped = ((WrapperBooleanStatement) top).getWrapped();
						if (wrapped == null || !isPeek) {
							throw new QueryParsingException("wrapper element was not closed");
						}
					}
					isPeek = false;
				}
			}

		};
	}
	
	private void addStatement(String originalStatement, String stopWord, Parser parser) throws QueryParsingException {
		final String statement = originalStatement.trim();
		final BooleanOperator operator;
		if (ReservedKeywords.BOOLEAN_OPERATOR_AND.equals(stopWord)) {
			operator = BooleanOperator.AND;
		} else if (ReservedKeywords.BOOLEAN_OPERATOR_OR.equals(stopWord)) {
			operator = BooleanOperator.OR;
		} else {
			operator = null;
		}
		if (!statement.isEmpty()) {
			// the expression might contain wildcards. the arguments for the wildcards can be retrieved from the parser.
			// TODO: look for wildcards and set the arguments in the expression
			ExpressionStatementHandler handler = parser.getExpressionStatementHandler();
			if (handler == null) {
				throw new QueryParsingException("can not parse statement, when no expression statementhandler is provided by the parser");
			}
			final ObjectReference<Boolean> isCreatedRef = new ObjectReference<>(Boolean.FALSE);
			handler.handleStatement(statement, parser, new ExpressionStatementHandler.Context() {

				@Override
				public BooleanOperator nextOperator() {
					return operator;
				}
				
				@Override
				public void expressionCreated(Expression expression) throws QueryParsingException {
					if (!isCreatedRef.get()) {
						isCreatedRef.set(Boolean.TRUE);
						if (expression.getNextOperator() != operator) {
							throw new IllegalArgumentException("created expression did not contain the boolean operator for the next expression");
						}
						appendBooleanStatement(expression);
					} else {
						throw new IllegalStateException("already created boolean statement for expression");
					}
				}
				
			});
			if (!isCreatedRef.get()) {
				throw new QueryParsingException("no expression created for statement string");
			}
		} else {
			if (ReservedKeywords.GROUP_START.equals(stopWord)) {
				appendBooleanStatement(new WrapperBooleanStatementImpl());
			} else if (operator != null && !statementStack.isEmpty() && (WrapperBooleanStatementImpl.class.isInstance(statementStack.peek()))) {
				((WrapperBooleanStatementImpl)statementStack.peek()).setNextOperator(operator);
			} else if (operator != null && statementStack.isEmpty()) {
				throw new QueryParsingException("can not append a boolean operator, when there is no preceding element in the statement stack");
			}
		}
		
		if (ReservedKeywords.GROUP_END.equals(stopWord)) {
			BooleanStatement top = statementStack.pop();
			if (!WrapperBooleanStatement.class.isInstance(top)) {
				if (statementStack.isEmpty()) {
					throw new QueryParsingException("wrapper was closed before it was actually opened");
				}
			}
			if (!statementStack.isEmpty()) {
				top = statementStack.pop();
			}
			if (!WrapperBooleanStatement.class.isInstance(top)) {
				throw new QueryParsingException("stack of boolean statements was messed up when closing a wrapper.");
			} else {
				if (((WrapperBooleanStatement) top).getWrapped() != null) {
					statementStack.push(top);
				}
			}
		}
	}
	
	private void appendBooleanStatement(BooleanStatement toAppend) throws QueryParsingException {
		if (first == null) {
			first = toAppend;
			onFirstBooleanStatement(toAppend);
		}
		if (statementStack.isEmpty()) {
			statementStack.push(toAppend);
		} else {
			BooleanStatement peek = statementStack.peek();
			BooleanOperator operator = peek.getNextOperator();
			if (ExpressionImpl.class.isInstance(peek)) {
				if (operator == null) {
					throw new QueryParsingException("can not append a statement when there is not operator provided");
				}
				((ExpressionImpl)peek).setNext(toAppend);
				statementStack.pop();
				statementStack.push(toAppend);
			} else if (WrapperBooleanStatementImpl.class.isInstance(peek)) {
				WrapperBooleanStatementImpl wrapper = (WrapperBooleanStatementImpl) peek;
				BooleanStatement wrapped = wrapper.getWrapped();
				if (wrapped != null) {
					if (operator == null) {
						throw new QueryParsingException("can not append a statement when there is not operator provided");
					}
					wrapper.setNext(toAppend);
					statementStack.pop();
				} else {
					wrapper.setWrapped(toAppend);
				}
				statementStack.push(toAppend);
			} else {
				throw new IllegalStateException("unsupported stackelement. can not append boolean statement.");
			}
		}
	}
	
	protected abstract void onFirstBooleanStatement(BooleanStatement first);
	
	protected abstract void onEndOfBooleanStatement(String stopKeyword, Parser parser) throws QueryParsingException ;
	
}
