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

import org.bndly.common.ical.api.exceptions.CalendarException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * Created by alexp on 20.05.15.
 */
public interface OrganizeExtension<T> {

    T organizerName(String name);

    T organizerMailAddress(String mailAddress) throws CalendarException, URISyntaxException;

    T organizerDIR(URI dir);

    T organizerSentBy(String mailAddress) throws CalendarException, URISyntaxException;

    boolean validateOrganizerFields() throws IllegalArgumentException;

}
