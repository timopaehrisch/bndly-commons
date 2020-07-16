package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.Converter;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.cors.api.CORSRequestDetector;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.common.mapper.MappingContext;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.mapper.NoOpMappingListener;
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkConstraint;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.atomlink.api.annotation.BeanID;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.cache.api.CacheLinkingService;
import org.bndly.rest.common.beans.ListRestBean;
import org.bndly.rest.common.beans.PaginationRestBean;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.rest.common.beans.SmartReferable;
import org.bndly.rest.common.beans.SortRestBean;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.PUT;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.QueryParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.entity.resources.descriptor.BeanConsumingDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.ListLinkDescriptor;
import org.bndly.rest.entity.resources.descriptor.ListRestBeanProducingDocumentationProvider;
import org.bndly.rest.entity.resources.descriptor.PaginationSupportDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.RestBeanProducingDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.SchemaAnnotationTagsDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.SchemaLinkDescriptor;
import org.bndly.rest.entity.resources.descriptor.SelfLinkDescriptor;
import org.bndly.rest.entity.resources.descriptor.SortingSupportDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.TypeNameReplacingDocumentationDecorator;
import org.bndly.rest.schema.beans.TypeBean;
import org.bndly.schema.api.DeletionStrategy;
import org.bndly.schema.api.NQueryUtil;
import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.query.NestedQueryAttribute;
import org.bndly.schema.api.query.QueryAttribute;
import org.bndly.schema.api.query.SimpleQueryAttribute;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.services.QueryByExample;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("")
@Documentation(
		authors = "bndly@bndly.org",
		value = "This is a generic resource to deal with a schema based entity type.",
		tags = { "entityresource", "schema" }
)
public class EntityResource {
	private static final Logger LOG = LoggerFactory.getLogger(EntityResource.class);

	private final Type type;
	private final Class<?> listRestBean;
	private final Class<?> referenceRestBean;
	private final Class<?> restBeanType;
	private final Class<?> schemaBeanType;
	private final Field beanIdField;
	private final JAXBContext jaxbContext;
	private ConverterRegistry converterRegistry;
	private CacheLinkingService cacheLinkingService;
	private Engine engine;
	private DeletionStrategy deletionStrategy;
	private SchemaBeanFactory schemaBeanFactory;
	private MapperFactory mapperFactory;
	private AtomLinkConstraint atomLinkConstraint;
	private CORSRequestDetector corsRequestDetector;

	public static final String SORTING_DIRECTION = "sortingDirection";
	public static final String SORTING_FIELD = "sortingField";
	public static final String PAGINATION_SIZE = "pageSize";
	public static final String PAGINATION_START = "pageStart";
	private static final Long DEFAULT_PAGINATION_SIZE = 10L;

	public static final Set<String> TECHNICAL_REQUEST_PARAMETERS = new HashSet<>(Arrays.asList(
			SORTING_DIRECTION,
			SORTING_FIELD,
			PAGINATION_SIZE,
			PAGINATION_START,
			"preventLinkInjection"
	));
	private final Map<String, Class> javaTypesByParameterName;

	public EntityResource(Type type, Class<?> listRestBean, Class<?> referenceRestBean, Class<?> restBeanType, Class<?> schemaBeanType) throws JAXBException {
		this.type = type;
		this.listRestBean = listRestBean;
		this.restBeanType = restBeanType;
		this.referenceRestBean = referenceRestBean;
		this.schemaBeanType = schemaBeanType;
		
		Field tmp;
		try {
			tmp = ReflectionUtil.getFieldByAnnotation(BeanID.class, restBeanType);
		} catch (IllegalStateException e) {
			tmp = null;
		}
		beanIdField = tmp;
		javaTypesByParameterName = new QueryParameterUtil(type, schemaBeanType).getJavaTypesByParameterName();
		jaxbContext = JAXBContext.newInstance(restBeanType);
	}

	public Class<?> getListRestBean() {
		return listRestBean;
	}

	public Class<?> getRestBeanType() {
		return restBeanType;
	}

	public Class<?> getReferenceRestBean() {
		return referenceRestBean;
	}

	public AtomLinkConstraint getAtomLinkConstraint() {
		return atomLinkConstraint;
	}

	public Type getType() {
		return type;
	}

