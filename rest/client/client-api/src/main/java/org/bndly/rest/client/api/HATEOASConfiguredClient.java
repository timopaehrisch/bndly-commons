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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.http.Request;
import java.io.OutputStream;

public interface HATEOASConfiguredClient<T> extends RequestBuilderWrapper, ResponseCallbackFactory<T> {
	Request buildRequest() throws ClientException;

	T execute() throws ClientException;
	T execute(Object payload) throws ClientException;
	T execute(ReplayableInputStream payload) throws ClientException;
	<B> B execute(Class<B> responseType) throws ClientException;
	<B> B execute(Object payload, Class<B> responseType) throws ClientException;
	T execute(OutputStream responseOutputStream) throws ClientException;

	HATEOASConfiguredClient<T> preventRedirect();
	
	HATEOASConfiguredClient<T> payload(Object payload);
	HATEOASConfiguredClient<T> payload(ReplayableInputStream payload);
	HATEOASConfiguredClient<T> payload(ReplayableInputStream payload, String contentType);
	HATEOASConfiguredClient<T> payload(ReplayableInputStream payload, String contentType, long contentLength);
}
