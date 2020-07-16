package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import java.util.Collection;

public interface Linkable {
    /**
     * return the url for the given link name
     * @param rel the link relation name
     * @return the url of the link or null, if the link can not be found
     */
    String follow(String rel);
    
    /**
     * return the HTTP method for the given link name
     * @param rel the link relation name
     * @return the HTTP method of the link or null, if the link can not be found
     */
    String followForMethod(String rel);
    
    /**
     * sets the url of a link. if a link with the same name already existed, it will get overwritten
     * @param rel the link relation name
     * @param url the link url
     */
    void addLink(String rel, String url, String method);
    
    /**
     * removes a link from an entity by its relationship name
     * @param rel relationship name
     */
    void removeLink(String rel);
    
    /**
     * returns a collection of link relation names that exist in the given object
     * @return a collection with the link relation names of the existing links
     */
    Collection<String> getAllLinkNames();
}
