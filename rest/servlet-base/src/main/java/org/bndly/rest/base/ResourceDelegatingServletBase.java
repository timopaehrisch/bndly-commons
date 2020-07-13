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

import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.Header;
import org.bndly.rest.api.QuantifiedContentType;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceBuildingException;
import org.bndly.rest.api.ResourceInterceptor;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.api.SecurityContext;
import org.bndly.rest.api.StatusWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceDelegatingServletBase.class, immediate = true)
public class ResourceDelegatingServletBase {

	private static final Logger LOG = LoggerFactory.getLogger(ResourceDelegatingServletBase.class);

	private final List<ResourceRenderer> renderers = new ArrayList<>();
	private final ReadWriteLock renderersLock = new ReentrantReadWriteLock();
	
	private final List<ResourceProvider> providers = new ArrayList<>();
	private final ReadWriteLock providersLock = new ReentrantReadWriteLock();
	
	private final DelegatingResourceRenderer renderer = new DelegatingResourceRenderer();
	private final DelegatingResourceProviderImpl provider = new DelegatingResourceProviderImpl();
	@Reference
	private ContextProvider contextProvider;
	@Reference
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;
	private final List<ResourceInterceptor> interceptors = new ArrayList<>();
	private final ReadWriteLock interceptorsLock = new ReentrantReadWriteLock();
	
	@Reference(
			cardinality = ReferenceCardinality.OPTIONAL,
			policy = ReferencePolicy.DYNAMIC,
			service = FileExtensionContentTypeMapper.class
	)
	volatile private FileExtensionContentTypeMapper fileExtensionContentTypeMapper;
	private final DelegatingResourceInterceptor interceptor = new DelegatingResourceInterceptor();

