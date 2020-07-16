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

import org.bndly.common.ical.api.extensions.OrganizeExtension;
import org.bndly.common.ical.api.extensions.RepeatExtension;

import java.net.URL;
import java.util.Date;

/**
 * Created by alexp on 20.05.15.
 */
public interface ICSComponentBuilder<T> extends ICSBuilder, RepeatExtension<T>, OrganizeExtension<T> {

    T start(Date start);

    T publicClassification();

    T privateClassification();

    T confidentialClassification();

    T cancelledStatus();

    T created(Date created);

    T sequence(int sequence);

    T summary(String summary);

    T url(URL url);

    T lastModified(Date lastModified);
}
