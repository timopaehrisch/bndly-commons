package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import java.io.Serializable;
import java.util.Date;

public class PointInTime implements Serializable, Comparable<PointInTime> {

	public static Date date(PointInTime pit) {
		if (pit != null && pit.timeStamp != null) {
			return new Date(pit.timeStamp);
		} else {
			return null;
		}
	}

	public static PointInTime pointInTime(Date d) {
		if (d != null) {
			return new PointInTime(d);
		} else {
			return null;
		}
	}

	private Long timeStamp;

	public PointInTime() {
		this(new Date());
	}

	public PointInTime(long l) {
		timeStamp = l;
	}

	public PointInTime(Date d) {
		if (d != null) {
			timeStamp = d.getTime();
		}
	}

	/**
	 * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT see also java.util.Date.getTime()
	 */
	public Long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public int compareTo(PointInTime t) {
		if (t == null || t.timeStamp == null) {
			return 1;
		} else if (timeStamp == null) {
			return -1;
		} else {
			return (int) (timeStamp - t.timeStamp);
		}
	}

}
