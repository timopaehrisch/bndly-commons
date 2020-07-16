package org.bndly.rest.query.resource;

/*-
 * #%L
 * REST Query Resource
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

import org.bndly.rest.query.beans.SchemaQueries;
import org.bndly.rest.query.beans.SchemaQueryArgumentListRestBean;
import org.bndly.rest.query.beans.SchemaQueryArgumentRestBean;
import org.bndly.rest.query.beans.SchemaQueryRestBean;
import org.bndly.rest.query.beans.SchemaQueryResultRestBean;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.common.mapper.MappingContext;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.common.beans.AnyBean;
import org.bndly.rest.common.beans.PaginationRestBean;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.entity.resources.EntityResource;
import org.bndly.rest.entity.resources.SchemaAdapter;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.services.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SchemaQueryResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(SchemaQueryResource.class);
	private static final Object[] EMPTY_ARGS = new Object[]{};
	private static final String PICK_PATTERN = "^\\s*(PICK)\\s+";
	private static final String ORDERBY_PATTERN = "\\s+ORDERBY\\s[a-zA-Z0-9\\.]+(\\s+DESC)?";
	private final MapperFactoryProvider mapperFactoryProvider;
	private final SchemaAdapter schemaAdapter;
	private final List<EntityResource> entityResources = new ArrayList<>();
	private final Map<String, Class<?>> schemaTypeToRestBean = new HashMap<>();
	private final String schemaName;

	public SchemaQueryResource(SchemaAdapter schemaAdapter, MapperFactoryProvider mapperFactoryProvider) {
		this.schemaAdapter = schemaAdapter;
		this.mapperFactoryProvider = mapperFactoryProvider;
		this.schemaName = schemaAdapter.getEngine().getDeployer().getDeployedSchema().getName();
	}

	public String getSchemaName() {
		return schemaName;
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "query", target = SchemaBean.class, constraint = "${controller.schemaName == this.name}"),
		@AtomLink(target = SchemaQueryRestBean.class, constraint = "${controller.schemaName == this.schemaName}")
	})
	@Documentation(
			authors = "bndly@bndly.org",
			produces = Documentation.ANY_CONTENT_TYPE,
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.OK, description = "A query prototype object", messageType = SchemaQueryRestBean.class)
			},
			value = "Gets a query prototype object, that can be POSTed back to the application. The prototype contains a 'query' link, that should receive the filled query object.",
			summary = "Get query prototype",
			tags = "schema"
	)
	public Response getRawQueryPrototype() {
		SchemaQueryRestBean schemaQueryRestBean = new SchemaQueryRestBean();
		schemaQueryRestBean.setSchemaName(schemaAdapter.getSchema().getName());
		return Response.ok(schemaQueryRestBean);
	}
	
	/**
	 * Perform schema bean query against with paging eBX.
	 *
	 * @param schemaQueryRestBean the schema query rest bean
	 * @return the response
	 */
	@POST
	@AtomLink(rel = "query", target = SchemaQueryRestBean.class, constraint = "${controller.schemaName == this.schemaName}")
	@Documentation(
			authors = "bndly@bndly.org",
			produces = Documentation.ANY_CONTENT_TYPE,
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.OK, description = "A query result rest bean", messageType = SchemaQueryResultRestBean.class),
				@DocumentationResponse(
						code = StatusWriter.Code.INTERNAL_SERVER_ERROR, 
						description = "Something has gone out of control. A reason may be a missing RestBean class for the queried type."
				)
			},
			value = "Performs the provided query. Please note, that the query string should not contain LIMIT and OFFSET statements. "
					+ "Those have to be provided within the PaginationRestBean in the POSTed query object.",
			summary = "Executes a query",
			tags = "schema"
	)
	public Response performQuery(SchemaQueryRestBean schemaQueryRestBean) {
		schemaQueryRestBean.setSchemaName(schemaAdapter.getSchema().getName());
		
		MapperFactory mapperFactory = mapperFactoryProvider.getMapperFactoryForSchema(schemaAdapter.getSchema().getName());
		if (mapperFactory == null) {
			LOG.error("could not get mapper factory for schema {}", schemaAdapter.getSchema().getName());
			return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode());
		}
		MappingContext mappingContext = mapperFactory.buildContext();
		Accessor accessor = schemaAdapter.getEngine().getAccessor();
		
		// do a real query
		StringBuilder queryStringBuilder = new StringBuilder(schemaQueryRestBean.getQuery());
		Object[] args = buildArgs(queryStringBuilder, schemaQueryRestBean);
		String query = queryStringBuilder.toString();
		String countQuery = schemaQueryRestBean.getQuery()
				.replaceFirst(PICK_PATTERN, "COUNT ")
				.replaceFirst(ORDERBY_PATTERN, "")
				;
		Long totalCount = accessor.count(countQuery, args);
		Iterator<Record> result = accessor.query(query, args);
		SchemaQueryResultRestBean resultList = new SchemaQueryResultRestBean();
		while (result.hasNext()) {
			// convert to rest beans
			Record record = result.next();
			Object schemaBean = schemaAdapter.getSchemaBeanFactory().getSchemaBean(record);
			Class<?> restBeanType = getRestBeanType(record);
			if (restBeanType == null) {
				LOG.error("could not get rest bean type for type {} in schema {}", record.getType().getName(), schemaAdapter.getSchema().getName());
				return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode());
			}
			// add restBean to a query result list
			Object restBean = mappingContext.map(schemaBean, restBeanType);
			AnyBean anyBean = new AnyBean();
			anyBean.setElement(restBean);
			resultList.add(anyBean);
		}
		PaginationRestBean page = schemaQueryRestBean.getPage();
		if (page == null) {
			page = new PaginationRestBean();
		}
		page.setTotalRecords(totalCount);
		resultList.setPage(page);
		return Response.ok(resultList);
	}

	void add(EntityResource entityResource) {
		entityResources.add(entityResource);
		schemaTypeToRestBean.put(entityResource.getType().getName(), entityResource.getRestBeanType());
	}

	void remove(EntityResource entityResource) {
		entityResources.remove(entityResource);
		schemaTypeToRestBean.remove(entityResource.getType().getName());
	}

	boolean isHavingEntities() {
		return !entityResources.isEmpty();
	}

	private Object[] buildArgs(StringBuilder queryStringBuilder, SchemaQueryRestBean schemaQueryRestBean) {
		SchemaQueryArgumentListRestBean arguments = schemaQueryRestBean.getArguments();
		if (arguments == null) {
			return EMPTY_ARGS;
		}
		ArrayList<Object> args = new ArrayList<>();
		for (SchemaQueryArgumentRestBean argument : arguments) {
			args.add(SchemaQueries.valueOf(argument));
		}
		PaginationRestBean page = schemaQueryRestBean.getPage();
		if (page != null) {
			Long offset = page.getStart();
			if (offset != null) {
				queryStringBuilder.append(" OFFSET ?");
				args.add(offset);
			}
			Long size = page.getSize();
			if (size != null) {
				queryStringBuilder.append(" LIMIT ?");
				args.add(size);
			}
		}
		return args.toArray();
	}

	private Class<?> getRestBeanType(Record record) {
		return schemaTypeToRestBean.get(record.getType().getName());
	}
}
