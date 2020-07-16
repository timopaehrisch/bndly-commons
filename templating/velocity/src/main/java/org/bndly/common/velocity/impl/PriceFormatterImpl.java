package org.bndly.common.velocity.impl;

/*-
 * #%L
 * Velocity
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

import org.bndly.common.velocity.api.PriceFormatter;
import org.bndly.common.velocity.api.VelocityTemplate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;

public class PriceFormatterImpl implements PriceFormatter {

	private final VelocityTemplate template;

	public PriceFormatterImpl(VelocityTemplate template) {
		this.template = template;
	}

	@Override
	public String formatDecimal(BigDecimal d) {
		if (d == null) {
			return "";
		}

		Locale locale = Locale.getDefault();
		if (template.getLocale() != null) {
			locale = template.getLocale();
		}
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
		df.applyPattern("####,##0.00");

		String formatedPrice = df.format(d.doubleValue());
		return formatedPrice;
	}

	@Override
	public String formatDecimal(BigDecimal d, String currencySymbol) {
		if (d == null) {
			return "";
		}

		String format = formatDecimal(d);
		format += " " + currencySymbol;
		return format;
	}

	@Override
	public String formatTaxRate(BigDecimal d) {
		return (formatDecimal(d) + " %");
	}

}
