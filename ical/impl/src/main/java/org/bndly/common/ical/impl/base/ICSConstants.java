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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by alexp on 07.05.15.
 */
public final class ICSConstants {

    public static final String EVENT = "event";
    public static final String TODO = "todo";
    public static final String JOURNAL = "journal";

    public static final String VCALENDER_START = "BEGIN:VCALENDAR";
    public static final String VCALENDER_END = "END:VCALENDAR";

    public static final String VERSION = "VERSION:";
    public static final String V1 = "1.0";
    public static final String V2 = "2.0";

    public static final String PRODID = "PRODID:";
    public static final String PRODID__VALUE_BNDLY = "-// www.bndly.org / bndly-applications / DE";

    public static final String METHOD = "METHOD:";
    public static final String METHOD_PUBLISH = "PUBLISH";

    public static final String VEVENT_START = "BEGIN:VEVENT";
    public static final String VEVENT_END = "END:VEVENT";

    public static final String VTODO_START = "BEGIN:VTODO";
    public static final String VTODO_END = "END:VTODO";

    public static final String VJOURNAL_START = "BEGIN:VJOURNAL";
    public static final String VJOURNAL_END = "END:VJOURNAL";

    public static final String VALARM_START = "BEGIN:VALARM";
    public static final String VALARM_END = "END:VALARM";

    public static final String UID = "UID:";

    public static final String URL = "URL:";

    public static final String CAL_SCALE = "CALSCALE:";
    public static final String CAL_SCALE_GREGORIAN = "GREGORIAN";

    public static final String ORGANIZER = "ORGANIZER";
    public static final String CUSTOMER_NAME = "CN=";
    public static final String DIR = "DIR=";
    public static final String SENT_BY = "SENT-BY=";
    public static final String ATTENDEE = "ATTENDEE;";
    public static final String ATTENDEE_MEMBER = "MEMBER=";
//    public static final String MAIL = "MAILTO:";

    public static final String GEO = "GEO:";
    public static final String LOCATION = "LOCATION:";

    public static final String SUMMARY = "SUMMARY:";
    public static final String DESCRPITION = "DESCRIPTION:";

    public static final String SEUQENCE = "SEQUENCE:";
    public static final String PRIORITY = "PRIORITY:";

    public static final String STATUS = "STATUS:";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EVENT_TENTATIVE = "TENTATIVE";
    public static final String STATUS_EVENT_CONFIRMED = "CONFIRMED";
    public static final String STATUS_TODO_NEEDS_ACTION = "NEEDS-ACTION";
    public static final String STATUS_TODO_COMPLETED = "COMPLETED";
    public static final String STATUS_TODO_IN_PROCESS = "IN-PROCESS";
    public static final String STATUS_JOURNAL_DRAFT = "DRAFT";
    public static final String STATUS_JOURNAL_FINAL = "FINAL";

    public static final String CLASS = "CLASS:";
    public static final String CLASS_PUBLIC = "PUBLIC";
    public static final String CLASS_PRIVATE = "PRIVATE";
    public static final String CLASS_CONFIDENTIAL = "CONFIDENTIAL";

    public static final String TRANSPARENCY = "TRANSP:";
    public static final String TRANSPARENCY_TRANSPARENT = "TRANSPARENT";
    public static final String TRANSPARENCY_OPAQUE = "OPAQUE";

    public static final String DATE_START = "DTSTART:";
    public static final String DATE_END = "DTEND:";
    public static final String DATE_STAMP = "DTSTAMP:";
    public static final String DATE_CREATED = "CREATED:";
    public static final String DATE_LAST_MODIFIED = "LAST-MODIFIED:";
    public static final String DATE_DUE = "DUE:";
    public static final String DATE_COMPLETED = "COMPLETED:";
    public static final String DATE = "DATE:";
    public static final String DATE_TIME = "DATE-TIME:";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    public static final String DURATION = "DURATION:";

    public static final String VALUE = "VALUE=";

    public static final String RANGE = "RANGE=";

    public static final String THIS_AND_FUTURE = "THISANDFUTURE:";

    public static final String RRULE = "RRULE:";
    public static final String RRULE_FREQUENCE = "FREQ=";
    public static final String RRULE_UNTIL = "UNTIL=";
    public static final String RRULE_COUNT = "COUNT=";
    public static final String RRULE_INTERVAL = "INTERVAL=";
    public static final String RRULE_BYSECOND = "BYSECOND=";
    public static final String RRULE_BYMINUTE = "BYMINUTE=";
    public static final String RRULE_BYHOUR = "BYHOUR=";
    public static final String RRULE_BYDAY = "BYDAY=";
    public static final String RRULE_BYMONTHDAY = "BYMONTHDAY=";
    public static final String RRULE_BYYEARDAY = "BYYEARDAY=";
    public static final String RRULE_BYWEEKNO = "BYWEEKNO=";
    public static final String RRULE_BYMONTH = "BYMONTH=";
    public static final String RRULE_BYSETPOS = "BYSETPOS=";
    public static final String RRULE_WKST = "WKST=";

    public static final String RECURRENCE_ID = "RECURRENCE-ID;";

    public static final String PERCENT_COMPLETE = "PERCENT-COMPLETE:";

    private ICSConstants() {}
}
