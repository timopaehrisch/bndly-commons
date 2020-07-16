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

import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ReservedKeywordState extends TokenState {
	
	@Override
	protected void onTokenComplete(Character character, Parser parser) throws QueryParsingException {
		String kw = getToken();
		if (parser.getReservedKeywords().isReservedKeyword(kw)) {
			onKeyword(kw, character, parser);
		} else {
			onAlias(kw, character, parser);
		}
	}

	protected abstract void onAlias(String kw, Character character, Parser parser) throws QueryParsingException;

	protected abstract void onKeyword(String kw, Character character, Parser parser) throws QueryParsingException;

}
