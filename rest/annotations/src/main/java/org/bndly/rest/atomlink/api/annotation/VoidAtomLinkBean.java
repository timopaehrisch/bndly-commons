package org.bndly.rest.atomlink.api.annotation;

/*-
 * #%L
 * REST Atom Link API
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

import org.bndly.rest.atomlink.api.AtomLinkBean;

public final class VoidAtomLinkBean implements AtomLinkBean {

    public String getRel() {
	return null;
    }

    public void setRel(String rel) {
    }

    public String getHref() {
	return null;
    }

    public void setHref(String href) {
    }

    public String getMethod() {
	return null;
    }

    public void setMethod(String method) {
    }
}
