package org.bndly.code.common;

/*-
 * #%L
 * Code Common
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
import java.util.List;

public final class StringUtil {
	private StringUtil() {
	}
	
	public static List<String> extractPathElements(String path) {
		List<String> elements = new ArrayList<>();
		int li = -1;
		int i = path.indexOf(".");
		if (i > -1) {
			while (i > -1) {
				elements.add(path.substring(li + 1, i));
				li = i;
				i = path.indexOf(".", i + 1);
			}
			if (li + 1 < path.length() - 1) {
				elements.add(path.substring(li + 1));
			}
		} else {
			elements.add(path);
		}
		return elements;
	}
	
	public static List<String> extractFragments(String pathExpression) {
		List<String> fragments = new ArrayList<>();

		int s = pathExpression.indexOf("{");
		int e = pathExpression.indexOf("}");
		if (s > -1 && e > -1 && s < e) {
			while (s > -1 && e > -1 && s < e) {
				if (s > 0 && fragments.isEmpty()) {
					fragments.add(pathExpression.substring(0, s));
				}
				String element = pathExpression.substring(s, e + 1);
				fragments.add(element);

				int ns = pathExpression.indexOf("{", e);
				int ne = pathExpression.indexOf("}", e + 1);
				if (ne == -1 || ns == -1) {
					if (e < pathExpression.length() - 1) {
						fragments.add(pathExpression.substring(e + 1));
					}
				} else {
					if (ns - (e + 1) > 0) {
						fragments.add(pathExpression.substring(e + 1, ns));
					}
				}

				s = ns;
				e = ne;
			}
		} else {
			fragments.add(pathExpression);
		}
		return fragments;
	}
}
