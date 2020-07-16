package org.bndly.common.ical.api.extensions;

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

import org.bndly.common.ical.api.base.FrequencyEnum;
import org.bndly.common.ical.api.base.WeekDays;

import java.util.Date;
import java.util.List;

/**
 * Created by alexp on 15.05.15.
 */
public interface RepeatExtension<T> {

    T repeat(
          FrequencyEnum frequency,
          int interval,
          int count,
          Date until,
          int[] bySeconds,
          int[] byMinutes,
          int[] byHours,
          List<WeekDays> byDays,
          int[] byMonthDays,
          int[] byYearDays,
          int[] byWeekNumbers,
          int[] byMonths,
          WeekDays weekStartDay
    );


    T repeat(
          FrequencyEnum frequency,
          int interval,
          Date until,
          int[] byMinutes,
          int[] byHours,
          List<WeekDays> byDays,
          int[] byMonthDays,
          int[] byYearDays,
          int[] byWeekNumbers,
          int[] byMonths
    );


    T repeat(
           FrequencyEnum frequency,
           int interval,
           int count,
           int[] byMinutes,
           int[] byHours,
           List<WeekDays> byDays,
           int[] byMonthDays,
           int[] byYearDays,
           int[] byWeekNumbers,
           int[] byMonths
    );

//    T rangeTHISandFUTURE(boolean thisAndFuture);
}
