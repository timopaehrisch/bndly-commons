package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.rest.api.ContentType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ContentTypeParser {

	private static final String REGEX = "([a-zA-Z0-9\\-]*)\\/([a-zA-Z0-9\\-]*)(;\\ *(charset|CHARSET)\\=([a-zA-Z0-9\\-]*))?";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	private ContentTypeParser() {
	}

	public static ContentType getContentTypeFromString(String inputString) {
		Matcher matcher = PATTERN.matcher(inputString);
		if (!matcher.matches()) {
			return null;
		}
		final String prefix = matcher.group(1);
		final String suffix = matcher.group(2);
		final String name = prefix + "/" + suffix;
		return new ContentType() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getExtension() {
				return null;
			}
		};
	}

	public static String getCharsetFromContentTypeString(String inputString) {
		Matcher matcher = PATTERN.matcher(inputString);
		if (!matcher.matches()) {
			return null;
		}
		String charset = matcher.group(5);
		return charset;
	}
}
