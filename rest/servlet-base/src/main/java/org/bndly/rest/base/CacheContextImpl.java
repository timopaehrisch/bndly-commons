package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.rest.api.CacheContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class CacheContextImpl implements CacheContext {
	private final ContextImpl context;
	private final SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	private Integer serverSideMaxAge;

	public CacheContextImpl(ContextImpl context) {
		this.context = context;
	}

	@Override
	public void preventCache() {
		if (isHttpResponse()) {
			context.getHeaderWriter().write("Cache-Control", "no-cache");
		}
		context.setPreventCache(true);
	}

	@Override
	public boolean isCachingPrevented() {
		return context.isCachingPrevented();
	}
	
	private boolean isHttpRequest() {
		return context.isHttpRequest();
	}
	private boolean isHttpResponse() {
		return context.isHttpResponse();
	}

	@Override
	public String getETag() {
		if (!isHttpRequest()) {
			return null;
		}
		String etag = context.getHeaderReader().read("If-None-Match");
		return etag;
	}
	
	@Override
	public CacheContext setETag(String etag) {
		if (isHttpResponse()) {
			if (etag != null) {
				context.getHeaderWriter().write("ETag", etag);
			}
		}
		return this;
	}
	
	@Override
	public CacheContext setLastModified(Date date) {
		if (isHttpResponse()) {
			String asString = headerDateFormat.format(date);
			context.getHeaderWriter().write("Last-Modified", asString);
		}
		return this;
	}
	
	@Override
	public Date getIfModifiedSince() {
		if (!isHttpRequest()) {
			return null;
		}
		String modifiedSince = context.getHeaderReader().read("If-Modified-Since");
		if (modifiedSince == null) {
			return null;
		}
		try {
			Date date = headerDateFormat.parse(modifiedSince);
			return date;
		} catch (java.text.ParseException ex) {
			return null;
		}
	}

	@Override
	public CacheContext setMaxAge(int maxAgeInSeconds) {
		if (!isHttpResponse()) {
			return this;
		}
		serverSideMaxAge = maxAgeInSeconds;
		context.getHeaderWriter().write("Cache-Control", "max-age=" + maxAgeInSeconds);
		return this;
	}

	@Override
	public Integer getServerSideMaxAge() {
		return serverSideMaxAge;
	}
	
}
