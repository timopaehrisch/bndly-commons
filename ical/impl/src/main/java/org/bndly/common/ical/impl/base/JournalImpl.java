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

import org.bndly.common.ical.api.base.Journal;
import org.bndly.common.ical.api.base.Repeatable;

import java.net.URI;
import java.net.URL;
import java.util.Date;

/**
 * Created by alexp on 21.05.15.
 */
public class JournalImpl extends CalendarComponentImpl implements Journal {

    public JournalImpl(Date start, String classification, Date created, int sequence, String status, String summary,
                       URL url, Date lastModified, Repeatable rule, String organizersCommonName, URI organizersDIR,
                       URI organizersMailAddress, URI organizersSentBy) {
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
    }
}
