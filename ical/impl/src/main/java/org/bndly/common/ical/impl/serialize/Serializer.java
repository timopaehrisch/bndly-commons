package org.bndly.common.ical.impl.serialize;

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


import org.bndly.common.ical.api.base.Duration;
import org.bndly.common.ical.api.base.Calendar;
import org.bndly.common.ical.api.base.Geographical;
import org.bndly.common.ical.api.base.CalendarComponent;
import org.bndly.common.ical.api.base.Journal;
import org.bndly.common.ical.api.base.WeekDays;
import org.bndly.common.ical.api.base.Event;
import org.bndly.common.ical.api.base.ToDo;
import org.bndly.common.ical.api.base.Repeatable;
import org.bndly.common.ical.api.serialize.CalendarSerializer;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Component;

/**
 * Created by alexp on 11.05.15.
 */

@Component(service = CalendarSerializer.class, immediate = true)
public class Serializer implements CalendarSerializer {

    private final String lf = System.lineSeparator();

    @Override
    public void serializeAsICS(Calendar calendar, OutputStream stream) throws IOException {

        OutputStreamWriter osw = new OutputStreamWriter(stream, Charset.forName("UTF-8"));
        serializeAsICS(calendar, osw);
    }

    @Override
    public void serializeAsICS(Calendar calendar, Writer w) throws IOException {

        try {
            w.append(ICSConstants.VCALENDER_START);
            w.append(lf);

            //-- set Calendar-Config
            w.append(ICSConstants.PRODID).append(calendar.getConfig().getProdID());
            w.append(lf);

            w.append(ICSConstants.VERSION).append(calendar.getConfig().getVersion());
            w.append(lf);

            w.append(ICSConstants.CAL_SCALE).append(calendar.getConfig().getCalScale());
            w.append(lf);

            //-- set CalendarComponents
            for (Map.Entry<String, List> entry : calendar.getComponents().entrySet()) {
                String key = entry.getKey();

                switch (entry.getKey()) {
                    case ICSConstants.EVENT:
                        if (entry.getValue() != null) {
                            List<Event> evts = (List<Event>) entry.getValue();
                            for (Event e : evts) {
                                w.append(ICSConstants.VEVENT_START).append(lf);


                                serializeComponentStandardFields(w, e);
                                serializeEvent(w, e);

                                w.append(ICSConstants.VEVENT_END);
                                w.append(lf);
                            }
                        }
                        break;

                    case ICSConstants.TODO:
                        if (entry.getValue() != null) {
                            List<ToDo> tos = (List<ToDo>) entry.getValue();
                            for (ToDo t : tos) {
                                w.append(ICSConstants.VTODO_START).append(lf);

                                serializeComponentStandardFields(w, t);
                                serializeToDo(w, t);

                                w.append(ICSConstants.VTODO_END);
                                w.append(lf);
                            }
                        }
                        break;

                    case ICSConstants.JOURNAL:
                        if (entry.getValue() != null) {
                            List<Journal> jous = (List<Journal>) entry.getValue();
                            for (Journal j : jous) {
                                w.append(ICSConstants.VJOURNAL_START).append(lf);

                                serializeComponentStandardFields(w, j);
                                serializeJournal(w, j);

                                w.append(ICSConstants.VJOURNAL_END);
                                w.append(lf);
                            }
                        }
                        break;

                    default:

                        break;
                }
            }

            w.append(ICSConstants.VCALENDER_END);

        } catch (IOException e) {
            throw new IOException(e.getCause());
        } finally {
            w.flush();
            w.close();
        }
    }

