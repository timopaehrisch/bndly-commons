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
import org.bndly.schema.api.nquery.ParsingState;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.ContextVariable;
import org.bndly.schema.impl.nquery.EntityAttributeReferenceVariable;
import org.bndly.schema.impl.nquery.QueryArgumentContextVariable;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ReadContextVariableState implements ParsingState {

	private static final String ENTITY_ATTRIBUTE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.";
	
	private StringBuffer sb = null;
	private String raw;

	@Override
	public void handleChar(char character, Parser parser) throws QueryParsingException {
		if (Character.isWhitespace(character)) {
			if (sb == null) {
				return;
			} else {
				finish(parser, character);
				return;
			}
		}
		if (Character.toString(character).equals(ReservedKeywords.PARAM_WILDCARD)) {
			if (sb == null) {
				sb = new StringBuffer(ReservedKeywords.PARAM_WILDCARD);
				finish(parser, character);
				return;
			} else {
				parser.pop();
				throw new QueryParsingException("unsupported character in context variable");
			}
		} else {
			if (ENTITY_ATTRIBUTE_CHARS.indexOf(character) < 0) {
				if (sb == null) {
					parser.pop();
					throw new QueryParsingException("unsupported character in context variable");
				} else {
					finish(parser, character);
				}
			}
		}
		if (sb == null) {
			sb = new StringBuffer();
		}
		sb.append(character);
	}

	@Override
	public void onEnd(Parser parser) throws QueryParsingException {
		finish(parser, null);
	}
	
	private void finish(Parser parser, Character character) throws QueryParsingException {
		raw = sb == null ? null : sb.toString();
		parser.pop();
		ContextVariable contextVariable;
		if (ReservedKeywords.PARAM_WILDCARD.equals(raw)) {
			contextVariable = new QueryArgumentContextVariable(parser.getNextQueryArg());
		} else if (raw != null) {
			contextVariable = new EntityAttributeReferenceVariable(raw);
		} else {
			throw new QueryParsingException("could not create context variable");
		}
		onContextVariable(contextVariable, character, parser);
	}

	protected abstract void onContextVariable(ContextVariable contextVariable, Character character, Parser parser) throws QueryParsingException;

}
