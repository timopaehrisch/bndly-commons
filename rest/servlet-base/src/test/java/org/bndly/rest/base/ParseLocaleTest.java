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

import static java.lang.Math.abs;
import java.util.List;
import java.util.Locale;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ParseLocaleTest {

	@Test
	public void parseEasyLocales() {
		Locale germanGermany = QuantifiedLocaleFactory.parseLanguageTag("de-DE");
		Assert.assertNotNull(germanGermany);
		Assert.assertEquals(germanGermany.getLanguage(), "de");
		Assert.assertEquals(germanGermany.getCountry(), "DE");
		
		Locale german = QuantifiedLocaleFactory.parseLanguageTag("de");
		Assert.assertNotNull(german);
		Assert.assertEquals(german.getLanguage(), "de");
		Assert.assertEquals(german.getCountry(), "");
	}
	
	
	@Test
	public void parseListOfEasyLocales() {
		String header = "en-US,en;q=0.9,it;q=0.7,es;q=0.5";
		List<QuantifiedLocale> listOfLocales = QuantifiedHeaderParser.parseQuantifiedHeader(header, new QuantifiedLocaleFactory());
		Assert.assertNotNull(listOfLocales);
		Assert.assertEquals(listOfLocales.size(),4);
		checkLocale(listOfLocales.get(0), "en", "US", 0.9f);
		checkLocale(listOfLocales.get(1), "en", "", 0.9f);
		checkLocale(listOfLocales.get(2), "it", "", 0.7f);
		checkLocale(listOfLocales.get(3), "es", "", 0.5f);
	}
	
	private void checkLocale(QuantifiedLocale locale, String language, String country, float quantity) {
		Assert.assertTrue(abs(locale.getQ() - quantity) < 0.001);
		Assert.assertEquals(locale.getLocale().getLanguage(), language);
		Assert.assertEquals(locale.getLocale().getCountry(), country);
	}
	
	@Test
	public void parseBadLocales() {
		Locale germanGermany = QuantifiedLocaleFactory.parseLanguageTag("de-DE-broken-beyond-belief");
		// should at least not throw an exception
	}

}