    private void serializeComponentStandardFields(Writer w, CalendarComponent comp) throws IOException {

        w.append( ICSConstants.UID ).append(comp.getUID()).append(lf);
        w.append( ICSConstants.DATE_STAMP ).append(ICSConstants.DATE_FORMAT.format(comp.getStamp())).append(lf);

        if ( !Util.isNullOrEmpty(comp.getStart()) ) {
            w.append(ICSConstants.DATE_START).append(ICSConstants.DATE_FORMAT.format(comp.getStart())).append(lf);
        }

        if ( comp.getRepeatable() != null ) {
            appendRecurrenceRule(w, comp.getRepeatable());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(comp.getCreated()) ) {
            w.append( ICSConstants.DATE_CREATED ).append(ICSConstants.DATE_FORMAT.format(comp.getCreated()));
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(comp.getLastModified()) ) {
            w.append( ICSConstants.DATE_LAST_MODIFIED ).append(ICSConstants.DATE_FORMAT.format(comp.getLastModified()));
            w.append(lf);
        }

        if ( comp.getSequence() >= 0 ) {
            w.append( ICSConstants.SEUQENCE ).append(String.valueOf(comp.getSequence()));
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(comp.getClassification()) ) {
            w.append(ICSConstants.CLASS ).append(comp.getClassification());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(comp.getStatus()) ) {
            w.append( ICSConstants.STATUS ).append(comp.getStatus());
            w.append(lf);
        }

        if ( comp.hasOrganizingInfo() ) {
            w.append( ICSConstants.ORGANIZER );

            appendOrganizerInfos(w, comp.getOrganizersCommonName(), comp.getOrganizersMailAddress(), comp.getOrganizersDIR(), comp.getOrganizersSentBy());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(comp.getSummary()) ) {
            appendAsTransformedBlock(w, ICSConstants.SUMMARY + comp.getSummary());
            w.append(lf);
        }

        if ( comp.getURL() != null ) {
            appendAsTransformedBlock(w, ICSConstants.URL + comp.getURL().toExternalForm());
            w.append(lf);
        }

//        if( !Util.isNullOrEmpty(comp.getRecurrenceID()) ){
//            w.append( ICSConstants.RECURRENCE_ID );
//
//            if( comp.isRecurrenceIDRangeTHISandFUTURE() ) {
//                w.append(ICSConstants.RANGE).append(ICSConstants.THIS_AND_FUTURE);
//                w.append(ICSConstants.DATE_FORMAT.format(comp.getRecurrenceID()));
//            }
//            else {
//                w.append( ICSConstants.VALUE ).append( ICSConstants.DATE_TIME );
//                w.append( ICSConstants.DATE_FORMAT.format(comp.getRecurrenceID()) );
//            }
//
//            w.append(lf);
//        }
    }

    private void serializeEvent(Writer w, Event e) throws IOException {
        if ( !Util.isNullOrEmpty(e.getEnd()) ) {
            w.append( ICSConstants.DATE_END ).append(ICSConstants.DATE_FORMAT.format(e.getEnd()));
            w.append(lf);
        }

        if ( e.getDuration() != null ) {
            w.append( ICSConstants.DURATION );
            appendDuration(w, e.getDuration());
            w.append(lf);
        }

        if ( e.getPriority() > 0 ) {
            w.append( ICSConstants.PRIORITY ).append(String.valueOf(e.getPriority()));
            w.append(lf);
        }

        if ( e.getGeo() != null ) {
            appendGeoData(w, e.getGeo());
        }

        if ( !Util.isNullOrEmpty(e.getLocation()) ) {
            appendAsTransformedBlock(w, ICSConstants.LOCATION + e.getLocation());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(e.getDescription()) ) {
            appendAsTransformedBlock(w, ICSConstants.DESCRPITION + e.getDescription());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(e.getTransparency()) ) {
            w.append( ICSConstants.TRANSPARENCY ).append(e.getTransparency());
            w.append(lf);
        }
    }

    private void serializeToDo(Writer w, ToDo t) throws IOException {
        if ( !Util.isNullOrEmpty(t.getDue()) ) {
            w.append( ICSConstants.DATE_DUE ).append(ICSConstants.DATE_FORMAT.format(t.getDue()));
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(t.getCompleted()) ) {
            w.append( ICSConstants.DATE_COMPLETED ).append(ICSConstants.DATE_FORMAT.format(t.getCompleted()));
            w.append(lf);
        }

        if ( t.getCompletedPercent() > 0) {
            w.append( ICSConstants.PERCENT_COMPLETE ).append(String.valueOf(t.getCompletedPercent()));
        }

        if ( t.getDuration() != null ) {
            w.append( ICSConstants.DURATION );
            appendDuration(w, t.getDuration());
            w.append(lf);
        }

        if ( t.getPriority() > 0 ) {
            w.append( ICSConstants.PRIORITY ).append(String.valueOf(t.getPriority()));
            w.append(lf);
        }

        if ( t.getGeo() != null ) {
            appendGeoData(w, t.getGeo());
        }

        if ( !Util.isNullOrEmpty(t.getLocation()) ) {
            appendAsTransformedBlock(w, ICSConstants.LOCATION + t.getLocation());
            w.append(lf);
        }

        if ( !Util.isNullOrEmpty(t.getDescription()) ) {
            appendAsTransformedBlock(w, ICSConstants.DESCRPITION + t.getDescription());
            w.append(lf);
        }
    }

    private void serializeJournal(Writer w, Journal j) throws IOException {

    }

    private void appendGeoData(Writer w, Geographical g) throws IOException {
        w.append( ICSConstants.GEO );
        w.append(String.valueOf(g.getLatitude()));
        w.append(";");
        w.append(String.valueOf(g.getLongitude()));
        w.append(lf);
    }