	private final QuantifiedContentTypeFactory quantifiedContentTypeFactory = new QuantifiedContentTypeFactory() {

		@Override
		public QuantifiedContentType createQuantifiedSomething(Float quantity, String data) {
			FileExtensionContentTypeMapper tmp = fileExtensionContentTypeMapper;
			if (tmp != null) {
				final String ext = tmp.mapContentTypeToExtension(data);
				return new QuantifiedContentTypeImpl(quantity, data, ext);
			} else {
				return super.createQuantifiedSomething(quantity, data);
			}
		}
		
	};

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			bind = "addRenderer",
			unbind = "removeRenderer",
			service = ResourceRenderer.class
	)
	public void addRenderer(ResourceRenderer resourceRenderer) {
		if (resourceRenderer != null) {
			renderersLock.writeLock().lock();
			try {
				renderers.add(resourceRenderer);
				renderer.addResourceRenderer(resourceRenderer);
			} finally {
				renderersLock.writeLock().unlock();
			}
		}
	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			bind = "addProvider",
			unbind = "removeProvider",
			service = ResourceProvider.class
	)
	public void addProvider(ResourceProvider resourceProvider) {
		if (resourceProvider != null) {
			providersLock.writeLock().lock();
			try {
				providers.add(resourceProvider);
				provider.addResourceProvider(resourceProvider);
			} finally {
				providersLock.writeLock().unlock();
			}
		}
	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			bind = "addInterceptor",
			unbind = "removeInterceptor",
			service = ResourceInterceptor.class
	)
	public void addInterceptor(ResourceInterceptor resourceInterceptor) {
		if (resourceInterceptor != null) {
			interceptorsLock.writeLock().lock();
			try {
				interceptors.add(resourceInterceptor);
				interceptor.addResourceInterceptor(resourceInterceptor);
			} finally {
				interceptorsLock.writeLock().unlock();
			}
		}
	}

	public void removeRenderer(ResourceRenderer resourceRenderer) {
		if (resourceRenderer != null) {
			renderersLock.writeLock().lock();
			try {
				renderers.remove(resourceRenderer);
				renderer.removeResourceRenderer(resourceRenderer);
			} finally {
				renderersLock.writeLock().unlock();
			}
		}
	}

	public void removeProvider(ResourceProvider resourceProvider) {
		if (resourceProvider != null) {
			providersLock.writeLock().lock();
			try {
				providers.remove(resourceProvider);
				provider.removeResourceProvider(resourceProvider);
			} finally {
				providersLock.writeLock().unlock();
			}
		}
	}

	public void removeInterceptor(ResourceInterceptor resourceInterceptor) {
		if (resourceInterceptor != null) {
			interceptorsLock.writeLock().lock();
			try {
				interceptors.remove(resourceInterceptor);
				interceptor.removeResourceInterceptor(resourceInterceptor);
			} finally {
				interceptorsLock.writeLock().unlock();
			}
		}
	}

	@Deactivate
	public void deactivate() {
		try {
			renderersLock.writeLock().lock();
			try {
				renderer.clear();
				renderers.clear();
			} finally {
				renderersLock.writeLock().unlock();
			}
			providersLock.writeLock().lock();
			try {
				provider.clear();
				providers.clear();
			} finally {
				providersLock.writeLock().unlock();
			}
			interceptorsLock.writeLock().lock();
			try {
				interceptor.clear();
				interceptors.clear();
			} finally {
				interceptorsLock.writeLock().unlock();
			}
		} catch (Exception e) {
			LOG.error("failed to deactivate resource delegating servlet base: " + e.getMessage(), e);
		}
	}

	public void service(ServletRequest sr, ServletResponse sr1, ResourceURI linkUri, ResourceURI contextUri, ServletConfig servletConfig) throws ServletException, IOException {
		if (HttpServletRequest.class.isInstance(sr)) {
			sr = logIncommingRequest((HttpServletRequest) sr);
		}
		ResourceURI uri = buildResourceURI(sr);
		if (uri == null) {
			// return error: wrong protocol
			return;
		}
		try (ContextImpl contextImpl = new ContextImpl(sr, sr1, uri, linkUri, servletConfig.getServletContext(), contextUri, defaultCharacterEncodingProvider)) {
			contextImpl.setQuantifiedContentTypeFactory(quantifiedContentTypeFactory);
			Context context = contextImpl;
			context.getStatusWriter().write(StatusWriter.Code.OK);
			ContextProvider localProvider = getContextProvider();
			if (localProvider != null) {
				localProvider.setCurrentContext(context);
			}

			ResourceInterceptor localInterceptor = getResourceInterceptor();
			interceptorsLock.readLock().lock();
			try {
				if (localInterceptor != null) {
					localInterceptor.beforeResourceResolving(context);
				}
				
				SecurityContext sc = context.getSecurityContext();
				if (sc == null || !sc.isServableContext()) {
					if (context.getStatus() == null || context.getStatus() == StatusWriter.Code.OK) {
						context.getStatusWriter().write(StatusWriter.Code.FORBIDDEN);
					}
					return;
				}

				if (context.canBeServedFromCache()) {
					context.serveFromCache();
				} else {
					if (context.canBeCached()) {
						ContextWrapperImpl wrapped = new CacheWritingContextWrapper(context);
						context = wrapped;
						if (localProvider != null) {
							localProvider.setCurrentContext(wrapped);
						}
					}
					ResourceProvider resourceProvider = getResourceProvider();
					ResourceRenderer resourceRenderer = getResourceRenderer();
					providersLock.readLock().lock();
					try {
						if (resourceProvider.supports(context)) {
							Resource resource;
							try {
								resource = resourceProvider.build(context, resourceProvider);
							} catch (ResourceBuildingException e) {
								resource = handleResourceBuildingException(e);
							}
							if (resource != null) {
								if (localInterceptor != null) {
									long start = System.currentTimeMillis();
									try {
										resource = localInterceptor.intercept(resource);
									} finally {
										long end = System.currentTimeMillis();
										LOG.trace("intercepting resource took {}ms", (end - start));
									}
								}
								renderersLock.readLock().lock();
								try {
									if (resourceRenderer.supports(resource, context)) {
										try {
											long start = System.currentTimeMillis();
											try {
												resourceRenderer.render(resource, context);
											} catch (Exception e) {
												LOG.error("failed to render resource: {}", resource == null ? null : resource.getURI().asString(), e);
												CacheContext cc = context.getCacheContext();
												if (cc != null) {
													cc.preventCache();
												}
												context.getStatusWriter().write(StatusWriter.Code.INTERNAL_SERVER_ERROR);
												context.setOutputContentType(ContentType.TEXT, "UTF-8");
												context.getOutputStream().write(("INTERNAL ERROR: " + e.getMessage()).getBytes("UTF-8"));
											} finally {
												long end = System.currentTimeMillis();
												LOG.trace("rendering resource took {}ms", (end - start));
											}
										} finally {
											try (OutputStream os = context.getOutputStream()) {
												os.flush();
											}
										}
									} else {
										// return error: no renderer for resource found
									}
								} finally {
									renderersLock.readLock().unlock();
								}
							}
						} else {
							// return error: resource not found
						}
					} finally {
						providersLock.readLock().unlock();
					}
				}
			} finally {
				try {
					try {
						if (localInterceptor != null) {
							localInterceptor.doFinally(context);
						}
					} finally {
						logOutgoingResponse(context);
						if (localProvider != null) {
							localProvider.setCurrentContext(null);
						}
					}
				} finally {
					interceptorsLock.readLock().unlock();
				}
			}
		} catch (Exception e) {
			LOG.error("exception while dealing with context implementation: " + e.getMessage(), e);
		}
	}

	private Resource handleResourceBuildingException(ResourceBuildingException e) {
		LOG.warn("failed to build resource {}" + e.getContext().getURI().asString(), e);
		StatusWriter sw = e.getContext().getStatusWriter();
		sw.write(StatusWriter.Code.INTERNAL_SERVER_ERROR);
		return null;
	}
	
	private void logOutgoingResponse(Context logContext) {
		if (LOG.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append(logContext.getStatus().getHttpCode()).append(" ").append(logContext.getStatus().toString());
			Iterator<Header> it = logContext.getHeaderWriter().getWrittenHeaders();
			while (it.hasNext()) {
				Header next = it.next();
				sb.append("\n");
				sb.append(next.getName());
				String headerValue = next.getValue();
				if (headerValue != null) {
					sb.append(": ").append(headerValue);
				}
			}
			LOG.debug(sb.toString());
		}
	}
	
	private HttpServletRequest logIncommingRequest(HttpServletRequest req) {
		if (LOG.isDebugEnabled()) {
			StringWriter sb = new StringWriter();
			sb.append(req.getMethod()).append(" ").append(req.getRequestURL());
			String qs = req.getQueryString();
			if (qs != null) {
				sb.append("?").append(qs);
			}
			Enumeration n = req.getHeaderNames();
			while (n.hasMoreElements()) {
				sb.append("\n");
				String headerName = (String) n.nextElement();
				sb.append(headerName);
				String headerValue = req.getHeader(headerName);
				if (headerValue != null) {
					sb.append(": ").append(headerValue);
				}
			}
			try {
				final ReplayableInputStream ris = ReplayableInputStream.newInstance(req.getInputStream());
				String data = IOUtils.readToString(ris, "UTF-8");
				sb.append("\n").append(data);
				return new HttpServletRequestWrapper(req) {

					@Override
					public ServletInputStream getInputStream() throws IOException {
						ris.doReplay();
						
						return new ServletInputStream() {

							@Override
							public int read() throws IOException {
								return ris.read();
							}
						};
					}
					
				};
			} catch (IOException ex) {
				LOG.error("could not log the incoming request data");
			} finally {
				LOG.debug(sb.toString());
			}
		}
		return req;
	}

	private ResourceURI buildResourceURI(ServletRequest sr) {
		if (HttpServletRequest.class.isInstance(sr)) {
			HttpServletRequest req = (HttpServletRequest) sr;
			String uriAsString = req.getRequestURL().toString();
			if (req.getQueryString() != null && !"".equals(req.getQueryString())) {
				uriAsString += '?' + req.getQueryString();
			}
			return new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), uriAsString).parse().getResourceURI();
		} else {
			return null;
		}
	}

	protected ResourceProvider getResourceProvider() {
		return provider;
	}

	protected ResourceRenderer getResourceRenderer() {
		return renderer;
	}

	protected ResourceInterceptor getResourceInterceptor() {
		return interceptor;
	}

	protected ContextProvider getContextProvider() {
		return contextProvider;
	}

}
