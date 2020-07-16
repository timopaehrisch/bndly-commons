package org.bndly.rest.client.impl.http;

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

import org.bndly.rest.client.api.ResponseAdapter;
import org.bndly.rest.client.http.Response;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ResponseImpl implements Response, ResponseAdapter {
	private final HttpResponse response;
	private final HttpEntity entity;

	public ResponseImpl(HttpResponse response) {
		this.response = response;
		this.entity = response.getEntity();
	}

	
	// ResponseAdapter - START
	@Override
	public int getStatusCode() {
		return response.getStatusLine().getStatusCode();
	}

	@Override
	public String getReasonPhrase() {
		return response.getStatusLine().getReasonPhrase();
	}

	@Override
	public org.bndly.rest.client.api.Header[] getAllHeaders() {
		org.apache.http.Header[] headers = response.getAllHeaders();
		if (headers != null) {
			org.bndly.rest.client.api.Header[] result = new org.bndly.rest.client.api.Header[headers.length];
			for (int i = 0; i < headers.length; i++) {
				org.bndly.rest.client.api.Header h = new org.bndly.rest.client.api.Header();
				h.setName(headers[i].getName());
				h.setValue(headers[i].getValue());
				result[i] = h;
			}
			return result;
		}
		return null;
	}
	// ResponseAdapter - END

	@Override
	public String getHeaderValue(String headerName) {
		if (headerName == null) {
			return null;
		}
		Header header = response.getFirstHeader(headerName);
		if (header == null) {
			return null;
		}
		return header.getValue();
	}

	@Override
	public InputStream getEntityData() throws IOException {
		return entity == null ? null : entity.getContent();
	}

	@Override
	public String getEntityContentType() {
		if (entity == null) {
			return null;
		}
		Header contentType = entity.getContentType();
		return contentType == null ? null : contentType.getValue();
	}

	@Override
	public boolean isHavingEntity() {
		return entity != null;
	}

	public HttpResponse getHttpResponse() {
		return response;
	}

	@Override
	public void consumeEntitySilently() {
		if (isHavingEntity()) {
			EntityUtils.consumeQuietly(entity);
		}
	}

	public String getEntityContentEncoding() {
		if (!isHavingEntity()) {
			return null;
		}
		Header header = entity.getContentEncoding();
		return header == null ? null : header.getValue();
	}
	
}
