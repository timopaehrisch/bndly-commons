package org.bndly.common.html;

/*-
 * #%L
 * HTML
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
 * A parser config is used by {@link Parser} while parsing a HTML document. The
 * config allows the parser to detect self closing tags such as &lt;br&gt; or
 * whitespaces.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ParserConfig {

	boolean isWhiteSpace(char c);

	boolean isSelfClosingTag(String tagName);
	
	boolean isAutomaticLowerCaseEnabled(String tagName);
	
	boolean isAllowedAttributeNameCharacter(char c);
	
	boolean isCharacterWithRequiredHtmlEntity(char c);
	
	boolean isCharacterWithRequiredHtmlEntityInText(char c);
	
	boolean isCharacterWithRequiredHtmlEntityInAttribute(char c);
	
	boolean isUnquotedAttributeValueTolerated();
	
	boolean isUnbalancedTagTolerated();
	
	boolean isIncompleteEntityTolerated();
	
	boolean isCommentParsingEnabled();
}
