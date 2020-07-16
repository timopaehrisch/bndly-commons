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
import org.bndly.rest.client.api.BackendAccountProvider;
import org.bndly.rest.client.api.ExceptionThrower;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.HATEOASClientFactory;
import org.bndly.rest.client.api.HttpHeaders;
import org.bndly.rest.client.api.LanguageProvider;
import org.bndly.rest.client.api.LinkExtractor;
import org.bndly.rest.client.api.MessageClassesProvider;
import org.bndly.rest.client.api.RequestInterceptor;
import org.bndly.rest.client.api.ResponseInterceptor;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.http.RequestBuilder;
import org.bndly.rest.client.http.RequestBuilderFactory;
import org.bndly.rest.client.http.RequestBuilderListener;
import org.bndly.rest.client.impl.http.HttpRequestExecutor;
import org.bndly.rest.client.impl.http.RequestBuilderImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HATEOASClientFactoryImpl implements HATEOASClientFactory, ObjectToInputStreamConverter, RequestBuilderFactory {
	private static final Logger logger = LoggerFactory.getLogger(HATEOASClientFactoryImpl.class);

	private HttpRequestExecutor executor;
	private final HttpClient httpClient;
	private JAXBContext context;
	private BackendAccountProvider backendAccountProvider;
	private LanguageProvider languageProvider;
	private ExceptionThrower exceptionThrower;
	private ExecutorService executorService;
	private LinkExtractor linkExtractor;
	private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
	private final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();
	private final List<RequestBuilderListener> requestBuilderListeners = new ArrayList<>();
	private final Class<?>[] errorMessages;
	private Base64Service base64Service;

	public HATEOASClientFactoryImpl(
			HttpClient httpClient,
			MessageClassesProvider messageClassesProvider, 
			BackendAccountProvider backendAccountProvider, 
			LanguageProvider languageProvider, 
			ExceptionThrower exceptionThrower, 
			Base64Service base64Service
	) {
		executor = new HttpRequestExecutor(httpClient, new BasicHttpContext());
		this.httpClient = httpClient;
		this.backendAccountProvider = backendAccountProvider;
		this.languageProvider = languageProvider;
		this.exceptionThrower = exceptionThrower;
		this.base64Service = base64Service;
		try {
			errorMessages = messageClassesProvider.getAllErrorMessageClasses();
			context = JAXBContext.newInstance(messageClassesProvider.getAllUseableMessageClasses());
		} catch (JAXBException e) {
			throw new IllegalStateException("could not set up JAXB context. " + e.getMessage(), e);
		}
		initDefaultProvidersAsListeners();
	}

	private void initDefaultProvidersAsListeners() {
		if (backendAccountProvider != null) {
			if (base64Service == null) {
				logger.warn("Base64Service is not initialized. An instance of the Base64Service is required for HTTP basic access authentication.");
			} else {
				requestBuilderListeners.add(new RequestBuilderListener() {
					@Override
					public void beforeRequestBuild(RequestBuilder requestBuilder) throws ClientException {
						String toEnc = backendAccountProvider.getBackendAccountName() + ":" + backendAccountProvider.getBackendAccountSecret();
						String enc = base64Service.base64Encode(toEnc.getBytes());
						String headerValue = "Basic " + enc;
						requestBuilder.header(HttpHeaders.AUTHORIZATION, headerValue);

					}
				});
			}
		}

		if (languageProvider != null) {
			requestBuilderListeners.add(new RequestBuilderListener() {
				@Override
				public void beforeRequestBuild(RequestBuilder requestBuilder) throws ClientException {
					String lang = languageProvider.getCurrentLanguage();
					if (lang != null) {
						lang = lang.trim();
						if (lang != null && lang.length() == 2) {
							String headerValue = lang + ";q=1.0";
							requestBuilder.header(HttpHeaders.ACCEPT_LANGUAGE, headerValue);
						}
					}
				}
			});
		}
	}
	
	@Override
	public RequestBuilderImpl createRequestBuilder() {
		return new RequestBuilderImpl(httpClient, requestBuilderListeners, executorService, responseInterceptors, requestInterceptors);
	}

	@Override
	public <T> HATEOASClient<T> build(T resource) {
		if (resource == null) {
			throw new IllegalArgumentException("can not build a client when the wrapped resource is null");
		}
		return new HATEOASClientImpl<>(resource, this, linkExtractor == null ? LegacyLinkExtractor.INSTANCE : linkExtractor);
	}

	@Override
	public InputStream convert(Object input) {
		return buildInputStreamFrom(input);
	}

	private InputStream buildInputStreamFrom(final Object rawRequestEntity) {
		PipedInputStream s = new PipedInputStream();
		final PipedOutputStream o = new PipedOutputStream();
		try {
			o.connect(s);
		} catch (IOException ex) {
			throw new IllegalStateException("could not set up pipe: " + ex.getMessage(), ex);
		}
		Runnable marshallingTask = new Runnable() {
			@Override
			public void run() {
				try (OutputStream os = o) {
					getJAXBContext().createMarshaller().marshal(rawRequestEntity, os);
					os.flush();
				} catch (Exception ex) {
					throw new IllegalStateException("could not build input stream for request entity " + rawRequestEntity.getClass().getSimpleName(), ex);
				}
			}
		};
		executorService.submit(marshallingTask);
		return s;

	}
	
	public Iterator<RequestInterceptor> getRequestInterceptors() {
		return requestInterceptors.iterator();
	}
	
	public Iterator<ResponseInterceptor> getResponseInterceptor() {
		return responseInterceptors.iterator();
	}
	
	public HttpClient getHttpClient() {
		return httpClient;
	}
	
	public HttpRequestExecutor getExecutor() {
		return executor;
	}

	public ExceptionThrower getExceptionThrower() {
		return exceptionThrower;
	}

	public LanguageProvider getLanguageProvider() {
		return languageProvider;
	}

	public BackendAccountProvider getBackendAccountProvider() {
		return backendAccountProvider;
	}

	public Base64Service getBase64Service() {
		return base64Service;
	}

	public JAXBContext getJAXBContext() {
		return context;
	}

	public Class<?>[] getErrorMessageTypes() {
		return errorMessages;
	}

	/**
	 * Sets a thread pool executor as the executor service
	 * @param threadPoolExecutor
	 * @deprecated please use {@link #setExecutorService(java.util.concurrent.ExecutorService)}
	 */
	@Deprecated
	public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
		this.executorService = threadPoolExecutor;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	@Override
	public void addRequestInterceptor(RequestInterceptor requestInterceptor) {
		if (requestInterceptor == null) {
			return;
		}
		requestInterceptors.add(requestInterceptor);
	}

	@Override
	public void addResponseInterceptor(ResponseInterceptor responseInterceptor) {
		if (responseInterceptor == null) {
			return;
		}
		responseInterceptors.add(responseInterceptor);
	}

	@Override
	public void addRequestBuilderListener(RequestBuilderListener listener) {
		if (listener == null) {
			return;
		}
		requestBuilderListeners.add(listener);
	}

	@Override
	public void removeRequestInterceptor(RequestInterceptor requestInterceptor) {
		if (requestInterceptor == null) {
			return;
		}
		Iterator<RequestInterceptor> iterator = requestInterceptors.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == requestInterceptor) {
				iterator.remove();
			}
		}
	}

	@Override
	public void removeResponseInterceptor(ResponseInterceptor responseInterceptor) {
		if (responseInterceptor == null) {
			return;
		}
		Iterator<ResponseInterceptor> iterator = responseInterceptors.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == responseInterceptor) {
				iterator.remove();
			}
		}
	}

	@Override
	public void removeRequestBuilderListener(RequestBuilderListener listener) {
		if (listener == null) {
			return;
		}
		Iterator<RequestBuilderListener> iterator = requestBuilderListeners.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == listener) {
				iterator.remove();
			}
		}
	}

	public void setLinkExtractor(LinkExtractor linkExtractor) {
		this.linkExtractor = linkExtractor;
	}
	
}
