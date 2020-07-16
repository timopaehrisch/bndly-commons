package org.bndly.common.ical.api.base;

/*-
 * #%L
 * iCal API
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

import java.util.Date;
import java.util.List;

/**
 * Created by alexp on 13.05.15.
 */
public interface Repeatable {

    /**
     *
     * @return Could be a mix of "SECONDLY" / "MINUTELY" / "HOURLY" / "DAILY" / "WEEKLY" / "MONTHLY" / "YEARLY"
     */
    String getFrequency();

    Date getUntil();

    int getCount();

    int getInterval();

    /**
     * Range 0 - 60
     * @return Range 0 - 60
     */
    int[] getBySeconds();

    /**
     * Range 0 - 59
     * @return Range 0 - 59
     */
    int[] getByMinutes();

    /**
     *
     * @return Range 0 - 23
     */
    int[] getByHours();

    /**
     *
     * @return List of "MO", "TU", "WE", "TH", "FR", "SA", "SU"
     */
    List<WeekDays> getByDays();

    /**
     *
     * @return Range 1 - 31
     */
    int[] getByMonthDays();

    /**
     *
     * @return Range 1 - 366
     */
    int[] getByYearDays();

    /**
     *
     * @return Range 1 - 53
     */
    int[] getByWeekNumbers();

    /**
     *
     * @return Range 1 - 12
     */
    int[] getByMonths();

    /**
     *
     * @return Range 1 - 366
     */
//    int getBySetpos();

    String getWeekStartDay();
}
