package org.bndly.rest.cache.impl;

/*-
 * #%L
 * REST Cache
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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.cache.api.CacheEntry;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class CacheEntryImpl implements CacheEntry {
	private Date lastModified;
	private String etag;
	private String contentType;
	private String encoding;
	private String contentLanguage;
	private Long contentLength;
	private Integer maxAge;
	private ReplayableInputStream data;
	private ResourceURI uri;

	@Override
	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public String getContentLanguage() {
		return contentLanguage;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public void setData(ReplayableInputStream data) {
		this.data = data;
	}

	public void setUri(ResourceURI uri) {
		this.uri = uri;
	}

	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public Long getContentLength() {
		return contentLength;
	}

	@Override
	public ResourceURI getResourceURI() {
		return uri;
	}

	@Override
	public ReplayableInputStream getData() {
		return data;
	}

	@Override
	public String getETag() {
		return etag;
	}

	@Override
	public Date getLastModified() {
		return lastModified;
	}

	public void setContentLanguage(String contentLanguage) {
		this.contentLanguage = contentLanguage;
	}
	
}
