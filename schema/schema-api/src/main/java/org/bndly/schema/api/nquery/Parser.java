package org.bndly.schema.api.nquery;

/*-
 * #%L
 * Schema API
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Parser {
	ParsingState pop();
	ParsingState push(ParsingState parsingState);
	ParsingState peek();
	ExpressionStatementHandler getExpressionStatementHandler();
	ReservedKeywords getReservedKeywords();
	Object[] getQueryArgs();
	Object getQueryArg(int index);
	Object getNextQueryArg();
	Parser markQueryArgPosition();
	Parser resetQueryArgPosition();
	Query getQuery();
	Parser parse(String string) throws QueryParsingException;
	Parser reparse(String string) throws QueryParsingException;

}
