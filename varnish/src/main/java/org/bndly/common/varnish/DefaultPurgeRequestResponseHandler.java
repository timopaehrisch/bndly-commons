package org.bndly.common.varnish;

/*-
 * #%L
 * Varnish
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

import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = PurgeRequestResponseHandler.class)
public class DefaultPurgeRequestResponseHandler implements PurgeRequestResponseHandler {

	@Override
	public Iterable<HttpRequestBase> createPurgeRequests(String pathToFlush, String urlWithPathToFlush) {
		HttpRequestBase purgeRequest = new HttpRequestBase() {
			@Override
			public String getMethod() {
				return "PURGE";
			}
		};
		purgeRequest.setURI(URI.create(urlWithPathToFlush));
		return new SingleItemIterable<>(purgeRequest);
	}

	@Override
	public Boolean isPurgeSuccessResponse(HttpResponse hr, HttpRequestBase purgeRequest) {
		StatusLine statusLine = hr.getStatusLine();
		int code = statusLine.getStatusCode();
		return code == 200;
	}

}
