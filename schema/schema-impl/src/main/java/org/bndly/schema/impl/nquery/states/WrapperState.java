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
import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class WrapperState extends ConsumeWhiteSpacesState {
	private boolean didOpen;
	private boolean didClose;

	@Override
	protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
		if (ReservedKeywords.GROUP_START.equals(Character.toString(character))) {
			if (!didOpen) {
				didOpen = true;
			} else {
				onNestedWrapperOpened(character, parser);
			}
		} else if (ReservedKeywords.GROUP_END.equals(Character.toString(character))) {
			if (didOpen && !didClose) {
				didClose = true;
				onWrapperClosed(character, parser);
			} else {
				throw new QueryParsingException("wrong wrapper was closed");
			}
		} else {
			if (!didOpen || didClose) {
				throw new QueryParsingException("wrapper state found illegal character before/after wrapper was opened/closed");
			}
			onWrappedCharacter(character, parser);
		}
	}

	protected abstract void onWrappedCharacter(char character, Parser parser) throws QueryParsingException;
	protected abstract void onNestedWrapperOpened(char character, Parser parser) throws QueryParsingException;
	protected abstract void onWrapperClosed(char character, Parser parser) throws QueryParsingException;
	
}
