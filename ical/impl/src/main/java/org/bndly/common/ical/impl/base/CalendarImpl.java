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

import org.bndly.common.ical.api.base.Calendar;
import org.bndly.common.ical.api.base.Journal;
import org.bndly.common.ical.api.base.Event;
import org.bndly.common.ical.api.base.ToDo;
import org.bndly.common.ical.api.base.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alexp on 07.05.15.
 */
public class CalendarImpl implements Calendar {

    private Configuration conf = null;
    private List<Event> events = null;
    private List<ToDo> toDos = null;
    private List<Journal> journals = null;

    public CalendarImpl(Configuration config, List<Event> evts, List<ToDo> toDos, List<Journal> journals) {
        this.conf = config;
        this.events = new ArrayList<Event>(evts);
        this.toDos = new ArrayList<ToDo>(toDos);
        this.journals = new ArrayList<Journal>(journals);
    }

    @Override
    public Configuration getConfig() {
        return conf;
    }

    @Override
    public List<Event> getEvents() {
        return events;
    }

    @Override
    public List<ToDo> getToDos() {
        return toDos;
    }

    @Override
    public List<Journal> getJournals() {
        return journals;
    }

    @Override
    public Map<String, List> getComponents() {
        Map<String, List> comps = new HashMap<>();

        comps.put(ICSConstants.EVENT, events);
        comps.put(ICSConstants.TODO, toDos);
        comps.put(ICSConstants.JOURNAL, journals);

        return comps;
    }
}
