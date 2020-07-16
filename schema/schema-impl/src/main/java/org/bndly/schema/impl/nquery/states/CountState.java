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
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.impl.nquery.CountImpl;
import org.bndly.schema.impl.nquery.IfClauseImpl;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CountState extends ConsumeWhiteSpacesState {
	private final CountImpl countImpl;

	public CountState(CountImpl countImpl) {
		this.countImpl = countImpl;
	}
	
	@Override
	protected void onNonWhiteSpace(char character, Parser parser) throws QueryParsingException {
		parser.pop();
		parser.push(new TokenState() {

			@Override
			protected void onTokenComplete(Character character, Parser parser) throws QueryParsingException {
				countImpl.setAttributeHolderName(getToken());
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
		if (countImpl.getAttributeHolderName() == null) {
			throw new QueryParsingException("parsing a PICK failed because not attribute holder was provided");
		}
	}
	
	private void onParsedKeyword(String kw, Character character, Parser parser) throws QueryParsingException {
		if (ReservedKeywords.IF_CLAUSE.equals(kw)) {
			IfClauseImpl ifClauseImpl = new IfClauseImpl();
			countImpl.setIfClause(ifClauseImpl);
			parser.push(new IfClauseState(ifClauseImpl));
		} else if (kw == null) {
			// no-op
			return;
		} else {
			throw new QueryParsingException("unsupported keyword " + kw);
		}
		doContinue(character, parser);
	}
	
	private void onParsedAlias(String alias, Character character, Parser parser) throws QueryParsingException {
		countImpl.setAttributeHolderNameAlias(alias);
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
