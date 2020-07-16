package org.bndly.rest.client.http;

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

import org.bndly.rest.client.exception.ClientException;
import java.io.InputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface RequestBuilder {
	RequestBuilder url(String url);

	RequestBuilder get();
	RequestBuilder put();
	RequestBuilder post();
	RequestBuilder delete();
	RequestBuilder head();
	RequestBuilder options();
	RequestBuilder patch();
	RequestBuilder method(String methodName);

	RequestBuilder header(String name, String value);

	RequestBuilder preventRedirect();

	RequestBuilder payload(InputStream inputStream);
	RequestBuilder payload(InputStream inputStream, String contentType);
	RequestBuilder payload(InputStream inputStream, String contentType, long contentLength);

	Request build() throws ClientException;
}
