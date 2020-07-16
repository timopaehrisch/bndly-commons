package org.bndly.common.cors.impl;

/*-
 * #%L
 * CORS
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

import java.util.HashSet;
import java.util.Set;

public class CORSContext {
    private boolean requestCORS;
    private String origin;
    private Set<String> allowedMethods = new HashSet<String>();
    private Set<String> allowedHeaders = new HashSet<String>();
    private Set<String> exposedHeaders = new HashSet<String>();
    private boolean credentialsAllowed;
    private boolean preflight;

    public boolean isPreflight() {
        return preflight;
    }

    public void setPreflight(boolean preflight) {
        this.preflight = preflight;
    }

    public boolean isCredentialsAllowed() {
        return credentialsAllowed;
    }

    public void setCredentialsAllowed(boolean credentialsAllowed) {
        this.credentialsAllowed = credentialsAllowed;
    }

    public Set<String> getAllowedMethods() {
        return allowedMethods;
    }

    public Set<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public Set<String> getExposedHeaders() {
        return exposedHeaders;
    }
    
    public boolean isRequestCORS() {
        return requestCORS;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setRequestCORS(boolean requestCORS) {
        this.requestCORS = requestCORS;
    }
    
}
