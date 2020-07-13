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

import org.bndly.rest.api.QuantifiedSomething;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class QuantifiedHeaderParser {

	public static interface QuantifiedItemFactory<E extends QuantifiedSomething> {

		E createQuantifiedSomething(Float quantity, String data);
	}

	private QuantifiedHeaderParser() {
	}

	public static <E extends QuantifiedSomething> List<E> parseQuantifiedHeader(String header, QuantifiedItemFactory<E> quantifiedItemFactory) {
		boolean inQ = false;
		List<E> quantifiedEntries = null;
		Float quantity = null;
		List<String> names = null;
		StringBuffer sb = null;
		for (int index = 0; index < header.length(); index++) {
			char character = header.charAt(index);
			if (inQ) {
				if (',' == character) {
					quantifiedEntries = finishParsing(quantifiedEntries, names, sb, quantifiedItemFactory);
					sb = null;
					inQ = false;
				} else {
					if (sb == null) {
						sb = new StringBuffer();
					}
					sb.append(character);
				}
			} else {
				if (',' == character) {
					if (sb != null) {
						if (names == null) {
							names = new ArrayList<>();
						}
						names.add(sb.toString());
						sb = null;
					}
				} else if (';' == character) {
					inQ = true;
					if (sb != null) {
						if (names == null) {
							names = new ArrayList<>();
						}
						names.add(sb.toString());
						sb = null;
					}
				} else {
					if (sb == null) {
						sb = new StringBuffer();
					}
					sb.append(character);
				}
			}
		}
		if (!inQ) {
			if (sb != null) {
				if (names == null) {
					names = new ArrayList<>();
				}
				names.add(sb.toString());
			}
		}
		quantifiedEntries = finishParsing(quantifiedEntries, names, sb, quantifiedItemFactory);
		if (quantifiedEntries == null) {
			quantifiedEntries = Collections.EMPTY_LIST;
		}
		return quantifiedEntries;
	}

	private static <E extends QuantifiedSomething> List<E> finishParsing(List<E> quantifiedContentTypes, List<String> names, StringBuffer sb, QuantifiedItemFactory<E> quantifiedItemFactory) {
		Float quantity = finishQParsing(sb);
		if (names != null) {
			for (String name : names) {
				if (quantifiedContentTypes == null) {
					quantifiedContentTypes = new ArrayList<>();
				}
				E thing = quantifiedItemFactory.createQuantifiedSomething(quantity, name);
				if (thing != null) {
					quantifiedContentTypes.add(thing);
				}
			}
			names.clear();
		}
		return quantifiedContentTypes;
	}

	private static Float finishQParsing(StringBuffer sb) {
		Float quantity = null;
		if (sb != null) {
			String quantityString = sb.toString();
			if (quantityString.startsWith("q=")) {
				quantityString = quantityString.substring(2);
				try {
					quantity = Float.valueOf(quantityString);
				} catch (NumberFormatException e) {
					// header is broken
					quantity = null;
				}
			}
		}
		return quantity;
	}
}
