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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SolrRequestCommiter.class, immediate = true)
@Designate(ocd = SolrRequestCommiter.Configuration.class)
public class SolrRequestCommiter extends AbstractSolrServerTracker implements Runnable {

	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Sleep time",
				description = "The time in milliseconds to sleep between commits towards solr."
		)
		long sleepTime() default 3000;
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(SolrRequestCommiter.class);
	private ComponentContext componentContext;

	static enum WorkMode {

		ADD, DELETE
	}

	static interface WorkItem {
		WorkMode getMode();
		String getTargetServer();
		void attachTo(UpdateRequest updateRequest);
	};
	
	private final List<WorkItem> workItems = new ArrayList<>();
	private final ReadWriteLock workItemsLock = new ReentrantReadWriteLock();
	
	private long sleepTime = 3000;
	private ScheduledExecutorService service;

	@Activate
	public void activate(Configuration configuration, ComponentContext componentContext) {
		this.componentContext = componentContext;
		sleepTime = configuration.sleepTime();
		service = Executors.newSingleThreadScheduledExecutor();
		// schedule a runnable, that will periodically flush the changes to solr
		service.scheduleAtFixedRate(this, 0, sleepTime, TimeUnit.MILLISECONDS);
		startTracking();
	}

	@Deactivate
	public void deactivate() {
		workItemsLock.writeLock().lock();
		try {
			stopTracking();
			if (service != null) {
				service.shutdown();
			}
			service = null;
			componentContext = null;
			LOG.info("deactivating solr request commiter, but {} work items had not been processes", workItems.size());
			workItems.clear();
		} finally {
			workItemsLock.writeLock().unlock();
		}
	}

	@Override
	protected ComponentContext getComponentContext() {
		return componentContext;
	}

	public void append(final SolrInputDocument inputDocument, final String targetServer) {
		workItemsLock.writeLock().lock();
		try {
			workItems.add(new WorkItem() {
				@Override
				public WorkMode getMode() {
					return WorkMode.ADD;
				}

				@Override
				public String getTargetServer() {
					return targetServer;
				}

				@Override
				public void attachTo(UpdateRequest updateRequest) {
					updateRequest.add(inputDocument, true);
				}

			});
		} finally {
			workItemsLock.writeLock().unlock();
		}
	}

	public void delete(final String deleteQuery, final String targetServer) {
		workItemsLock.writeLock().lock();
		try {
			workItems.add(new WorkItem() {
				@Override
				public WorkMode getMode() {
					return WorkMode.DELETE;
				}

				@Override
				public String getTargetServer() {
					return targetServer;
				}

				@Override
				public void attachTo(UpdateRequest updateRequest) {
					updateRequest.deleteByQuery(deleteQuery);
				}

			});
		} finally {
			workItemsLock.writeLock().unlock();
		}
	}

	public void flushAll() {
		run();
	}
	
	public void flush(final String serverName) {
		flushWorkItems(serverName);
	}

	@Override
	public void run() {
		flushAllWorkItems();
	}
	
	private void flushAllWorkItems() {
		lock.readLock().lock();
		workItemsLock.writeLock().lock();
		try {
			Iterator<WorkItem> iterator = workItems.iterator();
			TransactionState transactionState = new TransactionState(updateServers);
			// iterate over the items and execute them
			while (iterator.hasNext()) {
				WorkItem item = iterator.next();
				transactionState.append(item);
				iterator.remove();
			}
			transactionState.flush();
		} finally {
			workItemsLock.writeLock().unlock();
			lock.readLock().unlock();
		}
	}
	
	private void flushWorkItems(String serverName) {
		lock.readLock().lock();
		workItemsLock.writeLock().lock();
		try {
			Iterator<WorkItem> iterator = workItems.iterator();
			WorkMode currentMode = null;
			UpdateRequest currentUpdateRequest = null;
			// iterate over the items and execute them
			while (iterator.hasNext()) {
				WorkItem item = iterator.next();
				if (!serverName.equals(item.getTargetServer())) {
					continue;
				}
				if (currentMode == null && currentUpdateRequest == null) {
					currentMode = item.getMode();
					currentUpdateRequest = new UpdateRequest();
					item.attachTo(currentUpdateRequest);
					iterator.remove();
				} else {
					if (currentMode != item.getMode()) {
						// submit the request and null everything
						if (currentUpdateRequest != null) {
							SolrServer updateSolrServer = updateServers.get(serverName);
							if (updateSolrServer == null) {
								LOG.warn("could not flush work items, because the target solr update server {} was not available", serverName);
								continue;
							}
							try {
								UpdateResponse updateResponse = currentUpdateRequest.process(updateSolrServer);
								updateSolrServer.commit();
							} catch (SolrServerException | IOException e) {
								LOG.error("failed to commit update request to solr update server " + serverName, e);
							}
						}
						
						currentMode = null;
						currentUpdateRequest = null;
					} else if (currentUpdateRequest != null) {
						// we can just append to the request
						item.attachTo(currentUpdateRequest);
						iterator.remove();
					}
				}
			}
		} finally {
			workItemsLock.writeLock().unlock();
			lock.readLock().unlock();
		}
	}
	
	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
	}

}
