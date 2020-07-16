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

import javax.servlet.Servlet;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DeployedServlet implements Stoppable {
	private static final Logger LOG = LoggerFactory.getLogger(DeployedServlet.class);
	
	final ContextHandlerCollection contextHandlerCollection;
	final ServletContextHandler servletContextHandler;
	final Servlet servlet;
	final String servletContextPath;

	public DeployedServlet(ContextHandlerCollection contextHandlerCollection, Servlet servlet, String servletContextPath) {
		servletContextHandler = new ServletContextHandler();
		servletContextHandler.setContextPath(servletContextPath);
		servletContextHandler.addServlet(new ServletHolder(servlet), "/*");
		this.servletContextPath = servletContextPath;
		this.servlet = servlet;
		this.contextHandlerCollection = contextHandlerCollection;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public String getServletContextPath() {
		return servletContextPath;
	}
	
	public void start() {
		// the jetty is doing some manual classloading which might fail in 
		// an osgi environement.
		// hence this bundle is already capable to access the required 
		// classes via its classloader and sets the classload to the current 
		// context.
		if (contextHandlerCollection != null) {
			contextHandlerCollection.addHandler(servletContextHandler);
		}
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(servlet.getClass().getClassLoader());
		try {
			servletContextHandler.start();
		} catch (Exception ex) {
			LOG.error("could not start handler: " + ex.getMessage(), ex);
		} finally {
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}

	@Override
	public void stop() throws Exception {
		servletContextHandler.stop();
		if (contextHandlerCollection != null) {
			contextHandlerCollection.removeHandler(servletContextHandler);
		}
	}

	public ServletContextHandler getServletContextHandler() {
		return servletContextHandler;
	}
	
}
