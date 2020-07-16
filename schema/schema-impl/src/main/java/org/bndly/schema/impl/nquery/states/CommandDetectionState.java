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
import org.bndly.schema.impl.nquery.PickImpl;
import org.bndly.schema.api.nquery.Query;
import org.bndly.schema.api.nquery.QueryParsingException;
import static org.bndly.schema.api.nquery.ReservedKeywords.CMD_COUNT;
import static org.bndly.schema.api.nquery.ReservedKeywords.CMD_PICK;
import org.bndly.schema.impl.nquery.CountImpl;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CommandDetectionState extends TokenState {
	
	private Query query;
	
	@Override
	protected void onTokenComplete(Character character, Parser parser) throws QueryParsingException {
		parser.pop();
		String kw = getToken();
		if (CMD_PICK.equalsIgnoreCase(kw)) {
			PickImpl pickImpl = new PickImpl();
			query = pickImpl;
			parser.push(new PickState(pickImpl));
		} else if (CMD_COUNT.equals(kw)) {
			CountImpl countImpl = new CountImpl();
			query = countImpl;
			parser.push(new CountState(countImpl));
		} else {
			throw new QueryParsingException("unsupported command keyword: " + kw);
		}
		// delegate to the according command parser
		doContinue(character, parser);
	}

	public Query getQuery() {
		return query;
	}
}
