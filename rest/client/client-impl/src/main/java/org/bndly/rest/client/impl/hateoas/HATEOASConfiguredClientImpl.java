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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.client.api.HATEOASConfiguredClient;
import org.bndly.rest.client.api.HttpHeaders;
import org.bndly.rest.client.api.MediaType;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.http.Request;
import org.bndly.rest.client.http.RequestBuilder;
import org.bndly.rest.client.http.Response;
import org.bndly.rest.client.http.ResponseCallback;
import org.bndly.rest.client.impl.http.RequestBuilderImpl;
import org.bndly.rest.client.impl.http.RequestImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HATEOASConfiguredClientImpl<T> implements HATEOASConfiguredClient<T> {

	private static final Logger LOG = LoggerFactory.getLogger(HATEOASConfiguredClientImpl.class);
	
	private final RequestBuilderImpl requestBuilder;
	private final HATEOASClientImpl<T> clientImpl;

	private Object payload;
	private Class<?> responseType = null;
	private boolean preventRedirect = false;

	public HATEOASConfiguredClientImpl(RequestBuilderImpl requestBuilder, HATEOASClientImpl<T> clientImpl) {
		if (requestBuilder == null) {
			throw new IllegalArgumentException("requestBuilder is mandatory");
		}
		this.requestBuilder = requestBuilder;
		if (clientImpl == null) {
			throw new IllegalArgumentException("clientImpl is mandatory");
		}
		this.clientImpl = clientImpl;
	}

	@Override
	public HATEOASConfiguredClient<T> preventRedirect() {
		preventRedirect = true;
		return this;
	}

	@Override
	public T execute() throws ClientException {
		// build a httprequest from the url and the http method
		final RequestImpl request = buildRequest();
		return executeRequest(request, createDefaultResponseCallback(request));
	}

	@Override
	public T execute(Object payload) throws ClientException {
		return payload(payload).execute();
	}

	@Override
	public <B> B execute(final Class<B> responseType) throws ClientException {
		this.responseType = responseType;
		final RequestImpl request = buildRequest();
		return executeRequest(request, createTypedResponseCallback(request, responseType));
	}
	
	@Override
	public <B> B execute(Object payload, Class<B> responseType) throws ClientException {
		return payload(payload).execute(responseType);
	}

	@Override
	public T execute(final OutputStream responseOutputStream) throws ClientException {
		RequestImpl request = buildRequest();
		return executeRequest(request, createStreamWritingResponseCallback(responseOutputStream));
	}
	
		@Override
	public ResponseCallback<T> createDefaultResponseCallback(final Request request) {
		return new ResponseCallback<T>() {
			@Override
			public T doWithResponse(Response response) throws ClientException {
				Object entity = extractEntityFromResponse(response, request);
				return (T) entity;
			}
		};
	}
	
	@Override
	public <B> ResponseCallback<B> createTypedResponseCallback(final Request request, final Class<B> responseType) {
		return new ResponseCallback<B>() {
			@Override
			public B doWithResponse(Response response) throws ClientException {
				Object entity = extractEntityFromResponse(response, request);
				if (entity == null) {
					return null;
				}

				if (responseType.isAssignableFrom(entity.getClass())) {
					return responseType.cast(entity);
				} else {
					throw new ClientException("expected response type " + responseType.getSimpleName() + " but found " + entity.getClass());
				}
			}
		};
	}
	
	@Override
	public ResponseCallback<T> createStreamWritingResponseCallback(final OutputStream responseOutputStream) {
		return new ResponseCallback<T>() {
			@Override
			public T doWithResponse(Response response) throws ClientException {
				writeEntityToOutputStream(response, responseOutputStream);
				return clientImpl.getResource();
			}
		};
	}
	
	@Override
	public RequestImpl buildRequest() throws ClientException {
		applyStateToRequestBuilder(requestBuilder);
		return requestBuilder.build();
	}


	@Override
	public RequestBuilder requestBuilder() {
		return requestBuilder;
	}

	@Override
	public RequestBuilder copyRequestBuilder() {
		RequestBuilderImpl copy = requestBuilder.copy();
		applyStateToRequestBuilder(copy);
		return copy;
	}
	
	private void applyStateToRequestBuilder(RequestBuilder requestBuilder) {
		Object rawRequestEntity = payload;
		if (rawRequestEntity != null) {
			applyPayloadEntityToRequestBuilder(rawRequestEntity);
		}

		String accept = clientImpl.getAcceptedContentType();
		if (accept != null) {
			requestBuilder.header(HttpHeaders.ACCEPT, accept);
		}
		addLanguageHeader();
		
		if (preventRedirect) {
			requestBuilder.preventRedirect();
		}
	}

	private void applyPayloadEntityToRequestBuilder(Object rawRequestEntity) {
		InputStream is;
		if (InputStream.class.isInstance(rawRequestEntity)) {
			is = (InputStream) rawRequestEntity;
		} else {
			is = clientImpl.getObjectToInputStreamConverter().convert(rawRequestEntity);
		}
		String contentType = clientImpl.getContentTypeOfPayload();
		if (contentType == null) {
			requestBuilder.payload(is);
		} else {
			requestBuilder.payload(is, contentType);
		}
	}

	@Override
	public HATEOASConfiguredClient<T> payload(Object payload) {
		this.payload = payload;
		return this;
	}

	@Override
	public HATEOASConfiguredClient<T> payload(ReplayableInputStream payload) {
		requestBuilder.payload(payload);
		return this;
	}

	@Override
	public HATEOASConfiguredClient<T> payload(ReplayableInputStream payload, String contentType) {
		requestBuilder.payload(payload, contentType);
		return this;
	}

	@Override
	public HATEOASConfiguredClient<T> payload(ReplayableInputStream payload, String contentType, long contentLength) {
		requestBuilder.payload(payload, contentType,contentLength);
		return this;
	}

	private <E> E executeRequest(Request request, ResponseCallback<E> callback) throws ClientException {
		return request.execute(callback);
	}

	private void addLanguageHeader() {
		String language = clientImpl.getAcceptLanguage();
		if (language != null) {
			language = language.trim();
			if (language != null && language.length() == 2) {
				String headerValue = language + ";q=1.0";
				requestBuilder.header(HttpHeaders.ACCEPT_LANGUAGE, headerValue);
			}
		}
	}

	private Object extractEntityFromResponse(Response response, Request request) throws ClientException {
		int sc = response.getStatusCode();
		if (sc == HttpStatus.SC_CREATED || sc == HttpStatus.SC_MOVED_TEMPORARILY || sc == HttpStatus.SC_SEE_OTHER) {
			return handleRedirectResponse(response);
		} else if (sc == HttpStatus.SC_NO_CONTENT) {
			return null;
		} else {
			if (response.isHavingEntity()) {
				try (InputStream is = response.getEntityData()) {
					String contentTypeRaw = response.getEntityContentType();
					final boolean isError = sc >= 400;
					final boolean hasContentTypeHeader = contentTypeRaw != null;
					if (!hasContentTypeHeader) {
						if (isError) {
							clientImpl.getExceptionThrower().throwException(null, sc, request.getMethod(), request.getUrl());
						}
						return null;
					}
					final boolean isXMLContent = hasContentTypeHeader && contentTypeRaw != null && contentTypeRaw.startsWith(MediaType.APPLICATION_XML);
					if (!isXMLContent) {
						handleNonJAXBEntity(isError, request, is, response);
						return null; // this line would never be reached, because the previous method always throws an exception
					} else {
						return handleJAXBEntity(is, isError, sc, request);
					}
				} catch (IOException ex) {
					throw new ClientException("failed to read response entity: " + ex.getMessage(), ex);
				}
			} else {
				// there was not entity in the response
				return null;
			}
		}
	}

	private void handleNonJAXBEntity(boolean isError, Request request, final InputStream is, Response response) throws IOException, ClientException {
		if (isError) {
			clientImpl.getExceptionThrower().throwException(null, response.getStatusCode(), request.getMethod(), request.getUrl());
			throw new ClientException("could not extract entity because the media " + response.getEntityContentType() + " is not supported.");
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(is, bos);
			bos.flush();
			bos.close();
			String contentEncoding = response.getEntityContentEncoding();
			if (contentEncoding == null) {
				contentEncoding = "UTF-8";
			}
			String responseContent = new String(bos.toByteArray(), contentEncoding);
			throw new ClientException("could not extract entity because the media " + response.getEntityContentType() + " is not supported. content: " + responseContent);
		}
	}

	private Object handleJAXBEntity(final InputStream is, final boolean isError, int sc, Request request) throws ClientException {
		Object unmarshalledEntity;
		try {
			JAXBContext context = clientImpl.getJAXBContext();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			XMLReader xmlReader = spf.newSAXParser().getXMLReader();
			InputSource inputSource = new InputSource(is);
			SAXSource source = new SAXSource(xmlReader, inputSource);
			
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshalledEntity = unmarshaller.unmarshal(source);
			if (isError) {
				clientImpl.getExceptionThrower().throwException(unmarshalledEntity, sc, request.getMethod(), request.getUrl());
				return null;
			} else {
				if (responseType != null) {
					return responseType.cast(unmarshalledEntity);
				} else {
					return unmarshalledEntity;
				}
			}
		} catch (ClientException e) {
			// just re-throw it.
			throw e;
		} catch (Exception e) {
			throw new ClientException("could not unmarshall response entity: " + e.getMessage(), e);
		}
	}

	private Object handleRedirectResponse(Response response) throws ClientException {
		if (response.isHavingEntity()) {
			response.consumeEntitySilently();
		}
		String locationUrl = response.getHeaderValue(HttpHeaders.LOCATION);
		if (locationUrl == null) {
			throw new ClientException("a resource was created, but the server did not respond with a location header");
		}
		if (!preventRedirect) {
			RequestBuilderImpl builder = clientImpl.createRequestBuilder();
			builder.url(locationUrl).get();
			HATEOASConfiguredClient<T> client = new HATEOASConfiguredClientImpl<>(builder, clientImpl);
			return client.execute();
		} else {
			return locationUrl;
		}
	}

	private void writeEntityToOutputStream(Response response, OutputStream responseOutputStream) throws ClientException {
		if (response.isHavingEntity()) {
			try (InputStream content = response.getEntityData()) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = content.read(buffer)) != -1) {
					responseOutputStream.write(buffer, 0, len);
				}
				responseOutputStream.flush();
			} catch (IOException | IllegalStateException ex) {
				throw new ClientException("failed to write entity to output stream", ex);
			} finally {
				response.consumeEntitySilently();
			}
		}
	}

	@Override
	public T execute(ReplayableInputStream payload) throws ClientException {
		this.payload = payload;
		final RequestImpl request = buildRequest();
		return executeRequest(request, new ResponseCallback<T>() {
			@Override
			public T doWithResponse(Response response) throws ClientException {
				Object entity = extractEntityFromResponse(response, request);
				if (entity == null) {
					return null;
				}
				return (T) entity;
			}
		});
	}
	
}
