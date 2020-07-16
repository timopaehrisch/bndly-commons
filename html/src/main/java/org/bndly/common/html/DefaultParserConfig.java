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

import java.util.HashSet;
import java.util.Set;

/**
 * The default parser config is a basic static implementation of a HTML parser
 * configuration. Only &lt;br&gt; tags will be detected as self closing tags.
 * White spaces will be detected by
 * {@link java.lang.Character#isWhitespace(char) isWhitespace}.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultParserConfig implements ParserConfig {

	private static final Set<String> SELF_CLOSING_TAGS;
	public static final String ALLOWED_ATTRIBUTE_NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

	static {
		SELF_CLOSING_TAGS = new HashSet<>();
		SELF_CLOSING_TAGS.add("br");
		SELF_CLOSING_TAGS.add("img");
		SELF_CLOSING_TAGS.add("hr");
	}

	@Override
	public boolean isWhiteSpace(char c) {
		return Character.isWhitespace(c);
	}

	@Override
	public boolean isSelfClosingTag(String tagName) {
		return SELF_CLOSING_TAGS.contains(tagName);
	}

	@Override
	public boolean isAllowedAttributeNameCharacter(char c) {
		return ALLOWED_ATTRIBUTE_NAME_CHARS.indexOf(c) >= 0;
	}

	@Override
	public boolean isAutomaticLowerCaseEnabled(String tagName) {
		return false;
	}
	
	@Override
	public boolean isCharacterWithRequiredHtmlEntity(char c) {
		return isCharacterWithRequiredHtmlEntityInAttribute(c);
	}

	@Override
	public boolean isCharacterWithRequiredHtmlEntityInAttribute(char c) {
		return '>' == c || '<' == c || '&' == c || '"' == c || '\'' == c;
	}

	@Override
	public boolean isCharacterWithRequiredHtmlEntityInText(char c) {
		return '>' == c || '<' == c || '&' == c;
	}

	@Override
	public boolean isUnquotedAttributeValueTolerated() {
		return false;
	}

	@Override
	public boolean isUnbalancedTagTolerated() {
		return false;
	}

	@Override
	public boolean isIncompleteEntityTolerated() {
		return false;
	}

	@Override
	public boolean isCommentParsingEnabled() {
		return false;
	}
	

}
