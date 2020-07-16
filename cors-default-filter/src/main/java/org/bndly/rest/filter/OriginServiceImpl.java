package org.bndly.rest.filter;

/*-
 * #%L
 * CORS Default Filter
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

import org.bndly.common.cors.api.Origin;
import org.bndly.common.cors.api.OriginService;

public class OriginServiceImpl implements OriginService {

    private static class OriginImpl implements Origin {
        private String protocol;
        private String domainName;
        private int port;

        @Override
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        @Override
        public void setPort(int port) {
            this.port = port;
        }
    }

    @Override
    public Origin newInstance() {
        OriginImpl o = new OriginImpl();
        o.setPort(80);
        return o;
    }

    @Override
    public boolean isAcceptedOrigin(Origin origin) {
        // we probably might want to store a list of allowed origins somewhere
        return true;
    }
    
}
