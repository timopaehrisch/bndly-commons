package org.bndly.shop.common.csv.model;

/*-
 * #%L
 * CSV
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

import org.bndly.shop.common.csv.CSVConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ValueImpl implements Value {

	private final String raw;
	private final Row row;
	private final long index;
	private final boolean requiresQuotes;
	
	private static class Matcher {
		private final String expected;
		private boolean didMatch;
		private boolean didNotMatch;

		public Matcher(String expected) {
			this.expected = expected;
		}

		private int pos = 0;
		public final void match(char c) {
			if (pos >= expected.length()) {
				return;
			}
			if (expected.charAt(pos) == c) {
				pos++;
				if (pos == expected.length()) {
					didMatch = true;
				}
			} else {
				didNotMatch = true;
			}
		}
		
		protected boolean didMatch() {
			return didMatch;
		}
		protected boolean didNotMatch() {
			return didNotMatch;
		}
	}
	
	public static boolean requiresQuotes(CSVConfig config, String rawValueData) {
		String[] watchOut = new String[]{config.getQuote(), config.getNewLine(), config.getSeparator()};
		final List<Matcher> matchers = new ArrayList<>();
		for (int i = 0; i < rawValueData.length(); i++) {
			char c = rawValueData.charAt(i);
			for (String wo : watchOut) {
				if (wo.length() > 0) {
					if (wo.charAt(0) == c) {
						matchers.add(new Matcher(wo));
					}
				}
			}
			Iterator<Matcher> iterator = matchers.iterator();
			while (iterator.hasNext()) {
				Matcher next = iterator.next();
				next.match(c);
				if (next.didMatch()) {
					return true;
				} else if (next.didNotMatch()) {
					iterator.remove();
				}
			}
		}
		return false;
	}

	public ValueImpl(String raw, Row row, long index, boolean requiresQuotes) {
		if (raw == null) {
			throw new IllegalArgumentException("raw of a csv value has to be at least the empty string");
		}
		this.raw = raw;
		this.row = row;
		this.index = index;
		this.requiresQuotes = requiresQuotes;
	}

	@Override
	public boolean requiresQuotes() {
		return requiresQuotes;
	}

	@Override
	public String getRaw() {
		return raw;
	}

	@Override
	public Row getRow() {
		return row;
	}

	@Override
	public long getIndex() {
		return index;
	}
}
