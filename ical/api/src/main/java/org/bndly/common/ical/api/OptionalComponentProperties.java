package org.bndly.common.ical.api;

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

//import org.apache.commons.codec.binary.Base64InputStream;

import java.io.FilterInputStream;
import java.net.URI;
import java.util.List;

/**
 * Created by alexp on 06.05.15.
 */
public interface OptionalComponentProperties {

    void putAttachment(URI uri);
    void putAttachment(FilterInputStream inputStream);
    void removeAttachment(URI uri);
    void removeAttachement(int count);
    void removeAttachments();


    void putAttendee(String customerName, String mailAddress, String role);
    void removeAttendee(String customerName, String mailAddress, String role);
    void removeAttendees();

    /**
     * This property defines the equipment or resources
     * anticipated for an activity specified by a calendar component.
     */
    void putResource(String resource);
    void removeResource(String resource);
    void removeResource(int count);
    void removeResources();

    void putComment(String comment);
    void removeComment(String comment);
    void removeComment(int count);
    void removeComments();


    URI getAttachment(int count);
    FilterInputStream getBase64Attachment(int count);
    List<URI> getURIAttachments();
    List<FilterInputStream> getBase64Attachments();

    String getAttendee(int count);
    List<String> getAttendees();

    String getResource(int count);
    List<String> getResources();

    String getComment(int count);
    List<String> getComments();
}
