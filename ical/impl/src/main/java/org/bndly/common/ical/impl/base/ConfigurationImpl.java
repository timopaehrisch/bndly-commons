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

import org.bndly.common.ical.api.base.Configuration;

/**
 * Created by alexp on 07.05.15.
 */
public class ConfigurationImpl implements Configuration {

    private String prodID = null;
    private String version = null;
    private String calScale = null;
    //    private String method = null;

//    public ConfigurationImpl(){
//        prodID = ICSConstants.PRODID__VALUE_BNDLY;
//        calScale = ICSConstants.CAL_SCALE_GREGORIAN;
//        version = ICSConstants.V2;
//    }

    public ConfigurationImpl(String prodID, String version, String calScale) {
        this.prodID = prodID;
        this.version = version;
        this.calScale = calScale;
    }

    public ConfigurationImpl(Configuration config) {
        prodID = config.getProdID();
        calScale = config.getCalScale();
        version = config.getVersion();
//        method = config.method();
    }

//    public ConfigurationImpl(String prodID, float version, String calScale, String method) {
//        this.prodID = prodID;
//        this.calScale = calScale;
//        this.version = version;
//        this.method = method;
//    }

    @Override
    public void setProdID(String prodID) {
        this.prodID = prodID;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

//    @Override
//    public void setMethod(String method) {
//
//        this.method = method;
//    }

    @Override
    public String getProdID() {
        return prodID;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCalScale() {
        return calScale;
    }

//    @Override
//    public String getMethod() {
//        return method;
//    }
}
