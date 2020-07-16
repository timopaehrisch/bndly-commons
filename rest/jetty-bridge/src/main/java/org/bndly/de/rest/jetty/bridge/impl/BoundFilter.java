package org.bndly.de.rest.jetty.bridge.impl;

/*-
 * #%L
 * REST Jetty Bridge
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

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BoundFilter implements Stoppable {
	
	final FilterHolder filterHolder;
	final DeployedFilter filter;
	final DeployedServlet servlet;

	public BoundFilter(DeployedFilter deployedFilter, DeployedServlet deployedServlet) {
		filterHolder = new FilterHolder(deployedFilter.getFilter());
		this.filter = deployedFilter;
		this.servlet = deployedServlet;
	}

	public DeployedFilter getFilter() {
		return filter;
	}

	public DeployedServlet getServlet() {
		return servlet;
	}

	@Override
	public void stop() throws Exception {
		filterHolder.stop();
	}

	public void start() throws Exception {
		ServletContextHandler servletContextHandler = servlet.getServletContextHandler();
		servletContextHandler.addFilter(filterHolder, "/*", null); // filter everything that is attached to the sevlet context holder
		filterHolder.start();
	}
	
}
