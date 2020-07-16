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

/**
 * Created by alexp on 05.05.15.

 From: http://tools.ietf.org/html/rfc5545
 An iCalendar object MUST include the "PRODID" and "VERSION" calendar
 properties.  In addition, it MUST include at least one calendar
 component.  Special forms of iCalendar objects are possible to
 publish just busy time (i.e., only a "VFREEBUSY" calendar component)
 or time zone (i.e., only a "VTIMEZONE" calendar component)
 information.  In addition, a complex iCalendar object that is used to
 capture a complete snapshot of the contents of a calendar is possible
 (e.g., composite of many different calendar components).  More
 commonly, an iCalendar object will consist of just a single "VEVENT",
 "VTODO", or "VJOURNAL" calendar component.  Applications MUST ignore
 x-comp and iana-comp values they don't recognize.  Applications that
 support importing iCalendar objects SHOULD support all of the
 component types defined in this document, and SHOULD NOT silently
 drop any components as that can lead to user data loss.
 */

public interface Configuration {

    /**
     * Example: "-//ABC Corporation//NONSGML My Product//EN"
     * @param prodID as String
     */
    void setProdID(String prodID);

    /**
     * This property specifies the identifier corresponding to the
     * highest version number or the minimum and maximum range of the
     * iCalendar specification that is required in order to interpret the
     * iCalendar object.
     *
     * @param version as float // value could be '2f'
     */
    void setVersion(String version);

    /**
     * This property defines the calendar scale used for the
     * calendar information specified in the iCalendar object.
     *
     * Example: "GREGORIAN"
     * @param calScale as String
     */
//    void setCalScale(String calScale);  DEFAULT is the Gregorian when this property isn't set

    /**
     * This property defines the iCalendar object method
     * associated with the calendar object.
     *
     * Example: "REQUEST"
     * @param method as String
     */
//    void setMethod(String method);


    /** @return prodID as String */
    String getProdID();


    /** @return version as float */
    String getVersion();

    /**
     * DEFAULT is the Gregorian when this property isn't set
     * @return calScale as String
     */
    String getCalScale();

    /** @return method as String */
//    String getMethod();
}
