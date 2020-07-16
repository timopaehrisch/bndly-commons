package org.bndly.common.ical.impl.base;

/*-
 * #%L
 * iCal Impl
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

import org.bndly.common.ical.api.exceptions.CalendarException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Date;

/**
 * Created by alexp on 13.05.15.
 */
public final class Util {

	private Util() {

	}

	public static String removeLineFeeds(String s) {
		if (s == null) {
			return null;
		}

		return s.replaceAll(System.lineSeparator(), " ");
	}

	public static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}

	public static boolean isNullOrEmpty(Date d) {
		return d == null || d.getTime() == 0L;
	}

	public static String escapeLineFeeds(String s) {
		if (s == null) {
			return null;
		}

		return s.replace(System.lineSeparator(), "\\n");
	}

	public static String escapeChars(String s, String escapingChars) {
		if (s == null) {
			return null;
		}

		if (System.lineSeparator().equalsIgnoreCase(escapingChars) || "\n".equalsIgnoreCase(escapingChars)) {
			return escapeLineFeeds(s);
		}

		return s.replace(escapingChars, "\\".concat(escapingChars));
	}

	public static boolean isValidEmailAddress(String email) throws AddressException {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

	public static <E> E assertNotNull(E input, String message) {
		if (input == null) {
			throw new CalendarException(message);
		}
		return input;
	}
}
