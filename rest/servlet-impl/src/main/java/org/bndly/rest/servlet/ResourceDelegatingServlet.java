package org.bndly.rest.servlet;

/*-
 * #%L
 * REST Servlet Impl
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

import org.bndly.de.rest.jetty.bridge.JettyBridge;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.base.ResourceDelegatingServletBase;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceDelegatingServlet.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ResourceDelegatingServlet.Configuration.class)
public class ResourceDelegatingServlet implements Servlet {

	private static final Logger LOG = LoggerFactory.getLogger(ResourceDelegatingServlet.class);

	@Reference
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;

	@ObjectClassDefinition(
			name = "Resource Delegating Servlet"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Context Path",
				description = "context path for the servlet. starts with a slash. e.g.: /bndly"
		)
		String contextPath() default "/bndly";

		@AttributeDefinition(
				name = "Link URI",
				description = "URI to use as a base for building links. If empty, the links will be created with the current request URI"
		)
		String linkUri();

	}

	@Reference
	private ResourceDelegatingServletBase resourceDelegatingServletBase;

	private ServletConfig servletConfig;

	@Reference
	private JettyBridge jettyBridge;

	private final List<RegisteredFilter> registeredFilters = new ArrayList<>();
	private final ReadWriteLock filtersLock = new ReentrantReadWriteLock();

	@Reference
	private ContextProvider contextProvider;

	private String contextPath;
	private ResourceURI linkUri;
	private ResourceURI contextUri;
	private boolean didActivate;

	private class RegisteredFilter {

		private final Filter filter;
		private String pattern;
		private boolean installed;

		public RegisteredFilter(Filter filter) {
			this.filter = filter;
		}

		public Filter getFilter() {
			return filter;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return pattern;
		}

		public void install() {
			if (didActivate && !installed) {
				_registerFilter(filter);
				installed = true;
			}
		}

		public void uninstall() {
			if (didActivate && installed) {
				_unregisterFilter(filter);
			}
		}

	}

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			bind = "addFilter",
			unbind = "removeFilter",
			service = Filter.class
	)
	public void addFilter(Filter filter) {
		if (filter != null) {
			filtersLock.writeLock().lock();
			try {
				RegisteredFilter rf = new RegisteredFilter(filter);
				registeredFilters.add(rf);
				rf.install();
			} finally {
				filtersLock.writeLock().unlock();
			}
		}
	}

	public void removeFilter(Filter filter) {
		if (filter != null) {
			filtersLock.writeLock().lock();
			try {
				Iterator<RegisteredFilter> iter = registeredFilters.iterator();
				while (iter.hasNext()) {
					RegisteredFilter rf = iter.next();
					if (rf.getFilter() == filter) {
						rf.uninstall();
						iter.remove();
					}
				}
			} finally {
				filtersLock.writeLock().unlock();
			}
		}
	}

	@Activate
	public void activate(Configuration configuration) {
		contextPath = configuration.contextPath();
		contextUri = new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), contextPath).parse().getResourceURI();
		String linkUriString = configuration.linkUri();
		if (linkUriString != null) {
			linkUri = new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), linkUriString).parse().getResourceURI();
		}

		LOG.info("Starting resource delegating servlet");

		jettyBridge.deployServlet(contextPath, this);
		didActivate = true;
		LOG.info("Started resource delegating servlet");
		filtersLock.readLock().lock();
		try {
			for (RegisteredFilter registeredFilter : registeredFilters) {
				registeredFilter.install();
			}
		} finally {
			filtersLock.readLock().unlock();
		}
	}

	private void _registerFilter(Filter filter) {
		jettyBridge.deployFilter(contextPath, filter);
	}

	private void _unregisterFilter(Filter filter) {
		jettyBridge.undeployFilter(filter);
	}

	@Deactivate
	public void deactivate() {
		try {
			filtersLock.writeLock().lock();
			try {
				for (RegisteredFilter filter : registeredFilters) {
					filter.uninstall();
				}
				registeredFilters.clear();
			} finally {
				filtersLock.writeLock().unlock();
			}
			jettyBridge.undeployServlet(this);
			didActivate = false;
		} catch (Exception e) {
			LOG.error("failed to deactivate resource delegating servlet: " + e.getMessage(), e);
		}
	}

	@Override
	public void init(ServletConfig sc) throws ServletException {
		servletConfig = sc;
	}

	@Override
	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	@Override
	public void service(ServletRequest sr, ServletResponse sr1) throws ServletException, IOException {
		resourceDelegatingServletBase.service(sr, sr1, linkUri, contextUri, servletConfig);
	}

	@Override
	public String getServletInfo() {
		return "ResourceDelegatingServlet";
	}

	@Override
	public void destroy() {
		servletConfig = null;
	}
}
