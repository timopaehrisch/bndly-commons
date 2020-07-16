package org.bndly.search.impl;

/*-
 * #%L
 * Search Impl
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

import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.search.api.SearchServiceListener;
import java.util.concurrent.locks.Lock;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
final class SolrInstanceInitializer implements Runnable/*Callable<SolrServerFactory.SolrInstance>*/ {
	private final SolrServerFactory.SolrInstance solrInstance;
	private final BundleContext bundleContext;
	private final Lock listenerLock;
	private final Iterable<SearchServiceListener> listeners;
	private final HttpSolrServer queryServer;
	private final HttpSolrServer updateServer;
	private final SolrConfiguration configuration;
	private boolean didInit;

	private static final Logger LOG = LoggerFactory.getLogger(SolrInstanceInitializer.class);
	
	public SolrInstanceInitializer(SolrServerFactory.SolrInstance solrInstance, BundleContext bundleContext, Lock listenerLock, Iterable<SearchServiceListener> listeners) {
		if (solrInstance == null) {
			throw new IllegalArgumentException("solrInstance is not allowed to be null");
		}
		this.solrInstance = solrInstance;
		if (bundleContext == null) {
			throw new IllegalArgumentException("bundleContext is not allowed to be null");
		}
		this.bundleContext = bundleContext;
		if (listenerLock == null) {
			throw new IllegalArgumentException("listenerLock is not allowed to be null");
		}
		this.listenerLock = listenerLock;
		if (listeners == null) {
			throw new IllegalArgumentException("listeners is not allowed to be null");
		}
		this.listeners = listeners;
		queryServer = solrInstance.getQueryServer();
		updateServer = solrInstance.getUpdateServer();
		configuration = solrInstance.getConfiguration();
	}
	
	@Override
	public void run() {
		if (didInit) {
			return;
		}
		try {
			SolrPingResponse r = queryServer.ping();
			if (r != null) {
				// register the solr servers
				ServiceRegistration<SolrServer> queryServerRegistration = ServiceRegistrationBuilder.newInstance(SolrServer.class, queryServer)
						.pid(SolrServer.class.getName() + ".query." + configuration.getName())
						.property("serverName", configuration.getName())
						.property("query", Boolean.TRUE)
						.register(bundleContext);
				solrInstance.addRegistration(queryServerRegistration);

				ServiceRegistration<SolrServer> updateServerRegistration = ServiceRegistrationBuilder.newInstance(SolrServer.class, updateServer)
						.pid(SolrServer.class.getName() + ".update." + configuration.getName())
						.property("serverName", configuration.getName())
						.property("update", Boolean.TRUE)
						.register(bundleContext);
				solrInstance.addRegistration(updateServerRegistration);

				didInit = true;
				solrInstance.didInit();

				listenerLock.lock();
				try {
					for (SearchServiceListener searchServiceListener : listeners) {
						try {
							searchServiceListener.searchServiceIsReady(configuration.getName());
						} catch (Exception ex) {
							LOG.error("search service listener failed while handling ready event: " + ex.getMessage(), ex);
						}
					}
				} finally {
					listenerLock.unlock();
				}
			}
		} catch (Exception e) {
			// yes, we catch anything here
			LOG.error("could not initialize solr instance: " + e.getMessage(), e);
		}
	}

}
