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

import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.IfClauseImpl;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.impl.nquery.PickImpl;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.EntityAttributeReferenceVariable;
import org.bndly.schema.impl.nquery.OrderingImpl;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PickState extends ConsumeWhiteSpacesState {
	
	private final PickImpl pickImpl;

	public PickState(PickImpl pickImpl) {
		this.pickImpl = pickImpl;
	}

	@Override
	protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
		parser.pop();
		parser.push(new TokenState() {

			@Override
			protected void onTokenComplete(Character character, Parser parser) throws QueryParsingException {
				pickImpl.setAttributeHolderName(getToken());
				parser.pop();
				parser.push(new ConsumeWhiteSpacesState() {

					@Override
					protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
						parser.pop();
						parser.push(new ReservedKeywordState() {

							@Override
							protected void onAlias(String alias, Character character, Parser parser) throws QueryParsingException {
								onParsedAlias(alias, character, parser);
							}

							@Override
							protected void onKeyword(String kw, Character character, Parser parser) throws QueryParsingException {
								onParsedKeyword(kw, character, parser);
							}

						}).handleChar(character, parser);
					}
				});
				doContinue(character, parser);
			}
		}).handleChar(character, parser);
	}

	@Override
	public void onEnd(Parser parser) throws QueryParsingException {
		if (pickImpl.getAttributeHolderName() == null) {
			throw new QueryParsingException("parsing a PICK failed because not attribute holder was provided");
		}
	}
	
	private void onParsedKeyword(String kw, Character character, Parser parser) throws QueryParsingException {
		if (ReservedKeywords.IF_CLAUSE.equals(kw)) {
			IfClauseImpl ifClauseImpl = new IfClauseImpl();
			pickImpl.setIfClause(ifClauseImpl);
			parser.push(new IfClauseState(ifClauseImpl));
		} else if (ReservedKeywords.LIMIT.equals(kw)) {
			if (pickImpl.getLimit() != null) {
				throw new QueryParsingException("LIMIT has already been defined");
			}
			parser.push(new ReadContextVariableState() {

				@Override
				protected void onContextVariable(ContextVariable contextVariable, Character character, Parser parser) throws QueryParsingException {
					if (!contextVariable.isArg()) {
						throw new QueryParsingException("expected an argument as a context variable for LIMIT");
					}
					Object limit = contextVariable.getArg();
					if (!Number.class.isInstance(limit)) {
						throw new QueryParsingException("LIMIT has to be a number");
					}
					pickImpl.setLimit(((Number)limit).longValue());
				}
			});
		} else if (ReservedKeywords.OFFSET.equals(kw)) {
			if (pickImpl.getOffset() != null) {
				throw new QueryParsingException("OFFSET has already been defined");
			}
			parser.push(new ReadContextVariableState() {

				@Override
				protected void onContextVariable(ContextVariable contextVariable, Character character, Parser parser) throws QueryParsingException {
					if (!contextVariable.isArg()) {
						throw new QueryParsingException("expected an argument as a context variable for OFFSET");
					}
					Object offset = contextVariable.getArg();
					if (!Number.class.isInstance(offset)) {
						throw new QueryParsingException("OFFSET has to be a number");
					}
					pickImpl.setOffset(((Number)offset).longValue());
				}
			});
		} else if (ReservedKeywords.ORDERBY.equals(kw)) {
			parser.push(new ReadContextVariableState() {

				@Override
				protected void onContextVariable(ContextVariable contextVariable, Character character, Parser parser) throws QueryParsingException {
					if (EntityAttributeReferenceVariable.class.isInstance(contextVariable)) {
						EntityAttributeReferenceVariable v = (EntityAttributeReferenceVariable) contextVariable;
						final OrderingImpl orderingImpl = new OrderingImpl();
						orderingImpl.setField(v.getName());
						orderingImpl.setAscending(true);
						parser.push(new ConsumeWhiteSpacesState() {

							@Override
							protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
								parser.pop();
								if (ReservedKeywords.ORDER_DIRECTION_ASCENDING.charAt(0) == character) {
									parser.push(createParseDirectionAscendingState(orderingImpl)).handleChar(character, parser);
								} else if (ReservedKeywords.ORDER_DIRECTION_DESCENDING.charAt(0) == character) {
									parser.push(createParseDirectionDescendingState(orderingImpl)).handleChar(character, parser);
								} else {
									orderingImpl.setAscending(true);
									parser.peek().handleChar(character, parser);
								}
							}

						});
						pickImpl.setOrdering(orderingImpl);
					} else {
						throw new QueryParsingException("ORDERBY only supports EntityAttributeReferenceVariable");
					}
				}
			});
		} else if (kw == null) {
			// no-op
			return;
		} else {
			throw new QueryParsingException("unsupported keyword " + kw);
		}
		doContinue(character, parser);
	}
	
	private AcceptStringState createParseDirectionDescendingState(final OrderingImpl orderingImpl) {
		return new AcceptStringState(ReservedKeywords.ORDER_DIRECTION_DESCENDING) {

			@Override
			protected void accepted(Parser parser) throws QueryParsingException {
				orderingImpl.setAscending(false);
				parser.pop();
			}

			@Override
			protected void notAccepted(Character character, Parser parser) throws QueryParsingException {
				throw new QueryParsingException("unsupported ordering direction");
			}
		};
	}

	private AcceptStringState createParseDirectionAscendingState(final OrderingImpl orderingImpl) {
		return new AcceptStringState(ReservedKeywords.ORDER_DIRECTION_ASCENDING) {

			@Override
			protected void accepted(Parser parser) throws QueryParsingException {
				orderingImpl.setAscending(true);
				parser.pop();
			}

			@Override
			protected void notAccepted(Character character, Parser parser) throws QueryParsingException {
				throw new QueryParsingException("unsupported ordering direction");
			}
		};
	}
	
	private void onParsedAlias(String alias, Character character, Parser parser) throws QueryParsingException {
		pickImpl.setAttributeHolderNameAlias(alias);
		parser.pop();
		parser.push(new ConsumeWhiteSpacesState() {

			@Override
			protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
				parser.pop();
				parser.push(new TokenState() {

					@Override
					protected void onTokenComplete(Character character, Parser parser) throws QueryParsingException {
						String tkn = getToken();
						// has to be a reserved key word
						onParsedKeyword(tkn, character, parser);
						sb = null;
					}
				});
				doContinue(character, parser);
			}
		});
		doContinue(character, parser);
	}
	
}
