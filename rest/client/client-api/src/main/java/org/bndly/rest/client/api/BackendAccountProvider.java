package org.bndly.rest.client.api;

/*-
 * #%L
 * REST Client API
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
 * The BackenAccountProvider allows the Shop Client to add authentication 
 * information to each request that is sent to the Shop Core.
 * The authentication information is sent via HTTP Basic Auth.
 * Therefore it is strongly recommended to use HTTPS, because otherwise the 
 * authentication information could be easily read due to man-in-the-middle 
 * attacks.
 */
public interface BackendAccountProvider {
    String NAME = "shopClientBackendAccountProvider";
    
    /**
     * Tells the Shop Client the BackendAccount name that has to be used for the requests.
     * @return name of the BackendAccount
     */
    String getBackendAccountName();
    
    /**
     * Tells the Shop Client the Secret that will used to validate the BackenAccount name.
     * @return secret of the BackendAccount
     */
    String getBackendAccountSecret();
}
