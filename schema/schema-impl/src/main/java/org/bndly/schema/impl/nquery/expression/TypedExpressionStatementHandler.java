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

import org.bndly.schema.api.nquery.ExpressionStatementHandler;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.EntityAttributeReferenceVariable;
import org.bndly.schema.impl.nquery.TypedExpressionImpl;
import org.bndly.schema.impl.nquery.states.AcceptStringState;
import org.bndly.schema.impl.nquery.states.ConsumeWhiteSpacesState;
import org.bndly.schema.impl.nquery.states.ReadContextVariableState;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypedExpressionStatementHandler implements ExpressionStatementHandler {

	@Override
	public void handleStatement(final String statement, Parser parser, final Context context) throws QueryParsingException {
		parser.push(new ReadContextVariableState() {

			@Override
			protected void onContextVariable(final ContextVariable field, Character character, Parser parser) throws QueryParsingException {
				if (field.isArg()) {
					throw new QueryParsingException("expected an entity attribute reference left of TYPED keyword");
				}
				parser.push(new ConsumeWhiteSpacesState() {

					@Override
					protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
						parser.pop();
						parser.push(new AcceptStringState(ReservedKeywords.TYPED) {
							
							@Override
							protected void accepted(Parser parser) throws QueryParsingException {
								parser.pop();
								parser.push(new ReadContextVariableState() {

									@Override
									protected void onContextVariable(
											ContextVariable contextVariable, Character character, Parser parser
									) throws QueryParsingException {
										if (!contextVariable.isArg()) {
											throw new QueryParsingException("expected a query argument right of TYPED keyword");
										}
										Object val = contextVariable.getArg();
										if (!String.class.isInstance(val)) {
											throw new QueryParsingException("query argument should be a string for TYPED expression");
										}
										TypedExpressionImpl typedExpressionImpl = new TypedExpressionImpl(statement);
										EntityAttributeReferenceVariable f = new EntityAttributeReferenceVariable(field.getName() + ".id");
										typedExpressionImpl.setField(f);
										typedExpressionImpl.setTypeName((String) val);
										typedExpressionImpl.setNextOperator(context.nextOperator());
										context.expressionCreated(typedExpressionImpl);
									}
								});
							}
							
							@Override
							protected void notAccepted(Character character, Parser parser) throws QueryParsingException {
								parser.pop();
								throw new QueryParsingException("statement did not contain TYPED keyword");
							}
						}).handleChar(character, parser);
					}
				});
			}
		});
		parser.parse(statement);
	}
	
}
