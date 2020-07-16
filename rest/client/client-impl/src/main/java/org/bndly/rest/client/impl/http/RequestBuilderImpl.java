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

import org.bndly.rest.client.http.RequestBuilder;
import org.bndly.rest.client.http.RequestBuilderListener;
import org.bndly.rest.client.api.HttpHeaders;
import org.bndly.rest.client.api.RequestInterceptor;
import org.bndly.rest.client.api.ResponseInterceptor;
import org.bndly.rest.client.exception.ClientException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class RequestBuilderImpl implements RequestBuilder {

	private final HttpClient httpClient;
	private final Iterable<RequestBuilderListener> listeners;
	private final ExecutorService executorService;
	private final Iterable<ResponseInterceptor> responseInterceptors;
	private final Iterable<RequestInterceptor> requestInterceptors;
	private static final RequestConfig PREVENT_REDIRECT_CONFIG = RequestConfig.custom().setRedirectsEnabled(false).build();
	private static final RequestConfig DEFAULT_CONFIG = RequestConfig.DEFAULT;
	private String customHttpMethod;

	public RequestBuilderImpl(
			HttpClient httpClient, 
			Iterable<RequestBuilderListener> listeners, 
			ExecutorService executorService, 
			Iterable<ResponseInterceptor> responseInterceptors, 
			Iterable<RequestInterceptor> requestInterceptors
	) {
		this.httpClient = httpClient;
		this.listeners = listeners;
		this.executorService = executorService;
		this.responseInterceptors = responseInterceptors;
		this.requestInterceptors = requestInterceptors;
	}

	private long contentLength = -1;
	private static enum HttpMethod {
		GET, PUT, POST, DELETE, HEAD, OPTIONS, PATCH, CUSTOM
	}
	private final Map<String, String> headers = new LinkedHashMap<>();
	private String url;
	private boolean preventRedirect;
	private HttpMethod httpMethod;
	private InputStream payload;

	@Override
	public RequestBuilder url(String url) {
		this.url = url;
		return this;
	}

	@Override
	public RequestBuilder preventRedirect() {
		this.preventRedirect = true;
		return this;
	}
	
	@Override
	public RequestBuilderImpl get() {
		httpMethod = HttpMethod.GET;
		return this;
	}

	@Override
	public RequestBuilderImpl put() {
		httpMethod = HttpMethod.PUT;
		return this;
	}

	@Override
	public RequestBuilderImpl post() {
		httpMethod = HttpMethod.POST;
		return this;
	}

	@Override
	public RequestBuilderImpl delete() {
		httpMethod = HttpMethod.DELETE;
		return this;
	}

	@Override
	public RequestBuilderImpl head() {
		httpMethod = HttpMethod.HEAD;
		return this;
	}

	@Override
	public RequestBuilderImpl options() {
		httpMethod = HttpMethod.OPTIONS;
		return this;
	}

	@Override
	public RequestBuilderImpl patch() {
		httpMethod = HttpMethod.PATCH;
		return this;
	}

	@Override
	public RequestBuilder method(String methodName) {
		httpMethod = HttpMethod.CUSTOM;
		this.customHttpMethod = methodName;
		return this;
	}
	
	@Override
	public RequestBuilderImpl header(String name, String value) {
		if (name != null && !name.isEmpty()) {
			headers.put(name, value);
		}
		return this;
	}

	@Override
	public RequestBuilderImpl payload(InputStream inputStream) {
		return payload(inputStream, null, -1);
	}

	@Override
	public RequestBuilderImpl payload(InputStream inputStream, String contentType) {
		return payload(inputStream, contentType, -1);
	}

	@Override
	public RequestBuilderImpl payload(InputStream inputStream, String contentType, long contentLength) {
		if (inputStream != null) {
			payload = inputStream;
			if (contentType != null) {
				header(HttpHeaders.CONTENT_TYPE, contentType);
			}
			if (contentLength >= 0) {
				header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
			}
		}
		this.contentLength = contentLength;
		return this;
	}
	
	public RequestBuilderImpl copy() {
		RequestBuilderImpl copy = new RequestBuilderImpl(httpClient, listeners, executorService, responseInterceptors, requestInterceptors);
		copy.contentLength = contentLength;
		copy.url = url;
		copy.preventRedirect = preventRedirect;
		copy.httpMethod = httpMethod;
		copy.payload = payload;
		copy.headers.putAll(headers);
		return copy;
	}
	
	@Override
	public RequestImpl build() throws ClientException {
		for (RequestBuilderListener listener : listeners) {
			listener.beforeRequestBuild(this);
		}
		if (url == null) {
			throw new ClientException("no url provided");
		}
		if (httpMethod == null) {
			httpMethod = HttpMethod.GET;
		}
		HttpEntity httpEntity = null;
		if (payload != null) {
			if (httpMethod != HttpMethod.POST && httpMethod != HttpMethod.PUT && httpMethod != HttpMethod.PATCH) {
				throw new ClientException("payload requires POST, PUT or PATCH HTTP methods");
			}
			final String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
			if (contentLength >= 0) {
				if (contentType != null) {
					httpEntity = new InputStreamEntity(payload, contentLength, ContentType.parse(contentType));
				} else {
					httpEntity = new InputStreamEntity(payload, contentLength);
				}
			} else {
				if (contentType != null) {
					httpEntity = new InputStreamEntity(payload, ContentType.parse(contentType));
				} else {
					httpEntity = new InputStreamEntity(payload);
				}
			}
		}
		HttpRequestBase reqBase;
		if (httpMethod == HttpMethod.GET) {
			reqBase = new HttpGet(url);
		} else if (httpMethod == HttpMethod.POST) {
			reqBase = new HttpPost(url);
			if (httpEntity != null) {
				((HttpPost)reqBase).setEntity(httpEntity);
			}
		} else if (httpMethod == HttpMethod.PUT) {
			reqBase = new HttpPut(url);
			if (httpEntity != null) {
				((HttpPut)reqBase).setEntity(httpEntity);
			}
		} else if (httpMethod == HttpMethod.DELETE) {
			reqBase = new HttpDelete(url);
		} else if (httpMethod == HttpMethod.HEAD) {
			reqBase = new HttpHead(url);
		} else if (httpMethod == HttpMethod.OPTIONS) {
			reqBase = new HttpOptions(url);
		} else if (httpMethod == HttpMethod.PATCH) {
			reqBase = new HttpPatch(url);
			if (httpEntity != null) {
				((HttpPatch)reqBase).setEntity(httpEntity);
			}
		} else if (httpMethod == HttpMethod.CUSTOM) {
			final String method = customHttpMethod;
			if (httpEntity != null) {
				reqBase = new HttpEntityEnclosingRequestBase() {
					@Override
					public String getMethod() {
						return method;
					}
					
				};
			} else {
				reqBase = new HttpRequestBase() {
					@Override
					public String getMethod() {
						return method;
					}
				};
			}
			reqBase.setURI(URI.create(url));
		} else {
			throw new ClientException("unsupported http method: " + httpMethod);
		}
		
		if (preventRedirect) {
			reqBase.setConfig(PREVENT_REDIRECT_CONFIG);
		} else {
			reqBase.setConfig(DEFAULT_CONFIG);
		}
		
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String val = entry.getValue();
			if (val == null) {
				val = "";
			}
			reqBase.addHeader(entry.getKey(), val);
		}
		RequestImpl request = new RequestImpl(reqBase, httpClient, executorService, responseInterceptors, requestInterceptors);
		return request;
	}
}
