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
public abstract class TokenState extends AbstractParsingState {
	protected StringBuffer sb = null;
	
	@Override
	public void handleChar(char character, Parser parser) throws QueryParsingException {
		if (!isStopChar(character)) {
			if (sb == null) {
				sb = new StringBuffer();
			}
			sb.append(character);
		} else {
			if (sb != null) {
				onTokenComplete(character, parser);
			}
			sb = null;
		}
	}

	protected boolean isStopChar(char character) {
		return Character.isWhitespace(character);
	}
	
	@Override
	public final void onEnd(Parser parser) throws QueryParsingException {
		onTokenComplete(null, parser);
	}

	protected final String getToken() {
		return sb == null ? null : sb.toString();
	}
	
	protected abstract void onTokenComplete(Character character, Parser parser) throws QueryParsingException;
	
}