    private void appendDuration(Writer w, Duration d) throws IOException {
        w.append("P");

        if (d.getWeeks() > 0) {
            w.append(String.valueOf(d.getWeeks())).append("W");
        }

        if (d.getDays() > 0) {
            w.append(String.valueOf(d.getDays())).append("D");
        }

        if (d.getHours() > 0 || d.getMinutes() > 0 || d.getSeconds() > 0) {
            w.append("T");

            if (d.getHours() > 0) {
                w.append(String.valueOf(d.getHours())).append("H");
            }

            if (d.getMinutes() > 0) {
                w.append(String.valueOf(d.getMinutes())).append("M");
            }

            if (d.getSeconds() > 0) {
                w.append(String.valueOf(d.getMinutes())).append("S");
            }
        }
    }

    private void appendRecurrenceRule(Writer w, Repeatable r) throws IOException {

        w.append(ICSConstants.RRULE).append(ICSConstants.RRULE_FREQUENCE).append(r.getFrequency());

        if (r.getInterval() != 0) {
            w.append(";").append(ICSConstants.RRULE_INTERVAL).append(String.valueOf(r.getInterval()));
        }

        if (r.getByMonths() != null) {
            w.append(";").append(ICSConstants.RRULE_BYMONTH);
            addIntegerArray(w, r.getByMonths());
        }

        if (r.getByWeekNumbers() != null) {
            w.append(";").append(ICSConstants.RRULE_BYWEEKNO);
            addIntegerArray(w, r.getByWeekNumbers());
        }

        if (r.getByYearDays() != null) {
            w.append(";").append(ICSConstants.RRULE_BYYEARDAY);
            addIntegerArray(w, r.getByYearDays());
        }

        if (r.getByMonthDays() != null) {
            w.append(";").append(ICSConstants.RRULE_BYMONTHDAY);
            addIntegerArray(w, r.getByMonthDays());
        }

        if (r.getByDays() != null) {
            w.append(";").append(ICSConstants.RRULE_BYDAY);
            addDayArray(w, r.getByDays());
        }

        if (r.getByHours() != null) {
            w.append(";").append(ICSConstants.RRULE_BYHOUR);
            addIntegerArray(w, r.getByHours());
        }

        if (r.getByMinutes() != null) {
            w.append(";").append(ICSConstants.RRULE_BYMINUTE);
            addIntegerArray(w, r.getByMinutes());
        }

        if (r.getBySeconds() != null) {
            w.append(";").append(ICSConstants.RRULE_BYSECOND);
            addIntegerArray(w, r.getBySeconds());
        }

        if (r.getCount() != 0) {
            w.append(";").append(ICSConstants.RRULE_COUNT).append(String.valueOf(r.getCount()));
        }

        if (!Util.isNullOrEmpty(r.getUntil())) {
            w.append(";").append(ICSConstants.RRULE_UNTIL).append(ICSConstants.DATE_FORMAT.format(r.getUntil()));
        }
    }

    private void addIntegerArray(Writer w, int[] iA) throws IOException {
        for (int i = 0; i < iA.length; i++) {
            w.append(String.valueOf(iA[i]));

            if (i + 1 != iA.length) {
                w.append(",");
            }
        }
    }

    private void addDayArray(Writer w, List<WeekDays> days) throws IOException {
        Iterator it = days.iterator();
        while (it.hasNext()) {
            WeekDays d = (WeekDays) it.next();
            if (d == null) {
                continue;
            }

            w.append(d.toString());

            if (it.hasNext()) {
                w.append(",");
            }
        }
    }

    private void appendAsTransformedBlock(Writer w, String s) throws IOException {
        appendAsTransformedBlock(w, s, 74);
    }

    private void appendAsTransformedBlock(Writer w, String s, int blockSize) throws IOException {
        s = s.trim();

        for (int i = 0; i < s.length(); i += blockSize) {

            if (i + blockSize < s.length()) {
                w.append(s.substring(i, i + blockSize));
                w.append("\n ");
            } else {
                w.append(s.substring(i, s.length()));
            }
        }
    }

    private void appendOrganizerInfos(Writer w, String orgName, URI mail, URI dir, URI sentBy) throws IOException {
        StringBuilder bf = new StringBuilder();

        if (!Util.isNullOrEmpty(orgName)) {
            bf.append(";").append(ICSConstants.CUSTOMER_NAME).append(orgName);
        }

        if (dir != null) {
            bf.append(";").append(ICSConstants.DIR).append("\"").append(dir.toString()).append("\"");
        }

        if (sentBy != null) {
            bf.append(";").append(ICSConstants.SENT_BY).append("\"").append(sentBy.toString()).append("\"");
        }

        bf.append(":").append(mail.toString());

        appendAsTransformedBlock(w, bf.toString(), 74);
    }
}
