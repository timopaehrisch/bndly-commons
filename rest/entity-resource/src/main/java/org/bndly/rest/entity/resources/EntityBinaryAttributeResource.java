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

import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.entity.resources.descriptor.EntityBinaryAttributeDownloadDocumentationDecorator;
import org.bndly.rest.entity.resources.descriptor.EntityBinaryAttributeLinkDescriptor;
import org.bndly.rest.entity.resources.descriptor.EntityBinaryAttributeUploadDocumentationDecorator;
import org.bndly.schema.api.LoadedAttributes;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class EntityBinaryAttributeResource {

	private static final Logger LOG = LoggerFactory.getLogger(EntityBinaryAttributeResource.class);
	private SchemaBeanFactory schemaBeanFactory;
	private final EntityResource entityResource;
	private final BinaryAttribute binaryAttribute;
	private final String contentType;
	private final ContentType restContentType;
	private final boolean usingDefaultContentType;
	private final FileExtensionContentTypeMapper fileExtensionContentTypeMapper;
	private final String contentTypeProperty;
	private Method restBeanContentTypePropertyGetter;

	public EntityBinaryAttributeResource(EntityResource entityResource, BinaryAttribute binaryAttribute, FileExtensionContentTypeMapper proxyFileExtensionContentTypeMapper) {
		this.entityResource = entityResource;
		this.binaryAttribute = binaryAttribute;
		this.fileExtensionContentTypeMapper = proxyFileExtensionContentTypeMapper;
		Map<String, String> annotations = binaryAttribute.getAnnotations();
		String ct = null;
		if (annotations != null) {
			ct = annotations.get("contentType");
			contentTypeProperty = annotations.get("contentTypeProperty");
		} else {
			contentTypeProperty = null;
		}
		if (contentTypeProperty != null) {
			try {
				restBeanContentTypePropertyGetter = entityResource.getRestBeanType().getMethod(
						"get" + contentTypeProperty.substring(0, 1).toUpperCase() + contentTypeProperty.substring(1)
				);
			} catch (NoSuchMethodException | SecurityException ex) {
				ex.printStackTrace();
			}
		}
		if (ct == null) {
			usingDefaultContentType = true;
			contentType = "application/octet-stream";
		} else {
			usingDefaultContentType = false;
			contentType = ct;
		}
		restContentType = new ContentType() {

			@Override
			public String getName() {
				return contentType;
			}

			@Override
			public String getExtension() {
				return fileExtensionContentTypeMapper.mapContentTypeToExtension(contentType);
			}
		};
	}

	public EntityResource getEntityResource() {
		return entityResource;
	}

	public BinaryAttribute getBinaryAttribute() {
		return binaryAttribute;
	}

	public ContentType getRestContentType() {
		return restContentType;
	}

	private ContentType resolveServedContentType(final String contentTypeValue) {
		if (contentTypeValue != null) {
			return new ContentType() {

				@Override
				public String getName() {
					return contentTypeValue;
				}

				@Override
				public String getExtension() {
					return fileExtensionContentTypeMapper.mapContentTypeToExtension(contentTypeValue);
				}
			};
		}
		return restContentType;
	}

	private ContentType resolveServedContentType(Record rec) {
		if (contentTypeProperty != null) {
			return resolveServedContentType(rec.getAttributeValue(contentTypeProperty, String.class));
		}
		return restContentType;
	}
	
	public ContentType getServedContentTypeFromRestBean(RestBean restBean) {
		if (restBean != null && restBeanContentTypePropertyGetter != null) {
			if (entityResource.getRestBeanType().isInstance(restBean)) {
				try {
					Object val  = restBeanContentTypePropertyGetter.invoke(restBean);
					if (String.class.isInstance(val)) {
						return resolveServedContentType((String) val);
					}
				} catch (Exception ex) {
					LOG.warn("failed to invoked getter for content type resolution", ex);
				}
			}
		}
		return restContentType;
	}

	public static class TypeNameReplacingDocumentationDecorator extends org.bndly.rest.entity.resources.descriptor.TypeNameReplacingDocumentationDecorator {
		private final EntityBinaryAttributeResource entityBinaryAttributeResource;

		public TypeNameReplacingDocumentationDecorator(EntityBinaryAttributeResource entityBinaryAttributeResource) {
			super(entityBinaryAttributeResource.getEntityResource());
			this.entityBinaryAttributeResource = entityBinaryAttributeResource;
		}

		@Override
		protected String resolveInternal(String variableName) {
			if ("ATTRIBUTE.NAME".equals(variableName)) {
				return entityBinaryAttributeResource.getBinaryAttribute().getName();
			} else if ("ATTRIBUTE.CONTENTTYPE".equals(variableName)) {
				return entityBinaryAttributeResource.getRestContentType().getName();
			}
			return super.resolveInternal(variableName);
		}
		
	}
	
	public static class SchemaAnnotationTagsDocumentationDecorator extends org.bndly.rest.entity.resources.descriptor.SchemaAnnotationTagsDocumentationDecorator {

		public SchemaAnnotationTagsDocumentationDecorator(EntityBinaryAttributeResource entityBinaryAttributeResource) {
			super(entityBinaryAttributeResource.getEntityResource());
		}
		
	}
	
	@AtomLink(rel = "upload", descriptor = EntityBinaryAttributeLinkDescriptor.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			summary = "Upload {{ATTRIBUTE.NAME}} to {{TYPE.NAME}} instance as {{ATTRIBUTE.CONTENTTYPE}}", 
			value = 
				"Upload the binary data of the attribute '{{ATTRIBUTE.NAME}}' to the given {{TYPE.NAME}} instance. The content type is expected to be '{{ATTRIBUTE.CONTENTTYPE}}'. "
				+ "You don't have to set it in the request.", 
			documentationDecorator = {
				TypeNameReplacingDocumentationDecorator.class, EntityBinaryAttributeUploadDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class
			}
	)
	@Path("{id}")
	@POST
	public Response upload(@PathParam("id") long id, @Meta Context context) {
		Accessor accessor = schemaBeanFactory.getEngine().getAccessor();
		RecordContext recordContext = accessor.buildRecordContext();
		Iterator<Record> res = accessor.query("PICK " + entityResource.getType().getName() + " o IF o.id=?", recordContext, new LoadedAttributes() {

			@Override
			public LoadedAttributes.Strategy isLoaded(Attribute attribute, String attributePath) {
				if ("id".equals(attributePath)) {
					return LoadedAttributes.Strategy.LOADED;
				} else {
					return LoadedAttributes.Strategy.NOT_LOADED;
				}
			}
		}, id);
		if (res.hasNext()) {
			Record rec = res.next();
			try (InputStream is = context.getInputStream()) {
				rec.setAttributeValue(binaryAttribute.getName(), is);
				Transaction tx = schemaBeanFactory.getEngine().getQueryRunner().createTransaction();
				accessor.buildUpdateQuery(rec, tx);
				tx.commit();
				entityResource.createCacheLinksForRecordContext(recordContext, context);
				return Response.NO_CONTENT;
			} catch (IOException ex) {
				return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode()).entity("IO exception while uploading binary value");
			}
		} else {
			throw new UnknownResourceException("could not find " + entityResource.getType().getName() + " with id " + id);
		}
	}
	
	@AtomLink(
			rel = "download", 
			descriptor = EntityBinaryAttributeLinkDescriptor.class, 
			isContextExtensionEnabled = false, 
			parameters = @Parameter(name = "extension", expression = "${controller.getServedContentTypeFromRestBean(this).getExtension()}")
	)
	@Documentation(
			authors = "bndly@bndly.org",
			summary = "Download {{ATTRIBUTE.NAME}} of {{TYPE.NAME}} instance as {{ATTRIBUTE.CONTENTTYPE}}", 
			value = 
				"Download the binary data of the attribute '{{ATTRIBUTE.NAME}}' of the given {{TYPE.NAME}} instance. The content type is '{{ATTRIBUTE.CONTENTTYPE}}'. "
				+ "It is not tested, if the data is actually conform to the declared content type.",
			documentationDecorator = {
				TypeNameReplacingDocumentationDecorator.class, EntityBinaryAttributeDownloadDocumentationDecorator.class, SchemaAnnotationTagsDocumentationDecorator.class
			}
	)
	@Path("{id}.{extension}")
	@GET
	public Response download(@PathParam("id") long id, @PathParam("extension") String extension, @Meta Context context) {
		Accessor accessor = schemaBeanFactory.getEngine().getAccessor();
		RecordContext recordContext = accessor.buildRecordContext();
		Iterator<Record> res = accessor.query("PICK " + entityResource.getType().getName() + " o IF o.id=?", recordContext, new LoadedAttributes() {

			@Override
			public LoadedAttributes.Strategy isLoaded(Attribute attribute, String attributePath) {
				if ("id".equals(attributePath) || attribute == binaryAttribute) {
					return Strategy.LOADED;
				} else {
					return Strategy.NOT_LOADED;
				}
			}
		}, id);
		if (res.hasNext()) {
			Record rec = res.next();
			Object v = rec.getAttributeValue(binaryAttribute.getName());
			if (v == null) {
				return Response.NO_CONTENT;
			}
			ContentType contentTypeServed = resolveServedContentType(rec);
			context.setOutputContentType(contentTypeServed);
			InputStream is;
			if (byte[].class.isInstance(v)) {
				is = new ByteArrayInputStream((byte[]) v);
			} else if (InputStream.class.isInstance(v)) {
				is = (InputStream) v;
			} else {
				return Response.status(StatusWriter.Code.INTERNAL_SERVER_ERROR.getHttpCode()).entity("value of attribute had unsupported type");
			}
			entityResource.createCacheLinksForRecordContext(recordContext, context);
			return Response.ok(is);
		} else {
			throw new UnknownResourceException("could not find " + entityResource.getType().getName() + " with id " + id);
		}
	}
	
	@Path("{id}")
	@GET
	public Response download(@PathParam("id") long id, @Meta Context context) {
		return download(id, null, context);
	}

	public void setSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		this.schemaBeanFactory = schemaBeanFactory;
	}
	
}
