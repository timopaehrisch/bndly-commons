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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.solr.client.solrj.SolrServer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractSolrServerTracker implements ServiceTrackerCustomizer<SolrServer, SolrServer> {
	
	protected final ReadWriteLock lock = new ReentrantReadWriteLock();
	protected final Map<String, SolrServer> updateServers = new HashMap<>();
	protected final Map<String, SolrServer> queryServers = new HashMap<>();
	private ServiceTracker<SolrServer, SolrServer> tracker;
	
	protected abstract ComponentContext getComponentContext();

	protected final void startTracking() {
		tracker = new ServiceTracker<SolrServer, SolrServer>(getComponentContext().getBundleContext(), SolrServer.class, this);
		tracker.open();
	}

	protected final void stopTracking() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
	}
	
	@Override
	public final SolrServer addingService(ServiceReference<SolrServer> sr) {
		lock.writeLock().lock();
		try {
			SolrServer service = getComponentContext().getBundleContext().getService(sr);
			String serverName = (String) sr.getProperty("serverName");
			if (serverName != null) {
				if (sr.getProperty("update") == Boolean.TRUE) {
					updateServers.put(serverName, service);
				} else if (sr.getProperty("query") == Boolean.TRUE) {
					queryServers.put(serverName, service);
				}
			}
			return service;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public final void modifiedService(ServiceReference<SolrServer> sr, SolrServer service) {
	}

	@Override
	public final void removedService(ServiceReference<SolrServer> sr, SolrServer service) {
		lock.writeLock().lock();
		try {
			String serverName = (String) sr.getProperty("serverName");
			if (serverName != null) {
				if (sr.getProperty("update") == Boolean.TRUE) {
					updateServers.remove(serverName);
				} else if (sr.getProperty("query") == Boolean.TRUE) {
					queryServers.remove(serverName);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
}
