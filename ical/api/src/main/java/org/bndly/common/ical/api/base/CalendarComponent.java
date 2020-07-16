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

import java.net.URL;
import java.util.Date;

/**
 * Created by alexp on 06.05.15.
 */
public interface CalendarComponent extends Organizable {

    /**
     * In the case of an iCalendar object that specifies a
     * "METHOD" property, this property specifies the date and time that
     * the instance of the iCalendar object was created.  In the case of
     * an iCalendar object that doesn't specify a "METHOD" property, this
     * property specifies the date and time that the information
     * associated with the calendar component was last revised in the
     * calendar store.
     *
     * @return dateStamp as String
     */
    Date getStamp();

    /**
     * This property defines the persistent, globally unique
     * identifier for the calendar component.
     *
     * Example: "19960401T080045Z-4000F192713-0052@example.com"
     *
     * @return UID as String
     */
    String getUID();

    /**
     *
     * @return startDate as String
     */
    Date getStart();

    /**
     * "PUBLIC" / "PRIVATE" / "CONFIDENTIAL" -- DEFAULT is "PUBLIC"
     * @return
     */
    String getClassification();

    Date getCreated();

    /**
     * This property defines the revision sequence number of the
     * calendar component within a sequence of revisions.
     *
     * @return
     */
    int getSequence();

    /**
     * This property specifies the date and time that the
     * information associated with the calendar component was last
     * revised in the calendar store.
     *
     * @return
     */
//    void setLastModified(Date updateDate);

    Date getLastModified();

    /**
     * This property defines the overall status or confirmation
     * for the calendar component.
     *
     * statvalue-event = "TENTATIVE"    ;Indicates event is tentative.
     *                 / "CONFIRMED"    ;Indicates event is definite.
     *                 / "CANCELLED"    ;Indicates event was cancelled.
     *
     * statvalue-todo  = "NEEDS-ACTION" ;Indicates to-do needs action.
     *                 / "COMPLETED"    ;Indicates to-do completed.
     *                 / "IN-PROCESS"   ;Indicates to-do in process of.
     *                 / "CANCELLED"    ;Indicates to-do was cancelled.
     *
     * statvalue-jour  = "DRAFT"        ;Indicates journal is draft.
     *                 / "FINAL"        ;Indicates journal is final.
     *                 / "CANCELLED"    ;Indicates journal is removed.
     *
     * @return
     */
    String getStatus();

    String getSummary();

    URL getURL();

    Repeatable getRepeatable();

//    Date getRecurrenceID();

//    boolean isRecurrenceIDRangeTHISandFUTURE();
}
