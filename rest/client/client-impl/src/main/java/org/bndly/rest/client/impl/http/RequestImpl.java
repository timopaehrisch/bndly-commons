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

import org.bndly.rest.client.api.RequestAdapter;
import org.bndly.rest.client.api.RequestInterceptor;
import org.bndly.rest.client.api.ResponseInterceptor;
import org.bndly.rest.client.http.Request;
import org.bndly.rest.client.http.Response;
import org.bndly.rest.client.http.ResponseCallback;
import org.bndly.rest.client.exception.ClientException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class RequestImpl implements Request, ResponseCallback<Object>, RequestAdapter {
	private final HttpRequestBase reqBase;
	private final HttpClient httpClient;
	private final ExecutorService executorService;
	private final Iterable<ResponseInterceptor> responseInterceptors;
	private final Iterable<RequestInterceptor> requestInterceptors;
	
	/**
	 * This implementation just forwards the response to the legacy ReponseInterceptors.
	 * @param response
	 * @return always null
	 */
	@Override
	public Object doWithResponse(Response response) {
		return null;
	}

	public RequestImpl(
			HttpRequestBase reqBase, 
			HttpClient httpClient, 
			ExecutorService executorService, 
			Iterable<ResponseInterceptor> responseInterceptors, 
			Iterable<RequestInterceptor> requestInterceptors
	) {
		this.reqBase = reqBase;
		this.httpClient = httpClient;
		this.executorService = executorService;
		this.responseInterceptors = responseInterceptors;
		this.requestInterceptors = requestInterceptors;
	}

//	public HttpRequestBase getReqBase() {
//		return reqBase;
//	}
	
	@Override
	public void execute() throws ClientException {
		execute(this);
	}
	
	// RequestAdapter - START
	@Override
	public void addHeader(String headerName, String value) {
		reqBase.addHeader(headerName, value);
	}

	@Override
	public org.bndly.rest.client.api.Header[] getAllHeaders() {
		Header[] headers = reqBase.getAllHeaders();
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
	// RequestAdapter - END
	
	@Override
	public <E> E execute(final ResponseCallback<E> callback) throws ClientException {
		try {
			for (RequestInterceptor requestInterceptor : requestInterceptors) {
				requestInterceptor.process(this);
			}
			HttpResponse response = httpClient.execute(reqBase);
			HttpEntity entity = response.getEntity();
			try {
				ResponseImpl internalResponse = new ResponseImpl(response);
				for (ResponseInterceptor responseInterceptor : responseInterceptors) {
					responseInterceptor.process(internalResponse);
				}
				return callback.doWithResponse(internalResponse);
			} finally {
				if (entity != null) {
					EntityUtils.consumeQuietly(entity);
				}
			}
		} catch (org.apache.http.conn.HttpHostConnectException ex) {
			throw new ClientException("could not connect to " + reqBase.getURI(), ex);
		} catch (ClientException ex) {
			// just re-throw
			throw ex;
		} catch (Exception ex) {
			throw new ClientException("could not execute HTTP " + reqBase.getMethod() + " request for url '" + reqBase.getURI() + "': " + ex.getMessage(), ex);
		}
	}

	@Override
	public <E> Future<E> executeFuture(final ResponseCallback<E> callback) throws ClientException {
		return executorService.submit(new Callable<E>() {
			@Override
			public E call() throws Exception {
				return execute(callback);
			}
		});
	}

	@Override
	public Future<?> executeFuture() throws ClientException {
		return executorService.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				execute();
				return null;
			}
		});
	}

	@Override
	public String getMethod() {
		return reqBase.getMethod();
	}

	@Override
	public String getUrl() {
		return reqBase.getURI().toString();
	}
	
	/*public void abort() {
		reqBase.abort();
	}*/
}
