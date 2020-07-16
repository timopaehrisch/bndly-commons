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

import org.bndly.common.ical.api.base.Duration;
import org.bndly.common.ical.api.base.Event;
import org.bndly.common.ical.api.base.Repeatable;

import java.net.URI;
import java.net.URL;
import java.util.Date;

/**
 * Created by alexp on 07.05.15.
 */
public class EventImpl extends CalendarComponentImpl implements Event {

    // Prioritizable
    private int prio = -1;

    // Locatable
    private Geo geoData = null;
    private String location = null;

    // Describable
    private String description;

    // Durable
    private Duration duration = null;

    // VEvent
    private Date end = null;
    private String transparency = null;

    public EventImpl(Date start, String classification, Date created, int sequence, String status, String summary,
                     URL url, Date lastModified, Repeatable rule, String organizersCommonName, URI organizersDIR,
                     URI organizersMailAddress, URI organizersSentBy, String description, int prio, Date end,
                     Duration duration, Geo geoData, String location, String transparency) {

        super(  start,
                classification,
                created,
                sequence,
                status,
                summary,
                url,
                lastModified,
                rule,
                organizersCommonName,
                organizersDIR,
                organizersMailAddress,
                organizersSentBy
        );

        this.description = description;
        this.prio = prio;
        this.end = end;
        this.duration = duration;
        this.geoData = geoData;
        this.location = location;
        this.transparency = transparency;
    }


    // Remove
    @Override
    public void removeEndDate() {
        end = null;
    }

    @Override
    public void removeDuration() {
        duration = null;
    }

    @Override
    public void removeGeo() {
        geoData = null;
    }

    @Override
    public void removeLocation() {
        location = null;
    }

    @Override
    public void removeTransparency() {
        transparency = null;
    }


    // Get
    @Override
    public int getPriority() {
        return prio;
    }

    @Override
    public Date getEnd() {
        return end;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public Geo getGeo() {
        return geoData;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getTransparency() {
        return transparency;
    }
}
