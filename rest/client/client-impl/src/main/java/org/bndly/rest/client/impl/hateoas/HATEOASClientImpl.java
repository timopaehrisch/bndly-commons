package org.bndly.rest.client.impl.hateoas;

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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.annotation.AtomLinkHolder;
import org.bndly.rest.client.api.BackendAccountProvider;
import org.bndly.rest.client.api.ExceptionThrower;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.HATEOASConfiguredClient;
import org.bndly.rest.client.api.LanguageProvider;
import org.bndly.rest.client.api.LinkExtractor;
import org.bndly.rest.client.api.MediaType;
import org.bndly.rest.client.api.RequestInterceptor;
import org.bndly.rest.client.api.ResponseInterceptor;
import org.bndly.rest.client.exception.MissingLinkClientException;
import org.bndly.rest.client.http.RequestBuilder;
import org.bndly.rest.client.impl.http.HttpRequestExecutor;
import org.bndly.rest.client.impl.http.RequestBuilderImpl;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBContext;
import org.apache.http.client.HttpClient;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HATEOASClientImpl<T> implements HATEOASClient<T> {

	private final T resource;
	private final HATEOASClientFactoryImpl clientFactoryImpl;
	private final LinkExtractor linkExtractor;

	public HATEOASClientImpl(T resource, HATEOASClientFactoryImpl clientFactoryImpl, LinkExtractor linkExtractor) {
		if (resource == null) {
			throw new IllegalArgumentException("resource is mandatory");
		}
		this.resource = resource;
		if (clientFactoryImpl == null) {
			throw new IllegalArgumentException("clientFactoryImpl is mandatory");
		}
		this.clientFactoryImpl = clientFactoryImpl;
		if (linkExtractor == null) {
			throw new IllegalArgumentException("linkExtractor is mandatory");
		}
		this.linkExtractor = linkExtractor;
	}

	private String acceptType = MediaType.APPLICATION_XML;
	private String contentType = MediaType.APPLICATION_XML;
	private String acceptLanguage;

	@Override
	public T getWrappedBean() {
		return resource;
	}

	@Override
	public HATEOASConfiguredClient<T> read() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_READ);
	}

	@Override
	public HATEOASConfiguredClient<T> update() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_UPDATE);
	}

	@Override
	public HATEOASConfiguredClient<T> create() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_CREATE);
	}

	@Override
	public HATEOASConfiguredClient<T> delete() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_DELETE);
	}

	@Override
	public HATEOASConfiguredClient<T> next() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_NEXT);
	}

	@Override
	public HATEOASConfiguredClient<T> previous() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_PREVIOUS);
	}

	@Override
	public HATEOASConfiguredClient<T> list() throws MissingLinkClientException {
		return follow(HATEOASClient.LINK_LIST);
	}

	@Override
	public HATEOASConfiguredClient<T> follow(String rel) throws MissingLinkClientException {
		AtomLinkBean atomLinkBean = linkExtractor.extractLink(rel, resource);
		if (atomLinkBean != null && atomLinkBean.getRel().equals(rel)) {
			final String url = atomLinkBean.getHref();
			String method = atomLinkBean.getMethod();
			if (method == null) {
				method = "GET";
			}
			RequestBuilderImpl requestBuilder = clientFactoryImpl.createRequestBuilder();
			requestBuilder.url(url);
			if ("GET".equals(method)) {
				requestBuilder.get();
			} else if ("PUT".equals(method)) {
				requestBuilder.put();
			} else if ("POST".equals(method)) {
				requestBuilder.post();
			} else if ("DELETE".equals(method)) {
				requestBuilder.delete();
			} else if ("HEAD".equals(method)) {
				requestBuilder.head();
			} else if ("OPTIONS".equals(method)) {
				requestBuilder.options();
			} else if ("PATCH".equals(method)) {
				requestBuilder.patch();
			}
			return new HATEOASConfiguredClientImpl<>(requestBuilder, this);
		} else {
			throw new MissingLinkClientException("the provided resource " + resource.getClass().getSimpleName() + " has no link for relation " + rel);
		}
	}
	
	public RequestBuilderImpl createRequestBuilder() {
		return clientFactoryImpl.createRequestBuilder();
	}

	@Override
	public HATEOASClient<T> accept(String acceptType) {
		this.acceptType = acceptType;
		return this;
	}

	@Override
	public HATEOASClient<T> contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public HATEOASClient<T> acceptLanguage(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
		return this;
	}

	public String getAcceptLanguage() {
		return acceptLanguage;
	}

	public String getAcceptedContentType() {
		return acceptType;
	}

	public Iterator<RequestInterceptor> getRequestInterceptors() {
		return clientFactoryImpl.getRequestInterceptors();
	}

	public Iterator<ResponseInterceptor> getResponseInterceptor() {
		return clientFactoryImpl.getResponseInterceptor();
	}

	public T getResource() {
		return resource;
	}

	public String getContentTypeOfPayload() {
		return contentType;
	}
	
	public HttpClient getHttpClient() {
		return clientFactoryImpl.getHttpClient();
	}
	
	public HttpRequestExecutor getExecutor() {
		return clientFactoryImpl.getExecutor();
	}
	
	public ExceptionThrower getExceptionThrower() {
		return clientFactoryImpl.getExceptionThrower();
	}

	public LanguageProvider getLanguageProvider() {
		return clientFactoryImpl.getLanguageProvider();
	}

	public JAXBContext getJAXBContext() {
		return clientFactoryImpl.getJAXBContext();
	}

	public BackendAccountProvider getBackendAccountProvider() {
		return clientFactoryImpl.getBackendAccountProvider();
	}

	public Base64Service getBase64Service() {
		return clientFactoryImpl.getBase64Service();
	}
	
	public ObjectToInputStreamConverter getObjectToInputStreamConverter() {
		return clientFactoryImpl;
	}
}
