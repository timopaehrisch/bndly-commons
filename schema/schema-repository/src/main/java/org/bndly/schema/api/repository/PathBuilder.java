package org.bndly.schema.api.repository;

/*-
 * #%L
 * Schema Repository
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class PathBuilder {

	public static final String ALLOWED_CHARS = "ABCEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.";
	private final List<String> elements = new ArrayList<>();

	private PathBuilder() {
	}
	
	public static PathBuilder newInstance() {
		return new PathBuilder();
	}
	
	public static PathBuilder newInstance(String path) {
		PathBuilder pathBuilder = new PathBuilder();
		StringBuffer sb = null;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if ('/' == c) {
				if (sb != null) {
					pathBuilder.element(sb.toString());
					sb = null;
				}
			} else {
				if (sb == null) {
					sb = new StringBuffer();
				}
				sb.append(c);
			}
		}
		if (sb != null) {
			pathBuilder.element(sb.toString());
		}
		return pathBuilder;
	}
	
	public static PathBuilder newInstance(Path path) {
		PathBuilder pathBuilder = new PathBuilder();
		for (String elementName : path.getElementNames()) {
			pathBuilder.element(elementName);
		}
		return pathBuilder;
	}

	public PathBuilder element(String element) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < element.length(); i++) {
			char c = element.charAt(i);
			if (!isAllowedChar(c)) {
				continue;
			}
			sb.append(c);
		}
		if (sb.length() > 0) {
			elements.add(sb.toString());
		}
		return this;
	}
	
	public static boolean isAllowedChar(char c) {
		if (
				(c >= 'A' && c <= 'Z')
				|| (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9')
				|| c == '-'
				|| c == '_'
				|| c == '.'
		) {
			return true;
		}
		return false;
	}
	
	public static String filterUnallowedIndentifierChars(String input) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (!isAllowedChar(c)) {
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public Path build() {
		final List<String> elementNames = Collections.unmodifiableList(elements);
		final String lastElement = elementNames.isEmpty() ? null : elementNames.get(elementNames.size() - 1);
		return new Path() {

			@Override
			public String getLastElement() {
				return lastElement;
			}

			@Override
			public List<String> getElementNames() {
				return elementNames;
			}

			@Override
			public String toString() {
				StringBuffer sb = new StringBuffer();
				for (String elementName : elementNames) {
					sb.append("/").append(elementName);
				}
				return elementNames.isEmpty() ? "/" : sb.toString();
			}
			
		};
	}
}
