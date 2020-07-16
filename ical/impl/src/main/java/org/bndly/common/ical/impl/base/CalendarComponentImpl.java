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

import org.bndly.common.ical.api.base.CalendarComponent;
import org.bndly.common.ical.api.base.Repeatable;

import java.net.URI;
import java.net.URL;
import java.rmi.server.UID;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by alexp on 20.05.15.
 */
public class CalendarComponentImpl implements CalendarComponent {

    // CalendarComponent
    private Date stamp = null;
    private String uid = null;
    private Date start = null;
    private String classification = null;
    private Date created = null;
    private int sequence;
    private String status = null;
    private String summary = null;
    private URL url = null;

    // Organizable
    private String commonName = null;
    private URI mailAddress = null;
    private URI dir = null;
    private URI sentBy = null;
    private String language = null;

    // Modifyable
    private Date lastModified = null;

    private Repeatable rRule = null;
//    private Date recurrenceID = null;
//    private boolean recurrenceIDRange = false;

    private CalendarComponentImpl() {
        stamp = Calendar.getInstance().getTime();
        uid = new UID().toString();

        sequence = 0;
    }

    public CalendarComponentImpl(
            Date start, String classification, Date created, int sequence, String status, String summary,
            URL url, Date lastModified, Repeatable rule,/* Date recurrenceID, boolean range,*/
            String organizersCommonName, URI organizersDIR, URI organizersMailAddress, URI organizersSentBy) {

        this();

        this.start = start;
        this.classification = classification;
        this.created = created;
        this.sequence = sequence;
        this.status = status;
        this.summary = summary;
        this.url = url;
        this.lastModified = lastModified;

        this.rRule = rule;
//        this.recurrenceID = recurrenceID;
//        this.recurrenceIDRange = range;

        this.commonName = organizersCommonName;
        this.mailAddress = organizersMailAddress;
        this.dir = organizersDIR;
        this.sentBy = organizersSentBy;
    }


//    // Setter
//    @Override
//    public void setLastModified(Date updateDate) {
//        this.lastModified = updateDate;
//    }

    // Getter
    @Override
    public Date getStamp() {
        return stamp;
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public Date getStart() {
        return start;
    }

    @Override
    public String getClassification() {
        return classification;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public Repeatable getRepeatable() {
        return rRule;
    }

//    @Override
//    public Date getRecurrenceID() {
//        return recurrenceID;
//    }
//
//    @Override
//    public boolean isRecurrenceIDRangeTHISandFUTURE() {
//        return recurrenceIDRange;
//    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public String getOrganizersCommonName() {
        return commonName;
    }

    @Override
    public URI getOrganizersMailAddress() {
        return mailAddress;
    }

    @Override
    public URI getOrganizersDIR() {
        return dir;
    }

    @Override
    public URI getOrganizersSentBy() {
        return sentBy;
    }

    @Override
    public boolean hasOrganizingInfo() {
        return !Util.isNullOrEmpty(commonName) || mailAddress != null || dir != null || sentBy != null;
    }
}
