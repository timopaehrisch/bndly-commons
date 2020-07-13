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

import org.bndly.rest.api.DataRange;
import org.bndly.rest.api.DataRange.Unit;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class RangeHeadersUtil {

	private RangeHeadersUtil() {
	}
	
	public static Long getServedContentLength(DataRange range) {
		if (range == null) {
			return null;
		}
		Long start = range.getStart();
		Long end = range.getEnd();
		Long total = range.getTotal();
		Long length = null;
		if (end != null && start != null) {
			length = end - start + 1; // end position is inclusive
		} else if (start != null && total != null) {
			length = total - start; // total length is exclusive
		} else if (start == null && end != null) {
			length = end + 1; // assume that start is 0
		} else if (start == null && end == null) {
			length = total; // assume everything is sent
		}
		return length;
	}
	
	public static String formatContentRangeHeader(DataRange range) {
		if (range == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		Unit unit = range.getUnit();
		if (unit != null) {
			sb.append(unit.toHeaderString()).append(" ");
		}
		Long start = range.getStart();
		if (start != null) {
			sb.append(start);
		}
		sb.append('-');
		Long end = range.getEnd();
		if (end != null) {
			sb.append(end);
		}
		Long total = range.getTotal();
		if (total != null) {
			sb.append('/').append(total);
		}
		String v = sb.toString();
		return v;
	}
	
	public static DataRange parseRangeHeader(String value) {
		if (value == null) {
			return null;
		}
		String remaining = value;

		Long tmpStart = null;
		Long tmpEnd = null;
		Long tmpTotal = null;
		Unit tmpUnit = null;
		int indexOfEqual = remaining.indexOf("=");
		if (indexOfEqual >= 0) {
			String unitString = remaining.substring(0, indexOfEqual).trim();
			remaining = remaining.substring(indexOfEqual + 1);
			tmpUnit = Unit.fromHeaderString(unitString);
		}
		int indexOfRangeSeparator = remaining.indexOf("-");
		if (indexOfRangeSeparator >= 0) {
			String startNumberString = remaining.substring(0, indexOfRangeSeparator).trim();
			remaining = remaining.substring(indexOfRangeSeparator + 1);
			try {
				tmpStart = Long.valueOf(startNumberString);
				tmpEnd = Long.valueOf(remaining.trim());
			} catch (NumberFormatException e) {
				// oops. broken header. we ignore this.
			}
		}
		
		final Long start = tmpStart;
		final Long end = tmpEnd;
		final Long total = tmpTotal;
		final Unit unit = tmpUnit;
		return new DataRange() {

			@Override
			public Long getStart() {
				return start;
			}

			@Override
			public Long getEnd() {
				return end;
			}

			@Override
			public Long getTotal() {
				return total;
			}

			@Override
			public DataRange.Unit getUnit() {
				return unit;
			}
		};
	}
	
}
