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

import org.bndly.rest.atomlink.api.AtomLinkBean;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface LinkExtractor {
	/**
	 * Extracts the atom link of the source object, that matches with the provided relation value
	 * @param rel the relation of the link
	 * @param source the link owner object
	 * @return the link with the provided relation or null, if the link can not be found extracted
	 */
	AtomLinkBean extractLink(String rel, Object source);
}
