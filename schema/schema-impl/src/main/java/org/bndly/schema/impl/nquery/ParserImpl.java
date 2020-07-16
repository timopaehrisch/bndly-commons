package org.bndly.schema.impl.nquery;

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
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.Query;
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.ParsingState;
import org.bndly.schema.impl.nquery.states.CommandDetectionState;
import org.bndly.schema.impl.nquery.states.ConsumeWhiteSpacesState;
import org.bndly.schema.api.nquery.ReservedKeywords;
import java.util.Stack;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ParserImpl implements ReservedKeywords, Parser {

	private final Stack<ParsingState> states = new Stack<>();
	private final StringBuffer whatHasBeenParsed = new StringBuffer();
	private final Object[] queryArgs;
	private final CommandDetectionState cmdDetection;
	private ExpressionStatementHandler expressionStatementHandler;
	private int queryArgsPos = 0;
	private int markerQueryArgsPos;

	public ParserImpl(Object... queryArgs) {
		this.queryArgs = queryArgs;
		cmdDetection = new CommandDetectionState();
		push(new ConsumeWhiteSpacesState() {

			@Override
			protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
				parser.pop();
				parser.push(cmdDetection).handleChar(character, parser);
			}
		});
	}

	@Override
	public final Object[] getQueryArgs() {
		return queryArgs;
	}

	@Override
	public final Object getQueryArg(int index) {
		return getQueryArgs()[index];
	}

	@Override
	public Object getNextQueryArg() {
		Object arg = getQueryArg(queryArgsPos);
		queryArgsPos++;
		return arg;
	}

	@Override
	public Parser resetQueryArgPosition() {
		queryArgsPos = markerQueryArgsPos;
		return this;
	}

	@Override
	public Parser markQueryArgPosition() {
		markerQueryArgsPos = queryArgsPos;
		return this;
	}

	@Override
	public Parser parse(String queryString) throws QueryParsingException {
		return parseInternal(queryString, true, true);
	}

	@Override
	public Parser reparse(String string) throws QueryParsingException {
		return parseInternal(string, false, false);
	}
	
	@Override
	public final Query getQuery() {
		return cmdDetection.getQuery();
	}
	
	public ParserImpl expressionStatementHandler(ExpressionStatementHandler expressionStatementHandler) {
		this.expressionStatementHandler = expressionStatementHandler;
		return this;
	}
	
	private Parser parseInternal(String queryString, boolean appendToWhatHasBeenRead, final boolean throwEnd) throws QueryParsingException {
		final Parser that = this;
		Parser p = that;
		if (appendToWhatHasBeenRead) {
			// wrap the current parser, because we don't want to record read characters multiple times.
			p = new Parser() {

				@Override
				public ParsingState pop() {
					return that.pop();
				}

				@Override
				public ParsingState push(ParsingState parsingState) {
					return that.push(parsingState);
				}

				@Override
				public ParsingState peek() {
					return that.peek();
				}

				@Override
				public ExpressionStatementHandler getExpressionStatementHandler() {
					return that.getExpressionStatementHandler();
				}

				@Override
				public ReservedKeywords getReservedKeywords() {
					return that.getReservedKeywords();
				}

				@Override
				public Object[] getQueryArgs() {
					return that.getQueryArgs();
				}

				@Override
				public Object getQueryArg(int index) {
					return that.getQueryArg(index);
				}

				@Override
				public Object getNextQueryArg() {
					return that.getNextQueryArg();
				}

				@Override
				public Parser resetQueryArgPosition() {
					that.resetQueryArgPosition();
					return this;
				}

				@Override
				public Parser markQueryArgPosition() {
					that.markQueryArgPosition();
					return this;
				}

				@Override
				public Parser parse(String string) throws QueryParsingException {
					parseInternal(string, false, true);
					return this;
				}

				@Override
				public Parser reparse(String string) throws QueryParsingException {
					parseInternal(string, false, false);
					return this;
				}

				@Override
				public Query getQuery() {
					return that.getQuery();
				}
			};
		}
		for (int i = 0; i < queryString.length(); i++) {
			char character = queryString.charAt(i);
			if (appendToWhatHasBeenRead) {
				whatHasBeenParsed.append(character);
			}
			ParsingState currentPeek = peek();
			currentPeek.handleChar(character, p);
		}
		if (throwEnd) {
			ParsingState currentPeek = peek();
			currentPeek.onEnd(p);
		}
		return this;
	}

	@Override
	public final ParsingState peek() {
		if (states.isEmpty()) {
			return null;
		}
		return states.peek();
	}

	@Override
	public final ParsingState push(ParsingState parsingState) {
		if (parsingState != null) {
			states.push(parsingState);
		}
		return parsingState;
	}

	@Override
	public final ParsingState pop() {
		if (states.isEmpty()) {
			return null;
		}
		return states.pop();
	}

	@Override
	public ReservedKeywords getReservedKeywords() {
		return this;
	}

	@Override
	public ExpressionStatementHandler getExpressionStatementHandler() {
		return expressionStatementHandler;
	}

	@Override
	public boolean isReservedKeyword(String keyword) {
		return 
				CMD_PICK.equals(keyword)
				|| CMD_COUNT.equals(keyword)
				|| PARAM_WILDCARD.equals(keyword)
				|| NEGATION.equals(keyword)
				|| EQUAL.equals(keyword)
				|| GREATER.equals(keyword)
				|| GREATER_EQUAL.equals(keyword)
				|| LOWER.equals(keyword)
				|| LOWER_EQUAL.equals(keyword)
				|| INRANGE.equals(keyword)
				|| TYPED.equals(keyword)
				|| IF_CLAUSE.equals(keyword)
				|| BOOLEAN_OPERATOR_AND.equals(keyword)
				|| BOOLEAN_OPERATOR_OR.equals(keyword)
				|| GROUP_START.equals(keyword)
				|| GROUP_END.equals(keyword)
				|| ORDERBY.equals(keyword)
				|| LIMIT.equals(keyword)
				|| OFFSET.equals(keyword)
		;
	}
	
}
