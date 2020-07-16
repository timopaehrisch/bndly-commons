package org.bndly.rest.client.hateoas;

/*-
 * #%L
 * REST Client Impl
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

import org.bndly.rest.client.api.BackendAccountProvider;

public class TestBackendAccountProvider implements BackendAccountProvider {

    public String getBackendAccountName() {
	return "integration-test";
    }

    public String getBackendAccountSecret() {
	return "22d63650-1dbb-11e2-81c1-0800200c9a66";
    }
    
}
