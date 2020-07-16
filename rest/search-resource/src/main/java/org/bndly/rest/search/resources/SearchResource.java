package org.bndly.rest.search.resources;

/*-
 * #%L
 * REST Search Resource
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

import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.common.beans.PaginationRestBean;
import org.bndly.rest.search.beans.SearchDocument;
import org.bndly.rest.search.beans.SearchParameters;
import org.bndly.rest.search.beans.SearchResult;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.SortRestBean;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.search.api.Query;
import org.bndly.search.api.ReindexService;
import org.bndly.search.api.Result;
import org.bndly.search.api.SearchService;
import java.util.List;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SearchResource.class, immediate = true)
@Path("search")
public class SearchResource {

	private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);
	
	@org.osgi.service.component.annotations.Reference
	private SearchService searchService;
	@org.osgi.service.component.annotations.Reference
	private ReindexService reindexService;
	@org.osgi.service.component.annotations.Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Activate
	public void activate() {
		controllerResourceRegistry.deploy(this);
	}
	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "search", target = Services.class),
		@AtomLink(target = SearchParameters.class)
	})
	public Response getSearchParameters() {
		SearchParameters p = new SearchParameters();
		return Response.ok(p);
	}

	@GET
	@Path("reindex")
	@AtomLink(rel = "reindex", target = SearchParameters.class)
	public Response reindex() {
		reindexService.reindex();
		SearchParameters p = new SearchParameters();
		return Response.ok(p);
	}

	@POST
	@Path("query")
	@AtomLink(rel = "query", target = SearchParameters.class)
	public Response buildSearchURI(SearchParameters parameters, @Meta Context context) {
		ResourceURIBuilder builder = context.createURIBuilder();
		builder.pathElement("search").pathElement("result");
		appendNonNullParameterToQuery(builder, "q", parameters.getQuery());
		appendNonNullParameterToQuery(builder, "handler", parameters.getHandler());
		PaginationRestBean p = parameters.getPage();
		if (p != null) {
			if (p.getStart() != null) {
				appendNonNullParameterToQuery(builder, "pageStart", p.getStart().toString());
			}
			if (p.getSize() != null) {
				appendNonNullParameterToQuery(builder, "pageSize", p.getSize().toString());
			}
		}
		SortRestBean s = parameters.getSorting();
		if (s != null) {
			if (s.getField() != null) {
				appendNonNullParameterToQuery(builder, "sortingField", s.getField());
				if (s.isAscending() != null) {
					if (s.isAscending()) {
						appendNonNullParameterToQuery(builder, "sortingDirection", "ASC");
					} else {
						appendNonNullParameterToQuery(builder, "sortingDirection", "DESC");
					}
				}
			}
		}
		String uri = builder.build().asString();
		return Response.seeOther(uri);
	}

	@GET
	@Path("result")
	@AtomLink(rel = "self", target = SearchResult.class, reuseQueryParameters = true)
	public Response executeSearch(@Meta Context context) {
		Query q = buildQueryFromRequest(context);
		Result<SearchDocument> r = searchService.search(q, new DefaultDocumentMapper());
		List<SearchDocument> entries = r.getEntries();
		SearchResult sr = new SearchResult();
		sr.setEntries(entries);
		PaginationRestBean pg = new PaginationRestBean();
		if (q.getRows() != null) {
			pg.setSize(q.getRows().longValue());
		}
		if (q.getStart() != null) {
			pg.setStart(q.getStart().longValue());
		}
		pg.setTotalRecords(r.getNumberOfHits());
		sr.setPage(pg);
		return Response.ok(sr);
	}

	private Query buildQueryFromRequest(Context context) {
		ResourceURI uri = context.getURI();
		final int start = getIntRequestParameter(uri, "pageStart", 0);
		final int size = getIntRequestParameter(uri, "pageStart", 10);
		final String sortingField = getStringRequestParameter(uri, "sortingField", null);
		final String sortingDirection = getStringRequestParameter(uri, "sortingDirection", null);
		final String requestHandler = getStringRequestParameter(uri, "handler", "select");
		final String q = getStringRequestParameter(uri, "q", "*:*");
		return new Query() {

			@Override
			public Integer getStart() {
				return start;
			}

			@Override
			public Integer getRows() {
				return size;
			}

			@Override
			public String getRequestHandler() {
				return requestHandler;
			}

			@Override
			public String getQ() {
				return q;
			}

			@Override
			public String[] getFields() {
				return new String[]{"*"};
			}

			@Override
			public String getSortField() {
				return sortingField;
			}

			@Override
			public boolean isAscending() {
				return "ASC".equals(sortingDirection);
			}

		};
	}

	private String getStringRequestParameter(ResourceURI uri, String parameterName, String defaultValue) {
		ResourceURI.QueryParameter qp = uri.getParameter(parameterName);
		if (qp == null) {
			return defaultValue;
		}
		String p = qp.getValue();
		if (p == null) {
			p = defaultValue;
		}
		return p;
	}

	private Integer getIntRequestParameter(ResourceURI uri, String parameterName, Integer defaultValue) {
		ResourceURI.QueryParameter qp = uri.getParameter(parameterName);
		if (qp == null) {
			return defaultValue;
		}
		String p = qp.getValue();
		if (p != null) {
			try {
				return new Integer(p);
			} catch (NumberFormatException e) {
				// ignore this
			}
		}
		return defaultValue;
	}

	private void appendNonNullParameterToQuery(ResourceURIBuilder builder, String parameterName, String value) {
		if (value != null) {
			builder.parameter(parameterName, value);
		}
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setReindexService(ReindexService reindexService) {
		this.reindexService = reindexService;
	}

}
