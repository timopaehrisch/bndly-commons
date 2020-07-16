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

import org.bndly.search.impl.SolrRequestCommiter.WorkItem;
import java.io.IOException;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class TransactionState {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionState.class);
	private final Map<String, SolrServer> updateServers;
	private String currentTargetServer = null;
	private SolrRequestCommiter.WorkMode currentMode = null;
	private UpdateRequest currentUpdateRequest = null;

	public TransactionState(Map<String, SolrServer> updateServers) {
		this.updateServers = updateServers;
	}
	
	TransactionState append(WorkItem item) {
		if (isNoWorkQueued()) {
			currentTargetServer = item.getTargetServer();
			currentMode = item.getMode();
			currentUpdateRequest = new UpdateRequest();
			item.attachTo(currentUpdateRequest);
		} else if (isSimilarWorkQueued(item)) {
			item.attachTo(currentUpdateRequest);
		} else {
			flush();
			return append(item);
		}
		return this;
	}
	
	private boolean isSimilarWorkQueued(WorkItem item) {
		return item.getTargetServer().equals(currentTargetServer) && item.getMode().equals(currentMode);
	}
	
	private boolean isNoWorkQueued() {
		return currentTargetServer == null && currentMode == null && currentUpdateRequest == null;
	}
	
	private boolean isWorkQueued() {
		return currentTargetServer != null && currentMode != null && currentUpdateRequest != null;
	}
	
	void flush() {
		if (isNoWorkQueued()) {
			return;
		}
		try {
			SolrServer updateSolrServer = updateServers.get(currentTargetServer);
			if (updateSolrServer == null) {
				LOG.warn("could not flush work items, because the target solr update server {} was not available", currentTargetServer);
				return;
			}
			try {
				UpdateResponse updateResponse = currentUpdateRequest.process(updateSolrServer);
				updateSolrServer.commit();
			} catch (SolrServerException | IOException e) {
				LOG.error("failed to commit update request to solr update server " + currentTargetServer, e);
			}
		} finally {
			currentTargetServer = null;
			currentMode = null;
			currentUpdateRequest = null;
		}
	}
}
