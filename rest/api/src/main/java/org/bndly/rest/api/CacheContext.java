package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface CacheContext {
	void preventCache();
	boolean isCachingPrevented();
	String getETag();
	CacheContext setETag(String etag);
	CacheContext setLastModified(Date date);
	CacheContext setMaxAge(int maxAgeInSeconds);
	Date getIfModifiedSince();
	Integer getServerSideMaxAge();
}
