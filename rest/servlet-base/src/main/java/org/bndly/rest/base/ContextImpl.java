package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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
import org.bndly.rest.api.ByteServingContext;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.CacheHandler;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.Header;
import org.bndly.rest.api.HeaderReader;
import org.bndly.rest.api.HeaderWriter;
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.api.QuantifiedContentType;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.api.ResourceURIBuilderImpl;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.api.SecurityContext;
import org.bndly.rest.api.SecurityHandler;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.api.StatusWriter.Code;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ContextImpl implements Context, AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(ContextImpl.class);
	
	private final ServletRequest request;
	private final ServletResponse response;
	private final HTTPMethod httpMethod;
	private final ResourceURI uri;
	private final ResourceURI linkUri;
	private final ServletContext servletContext;
	private final ResourceURI localUri;
	private final ResourceURI servletContextUri;
	private final DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;
	private CacheContext cacheContext;
	private SecurityContext securityContext;
	private ByteServingContext byteServingContext;
	private HeaderWriter headerWriter;
	private CacheHandler cacheHandler;
	private SecurityHandler securityHandler;
	private boolean preventedCaching;
	private ContentType outputContentType;
	private Code statusCode;
	private List<QuantifiedContentType> quantifiedAcceptedContentTypes;
	private QuantifiedContentTypeFactory quantifiedContentTypeFactory;
	private boolean didParseAcceptedContentTypes;

	private static final QuantifiedLocaleFactory QUANTIFIED_LOCALE_FACTORY = new QuantifiedLocaleFactory();
	private boolean didLookForLocale;
	private Locale requestLocale;
	private ReplayableInputStream replayableInputStream;
	private HeaderReader headerReader;
	private StatusWriter statusWriter;
	private String charEncoding;
	private String outputEncoding;
	private ContentType inputContentType;
	private boolean didLookForInputContentType;

	public ContextImpl(
			ServletRequest request, 
			ServletResponse response, 
			ResourceURI uri, 
			ResourceURI linkUri, 
			ServletContext servletContext, 
			ResourceURI servletContextUri, 
			DefaultCharacterEncodingProvider defaultCharacterEncodingProvider
	) {
		this.defaultCharacterEncodingProvider = defaultCharacterEncodingProvider;
		this.request = request;
		this.response = response;
		this.servletContext = servletContext;
		if (HttpServletRequest.class.isInstance(request)) {
			HttpServletRequest req = (HttpServletRequest) request;
			httpMethod = HTTPMethod.valueOf(req.getMethod());
		} else {
			httpMethod = null;
		}
		this.servletContextUri = servletContextUri;
		this.uri = uri;
		this.linkUri = linkUri == null ? uri : linkUri;
		this.localUri = buildLocalResourceURI(uri);
	}

	@Override
	public ResourceURI getRequestURI() {
		return uri;
	}

	public boolean isHttpRequest() {
		return HttpServletRequest.class.isInstance(request);
	}

	public boolean isHttpResponse() {
		return HttpServletResponse.class.isInstance(response);
	}

	private HttpServletRequest getHttpRequest() {
		return HttpServletRequest.class.cast(request);
	}

	private HttpServletResponse getHttpResponse() {
		return HttpServletResponse.class.cast(response);
	}

	@Override
	public HTTPMethod getMethod() {
		return httpMethod;
	}

	@Override
	public ReplayableInputStream getInputStream() throws IOException {
		if (replayableInputStream == null) {
			replayableInputStream = ReplayableInputStream.newInstance(request.getInputStream());
		}
		return replayableInputStream;
	}

	@Override
	public PathCoder createPathCoder() {
		String inputEncoding = getInputEncoding();
		if ("UTF-8".equalsIgnoreCase(inputEncoding)) {
			return new PathCoder.UTF8();
		} else if ("ISO-8859-1".equalsIgnoreCase(inputEncoding)) {
			return new PathCoder.ISO88591();
		} else {
			return new PathCoder.UTF8();
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return response.getOutputStream();
	}

	public void setHeaderWriter(HeaderWriter headerWriter) {
		this.headerWriter = headerWriter;
	}

	@Override
	public HeaderReader getHeaderReader() {
		if (headerReader == null) {
			headerReader = new HeaderReader() {

				@Override
				public String read(String name) {
					if (isHttpRequest()) {
						return getHttpRequest().getHeader(name);
					}
					return null;
				}
			};
		}
		return headerReader;
	}

	@Override
	public StatusWriter getStatusWriter() {
		if (statusWriter == null) {
			statusWriter = new StatusWriter() {

				@Override
				public StatusWriter write(StatusWriter.Code code) {
					statusCode = code;
					if (code != null && isHttpResponse()) {
						getHttpResponse().setStatus(code.getHttpCode());
					}
					return this;
				}
			};
		}
		return statusWriter;
	}

	@Override
	public HeaderWriter getHeaderWriter() {
		if (headerWriter == null) {
			headerWriter = new HeaderWriter() {
				private final List<Header> headers = new ArrayList<>();

				@Override
				public HeaderWriter write(final String name, final String value) {
					if (isHttpResponse()) {
						headers.add(new Header() {

							@Override
							public String getName() {
								return name;
							}

							@Override
							public String getValue() {
								return value;
							}
						});
						if (getHttpResponse().isCommitted()) {
							LOG.warn("could not set header {} because response is already commited", name);
						} else {
							getHttpResponse().setHeader(name, value);
						}
					}
					return this;
				}

				@Override
				public Iterator<Header> getWrittenHeaders() {
					return headers.iterator();
				}

			};
		}
		return headerWriter;
	}

	@Override
	public ResourceURIBuilder createURIBuilder() {
		ResourceURIBuilderImpl builder;
		if (linkUri == uri) {
			builder = new ResourceURIBuilderImpl(createPathCoder())
					.scheme(linkUri.getScheme())
					.host(linkUri.getHost())
					.port(linkUri.getPort());
			ResourceURI.Path slp = servletContextUri.getPath();
			if (slp != null) {
				for (String string : slp.getElements()) {
					builder.pathElement(string);
				}
			}
		} else {
			builder = new ResourceURIBuilderImpl(createPathCoder(), linkUri);
		}

		return builder;
	}

	@Override
	public ResourceURI getURI() {
		return localUri;
	}

	@Override
	public ContentType getInputContentType() {
		if (didLookForInputContentType) {
			return inputContentType;
		}
		didLookForInputContentType = true;
		final String ct = request.getContentType();
		if (ct == null) {
			return null;
		}
		String marker = "; charset=";
		final String[] split = ct.split(";");
		if (split.length > 0) {
			inputContentType = new ContentType() {

				@Override
				public String getName() {
					return split[0];
				}

				@Override
				public String getExtension() {
					return null;
				}
			};
		} else {
			inputContentType = null;
		}
		return inputContentType;
	}

	@Override
	public void setOutputContentType(ContentType contentType) {
		setOutputContentType(contentType, null);
	}

	@Override
	public void setOutputContentType(ContentType contentType, String providedCharset) {
		if (contentType != null) {
			String charset = ContentTypeParser.getCharsetFromContentTypeString(contentType.getName());
			if (providedCharset == null) {
				if (charset != null) {
					// there is no charset provided, but the content type contains a charset
					contentType = ContentTypeParser.getContentTypeFromString(contentType.getName());
					response.setContentType(contentType.getName() + "; charset=" + charset);
					response.setCharacterEncoding(charset);
					providedCharset = charset;
				} else {
					response.setContentType(contentType.getName());
				}
			} else {
				if (charset != null) {
					// re-parse and replace the charset of the content type with the provided charset
					contentType = ContentTypeParser.getContentTypeFromString(contentType.getName());
					response.setContentType(contentType.getName() + "; charset=" + providedCharset);
					response.setCharacterEncoding(providedCharset);
				} else {
					// just set the content type and use the provided encoding
					response.setContentType(contentType.getName() + "; charset=" + providedCharset);
					response.setCharacterEncoding(providedCharset);
				}
			}
			this.outputContentType = contentType;
			outputEncoding = providedCharset;
		}
	}
	
	@Override
	public void setLocation(ResourceURI locationURI) {
		if (isHttpResponse()) {
			getHeaderWriter().write("Location", locationURI.asString());
		}
	}

	@Override
	public Locale getLocale() {
		if (!didLookForLocale) {
			didLookForLocale = true;
			if (isHttpRequest()) {
				HttpServletRequest req = (HttpServletRequest) request;
				String acceptLang = req.getHeader("Accept-Language");
				if (acceptLang != null && !"".equals(acceptLang)) {
					List<QuantifiedLocale> locales = QuantifiedHeaderParser.parseQuantifiedHeader(acceptLang, QUANTIFIED_LOCALE_FACTORY);
					if (locales != null) {
						QuantifiedLocale highest = null;
						for (QuantifiedLocale locale : locales) {
							if (highest == null) {
								highest = locale;
							} else {
								if (highest.getQ() < locale.getQ()) {
									highest = locale;
								}
							}
						}
						if (highest != null) {
							this.requestLocale = highest.getLocale();
						}
					}
				}
			}
			if (requestLocale == null) {
				requestLocale = request.getLocale();
			}
		}
		return requestLocale;
	}

	@Override
	public ContentType getDesiredContentType() {
		List<QuantifiedContentType> contentTypes = getDesiredContentTypes();
		boolean didContainWildCard = false;
		for (QuantifiedContentType act : contentTypes) {
			if (ContentType.WILD_CARD.getName().equals(act.getName())) {
				didContainWildCard = true;
				ResourceURI.Extension ext = getURI().getExtension();
				if (ext != null) {
					if ("json".equals(ext.getName())) {
						return ContentType.JSON;
					} else if ("xml".equals(ext.getName())) {
						return ContentType.XML;
					}
				}
			}
		}
		if (didContainWildCard || contentTypes.isEmpty()) {
			return null;
		}
		return quantifiedAcceptedContentTypes.get(0);
	}

	@Override
	public List<QuantifiedContentType> getDesiredContentTypes() {
		if (isHttpRequest()) {
			HttpServletRequest req = HttpServletRequest.class.cast(request);
			final String acceptHeader = req.getHeader("Accept");
			if (acceptHeader != null) {
				if (!didParseAcceptedContentTypes) {
					didParseAcceptedContentTypes = true;
					quantifiedAcceptedContentTypes = QuantifiedHeaderParser.parseQuantifiedHeader(acceptHeader, quantifiedContentTypeFactory);
				}
				return quantifiedAcceptedContentTypes;
			}
		}
		return Collections.EMPTY_LIST;
	}

	@Override
	public String getInputEncoding() {
		if (charEncoding == null) {
			charEncoding = request.getCharacterEncoding();
			if (charEncoding == null) {
				// see http://www.w3.org/International/O-HTTP-charset
				// this would mean, that we would have to fall back to ISO-8859-1, 
				// but this means that an XML response that contains links should 
				// be remembered as being served in UTF-8 encoding on the client side.
				// this is not very likely, hence we use a configured fallback value.
				charEncoding = defaultCharacterEncodingProvider.getCharacterEncoding();
			}
		}
		return charEncoding;
	}

	@Override
	public Code getStatus() {
		return statusCode;
	}

	@Override
	public void setOutputHeader(String name, String value) {
		if (isHttpResponse()) {
			getHeaderWriter().write(name, value);
		}
	}

	@Override
	public SecurityContext getSecurityContext() {
		if (securityContext != null) {
			return securityContext;
		}
		securityContext = new SecurityContextImpl(this);
		return securityContext;
	}

	@Override
	public CacheContext getCacheContext() {
		if (cacheContext != null) {
			return cacheContext;
		}
		cacheContext = new CacheContextImpl(this);
		return cacheContext;
	}

	@Override
	public ByteServingContext getByteServingContext() {
		if (byteServingContext != null) {
			return byteServingContext;
		}
		byteServingContext = new ByteServingContextImpl(this);
		return byteServingContext;
	}

	private ResourceURI buildLocalResourceURI(ResourceURI uri) {
		ResourceURIBuilderImpl builder = new ResourceURIBuilderImpl(createPathCoder());
		ResourceURI.Path uriPath = uri.getPath();
		ResourceURI.Path servletContextPath = servletContextUri.getPath();
		if (uriPath != null && servletContextPath != null) {
			for (int index = 0; index < uriPath.getElements().size(); index++) {
				String element = uriPath.getElements().get(index);
				if (index < servletContextPath.getElements().size()) {
					if (element.equals(servletContextPath.getElements().get(index))) {
						// fine
					} else {
						throw new IllegalStateException("the provided uri does not match with the current servlet context");
					}
				} else {
					// just append to new uri
					builder.pathElement(element);
				}
			}
		}
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (selectors != null) {
			for (ResourceURI.Selector selector : selectors) {
				builder.selector(selector.getName());
			}
		}
		ResourceURI.Extension ext = uri.getExtension();
		if (ext != null) {
			builder.extension(ext.getName());
		}
		String suffix = uri.getSuffix();
		if (suffix != null) {
			builder.suffix(suffix);
		}
		List<ResourceURI.QueryParameter> params = uri.getParameters();
		if (params != null) {
			for (ResourceURI.QueryParameter queryParameter : params) {
				builder.parameter(queryParameter.getName(), queryParameter.getValue());
			}
		}
		String fragment = uri.getFragment();
		if (fragment != null) {
			builder.fragment(fragment);
		}
		ResourceURI internalUri = builder.build();
		return internalUri;
	}

	@Override
	public ResourceURI parseURI(String uriAsString) {
		return new ResourceURIParser(createPathCoder(), uriAsString).parse().getResourceURI();
	}

	@Override
	public void setSecurityHandler(SecurityHandler securityHandler) {
		this.securityHandler = securityHandler;
	}

	@Override
	public void setCacheHandler(CacheHandler cacheHandler) {
		this.cacheHandler = cacheHandler;
	}

	@Override
	public boolean canBeServedFromCache() {
		if (isCachingPrevented()) {
			return false;
		}
		if (cacheHandler == null) {
			return false;
		}
		return cacheHandler.canBeServedFromCache(this);
	}

	@Override
	public void serveFromCache() {
		if (cacheHandler == null) {
			throw new IllegalStateException("can not serve from cache when there is no cache handler");
		}
		cacheHandler.returnCachedData(this);
	}

	@Override
	public boolean canBeCached() {
		if (isCachingPrevented()) {
			return false;
		}
		if (cacheHandler == null) {
			return false;
		}
		return cacheHandler.canBeCached(this);
	}

	@Override
	public void saveInCache(ReplayableInputStream is) {
		if (cacheHandler == null) {
			throw new IllegalStateException("can not save in cache when there is no cache handler");
		}
		cacheHandler.saveCacheData(is, this);
	}

	@Override
	public void setOutputContentLanguage(String contentLanguage) {
		if (contentLanguage != null) {
			setOutputHeader("Content-Language", contentLanguage);
		}
	}

	@Override
	public ContentType getOutputContentType() {
		return outputContentType;
	}

	@Override
	public String getOutputEncoding() {
		return outputEncoding;
	}

	public void setPreventCache(boolean preventedCaching) {
		this.preventedCaching = preventedCaching;
	}

	public boolean isCachingPrevented() {
		return preventedCaching;
	}

	public SecurityHandler getSecurityHandler() {
		return securityHandler;
	}

	@Override
	public void close() throws Exception {
		if (cacheHandler != null) {
			cacheHandler.close();
			cacheHandler = null;
		}
	}

	public void setQuantifiedContentTypeFactory(QuantifiedContentTypeFactory quantifiedContentTypeFactory) {
		this.quantifiedContentTypeFactory = quantifiedContentTypeFactory;
	}

}