	@GET
	@AtomLinks({
		@AtomLink(rel = "primaryResource", descriptor = SchemaLinkDescriptor.class, target = TypeBean.class),
		@AtomLink(rel = "list", descriptor = SelfLinkDescriptor.class, constraint = "${true}"),
		@AtomLink(rel = "self", descriptor = ListLinkDescriptor.class, reuseQueryParameters = true),
		@AtomLink(rel = "next", descriptor = ListLinkDescriptor.class, reuseQueryParameters = true, parameters = {
			@Parameter(name = "pageStart", expression = "${this.page.nextPageStart()}")
		}, constraint = "${this.page.hasNextPage()}"),
		@AtomLink(rel = "previous", descriptor = ListLinkDescriptor.class, reuseQueryParameters = true, parameters = {
			@Parameter(name = "pageStart", expression = "${this.page.previousPageStart()}")
		}, constraint = "${this.page.hasPreviousPage()}")
	})
	@Documentation(
			summary = "Browse {{TYPE.NAME}} entities",
			authors = "bndly@bndly.org", 
			value = "This resource should be used to load a paginated and possibly filtered list of {{TYPE.NAME}} entities.",
			responses = {
				@DocumentationResponse(description = "a list rest bean with the items that fit on the current page and match the filter criteria."),
				@DocumentationResponse(messageType = ErrorRestBean.class, code = StatusWriter.Code.NOT_FOUND, description = "if the pagination information is out of bounds")
			},
			produces = Documentation.ANY_CONTENT_TYPE,
			tags = { "paginated", "entityresource", "schema" },
			documentationDecorator = {
				TypeNameReplacingDocumentationDecorator.class, 
				ListRestBeanProducingDocumentationProvider.class, 
				SchemaAnnotationTagsDocumentationDecorator.class, 
				PaginationSupportDocumentationDecorator.class, 
				SortingSupportDocumentationDecorator.class
			}
	)
	public Response list(
			@QueryParam(value = PAGINATION_START, asSelector = true) Long pageStart,
			@QueryParam(value = PAGINATION_SIZE, asSelector = true) Long pageSize,
			@QueryParam(value = SORTING_DIRECTION, asSelector = true) String sortDirection,
			@QueryParam(value = SORTING_FIELD, asSelector = true) String sortField,
			@Meta Context context
	) {
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		QueryByExample countQ = prepareQueryByExample(ctx);
		QueryByExample listQ = prepareQueryByExample(ctx);
		appendQueryParamtersToQueryByExample(ctx, context, countQ, listQ);
		long totalRecords = countQ.count();
		Pagination p = getCurrentPaginationInfo(context);
		if (p.getOffset() >= totalRecords && totalRecords > 0) {
			throw new PaginationException(totalRecords, p.getOffset(), p.getSize(), "query exceeds amount of available records");
		}
		applySortingToQuery(listQ, context);
		List<Record> records = listQ.pagination(p).all();
		ListRestBean list = (ListRestBean) InstantiationUtil.instantiateType(listRestBean);
		if (records != null) {
			for (Record record : records) {
				Object schemaBean = schemaBeanFactory.getSchemaBean(schemaBeanType, record);
				Object restBean = mapperFactory.buildContext().map(schemaBean, restBeanType);
				if (ListRestBean.class.isInstance(list)) {
					((ListRestBean) list).add(restBean);
				}
			}
		}
		PaginationRestBean paginationInfo = new PaginationRestBean();
		paginationInfo.setSize(p.getSize());
		paginationInfo.setStart(p.getOffset());
		paginationInfo.setTotalRecords(totalRecords);
		list.setPage(paginationInfo);
		createCacheLinksForRecordContext(ctx, context);
		return Response.ok(list);
	}
	
