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

import org.bndly.common.ical.api.ICSJournalBuilder;
import org.bndly.common.ical.api.base.Journal;
import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.JournalImpl;
import org.bndly.common.ical.impl.base.Util;

import java.util.Date;

/**
 * Created by alexp on 21.05.15.
 */
public class ICSJournalBuilderImpl extends ICSComponentBuilderImpl<ICSJournalBuilder> implements ICSJournalBuilder {

    public ICSJournalBuilderImpl(ICSBuilderImpl masterBuilder) {
        super(masterBuilder);
    }

    @Override
    public ICSJournalBuilder start(Date start) {
        this.start = Util.assertNotNull(start, "You cannot set an empty 'start date'!");
        return this;
    }

    public Journal buildJournal() {
        validateMandatoryFields();
        validateOrganizerFields();

        return new JournalImpl(
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
                orgSentBy
        );
    }

    @Override
    public boolean validateMandatoryFields() throws IllegalArgumentException, CalendarException {
        return false;
    }

    @Override
    public ICSJournalBuilder draftStatus() {
        setStatus(ICSConstants.STATUS_JOURNAL_DRAFT);
        return this;
    }

    @Override
    public ICSJournalBuilder finalStatus() {
        setStatus(ICSConstants.STATUS_JOURNAL_FINAL);
        return this;
    }
}
