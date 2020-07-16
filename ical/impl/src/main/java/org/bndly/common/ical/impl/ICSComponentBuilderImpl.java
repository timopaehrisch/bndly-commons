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
import org.bndly.common.ical.api.base.Calendar;
import org.bndly.common.ical.api.CalendarBuilder;
import org.bndly.common.ical.api.base.WeekDays;
import org.bndly.common.ical.api.base.FrequencyEnum;
import org.bndly.common.ical.api.ICSComponentBuilder;
import org.bndly.common.ical.api.base.Repeatable;
import org.bndly.common.ical.api.ICSJournalBuilder;
import org.bndly.common.ical.api.ICSEventBuilder;
import org.bndly.common.ical.api.base.Configuration;
import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.RecurrenceRule;
import org.bndly.common.ical.impl.base.Util;

import javax.mail.internet.AddressException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Created by alexp on 20.05.15.
 */
public abstract class ICSComponentBuilderImpl<T> implements ICSComponentBuilder<T> {

	final ICSBuilderImpl master;

	// CalendarComponent
	private Date stamp = null;
	private String uid = null;

	protected Date start = null;
	protected String classification = null;
	protected Date created = null;
	protected Date lastModified = null;
	protected int sequence;
	protected String status = null;
	protected String summary = null;
	protected URL url = null;

	// Organizable
	protected String orgCommonName = null;
	protected URI orgMailAddress = null;
	protected URI orgDIR = null;
	protected URI orgSentBy = null;
	protected String orgLanguage = null;

	protected Repeatable repeatableRule = null;

	public ICSComponentBuilderImpl(ICSBuilderImpl masterBuilder) {
		this.master = masterBuilder;
	}

	@Override
	public abstract T start(Date start);

	@Override
	public abstract boolean validateMandatoryFields() throws IllegalArgumentException, CalendarException;

	@Override
	public T publicClassification() {
		setClassification(ICSConstants.CLASS_PUBLIC);
		return (T) this;
	}

	@Override
	public T privateClassification() {
		setClassification(ICSConstants.CLASS_PRIVATE);
		return (T) this;
	}

	@Override
	public T confidentialClassification() {
		setClassification(ICSConstants.CLASS_CONFIDENTIAL);
		return (T) this;
	}

	private void setClassification(String classification) {
		this.classification = Util.removeLineFeeds(classification);
	}

	@Override
	public T cancelledStatus() {
		setStatus(ICSConstants.STATUS_CANCELLED);
		return (T) this;
	}

	@Override
	public T created(Date created) {
		this.created = created;
		return (T) this;
	}

	@Override
	public T sequence(int sequence) {
		if (sequence < 0) {
			throw new CalendarException("The sequence must be positive or 0.");
		}

		this.sequence = sequence;
		return (T) this;
	}

	protected void setStatus(String status) {
		this.status = status;
	}

	@Override
	public T summary(String summary) {
		this.summary = Util.removeLineFeeds(summary);
		return (T) this;
	}

	@Override
	public T organizerName(String name) {
		this.orgCommonName = name;
		return (T) this;
	}

	@Override
	public T url(URL url) {
		this.url = url;
		return (T) this;
	}

	@Override
	public T lastModified(Date lastModified) {
		this.lastModified = lastModified;
		return (T) this;
	}

	@Override
	public T organizerMailAddress(String mailAddress) throws CalendarException, URISyntaxException {
		try {
			if (Util.isValidEmailAddress(mailAddress)) {
				this.orgMailAddress = new URI("mailto", mailAddress, null);
			}
		} catch (AddressException e) {
			throw new CalendarException("No e-mail address given! " + e.getMessage(), e);
		} catch (URISyntaxException use) {
			throw new URISyntaxException("No e-mail address given!" + use.getMessage(), use.getReason());
		}

		return (T) this;
	}

	@Override
	public T organizerDIR(URI dir) {
		this.orgDIR = dir;
		return (T) this;
	}

	@Override
	public T organizerSentBy(String mailAddress) throws CalendarException, URISyntaxException {
		try {
			if (Util.isValidEmailAddress(mailAddress)) {
				this.orgSentBy = new URI("mailto", mailAddress, null);
			}
		} catch (AddressException e) {
			throw new CalendarException("No e-mail address given!" + e.getMessage(), e);
		} catch (URISyntaxException use) {
			throw new URISyntaxException("No e-mail address given!" + use.getMessage(), use.getReason());
		}

		return (T) this;
	}

	@Override
	public boolean validateOrganizerFields() throws IllegalArgumentException {
		if (orgMailAddress == null) {
			if (Util.isNullOrEmpty(orgCommonName) == false || orgSentBy != null || orgDIR != null) {
				throw new IllegalArgumentException("Organizer without any mail address is not allowed!");
			}
		}
		return true;
	}

	@Override
	public T repeat(
			FrequencyEnum frequency, int interval, int count, Date until, int[] bySeconds, int[] byMinutes, int[] byHours,
			List<WeekDays> byDays, int[] byMonthDays, int[] byYearDays, int[] byWeekNumbers, int[] byMonths, WeekDays weekStartDay) {

		this.repeatableRule = new RecurrenceRule(
				frequency,
				interval,
				count,
				until,
				bySeconds,
				byMinutes,
				byHours,
				byDays,
				byMonthDays,
				byYearDays,
				byWeekNumbers,
				byMonths,
				weekStartDay
		);

		return (T) this;
	}

	@Override
	public T repeat(
			FrequencyEnum frequency, int interval, Date until, int[] byMinutes, int[] byHours, List<WeekDays> byDays,
			int[] byMonthDays, int[] byYearDays, int[] byWeekNumbers, int[] byMonths) {

		this.repeatableRule = new RecurrenceRule(
				frequency,
				interval,
				0,
				until,
				null,
				byMinutes,
				byHours,
				byDays,
				byMonthDays,
				byYearDays,
				byWeekNumbers,
				byMonths,
				null
		);

		return (T) this;
	}

	@Override
	public T repeat(
			FrequencyEnum frequency, int interval, int count, int[] byMinutes, int[] byHours, List<WeekDays> byDays,
			int[] byMonthDays, int[] byYearDays, int[] byWeekNumbers, int[] byMonths) {

		this.repeatableRule = new RecurrenceRule(
				frequency,
				interval,
				count,
				null,
				null,
				byMinutes,
				byHours,
				byDays,
				byMonthDays,
				byYearDays,
				byWeekNumbers,
				byMonths,
				null
		);

		return (T) this;
	}

	//--- Delegation to the CalendarBuilder
	@Override
	public CalendarBuilder configure(Configuration config) {
		return master.configure(config);
	}

	@Override
	public ICSEventBuilder event() {
		return master.event();
	}

	@Override
	public ICSToDoBuilder todo() {
		return master.todo();
	}

	@Override
	public ICSJournalBuilder journal() {
		return master.journal();
	}

	@Override
	public Calendar build() {
		return master.build();
	}
}
