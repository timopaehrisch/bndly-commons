package org.bndly.common.ical.impl;

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

import org.bndly.common.ical.api.ICSEventBuilder;
import org.bndly.common.ical.api.base.Duration;
import org.bndly.common.ical.api.base.Event;
import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.impl.base.DurationImpl;
import org.bndly.common.ical.impl.base.EventImpl;
import org.bndly.common.ical.impl.base.Geo;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.Util;


import java.util.Date;


/**
 * Created by alexp on 08.05.15.
 */
public class ICSEventBuilderImpl extends ICSComponentBuilderImpl<ICSEventBuilder> implements ICSEventBuilder {

	// Prioritizable
	private int prio = -1;

	// Locatable
	private Geo geo = null;
	private String location = null;

	// Describable
	private String description;

	// Durable
	private Duration duration = null;

	// VEvent
	private Date end = null;
	private String transparency = null;

	public ICSEventBuilderImpl(ICSBuilderImpl masterBuilder) {
		super(masterBuilder);
	}

	@Override
	public ICSEventBuilder start(Date start) {
		this.start = Util.assertNotNull(start, "You cannot set an empty 'start date'!");
		return this;
	}

	@Override
	public ICSEventBuilder end(Date end) {
		if (duration != null) {
			throw new CalendarException(
					"You cannot generate an event with an 'end date' and a 'duration'. "
					+ "Either you set an 'end date' or a 'duration'"
			);
		} else {
			this.end = Util.assertNotNull(end, "You can not set a null 'end date'!");
		}

		return this;
	}

	@Override
	public ICSEventBuilder description(String description) {
		description = Util.escapeLineFeeds(description);
		this.description = Util.escapeChars(description, ",");
		return this;
	}

	@Override
	public ICSEventBuilder priority(int prio) {
		if (prio < 0 || prio > 9) {
			throw new CalendarException("The 'priority' must be in range of [0..9]");
		}

		this.prio = prio;
		return this;
	}

	@Override
	public ICSEventBuilder duration(int weeks, int days, int hours, int minutes, int seconds) {
		if (end != null) {
			throw new CalendarException(
					"You cannot generate an event with an 'end date' and a 'duration'. "
					+ "Either you set an 'end date' or a 'duration'.");
		}

		this.duration = new DurationImpl(weeks, days, hours, minutes, seconds);
		return this;
	}

	@Override
	public ICSEventBuilder geo(double latitude, double longitude) {
		this.geo = new Geo(latitude, longitude);
		return this;
	}

	@Override
	public ICSEventBuilder location(String location) {
		location = Util.escapeLineFeeds(location);
		this.location = Util.escapeChars(location, ",");

		return this;
	}

	@Override
	public ICSEventBuilder tentativeStatus() {
		setStatus(ICSConstants.STATUS_EVENT_TENTATIVE);
		return this;
	}

	@Override
	public ICSEventBuilder confirmedStatus() {
		setStatus(ICSConstants.STATUS_EVENT_CONFIRMED);
		return this;
	}

	@Override
	public ICSEventBuilder opaque() {
		this.transparency = ICSConstants.TRANSPARENCY_OPAQUE;
		return this;
	}

	@Override
	public ICSEventBuilder transparent() {
		this.transparency = ICSConstants.TRANSPARENCY_TRANSPARENT;
		return this;
	}

	//-- Own methods
	public Event buildEvent() {
		validateMandatoryFields();

		validateOrganizerFields();

		return new EventImpl(
				start,
				classification,
				created,
				sequence,
				status,
				summary,
				url,
				lastModified,
				repeatableRule,
				orgCommonName,
				orgDIR,
				orgMailAddress,
				orgSentBy,
				description,
				prio,
				end,
				duration,
				geo,
				location,
				transparency
		);
	}

	@Override
	public boolean validateMandatoryFields() throws IllegalArgumentException, CalendarException {
		if (Util.isNullOrEmpty(this.start)) {
			throw new IllegalArgumentException("Field 'DTSTART' should be not null or empty.");
		} else if (this.end != null && this.start.compareTo(this.end) == 1) {
			throw new CalendarException("You cannot set the 'end date' time to a time before the event starts!");
		}

		return true;
	}
}
