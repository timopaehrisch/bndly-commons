package org.bndly.common.cors.impl;

/*-
 * #%L
 * CORS
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

import org.bndly.common.cors.api.HeaderProducer;
import org.bndly.common.cors.api.HeaderWriter;
import org.bndly.common.cors.api.CORSRequestDetector;
import org.bndly.common.cors.api.Origin;
import org.bndly.common.cors.api.OriginService;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is for supporting CORS in modern webbrowsers. This filter will
 * allow controller Cross Site Requests to this application.
 */
@Component(service = { Filter.class, CORSRequestDetector.class }, immediate = true)
public class AccessControlFilter implements Filter, CORSRequestDetector, HeaderProducer {
	private static final Logger LOG = LoggerFactory.getLogger(AccessControlFilter.class);
	private static final String REQ_ORIGIN = "Origin";
	private static final String REQ_METHOD = "Access-Control-Request-Method";
	private static final String REQ_HEADERS = "Access-Control-Request-Headers";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
	@Reference
	private OriginService originService;
	private final ThreadLocal<CORSContext> corsContexts = new ThreadLocal<>();

	@Override
	public void init(FilterConfig fc) throws ServletException {
	}

	public void setOriginService(OriginService originService) {
		this.originService = originService;
	}

	public void unsetOriginService(OriginService originService) {
		if (originService == this.originService) {
			this.originService = null;
		}
	}

	@Activate
	public void activate() {
		LOG.info("activating CORS filtering");
	}

	@Deactivate
	public void deactivate() {
		LOG.info("deactivating CORS filtering");
	}

	@Override
	public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
		if (originService == null) {
			fc.doFilter(sr, sr1);
			return;
		}

		CORSContext ctx = new CORSContext();
		corsContexts.set(ctx);
		try {
			boolean isPreflightRequest = false;
			if (HttpServletRequest.class.isInstance(sr)) {
				HttpServletRequest request = HttpServletRequest.class.cast(sr);
				final HttpServletResponse response = HttpServletResponse.class.cast(sr1);
				String originString = request.getHeader(REQ_ORIGIN);
				if (originString != null) {
					ctx.setOrigin(originString);
					ctx.setRequestCORS(true);
					String method = request.getMethod();
					isPreflightRequest = method.equals("OPTIONS");
					ctx.setPreflight(isPreflightRequest);

					String preflightHeaders = request.getHeader(REQ_HEADERS);
					if (preflightHeaders != null) {
						String[] tmp = preflightHeaders.split(",");
						for (String headerCandidate : tmp) {
                            // you can add a selection of allowed headers here
							// for now, i will just add all headers
							ctx.getAllowedHeaders().add(headerCandidate);
						}
					}

					boolean accepted = false;
					try {
						URL url = new URL(originString);
						Origin origin = originService.newInstance();
						origin.setProtocol(url.getProtocol());
						origin.setDomainName(url.getHost());
						Integer port = url.getPort();
						if (port == null || port == -1) {
							port = url.getDefaultPort();
						}
						origin.setPort(port);

						accepted = originService.isAcceptedOrigin(origin);
					} catch (java.net.MalformedURLException e) {
						LOG.warn("malformed URL: {}", originString);
					} catch (Exception e) {
						LOG.error("could not determine if request origin is accepted: " + e.getMessage(), e);
					}

					if (accepted) {
						Set<String> exposedHeaders = ctx.getExposedHeaders();
						exposedHeaders.add("Location");
						exposedHeaders.add("Content-Type");

						Set<String> allowedMethods = ctx.getAllowedMethods();
						allowedMethods.add(request.getMethod());
						allowedMethods.add("GET");
						allowedMethods.add("POST");
						allowedMethods.add("PUT");
						allowedMethods.add("DELETE");
						allowedMethods.add("HEAD");
						if (isPreflightRequest) {
							response.setStatus(204);
						}
					}
				}

				produceHeaders(new HeaderWriter() {
					@Override
					public void write(String name, String value) {
						if (name != null && value != null) {
							response.addHeader(name, value);
						}
					}
				});
				if (!isPreflightRequest) {
					fc.doFilter(sr, sr1);
				}

			} else {
				// any non HTTP servlet request is just forwarded
				fc.doFilter(sr, sr1);
			}
		} finally {
			corsContexts.remove();
		}
	}

	@Override
	public void produceHeaders(HeaderWriter writer) {
		CORSContext ctx = corsContexts.get();
		writer.write(ALLOW_CREDENTIALS, "true");
		StringBuffer allowedHeadersSb = concatenateStringSet(ctx.getAllowedHeaders());
		if (allowedHeadersSb != null) {
			writer.write(ALLOW_HEADERS, allowedHeadersSb.toString());
		}
		String origin = ctx.getOrigin();
		writer.write(ALLOW_ORIGIN, origin);
		StringBuffer exposedHeadersSb = concatenateStringSet(ctx.getExposedHeaders());
		if (exposedHeadersSb != null) {
			writer.write(EXPOSE_HEADERS, exposedHeadersSb.toString());
		}

		StringBuffer allowedMethodsSb = concatenateStringSet(ctx.getAllowedMethods());
		if (allowedMethodsSb != null) {
			writer.write(ALLOW_METHODS, allowedMethodsSb.toString());
		}
	}

	@Override
	public void destroy() {
	}

	private StringBuffer concatenateStringSet(Set<String> allowedHeaders) {
		StringBuffer aH = null;
		for (String header : allowedHeaders) {
			if (aH == null) {
				aH = new StringBuffer();
			} else {
				aH.append(", ");
			}
			aH.append(header);
		}
		return aH;
	}

	@Override
	public boolean isCORSRequest() {
		CORSContext ctx = corsContexts.get();
		if (ctx == null) {
			return false;
		}
		return ctx.isRequestCORS();
	}

}
