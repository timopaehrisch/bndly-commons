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

import org.bndly.common.ical.api.exceptions.CalendarException;
import org.bndly.common.ical.api.base.Configuration;
import org.bndly.common.ical.api.ICSConfigurationBuilder;
import org.bndly.common.ical.impl.base.ConfigurationImpl;
import org.bndly.common.ical.impl.base.ICSConstants;
import org.bndly.common.ical.impl.base.Util;

/**
 * Created by alexp on 07.05.15.
 */
public class ICSConfigurationBuilderImpl implements ICSConfigurationBuilder {

    private String prodID = null;
    private String version = null;
//    private String calScale = null;
//    private String method = null;

    @Override
    public ICSConfigurationBuilder prodID(String prodID) {
        this.prodID = Util.assertNotNull(prodID, "It's not allowed to have an empty or null 'prodID'.");
        return this;
    }

    @Override
    public ICSConfigurationBuilder version1() {
        return version(ICSConstants.V1);
    }

    @Override
    public ICSConfigurationBuilder version2() {
        return version(ICSConstants.V2);
    }


    private ICSConfigurationBuilder version(String version) {
        this.version = version;
        return this;
    }

//    @Override
//    public ICSConfigurationBuilder calScale(String calScale) {
//        this.calScale = calScale;
//        return this;
//    }

//    @Override
//    public ICSConfigurationBuilder method (String method) {
//        this.method = method;
//        return this;
//    }

    @Override
    public Configuration build() throws CalendarException {
        if ( Util.isNullOrEmpty(this.prodID) || Util.isNullOrEmpty(this.version) ) {
            throw new CalendarException("prodID and version must be setted before.");
        }

        return new ConfigurationImpl(prodID, version, ICSConstants.CAL_SCALE_GREGORIAN);
    }
}