	@DELETE
	@AtomLinks({
		@AtomLink(rel = "remove", descriptor = ListLinkDescriptor.class, reuseQueryParameters = true)
	})
	@Documentation(
			summary = "Delete all {{TYPE.NAME}} entities",
			authors = "bndly@bndly.org", 
			value = "This resource should be used to delete all instance of {{TYPE.NAME}}.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.NO_CONTENT, description = "The entities in the database could be deleted."),
				@DocumentationResponse(
						messageType = ErrorRestBean.class, 
						code = StatusWriter.Code.BAD_REQUEST, 
						description = "If at least one entity could not be deleted, this response will be returned. Typical reasons could be integrity violations or connectivity issues."
				)
			},
			tags = { "entityresource", "schema" },
			documentationDecorator = {
				TypeNameReplacingDocumentationDecorator.class, 
				SchemaAnnotationTagsDocumentationDecorator.class
			}
	)
	public Response deleteAll(@Documentation("the default size of a batch, that will be deleted.") @QueryParam(PAGINATION_SIZE) Long batchSize) {
		Long deleteAllBatchSize = batchSize;
		if (deleteAllBatchSize == null) {
			deleteAllBatchSize = DEFAULT_PAGINATION_SIZE;
		}
		final Long deleteAllBatchSizeFinal = deleteAllBatchSize;
		QueryByExample query = prepareQueryByExample(engine.getAccessor().buildRecordContext()).lazy();
		long count = query.count();
		while (count > 0) {
			RecordContext ctx = engine.getAccessor().buildRecordContext();
			// load a batch, then delete the entries individually to give event listeneres the possibility to react
			List<Record> entries = prepareQueryByExample(ctx).pagination(new Pagination() {
				@Override
				public Long getOffset() {
					return 0L;
				}

				@Override
				public Long getSize() {
					return deleteAllBatchSizeFinal;
				}
			}).all();
			Transaction tx = engine.getQueryRunner().createTransaction();
			for (Record entry : entries) {
				engine.getAccessor().buildDeleteQuery(entry, tx);
			}
			tx.commit();
			count = prepareQueryByExample(ctx).lazy().count();
		}

		return Response.NO_CONTENT;
	}
	
	@POST
	@AtomLinks({
		@AtomLink(rel = "addUnCascaded", descriptor = ListLinkDescriptor.class, parameters
				= @Parameter(name = "cascaded", expression = "false")),
		@AtomLink(descriptor = ListLinkDescriptor.class, parameters
				= @Parameter(name = "cascaded", expression = "true"))
	})
	@Documentation(
			summary = "Create a {{TYPE.NAME}} entity",
			authors = "bndly@bndly.org",
			value = 
					"This resource should be used to create a new instance of {{TYPE.NAME}}. "
					+ "The response will contain the location of the newly created {{TYPE.NAME}} entity as a resource.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.CREATED, description = "If the posted entity could successfully be created, this response will be returned."),
				@DocumentationResponse(
						messageType = ErrorRestBean.class, 
						code = StatusWriter.Code.BAD_REQUEST, 
						description = "If the posted entity could not be created, this response will be returned. Typical reasons could be constraint violations or connectivity issues."
				)
			},
			consumes = Documentation.ANY_CONTENT_TYPE,
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, BeanConsumingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response create(@Documentation("true, if nested entities shall be persisted as well") @QueryParam("cascaded") Boolean cascaded, @Meta Context context) {
		Object schemaBean = mapJAXBInputToSchemaBean(context);
		if (cascaded != null && cascaded) {
			((ActiveRecord) schemaBean).persistCascaded();
		} else {
			((ActiveRecord) schemaBean).persist();
		}
		ResourceURI uri = getSelfUri(((ActiveRecord) schemaBean).getId(), context);
		return Response.created(uri.asString());
	}

	@POST
	@Path("findAll")
	@AtomLink(rel = "findAll", descriptor = ListLinkDescriptor.class)
	@Documentation(
			summary = "Submit a {{TYPE.NAME}} query",
			authors = "bndly@bndly.org",
			value = 
				"This resource takes a prototype as input in order to redirect to a {{TYPE.NAME}} resource "
				+ "that lists all {{TYPE.NAME}} entities, that match with the provided prototype.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.FOUND, description = 
					"redirects to a resource, that lists all entities, that match the provided filter criteria. "
					+ "the resource is supporting pagination")
			},
			consumes = Documentation.ANY_CONTENT_TYPE,
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, BeanConsumingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response findAll(@Meta Context context) {
		Object bean = getPayloadBean(context);
		Field f = ReflectionUtil.getFieldByAnnotation(BeanID.class, bean);
		Long id = null;
		if (f != null) {
			id = (Long) ReflectionUtil.getFieldValue(f, bean);
		}
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		QueryByExample q = prepareFindQueryWithPrototype(bean, id, ctx);

		ResourceURIBuilder uriBuilder = context.createURIBuilder();
		uriBuilder = uriBuilder.pathElement(type.getSchema().getName());
		uriBuilder = uriBuilder.pathElement(type.getName());

		List<QueryAttribute> attributes = q.getQueryAttributes();
		appendQueryAttributes(attributes, uriBuilder, null);

		boolean hasDynamicQuery = attributes != null && !attributes.isEmpty();
		ContentType desiredContentType = context.getDesiredContentType();
		boolean paginationAsSelector = desiredContentType != null && desiredContentType.getExtension() != null && !hasDynamicQuery;
		if (paginationAsSelector) {
			uriBuilder.extension(desiredContentType.getExtension());
		}
		// append pagination info
		if (RestBean.class.isInstance(bean)) {
			RestBean b = (RestBean) bean;
			PaginationRestBean p = b.getPage();
			if (p != null) {
				if (p.getStart() != null) {
					if (paginationAsSelector) {
						uriBuilder.selector(PAGINATION_START + p.getStart().toString());
					} else {
						uriBuilder.parameter(PAGINATION_START, p.getStart().toString());
					}
				}
				if (p.getSize() != null) {
					if (paginationAsSelector) {
						uriBuilder.selector(PAGINATION_SIZE + p.getSize().toString());
					} else {
						uriBuilder.parameter(PAGINATION_SIZE, p.getSize().toString());
					}
				}
			}
			SortRestBean s = b.getSorting();
			if (s != null) {
				if (s.getField() != null) {
					if (paginationAsSelector) {
						uriBuilder.selector(SORTING_FIELD + s.getField());
					} else {
						uriBuilder.parameter(SORTING_FIELD, s.getField());
					}
				}
				if (s.isAscending() != null) {
					String d = "DESC";
					if (s.isAscending()) {
						d = "ASC";
					}
					if (paginationAsSelector) {
						uriBuilder.selector(SORTING_DIRECTION + d);
					} else {
						uriBuilder.parameter(SORTING_DIRECTION, d);
					}
				}
			}
		}

		return seeOther(uriBuilder.build().asString());
	}

	@POST
	@Path("find")
	@AtomLink(rel = "find", descriptor = ListLinkDescriptor.class)
	@Documentation(
			summary = "Search a single {{TYPE.NAME}}",
			authors = "bndly@bndly.org",
			value = 
				"This resource takes a prototype as input in order to redirect to the {{TYPE.NAME}} resource, that matches with the provided prototype. "
				+ "If no such {{TYPE.NAME}} can be found, a 404 is returned.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.FOUND, description = "Redirects to a resource, that returns the described entity."),
				@DocumentationResponse(code = StatusWriter.Code.NOT_FOUND, description = "if no or no unique entity matches the provided filter criteria")
			},
			consumes = Documentation.ANY_CONTENT_TYPE,
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, BeanConsumingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response findSingle(@Meta Context context) {
		Object bean = getPayloadBean(context);
		Field f = ReflectionUtil.getFieldByAnnotation(BeanID.class, bean);
		Long id = null;
		if (f != null) {
			id = (Long) ReflectionUtil.getFieldValue(f, bean);
		}
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		QueryByExample qbe = prepareFindQueryWithPrototype(bean, id, ctx);
		Record found = qbe.single();
		if (found != null) {
			ResourceURI uri = getSelfUri(found.getId(), context);
			return seeOther(uri.asString());
		}
		throw new UnknownResourceException("could not find " + getType().getName());
	}

	@GET
	@Path("{id}")
	@AtomLink(descriptor = SelfLinkDescriptor.class)
	@Documentation(
			summary = "Read {{TYPE.NAME}} by ID",
			authors = "bndly@bndly.org",
			value = "This resource reads a single instance of {{TYPE.NAME}} and returns it in the desired format. If the entity can not be found, a 404 will be returned.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.OK, description = "The entity with the database id."),
				@DocumentationResponse(messageType = ErrorRestBean.class, code = StatusWriter.Code.NOT_FOUND, description = "No entity with the id exists in the database.")
			},
			produces = Documentation.ANY_CONTENT_TYPE,
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, RestBeanProducingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response read(@Documentation("The database id of the entity to read.") @PathParam("id") long id, @Meta Context context) {
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record record;
		try {
			record = prepareQueryByExample(ctx).attribute("id", id).single();
		} catch (Exception e) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id, e);
		}
		if (record == null) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id);
		}
		Object schemaBean = schemaBeanFactory.getSchemaBean(schemaBeanType, record);
		Object restBean = mapperFactory.buildContext().map(schemaBean, restBeanType);
		createCacheLinksForRecordContext(ctx, context);
		return Response.ok(restBean);
	}

	public void createCacheLinksForRecordContext(RecordContext recordContext, Context context) {
		if (context.canBeCached()) {
			Iterator<Record> iter = recordContext.listPersistedRecords();
			String currentPath = context.getURI().pathAsString();
			while (iter.hasNext()) {
				Record next = iter.next();
				String targetPath = "/" + next.getType().getSchema().getName() + "/" + next.getType().getName() + "/" + next.getId();
				if (!targetPath.equals(currentPath)) {
					try {
						cacheLinkingService.link(targetPath, currentPath);
					} catch (IOException ex) {
						LOG.error("failed to link cache entry", ex);
					}
				}
			}
		}
	}

	@GET
	@Path("{id}.pdf")
	@AtomLink(rel = "print", descriptor = SelfLinkDescriptor.class)
	@Documentation(
			summary = "Generate PDF of {{TYPE.NAME}} by ID",
			authors = "bndly@bndly.org",
			value = "This resource reads a single instance of {{TYPE.NAME}} and returns it as a pdf document. "
					+ "The document is created using a velocity template, that describes the pdf document.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.OK, description = 
						"The entity with the database id rendered with a PDF tempalte. "
						+ "The template matches the entity type name as has to exist in the templating data store."
				),
				@DocumentationResponse(messageType = ErrorRestBean.class, code = StatusWriter.Code.NOT_FOUND, description = "No entity with the id exists in the database.")
			},
			produces = "application/pdf",
			tags = { "templating", "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response readPDF(@Documentation("The database id of the entity to read.") @PathParam("id") long id, @Meta Context context) {
		return read(id, context);
	}

	@PUT
	@Path("{id}")
	@AtomLinks({
		@AtomLink(rel = "updateUnCascaded", descriptor = SelfLinkDescriptor.class, parameters
				= @Parameter(name = "cascaded", expression = "false")),
		@AtomLink(descriptor = SelfLinkDescriptor.class, parameters
				= @Parameter(name = "cascaded", expression = "true"))
	})
	@Documentation(
			summary = "Update a {{TYPE.NAME}}",
			authors = "bndly@bndly.org",
			value = 
				"This resource updates a single instance of {{TYPE.NAME}}. If the entity can not be found, a 404 is returned. "
				+ "After the update is finished successfully, a 204 is returned.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.NO_CONTENT, description = "The entity with the database id did exist and could be updated."),
				@DocumentationResponse(messageType = ErrorRestBean.class, code = StatusWriter.Code.NOT_FOUND, description = "No entity with the id exists in the database."),
				@DocumentationResponse(
						messageType = ErrorRestBean.class, 
						code = StatusWriter.Code.BAD_REQUEST, 
						description = "If the posted entity could not be updated, this response will be returned. Typical reasons could be constraint violations or connectivity issues."
				)
			},
			consumes = Documentation.ANY_CONTENT_TYPE,
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, BeanConsumingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response update(
			@Documentation("The database id of the entity to update.") @PathParam("id") long id,
			@Documentation("true, if nested entities shall be persisted as well") @QueryParam("cascaded") Boolean cascaded,
			@Meta Context context
	) {
		RecordContext recordContext = engine.getAccessor().buildRecordContext();
		Record record;
		try {
			record = prepareQueryByExample(recordContext).attribute("id", id).single();
		} catch (Exception e) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id, e);
		}
		if (record == null) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id);
		}
		Object bean = getPayloadBean(context);
		if (beanIdField != null) {
			ReflectionUtil.setFieldValue(beanIdField, id, bean);
		}
		ActiveRecord schemaBean = (ActiveRecord) mapJAXBInputToSchemaBean(bean, id, true);
		if (cascaded != null && cascaded) {
			schemaBean.updateCascaded();
		} else {
			schemaBean.update();
		}
		return Response.NO_CONTENT;
	}

	@DELETE
	@Path("{id}")
	@AtomLink(descriptor = SelfLinkDescriptor.class)
	@Documentation(
			summary = "Delete a {{TYPE.NAME}}",
			authors = "bndly@bndly.org",
			value = "This resource deletes a single instance of {{TYPE.NAME}}. If the resource does not exist, a 404 is returned. If entity could be deleted, a 204 is returned.",
			responses = {
				@DocumentationResponse(code = StatusWriter.Code.NO_CONTENT, description = "The entity with the database id did exist and could be deleted."),
				@DocumentationResponse(messageType = ErrorRestBean.class, code = StatusWriter.Code.NOT_FOUND, description = "No entity with the id exists in the database."),
				@DocumentationResponse(
						messageType = ErrorRestBean.class, 
						code = StatusWriter.Code.BAD_REQUEST, 
						description = "If the entity could not be deleted, this response will be returned. Typical reasons could be integrity violations or connectivity issues."
				)
			},
			tags = { "entityresource", "schema" },
			documentationDecorator = { TypeNameReplacingDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class }
	)
	public Response delete(@Documentation("The database id of the entity to update.") @PathParam("id") long id) {
		Record record;
		try {
			RecordContext recordContext = engine.getAccessor().buildRecordContext();
			record = prepareQueryByExample(recordContext).lazy().attribute("id", id).single();
		} catch (Exception e) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id, e);
		}
		if (record == null) {
			throw new UnknownResourceException("could not read " + type.getName() + " with id " + id);
		}
		Transaction tx = engine.getQueryRunner().createTransaction();
		deletionStrategy.delete(record, tx);
		tx.commit();
		return Response.NO_CONTENT;
	}

	private Response seeOther(String uri) {
		int s = 302;
		if (corsRequestDetector.isCORSRequest()) {
			s = 201;
		}
		return Response.status(s).location(uri);
	}

	private Object getPayloadBean(Context context) {
		Object bean;
		try (ReplayableInputStream is = context.getInputStream()) {
			if (context.getInputContentType() != null && context.getInputContentType().getName().startsWith("application/json")) {
				Configuration config = new Configuration();
				MappedNamespaceConvention con = new MappedNamespaceConvention(config);
				StringWriter sw = new StringWriter();
				IOUtils.copy(is, sw);
				sw.flush();
				sw.close();
				JSONObject obj = new JSONObject(sw.getBuffer().toString());
				MappedXMLStreamReader stream = new MappedXMLStreamReader(obj, con);
				bean = jaxbContext.createUnmarshaller().unmarshal(stream);
			} else {
				bean = jaxbContext.createUnmarshaller().unmarshal(is);
			}
		} catch (XMLStreamException | JSONException | IOException | JAXBException e) {
			throw new IllegalStateException(e);
		}
		return bean;
	}

	private Record buildQueryRecord(Record source, NamedAttributeHolderAttribute attribute) {
		Record r;
		Object v = source.getAttributeValue(attribute.getName());
		String nestedRecordTypeName = attribute.getNamedAttributeHolder().getName();

		if (Record.class.isInstance(v)) {
			nestedRecordTypeName = ((Record) v).getType().getName();
		}

		if (Long.class.isInstance(v)) {
			r = source.getContext().create(nestedRecordTypeName, (Long) v);
		} else if (Record.class.isInstance(v)) {
			Record vr = (Record) v;

			r = source.getContext().create(nestedRecordTypeName);
			if (vr.getId() != null) {
				r.setId(vr.getId());
			}

			copyNonNullAttributesFromRecordToRecord(vr, r);
		} else {
			r = null;
		}
		return r;

	}

	private void copyNonNullAttributesFromRecordToRecord(Record source, final Record target) {
		source.iteratePresentValues(new RecordAttributeIterator() {
			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				if (!BinaryAttribute.class.isInstance(attribute) && !InverseAttribute.class.isInstance(attribute)) {
					Object v = record.getAttributeValue(attribute.getName());
					if (v != null) {
						if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
							Record nestedRecord = buildQueryRecord(record, (NamedAttributeHolderAttribute) attribute);
							if (nestedRecord != null) {
								target.setAttributeValue(attribute.getName(), nestedRecord);
							}
						} else {
							target.setAttributeValue(attribute.getName(), v);
						}
					}
				}
			}
		});
	}

	private QueryByExample prepareFindQueryWithPrototype(Object bean, Long id, RecordContext ctx) {
		Object schemaBean = mapJAXBInputToSchemaBean(bean, null, false);
		Record r = schemaBeanFactory.getRecordFromSchemaBean(schemaBean);
		final QueryByExample qbe = prepareQueryByExample(ctx);
		if (r.getType() == type) {
			if (id != null) {
				qbe.attribute(engine.getTableRegistry().getTypeTableByType(r.getType()).getPrimaryKeyColumn().getAttribute().getName(), id);
			}
			r.iteratePresentValues(new RecordAttributeIterator() {
				@Override
				public void handleAttribute(Attribute attribute, Record record) {
					if (!BinaryAttribute.class.isInstance(attribute) && !InverseAttribute.class.isInstance(attribute)) {
						if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
							Record nestedRecord = buildQueryRecord(record, (NamedAttributeHolderAttribute) attribute);
							if (nestedRecord != null) {
								qbe.attribute(attribute.getName(), nestedRecord);
							}
						} else {
							Object v = record.getAttributeValue(attribute.getName());
							if (v != null) {
								qbe.attribute(attribute.getName(), v);
							}
						}
					}
				}
			});
		}
		return qbe;
	}

	private Object mapJAXBInputToSchemaBean(Context context) {
		return mapJAXBInputToSchemaBean(context, true);
	}

	private Object mapJAXBInputToSchemaBean(Context context, boolean resolveSmarReferences) {
		return mapJAXBInputToSchemaBean(getPayloadBean(context), null, resolveSmarReferences);
	}

	private Object mapJAXBInputToSchemaBean(Object bean, Long id, boolean resolveSmarReferences) {
		final RecordContext context = schemaBeanFactory.getEngine().getAccessor().buildRecordContext();
		Record rootRecord;
		if (id == null) {
			rootRecord = context.create(schemaBeanType.getSimpleName());
		} else {
			rootRecord = context.create(schemaBeanType.getSimpleName(), id);
		}
		Object schemaBean = schemaBeanFactory.getSchemaBean(schemaBeanType, rootRecord);
		final List<ActiveRecord> smartReferredRecords;
		if (resolveSmarReferences) {
			smartReferredRecords = new ArrayList<>();
		} else {
			smartReferredRecords = Collections.EMPTY_LIST;
		}
		MappingContext mappingContext = mapperFactory.buildContext();

		if (resolveSmarReferences) {
			mappingContext.addListener(new NoOpMappingListener() {
				@Override
				public void afterMapping(Object source, Object target, Class<?> outputType, MappingState mappingState) {
					if (SmartReferable.class.isInstance(source)) {
						Boolean isSmartRef = ((SmartReferable) source).getSmartRef();
						isSmartRef = isSmartRef == null ? false : isSmartRef;
						if (isSmartRef) {
							// look up the mapped item
							if (ActiveRecord.class.isInstance(target)) {
								ActiveRecord activeRecord = (ActiveRecord) target;
								if (activeRecord.getId() != null) {
									// reduce to a reference
									Record recordFromSchemaBean = schemaBeanFactory.getRecordFromSchemaBean(activeRecord);
									recordFromSchemaBean.dropAttributes();
									recordFromSchemaBean.setIsReference(true);
								}
								smartReferredRecords.add(activeRecord);
							}
						}
					}
				}
			});
		}
		mappingContext.map(bean, schemaBean, schemaBeanType);

		if (resolveSmarReferences) {
			for (ActiveRecord smartReferredRecord : smartReferredRecords) {
				Long idOfSmartReference = smartReferredRecord.getId();
				if (idOfSmartReference != null && !smartReferredRecord.isReference()) {
					// since we have the id, we can fall back to a reference here
					continue;
				}
				if (idOfSmartReference == null) {
					// get the record 
					if (schemaBeanFactory.isSchemaBean(smartReferredRecord)) {
						Record record = schemaBeanFactory.getRecordFromSchemaBean(smartReferredRecord);
						NQueryUtil.NQuery q = NQueryUtil.queryByExampleFromRecordWithoutNullAttributes(record);
						Iterator<Record> result = engine.getAccessor().query(q.getQueryString(), q.getQueryArgs());
						if (result.hasNext()) {
							Record r = result.next();
							record.setId(r.getId());
							record.setIsReference(true);
							record.dropAttributes();
						}
						// TODO: build a query by example
					}
				}
			}
		}
		return schemaBean;
	}
	
	private void applySortingToQuery(QueryByExample qbe, Context context) {
		ResourceURI uri = context.getURI();
		String f = getStringQueryParameter(uri, SORTING_FIELD, null);
		if (f != null) {
			qbe.orderBy(f);
			String d = getStringQueryParameter(uri, SORTING_DIRECTION, null);
			if ("ASC".equals(d)) {
				qbe.asc();
			} else if ("DESC".equals(d)) {
				qbe.desc();
			}
		}
	}

	private Pagination getCurrentPaginationInfo(Context context) {
		final ResourceURI uri = context.getURI();
		Pagination p = new Pagination() {
			@Override
			public Long getOffset() {
				return getNumericSelectorParameter(uri, PAGINATION_START, getNumericQueryParameter(uri, PAGINATION_START, 0L));
			}

			@Override
			public Long getSize() {
				return getNumericSelectorParameter(uri, PAGINATION_SIZE, getNumericQueryParameter(uri, PAGINATION_SIZE, DEFAULT_PAGINATION_SIZE));
			}
		};
		return p;
	}

	private String getStringQueryParameter(ResourceURI uri, String name, String defaultValue) {
		ResourceURI.QueryParameter p = uri.getParameter(name);
		if (p == null) {
			return defaultValue;
		}
		String raw = p.getValue();
		if (raw != null) {
			return raw;
		}
		return defaultValue;
	}

	private Long getNumericSelectorParameter(ResourceURI uri, String name, Long defaultValue) {
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (selectors == null) {
			return defaultValue;
		}
		for (ResourceURI.Selector selector : selectors) {
			if (selector.getName().startsWith(name)) {
				String raw = selector.getName().substring(name.length());
				try {
					return new Long(raw);
				} catch (NumberFormatException e) {
					// ignore this
				}
			}
		}
		return defaultValue;
	}

	private Long getNumericQueryParameter(ResourceURI uri, String name, Long defaultValue) {
		ResourceURI.QueryParameter p = uri.getParameter(name);
		if (p == null) {
			return defaultValue;
		}
		String raw = p.getValue();
		if (raw != null) {
			try {
				return new Long(raw);
			} catch (NumberFormatException e) {
				// ignore this
			}
		}
		return defaultValue;
	}

	private QueryByExample prepareQueryByExample(RecordContext recordContext) {
		QueryByExample qbe = engine.getAccessor().queryByExample(type.getName(), recordContext);
		return qbe;
	}

	private ResourceURI getSelfUri(long id, Context context) {
		ResourceURIBuilder uriBuilder = context.createURIBuilder();
		ResourceURI uri;
		uri = uriBuilder
				.pathElement(type.getSchema().getName())
				.pathElement(type.getName())
				.pathElement(Long.toString(id))
				.build();
		return uri;
	}

	private void appendQueryAttributes(List<QueryAttribute> attributes, ResourceURIBuilder uriBuilder, String prefix) {
		if (attributes != null) {
			for (QueryAttribute queryAttribute : attributes) {
				appendQueryAttribute(queryAttribute, uriBuilder, prefix);
			}
		}
	}

	private void appendQueryAttribute(QueryAttribute queryAttribute, ResourceURIBuilder uriBuilder, String prefix) {
		if (SimpleQueryAttribute.class.isInstance(queryAttribute)) {
			String fragment = queryAttribute.getAttribute().getName();
			String p = prefix;
			if (p == null) {
				p = fragment;
			} else {
				p += "_" + fragment;
			}
			String valueAsString = null;
			Object v = ((SimpleQueryAttribute) queryAttribute).getValue();
			if (v != null) {
				valueAsString = v.toString();
			}
			uriBuilder.parameter(p, valueAsString);
		} else if (NestedQueryAttribute.class.isInstance(queryAttribute)) {
			NestedQueryAttribute nqa = NestedQueryAttribute.class.cast(queryAttribute);
			String fragment = nqa.getAttribute().getName() + "_" + nqa.getHolder().getName();
			String p = prefix;
			if (p == null) {
				p = fragment;
			} else {
				p += "_" + fragment;
			}
			appendQueryAttributes(nqa.getNested(), uriBuilder, p);
		}
	}

	private void appendQueryParamtersToQueryByExample(RecordContext ctx, Context context, QueryByExample... queries) {
		ResourceURI uri = context.getURI();
		List<ResourceURI.QueryParameter> parameters = uri.getParameters();
		List<String> parameterNames = new ArrayList<>();
		if (parameters != null) {
			for (ResourceURI.QueryParameter queryParameter : parameters) {
				parameterNames.add(queryParameter.getName());
			}
		}
		Iterator<String> names = parameterNames.iterator();
		Map<String, Record> nestedAttributeRecords = new HashMap<>();
		while (names.hasNext()) {
			String parameterName = names.next();

			if (TECHNICAL_REQUEST_PARAMETERS.contains(parameterName)) {
				continue;
			}

			ResourceURI.QueryParameter param = uri.getParameter(parameterName);
			if (param != null) {
				String parameterValue = param.getValue();
				Class javaTypeForValue = javaTypesByParameterName.get(parameterName);
				Object convertedValue;
				if (javaTypeForValue == null || String.class.equals(javaTypeForValue)) {
					convertedValue = parameterValue;
				} else {
					Converter converter = converterRegistry.getConverter(String.class, javaTypeForValue);
					if (converter == null) {
						LOG.error("missing a converter to convert a string to " + javaTypeForValue.getName());
						convertedValue = parameterValue;
					} else {
						try {
							convertedValue = converter.convert(parameterValue);
						} catch (ConversionException ex) {
							LOG.error("could not convert parameter value " + parameterValue, ex);
							convertedValue = parameterValue;
						}
					}
				}
				String[] parameterNameElements = parameterName.split("_");
				if (parameterNameElements.length == 1) {
					for (QueryByExample queryByExample : queries) {
						queryByExample.attribute(parameterName, convertedValue);
					}
				} else {
					// this is a bit more complex
					String rootAttributeName = parameterNameElements[0];
					Record rec = nestedAttributeRecords.get(rootAttributeName);
					if (rec == null) {
						String typeInAttribute = parameterNameElements[1];
						rec = ctx.create(typeInAttribute);
						nestedAttributeRecords.put(rootAttributeName, rec);
					}

					List<String> list = Arrays.asList(parameterNameElements);

					setValueInRecord(rec, list, 2, convertedValue);
				}
			}
		}

		for (Map.Entry<String, Record> entry : nestedAttributeRecords.entrySet()) {
			String attribute = entry.getKey();
			Record record = entry.getValue();
			for (QueryByExample queryByExample : queries) {
				queryByExample.attribute(attribute, record);
			}
		}
	}

	private void setValueInRecord(Record rec, List<String> fragments, int indexOfAttribute, Object convertedValue) {
		String attributeName = fragments.get(indexOfAttribute);

		if (indexOfAttribute == fragments.size() - 1) {
			if ("id".equals(attributeName)) {
				if (String.class.isInstance(convertedValue)) {
					rec.setId(new Long((String) convertedValue));
				} else if (Number.class.isInstance(convertedValue)) {
					rec.setId(((Number) convertedValue).longValue());
				} else {
					LOG.warn("could not set id attribute");
				}
			} else {
				rec.setAttributeValue(attributeName, convertedValue);
			}
		} else {
			// more nesting
			Record nestedRec;
			if (rec.isAttributePresent(attributeName)) {
				nestedRec = rec.getAttributeValue(attributeName, Record.class);
			} else {
				String nestedTypeName = fragments.get(indexOfAttribute + 1);
				nestedRec = rec.getContext().create(nestedTypeName);
				rec.setAttributeValue(attributeName, nestedRec);
			}
			setValueInRecord(nestedRec, fragments, indexOfAttribute + 2, convertedValue);
		}
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	public void setSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		this.schemaBeanFactory = schemaBeanFactory;
	}

	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapperFactory = mapperFactory;
	}

	public void setDeletionStrategy(DeletionStrategy deletionStrategy) {
		this.deletionStrategy = deletionStrategy;
	}

	public void setAtomLinkConstraint(AtomLinkConstraint atomLinkConstraint) {
		this.atomLinkConstraint = atomLinkConstraint;
	}

	public void setCorsRequestDetector(CORSRequestDetector corsRequestDetector) {
		this.corsRequestDetector = corsRequestDetector;
	}

	public void setCacheLinkingService(CacheLinkingService cacheLinkingService) {
		this.cacheLinkingService = cacheLinkingService;
	}

	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

}
