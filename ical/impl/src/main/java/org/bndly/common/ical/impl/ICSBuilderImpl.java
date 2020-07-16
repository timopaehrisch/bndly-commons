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
import org.bndly.common.ical.api.base.Journal;
import org.bndly.common.ical.api.base.Event;
import org.bndly.common.ical.api.ICSBuilder;
import org.bndly.common.ical.api.base.ToDo;
import org.bndly.common.ical.api.ICSJournalBuilder;
import org.bndly.common.ical.api.ICSEventBuilder;
import org.bndly.common.ical.api.base.Configuration;
import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.impl.base.CalendarImpl;
import org.bndly.common.ical.impl.base.ConfigurationImpl;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by alexp on 07.05.15.
 */

public class ICSBuilderImpl implements ICSBuilder {

    private Configuration config = null;
    private List<Event> evs = null;
    private List<ToDo> tos = null;
    private List<Journal> jous = null;

    private List<ICSEventBuilderImpl> evtBuilderList = null;
    private List<ICSToDoBuilderImpl> toDoBuilderList = null;
    private List<ICSJournalBuilderImpl> jouBuilderList = null;

    private boolean hasComponent;// = false;


    public ICSBuilderImpl() {
        config = new ConfigurationImpl(ICSConstants.PRODID__VALUE_BNDLY, ICSConstants.V2, ICSConstants.CAL_SCALE_GREGORIAN);
        evtBuilderList = new ArrayList<>();
        toDoBuilderList = new ArrayList<>();
        jouBuilderList = new ArrayList<>();
        hasComponent = false;
    }

    @Override
    public CalendarBuilder configure(Configuration config) {
        if (config == null) {
            throw new CalendarException("The calendar configuration must be not null - it is mandatory for creating calendars!");
        }

        this.config = config;
        return this;
    }

    @Override
    public ICSEventBuilder event() {
        ICSEventBuilderImpl evbi = new ICSEventBuilderImpl(this);
        evtBuilderList.add(evbi);

        hasComponent = evtBuilderList.size() > 0;
        return evbi;
    }

    @Override
    public ICSToDoBuilder todo() {
        ICSToDoBuilderImpl tobi = new ICSToDoBuilderImpl(this);
        toDoBuilderList.add(tobi);

        hasComponent = toDoBuilderList.size() > 0;

        return tobi;
    }

    @Override
    public ICSJournalBuilder journal() {
        ICSJournalBuilderImpl jobi = new ICSJournalBuilderImpl(this);
        jouBuilderList.add(jobi);

        hasComponent = jouBuilderList.size() > 0;

        return jobi;
    }

    @Override
    public Calendar build() {
        validateMandatoryFields();

        if (!hasComponent) {
            throw new CalendarException("You need one calendar component to build a calendar !");
        }

        buildEvents();
        buildToDos();
        buildJournals();

        return new CalendarImpl(config, evs, tos, jous);
    }

    public boolean validateMandatoryFields() throws IllegalArgumentException {
        if ( Util.isNullOrEmpty(this.config.getProdID()) ) {
            throw new IllegalArgumentException("Field 'PRODID' should be not null or empty");
        }

        if ( Util.isNullOrEmpty(this.config.getVersion()) ) {
            throw new IllegalArgumentException("Field 'VERSION' should be not null or empty");
        }

        if ( Util.isNullOrEmpty(this.config.getCalScale()) ) {
            throw new IllegalArgumentException("Field 'CAlSCALE' should be not null or empty");
        }

        return true;
    }

    private void buildEvents() {
        evs = new ArrayList<>();
        evs.clear();

        Iterator<ICSEventBuilderImpl> itr = evtBuilderList.iterator();
        while (itr.hasNext()) {
            evs.add(itr.next().buildEvent());
        }
    }

    private void buildToDos() {
        tos = new ArrayList<>();
        tos.clear();

        Iterator<ICSToDoBuilderImpl> itr = toDoBuilderList.iterator();
        while (itr.hasNext()) {
            tos.add(itr.next().buildToDo());
        }
    }

    private void buildJournals() {
        jous = new ArrayList<>();
        jous.clear();

        Iterator<ICSJournalBuilderImpl> itr = jouBuilderList.iterator();
        while (itr.hasNext()) {
            jous.add(itr.next().buildJournal());
        }
    }
}
