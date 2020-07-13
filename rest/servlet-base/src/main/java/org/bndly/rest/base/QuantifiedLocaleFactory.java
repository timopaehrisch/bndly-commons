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

import java.util.Locale;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QuantifiedLocaleFactory implements QuantifiedHeaderParser.QuantifiedItemFactory<QuantifiedLocale> {

	@Override
	public QuantifiedLocale createQuantifiedSomething(Float quantity, String data) {
		final Locale locale = parseLanguageTag(data);
		if (locale == null) {
			return null;
		}
		final float quan = quantity == null ? 1 : quantity;
		return new QuantifiedLocale() {

			@Override
			public Locale getLocale() {
				return locale;
			}

			@Override
			public float getQ() {
				return quan;
			}
		};
	}

	public static Locale parseLanguageTag(String languageTag) {
		int indexOfSplit = languageTag.indexOf("-");
		String language;
		String country;
		if (indexOfSplit > -1) {
			language = languageTag.substring(0, indexOfSplit);
			country = languageTag.substring(indexOfSplit + 1);
		} else {
			language = languageTag;
			country = null;
		}
		Locale l;
		if (country == null) {
			l = new Locale(language);
		} else {
			l = new Locale(language, country);
		}
		return l;
	}

}
