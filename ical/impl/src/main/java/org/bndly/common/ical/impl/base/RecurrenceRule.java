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

import org.bndly.common.ical.api.base.FrequencyEnum;
import org.bndly.common.ical.api.exceptions.RecurrenceRuleException;
import org.bndly.common.ical.api.base.Repeatable;
import org.bndly.common.ical.api.base.WeekDays;

import java.util.Date;
import java.util.List;

/**
 * Created by alexp on 15.05.15.
 */
public class RecurrenceRule implements Repeatable {

    private FrequencyEnum frequency = null;
    private Date until = null;
    private int count;
    private int interval;
    private int[] bySeconds = null;
    private int[] byMinutes = null;
    private int[] byHours = null;
    private List<WeekDays> byDays = null;
    private int[] byMonthDays = null;
    private int[] byYearDays = null;
    private int[] byWeekNumbers = null;
    private int[] byMonths = null;
//    private int bySetpos;
    private WeekDays weekStartDay;

    public RecurrenceRule(
            FrequencyEnum freq, int interval, int count, Date until, int[] bySeconds, int[] byMinutes,
            int[] byHours, List<WeekDays> byDays, int[] byMonthDays, int[] byYearDays, int[] byWeekNumbers, /*int bySetpos*/
            int[] byMonths, WeekDays weekStartDay) {

        validateFrequency(freq);
        validateInterval(interval);
        validateBySeconds(bySeconds);
        validateByMinutes(byMinutes);
        validateByHours(byHours);
        validateByMonthDays(byMonthDays);
        validateByYearDays(byYearDays);
        validateByWeek(byWeekNumbers);
        validateByMonths(byMonths);


        frequency = freq;
        this.interval = interval;
        this.count = count;
        this.until = until;
        this.bySeconds = bySeconds;
        this.byMinutes = byMinutes;
        this.byHours = byHours;
        this.byDays = byDays;
        this.byMonthDays = byMonthDays;
        this.byYearDays = byYearDays;
        this.byWeekNumbers = byWeekNumbers;
        this.byMonths = byMonths;
        this.weekStartDay = weekStartDay;
    }

    private void validateFrequency(FrequencyEnum f) {
        if (f == null) {
            throw new RecurrenceRuleException("The 'frequency' is required to repeat calendar components!");
        }
    }

    private void validateInterval(int ival) {
        if (ival < 0) {
            throw new RecurrenceRuleException("The 'interval' needs to be positive!");
        }
    }

    private void validateBySeconds(int[] secs) {
        if (secs != null) {
            for (int sec : secs) {
                if (sec < 0 || sec > 60) {
                    throw new RecurrenceRuleException("The 'seconds' have to be between 0 and 60!");
                }
            }
        }
    }

    private void validateByMinutes(int[] mins) {
        if (mins != null) {
            for (int min : mins) {
                if (min < 0 || min > 59) {
                    throw new RecurrenceRuleException("The 'minutes' have to be between 0 and 59!");
                }
            }
        }
    }

    private void validateByHours(int[] hours) {
        if (hours != null) {
            for (int hour : hours) {
                if (hour < 0 || hour > 23) {
                    throw new RecurrenceRuleException("The 'hours' have to be between 0 and 23!");
                }
            }
        }
    }

    private void validateByMonthDays(int[] monthDays) {
        if (monthDays != null) {
            for (int day : monthDays) {
                if (day < 1 || day > 31) {
                    throw new RecurrenceRuleException("The 'days' have to be between 1 and 31!");
                }
            }
        }
    }

    private void validateByYearDays(int[] yearDays) {
        if (yearDays != null) {
            for (int day : yearDays) {
                if (day < 1 || day > 366) {
                    throw new RecurrenceRuleException("The 'days' have to be between 1 and 366!");
                }
            }
        }
    }

    private void validateByWeek(int[] weeks) {
        if (weeks != null) {
            for (int week : weeks) {
                if (week < 1 || week > 53) {
                    throw new RecurrenceRuleException("The 'week numbers' have to be between 1 and 53!");
                }
            }
        }
    }

    private void validateByMonths(int[] months) {
        if (months != null) {
            for (int m : months) {
                if (m < 1 || m > 12) {
                    throw new RecurrenceRuleException("The 'days' have to be between 1 and 366!");
                }
            }
        }
    }

    @Override
    public String getFrequency() {
        return frequency.toString();
    }

    @Override
    public Date getUntil() {
        return until;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public int[] getBySeconds() {
        return bySeconds;
    }

    @Override
    public int[] getByMinutes() {
        return byMinutes;
    }

    @Override
    public int[] getByHours() {
        return byHours;
    }

    @Override
    public List<WeekDays> getByDays() {
        return byDays;
    }

    @Override
    public int[] getByMonthDays() {
        return byMonthDays;
    }

    @Override
    public int[] getByYearDays() {
        return byYearDays;
    }

    @Override
    public int[] getByWeekNumbers() {
        return byWeekNumbers;
    }

    @Override
    public int[] getByMonths() {
        return byMonths;
    }

//    @Override
//    public int getBySetpos() {
//        return 0;
//    }

    @Override
    public String getWeekStartDay() {
        return weekStartDay.toString();
    }
}
