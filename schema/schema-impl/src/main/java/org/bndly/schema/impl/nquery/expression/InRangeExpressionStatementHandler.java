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
import org.bndly.schema.api.nquery.ParsingState;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.EntityAttributeReferenceVariable;
import org.bndly.schema.impl.nquery.InRangeExpressionImpl;
import org.bndly.schema.impl.nquery.QueryArgumentContextVariable;
import org.bndly.schema.impl.nquery.states.ReadAheadState;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class InRangeExpressionStatementHandler implements ExpressionStatementHandler {

	@Override
	public void handleStatement(final String statement, Parser parser, final Context context) throws QueryParsingException {
		parser.push(new ReadAheadState(ReservedKeywords.NEGATION, ReservedKeywords.INRANGE) {
			private String left;
			private Boolean negated;
			private ContextVariable lower;
			private ContextVariable upper;
			
			@Override
			protected void onStopWord(String buffered, String stopWord, char character, Parser parser) throws QueryParsingException {
				parser.pop();
				if (ReservedKeywords.INRANGE.equals(stopWord)) {
					left = buffered;
					if (negated == null) {
						negated = false;
					}
					parser.push(readRight());
				} else if (ReservedKeywords.NEGATION.equals(stopWord)) {
					negated = true;
					left = buffered;
					parser.push(new ReadAheadState(ReservedKeywords.INRANGE) {

						@Override
						protected void onStopWord(String buffered, String stopWord, char character, Parser parser) throws QueryParsingException {
							parser.pop();
							if (!buffered.trim().equals("")) {
								throw new QueryParsingException("negation has to be followed by INRANGE");
							}
							parser.push(readRight());
						}

						@Override
						public void onEnd(Parser parser) throws QueryParsingException {
							parser.pop();
							throw new QueryParsingException("INRANGE statement was incomplete");
						}
					});
				} else {
					throw new QueryParsingException("unsupported stop word while parsing INRANGE statement");
				}
			}
			
			@Override
			public void onEnd(Parser parser) throws QueryParsingException {
				parser.pop();
				throw new QueryParsingException("INRANGE statement was incomplete");
			}
			
			private ParsingState readRight() {
				return new ParsingState() {
					boolean expectParam = true;
					@Override
					public void handleChar(char character, Parser parser) throws QueryParsingException {
						if (Character.isWhitespace(character)) {
							return;
						}
						String cs = Character.toString(character);
						if (expectParam && ReservedKeywords.PARAM_WILDCARD.equals(cs)) {
							expectParam = false;
							if (lower == null) {
								lower = new QueryArgumentContextVariable(parser.getNextQueryArg());
							} else if (upper == null) {
								upper = new QueryArgumentContextVariable(parser.getNextQueryArg());
							} else {
								parser.pop();
								throw new QueryParsingException("INRANGE requires two arguments");
							}
						} else if (!expectParam && character == ',') {
							expectParam = true;
							return;
						} else {
							parser.pop();
							throw new QueryParsingException("parameters of INRANGE have to be separated by a comma");
						}
					}

					@Override
					public void onEnd(Parser parser) throws QueryParsingException {
						parser.pop();
						if (left != null && negated != null && lower != null && upper != null) {
							InRangeExpressionImpl inRangeExpressionImpl = new InRangeExpressionImpl(statement);
							inRangeExpressionImpl.setNegated(negated);
							inRangeExpressionImpl.setNextOperator(context.nextOperator());
							inRangeExpressionImpl.setField(new EntityAttributeReferenceVariable(left.trim()));
							inRangeExpressionImpl.setLowerBorder(lower);
							inRangeExpressionImpl.setUpperBorder(upper);
							context.expressionCreated(inRangeExpressionImpl);
						} else {
							throw new QueryParsingException("failed to parse INRANGE statement. not all information could be collected");
						}
					}
				};
			}
		});
		parser.parse(statement);
	}
	
}
