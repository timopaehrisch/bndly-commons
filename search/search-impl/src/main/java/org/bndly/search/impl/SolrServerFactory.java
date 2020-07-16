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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.search.api.SearchServiceListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SolrServerFactory.class, immediate = true)
@Designate(ocd = SolrServerFactory.Configuration.class)
public class SolrServerFactory {

	@ObjectClassDefinition(
			name = "Solr Server Factory",
			description = "This is a factory to create connections to Solr instances"
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Sleep time",
				description = "The amount of milliseconds to wait between solr instance initialization retries."
		)
		int sleepTime() default 5000;
		@AttributeDefinition(
				name = "Thread Count",
				description = "The count of threads to use for initializing Solr connections."
		)
		int threadCount() default 2;
		@AttributeDefinition(
				name = "HTTP Client", 
				description = "An OSGI filter expression to select the HTTP Client service to use for Solr connections."
		)
		String httpClient_target() default "(service.pid=org.apache.http.client.HttpClient.solr)";
		
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(SolrServerFactory.class);

	@Reference(name = "httpClient")
	private HttpClient httpClient;
	private ComponentContext componentContext;
	private final List<Runnable> lazyInits = new ArrayList<>();
	private final List<SolrInstance> solrInstances = new ArrayList<>();
	private final ReadWriteLock solrInstancesLock = new ReentrantReadWriteLock();
	
	private final List<SearchServiceListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	private ScheduledExecutorService threadPoolExecutor;
	private int sleepTime = 5000;

	class SolrInstance {

		private final SolrConfiguration configuration;
		private HttpSolrServer queryServer;
		private HttpSolrServer updateServer;
		private final List<ServiceRegistration<SolrServer>> registrations = new ArrayList<>();
		private ScheduledFuture<?> initFuture;
		private boolean didInit;
		private SolrInstanceInitializer initializer;

		public SolrInstance(SolrConfiguration configuration) {
			if (configuration == null) {
				throw new IllegalArgumentException("configuration is not allowed to be null");
			}
			this.configuration = configuration;
		}

		public HttpSolrServer getUpdateServer() {
			return updateServer;
		}

		public HttpSolrServer getQueryServer() {
			return queryServer;
		}

		public SolrConfiguration getConfiguration() {
			return configuration;
		}

		private void init() {
			if (componentContext == null) {
				lazyInits.add(new Runnable() {

					@Override
					public void run() {
						init();
					}
				});
			} else {
				didInit = false;
				try {
					LOG.info("creating query solr server");
					queryServer = new HttpSolrServer(configuration.getBaseUrl(), httpClient);
					LOG.info("created query solr server");
				} catch (Exception e) {
					LOG.info("creation of query solr server failed: " + e.getMessage(), e);
				}
				try {
					LOG.info("creating update solr server");
					updateServer = new HttpSolrServer(configuration.getBaseUrl(), httpClient);
					LOG.info("created update solr server");
				} catch (Exception e) {
					LOG.info("creation of update solr server failed: " + e.getMessage(), e);
				}
				initializer = new SolrInstanceInitializer(this, componentContext.getBundleContext(), listenersLock.readLock(), listeners);
				initFuture = threadPoolExecutor.scheduleAtFixedRate(initializer, sleepTime, sleepTime, TimeUnit.MILLISECONDS);
			}
		}

		private void destroy() {
			if (initFuture != null) {
				initFuture.cancel(true);
			}
			for (ServiceRegistration<SolrServer> registration : registrations) {
				registration.unregister();
			}
			registrations.clear();
			if (queryServer != null) {
				queryServer.shutdown();
				queryServer = null;
			}
			if (updateServer != null) {
				updateServer.shutdown();
				updateServer = null;
			}
		}

		void addRegistration(ServiceRegistration<SolrServer> registration) {
			registrations.add(registration);
		}

		void didInit() {
			didInit = true;
		}

	}

	@Activate
	public void activate(ComponentContext componentContext) {
		LOG.info("activating solr server factory");
		DictionaryAdapter adapter = new DictionaryAdapter(componentContext.getProperties()).emptyStringAsNull();
		int threadCount = adapter.getInteger("threadCount", 2);
		sleepTime = adapter.getInteger("sleepTime", sleepTime);
		threadPoolExecutor = Executors.newScheduledThreadPool(threadCount);
		this.componentContext = componentContext;
		solrInstancesLock.writeLock().lock();
		try {
			for (Runnable lazyInit : lazyInits) {
				lazyInit.run();
			}
			lazyInits.clear();
		} finally {
			solrInstancesLock.writeLock().unlock();
		}
		LOG.info("activated solr server factory");
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		solrInstancesLock.writeLock().lock();
		try {
			for (SolrInstance solrInstance : solrInstances) {
				solrInstance.destroy();
			}
			solrInstances.clear();
		} finally {
			solrInstancesLock.writeLock().unlock();
		}
		lazyInits.clear();
		threadPoolExecutor.shutdown();
		httpClient = null;
		componentContext = null;
	}

	
	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		bind = "addSearchServiceListener",
		unbind = "removeSearchServiceListener",
		service = SearchServiceListener.class,
		policy = ReferencePolicy.DYNAMIC
	)
	public void addSearchServiceListener(SearchServiceListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
				solrInstancesLock.readLock().lock();
				try {
					for (SolrInstance solrInstance : solrInstances) {
						if (solrInstance.didInit) {
							listener.searchServiceIsReady(solrInstance.configuration.getName());
						}
					}
				} finally {
					solrInstancesLock.readLock().unlock();
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	public void removeSearchServiceListener(SearchServiceListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
		bind = "addSolrConfiguration",
		unbind = "removeSolrConfiguration",
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		service = SolrConfiguration.class
	)
	public void addSolrConfiguration(SolrConfiguration solrConfiguration) {
		if (solrConfiguration != null) {
			solrInstancesLock.writeLock().lock();
			try {
				SolrInstance solrInstance = new SolrInstance(solrConfiguration);
				solrInstances.add(solrInstance);
				solrInstance.init();
			} finally {
				solrInstancesLock.writeLock().unlock();
			}
		}
	}

	public void removeSolrConfiguration(SolrConfiguration solrConfiguration) {
		if (solrConfiguration != null) {
			solrInstancesLock.writeLock().lock();
			try {
				Iterator<SolrInstance> iterator = solrInstances.iterator();
				while (iterator.hasNext()) {
					SolrInstance next = iterator.next();
					if (next.configuration == solrConfiguration) {
						iterator.remove();
						next.destroy();
					}
				}
			} finally {
				solrInstancesLock.writeLock().unlock();
			}
		}
	}

	public SolrServer getUpdateServer(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		solrInstancesLock.readLock().lock();
		try {
			for (SolrInstance solrInstance : solrInstances) {
				if (name.equals(solrInstance.configuration.getName())) {
					return solrInstance.updateServer;
				}
			}
			return null;
		} finally {
			solrInstancesLock.readLock().unlock();
		}
	}
	
	public SolrServer getQueryServer(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		solrInstancesLock.readLock().lock();
		try {
			for (SolrInstance solrInstance : solrInstances) {
				if (name.equals(solrInstance.configuration.getName())) {
					return solrInstance.queryServer;
				}
			}
			return null;
		} finally {
			solrInstancesLock.readLock().unlock();
		}
	}
	
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
}
