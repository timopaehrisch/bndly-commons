package org.bndly.rest.client.impl.adapter;

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

import org.bndly.rest.client.api.RequestAdapter;
import org.apache.http.Header;
import org.apache.http.HttpRequest;

@Deprecated
public class RequestAdapterImpl implements RequestAdapter {
	private final HttpRequest request;
	
	public RequestAdapterImpl(HttpRequest request) {
		super();
		this.request = request;
	}

	@Override
	public void addHeader(String headerName, String value) {
		request.addHeader(headerName, value);
	}

	@Override
	public org.bndly.rest.client.api.Header[] getAllHeaders() {
		Header[] headers = request.getAllHeaders();
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

}
