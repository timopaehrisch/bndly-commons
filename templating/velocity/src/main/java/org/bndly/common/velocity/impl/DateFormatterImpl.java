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

import org.bndly.common.velocity.api.DateFormatter;
import org.bndly.common.velocity.api.VelocityTemplate;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatterImpl implements DateFormatter {

	private final String formatString;
	private final SimpleDateFormat df;

	public DateFormatterImpl(VelocityTemplate template) {
		if (template.getDateFormatString() == null) {
			this.formatString = "dd.MM.yyyy";
		} else {
			this.formatString = template.getDateFormatString();
		}
		if (template.getLocale() != null) {
			df = new SimpleDateFormat(formatString, template.getLocale());
		} else {
			df = new SimpleDateFormat(formatString);
		}
	}

	@Override
	public String format(Date d) {
		if (d == null) {
			return "";
		}
		return df.format(d);
	}

	@Override
	public String format(long timeStamp) {
		return df.format(new Date(timeStamp));
	}

	@Override
	public String today() {
		return format(new Date());
	}

}
