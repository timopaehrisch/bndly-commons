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
import org.bndly.schema.api.nquery.ExpressionStatementHandler;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.EntityAttributeReferenceVariable;
import org.bndly.schema.impl.nquery.QueryArgumentContextVariable;
import org.bndly.schema.impl.nquery.states.AcceptStringState;
import org.bndly.schema.impl.nquery.states.ConsumeWhiteSpacesState;
import org.bndly.schema.impl.nquery.states.ReadContextVariableState;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractSimpleExpressionStatementHandler implements ExpressionStatementHandler {
	private final String expressionOperator;

	public AbstractSimpleExpressionStatementHandler(String expressionOperator) {
		if (expressionOperator == null) {
			throw new IllegalArgumentException("expression operator is not allowed to be null");
		}
		this.expressionOperator = expressionOperator;
	}
	
	@Override
	public void handleStatement(final String statement, final Parser parser, final Context context) throws QueryParsingException {
		parser.push(new ReadContextVariableState() {

			private ContextVariable left;
			private ContextVariable right;
			private Boolean negated;
			
			@Override
			protected void onContextVariable(ContextVariable contextVariable, Character character, Parser parser) throws QueryParsingException {
				left = contextVariable;
				if (character == null) {
					throw new QueryParsingException("missing operator");
				} else {
					parser.push(new ConsumeWhiteSpacesState() {

						@Override
						protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
							parser.pop();
							parser.push(new AcceptStringState(expressionOperator) {

								@Override
								protected void accepted(Parser parser) throws QueryParsingException {
									parser.pop();
									parser.push(new ReadContextVariableState() {

										@Override
										protected void onContextVariable(
												ContextVariable contextVariable, Character character, Parser parser
										) throws QueryParsingException {
											right = contextVariable;
											if (negated != null && left != null && right != null) {
												Expression exp = createExpression(left, right, negated, context, parser, statement);
												if (exp == null) {
													throw new QueryParsingException("implementation did not create expression object");
												}
												context.expressionCreated(exp);
											}
										}
									});
								}

								@Override
								protected void notAccepted(Character character, Parser parser) throws QueryParsingException {
									parser.pop();
									throw new QueryParsingException("could not read expression operator");
								}

							});
							if (Character.toString(character).equals(ReservedKeywords.NEGATION)) {
								negated = true;
							} else {
								negated = false;
								parser.peek().handleChar(character, parser);
							}
						}
					}).handleChar(character, parser);
				}
			}
		});
		parser.parse(statement);
	}
	
	protected abstract Expression createExpression(ContextVariable left, ContextVariable right, Boolean negated, Context context, Parser parser, String statement) throws QueryParsingException;

	protected final ContextVariable createContextVariable(String rawString, Parser parser) throws QueryParsingException {
		if (ReservedKeywords.PARAM_WILDCARD.equals(rawString)) {
			return new QueryArgumentContextVariable(parser.getNextQueryArg());
		} else {
			return new EntityAttributeReferenceVariable(rawString);
		}
	}
	
}
