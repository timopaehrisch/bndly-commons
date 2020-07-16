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

import org.bndly.common.ical.api.ICSToDoBuilder;
import org.bndly.common.ical.api.base.Duration;
import org.bndly.common.ical.api.base.ToDo;
import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.impl.base.DurationImpl;
import org.bndly.common.ical.impl.base.Geo;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.ToDoImpl;
import org.bndly.common.ical.impl.base.Util;

import java.util.Date;

/**
 * Created by alexp on 21.05.15.
 */
public class ICSToDoBuilderImpl extends ICSComponentBuilderImpl<ICSToDoBuilder> implements ICSToDoBuilder {

	// Prioritizable
	private int prio = -1;

	// Locatable
	private Geo geoData = null;
	private String location = null;

	// Describable
	private String description = null;

	// Durable
	private Duration duration = null;

	// VToDo
	private Date due = null;
	private Date completed = null;
	private int percentCompleted = 0;

	public ICSToDoBuilderImpl(ICSBuilderImpl masterBuilder) {
		super(masterBuilder);
	}

	@Override
	public boolean validateMandatoryFields() throws IllegalArgumentException, CalendarException {
		if (this.start.compareTo(this.due) == 1) {
			throw new CalendarException("You cannot set the 'start date' time later as the todo 'due date'!");
		} else if (this.start.compareTo(this.completed) == 1) {
			throw new CalendarException("You cannot set the 'completed date' time before the todo starts!");
		}

		return false;
	}

	@Override
	public ICSToDoBuilder start(Date start) {
		this.start = Util.assertNotNull(start, "'start date' is not allowed to be null");
		return this;
	}

	@Override
	public ICSToDoBuilder needsActionStatus() {
		setStatus(ICSConstants.STATUS_TODO_NEEDS_ACTION);
		return this;
	}

	@Override
	public ICSToDoBuilder completedStatus() {
		setStatus(ICSConstants.STATUS_TODO_COMPLETED);
		return this;
	}

	@Override
	public ICSToDoBuilder inProcessStatus() {
		setStatus(ICSConstants.STATUS_TODO_IN_PROCESS);
		return this;
	}

	@Override
	public ICSToDoBuilder due(Date due) {
		if (duration != null) {
			throw new CalendarException(
					"You cannot generate a todo with an 'due date' and a 'duration'. "
					+ "Either you set a 'due date' or a 'duration'.");
		} else {
			this.due = Util.assertNotNull(due, "You can not set a null 'due date'!");
		}

		return this;
	}

	@Override
	public ICSToDoBuilder completed(Date completed) {
		this.completed = Util.assertNotNull(completed, "You cannot set the 'completed date' time before the todo starts!");

		return this;
	}

	@Override
	public ICSToDoBuilder percentComplete(int percent) {
		if (percent < 0 || percent > 100) {
			throw new CalendarException("The 'percent' must be in range of [0..100]");
		}

		return this;
	}

	@Override
	public ICSToDoBuilder description(String description) {
		description = Util.escapeLineFeeds(description);
		this.description = Util.escapeChars(description, ",");
		return this;
	}

	@Override
	public ICSToDoBuilder duration(int weeks, int days, int hours, int minutes, int seconds) {
		if (due != null) {
			throw new CalendarException(
					"You cannot generate a todo with an 'due date' and a 'duration'. "
					+ "Either you set an 'due date' or a 'duration'.");
		}

		this.duration = new DurationImpl(weeks, days, hours, minutes, seconds);
		return this;
	}

	@Override
	public ICSToDoBuilder geo(double latitude, double longitude) {
		this.geoData = new Geo(latitude, longitude);
		return this;
	}

	@Override
	public ICSToDoBuilder location(String location) {
		location = Util.escapeLineFeeds(location);
		this.location = Util.escapeChars(location, ",");

		return this;
	}

	@Override
	public ICSToDoBuilder priority(int prio) {
		if (prio < 0 || prio > 9) {
			throw new CalendarException("The 'priority' must be in range of [0..9]");
		}

		this.prio = prio;
		return this;
	}

	public ToDo buildToDo() {
		validateMandatoryFields();

		validateOrganizerFields();

		return new ToDoImpl(
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
				duration,
				geoData,
				location,
				due,
				completed,
				percentCompleted
		);
	}
}
