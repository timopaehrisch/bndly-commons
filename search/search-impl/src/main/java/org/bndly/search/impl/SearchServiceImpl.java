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

import org.bndly.search.api.BoostedDocumentFieldValue;
import org.bndly.search.api.DocumentFieldValue;
import org.bndly.search.api.DocumentFieldValueProvider;
import org.bndly.search.api.DocumentMapper;
import org.bndly.search.api.Query;
import org.bndly.search.api.Result;
import org.bndly.search.api.SearchException;
import org.bndly.search.api.SearchIndexService;
import org.bndly.search.api.SearchService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {SearchService.class, SearchIndexService.class})
public class SearchServiceImpl extends AbstractSolrServerTracker implements SearchService, SearchIndexService {

	private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

	@Reference
	private SolrRequestCommiter solrRequestCommiter;
	
	private ComponentContext componentContext;

	@Activate
	public void activate(ComponentContext componentContext) {
		LOG.info("activating search service");
		this.componentContext = componentContext;
		startTracking();
		LOG.info("activated search service. tracking solr servers now");
	}

	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		LOG.info("deactivating search service");
		stopTracking();
		this.componentContext = null;
		LOG.info("deactivated search service");
	}

	@Override
	public ComponentContext getComponentContext() {
		return componentContext;
	}
	
	public String getDefaultSearchServerName() {
		return "default";
	}
	
	@Override
	public <E> Result<E> search(final Query query, final DocumentMapper<E> documentMapper) {
		return search(getDefaultSearchServerName(), query, documentMapper);
	}
	
	@Override
	public <E> Result<E> search(final String searchServerName, final Query query, final DocumentMapper<E> documentMapper) {
		SolrQuery solrQuery = mapQueryToSolrQuery(query);
		lock.readLock().lock();
		try {
			SolrServer querySolrServer = queryServers.get(searchServerName);
			if (querySolrServer == null) {
				throw new SearchException("could not find query server for server name: " + searchServerName);
			}
			QueryResponse r = querySolrServer.query(solrQuery);
			final SolrDocumentList resultDocuments = r.getResults();
			final List<E> entries = new ArrayList<>();
			for (SolrDocument solrDocument : resultDocuments) {
				E instance = documentMapper.getInstance();
				if (instance != null) {
					entries.add(instance);
					for (String fieldName : solrDocument.getFieldNames()) {
						Object value = solrDocument.getFieldValue(fieldName);
						documentMapper.setValue(instance, fieldName, value);
					}
				}
			}

			return new Result<E>() {
				@Override
				public List<E> getEntries() {
					return entries;
				}

				@Override
				public long getNumberOfHits() {
					return resultDocuments.getNumFound();
				}

				@Override
				public Query getQuery() {
					return query;
				}

				@Override
				public Iterator<E> iterator() {
					return entries.iterator();
				}
			};
		} catch (SolrServerException ex) {
			throw new SearchException("could not execute search query: " + ex.getMessage(), ex);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void removeFromIndex(DocumentFieldValueProvider documentFieldValueProvider) {
		removeFromIndex(documentFieldValueProvider, getDefaultSearchServerName());
	}
	
	@Override
	public void removeFromIndex(DocumentFieldValueProvider documentFieldValueProvider, String searchServerName) {
		StringBuilder q = null;
		for (DocumentFieldValue documentFieldValue : documentFieldValueProvider.getDocumentFieldValues()) {
			if (q == null) {
				q = new StringBuilder();
			} else {
				q.append(" AND ");
			}
			q.append(documentFieldValue.getFieldName());
			q.append(':');
			q.append(documentFieldValue.getValue());
		}
		solrRequestCommiter.delete(q.toString(), searchServerName);
	}

	@Override
	public void flush() {
		solrRequestCommiter.flush(getDefaultSearchServerName());
	}

	@Override
	public void flush(String searchServerName) {
		solrRequestCommiter.flush(searchServerName);
	}
	
	@Override
	public void addToIndex(DocumentFieldValueProvider documentFieldValueProvider) {
		addToIndex(documentFieldValueProvider, getDefaultSearchServerName());
	}

	@Override
	public void addToIndex(DocumentFieldValueProvider documentFieldValueProvider, String searchServerName) {
		SolrInputDocument inputDocument = new SolrInputDocument();
		List<DocumentFieldValue> values = documentFieldValueProvider.getDocumentFieldValues();
		if (values != null) {
			for (DocumentFieldValue documentFieldValue : values) {
				String fieldName = documentFieldValue.getFieldName();
				Object value = documentFieldValue.getValue();
				if (fieldName != null) {
					if (BoostedDocumentFieldValue.class.isInstance(documentFieldValue)) {
						BoostedDocumentFieldValue b = BoostedDocumentFieldValue.class.cast(documentFieldValue);
						inputDocument.setField(fieldName, value, b.getBoost());
					} else {
						inputDocument.setField(fieldName, value);
					}
				}
			}
		}
		solrRequestCommiter.append(inputDocument, searchServerName);
	}

	private SolrQuery mapQueryToSolrQuery(Query query) {
		SolrQuery q = new SolrQuery();
		q.setStart(query.getStart());
		q.setRows(query.getRows());
		q.setRequestHandler(query.getRequestHandler());
		q.setQuery(query.getQ());
		q.setFields(query.getFields());
		if (query.getSortField() != null) {
			SolrQuery.ORDER order = SolrQuery.ORDER.desc;
			if (query.isAscending()) {
				order = SolrQuery.ORDER.asc;
			}
			q.setSort(query.getSortField(), order);
		}
		return q;
	}

	public void setSolrRequestCommiter(SolrRequestCommiter solrRequestCommiter) {
		this.solrRequestCommiter = solrRequestCommiter;
	}

}
