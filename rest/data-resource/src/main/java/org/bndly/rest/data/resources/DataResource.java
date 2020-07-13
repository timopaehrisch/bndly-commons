package org.bndly.rest.data.resources;

/*-
 * #%L
 * REST Data Resource
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

import org.bndly.common.data.api.ChangeableData;
import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.api.SimpleData;
import org.bndly.common.data.api.SimpleData.LazyLoader;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.common.beans.util.ExceptionMessageUtil;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.data.beans.DataObjects;
import org.bndly.rest.data.beans.DataStoreListRestBean;
import org.bndly.rest.data.beans.DataStoreRestBean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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

@Component(service = DataResource.class, immediate = true)
@Designate(ocd = DataResource.Configuration.class)
@Path("data")
public class DataResource {
	private static final Logger LOG = LoggerFactory.getLogger(DataResource.class);
	
	@ObjectClassDefinition(
			name = "Data Resource",
			description = "The data resource binds the data stores to the REST API."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Upload repository location",
				description = "The location of temporary upload data. If no location is defined, the value of the 'java.io.tmpdir' system property is used."
		)
		String repository();
	}

	private final List<DataStore> dataStores = new ArrayList<>();
	private final ReadWriteLock dataStoresLock = new ReentrantReadWriteLock();
	private DiskFileItemFactory factory;
	
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	
	
	private FileExtensionContentTypeMapper fileExtensionContentTypeMapper;
	
	private String repository;
	
	@Reference(
			bind = "setFileExtensionContentTypeMapper",
			unbind = "unsetFileExtensionContentTypeMapper",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = FileExtensionContentTypeMapper.class
	)
	public void setFileExtensionContentTypeMapper(FileExtensionContentTypeMapper mapper) {
		fileExtensionContentTypeMapper = mapper;
	}
	
	public void unsetFileExtensionContentTypeMapper(FileExtensionContentTypeMapper mapper) {
		fileExtensionContentTypeMapper = fileExtensionContentTypeMapper == mapper ? null : fileExtensionContentTypeMapper;
	}

	private RequestContext buildRequestContext(final Context context, final String contentType) throws IOException {
		final ReplayableInputStream rpis = context.getInputStream().doReplay();
		return new RequestContext() {

			@Override
			public String getCharacterEncoding() {
				return context.getInputEncoding();
			}

			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public int getContentLength() {
				return (int) rpis.getLength();
			}

			@Override
			public ReplayableInputStream getInputStream() throws IOException {
				return rpis;
			}
		};
	}
	
	private static interface Finalizer {
		void doFinally();
	}
	
	@Activate
	public void activate(Configuration configuration) {
		repository = configuration.repository();
		File rep;
		if (repository == null) {
			// Configure a repository (to ensure a secure temp location is used)
			String ioTmpdir = System.getProperty("java.io.tmpdir");
			File tempRepo = null;
			if (ioTmpdir != null) {
				java.nio.file.Path tempDirPath = Paths.get(ioTmpdir);
				if (Files.exists(tempDirPath) && Files.isDirectory(tempDirPath)) {
					tempRepo = tempDirPath.toFile();
				}
			}
			if (tempRepo == null) {
				LOG.error("there is no temp directory defined. see 'java.io.tmpdir' system property");
			}
			rep = tempRepo;
		} else {
			rep = new File(repository);
		}
		if (rep != null) {
			factory = new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, rep);
		} else {
			LOG.error("no factory defined for file upload, because temp folder was missing");
		}
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
		factory = null;
	}

	@Reference(
		bind = "addDataStore",
		unbind = "removeDataStore",
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		service = DataStore.class
	)
	public void addDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				dataStores.add(dataStore);
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}

	public void removeDataStore(DataStore dataStore) {
		if (dataStore != null) {
			dataStoresLock.writeLock().lock();
			try {
				dataStores.remove(dataStore);
			} finally {
				dataStoresLock.writeLock().unlock();
			}
		}
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "dataStores", target = Services.class),
		@AtomLink(rel = "list", target = DataStoreRestBean.class),
		@AtomLink(target = DataStoreListRestBean.class)
	})
	public Response listDataStores() {
		DataStoreListRestBean listOfDataStores = new DataStoreListRestBean();
		dataStoresLock.readLock().lock();
		try {
			for (DataStore dataStore : dataStores) {
				DataStoreRestBean rb = new DataStoreRestBean();
				rb.setName(dataStore.getName());
				listOfDataStores.add(rb);
			}
		} finally {
			dataStoresLock.readLock().unlock();
		}
		return Response.ok(listOfDataStores);
	}

	@GET
	@Path("{store}")
	@AtomLinks({
		@AtomLink(rel = "store", target = org.bndly.rest.data.beans.Data.class, parameters = @Parameter(name = "store", expression = "${this.dataStoreName}")),
		@AtomLink(rel = "items", target = DataStoreRestBean.class, parameters = @Parameter(name = "store", expression = "${this.name}")),
		@AtomLink(target = DataObjects.class)
	})
	public Response list(@PathParam("store") String storeName) {
		DataObjects objects = null;
		dataStoresLock.readLock().lock();
		try {
			for (DataStore dataStore : dataStores) {
				if (dataStore.getName().equals(storeName)) {
					objects = new DataObjects();
					objects.setDataStoreName(storeName);
					List<Data> dataObjects = dataStore.list();
					if (dataObjects != null) {
						for (Data data : dataObjects) {
							org.bndly.rest.data.beans.Data d = mapToRestBean(data, dataStore);
							objects.add(d);
						}
					}
				}
			}
		} finally {
			dataStoresLock.readLock().unlock();
		}
		if (objects == null) {
			return Response.status(404);
		}
		return Response.ok(objects);
	}

	@POST
	@Path("{store}")
	@AtomLink(target = DataStoreRestBean.class)
	public Response create(@PathParam("store") String storeName, org.bndly.rest.data.beans.Data data, @Meta Context context) {
		DataStore dataStore = getDataStoreForName(storeName);
		if (dataStore == null) {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setMessage("no datastore found");
			ExceptionMessageUtil.createKeyValue(errorBean, "storeName", storeName);
			return Response.status(400).entity(errorBean);
		}
		SimpleData m = mapToModel(data, null);
		dataStore.create(m);
		ResourceURIBuilder builder = context.createURIBuilder();
		builder
				.pathElement("data")
				.pathElement(dataStore.getName())
				.pathElement("view")
				.pathElement(m.getName());
		return Response.created(builder.build().asString());
	}
	
	private DataStore getDataStoreForName(String dataStoreName) {
		if (dataStoreName == null) {
			throw new IllegalArgumentException("store name is not allowed to be null");
		}
		dataStoresLock.readLock().lock();
		try {
			Iterator<DataStore> it = dataStores.iterator();
			while (it.hasNext()) {
				DataStore next = it.next();
				if (dataStoreName.equals(next.getName())) {
					return next;
				}
			}
		} finally {
			dataStoresLock.readLock().unlock();
		}
		throw new IllegalArgumentException("data store with name '" + dataStoreName + "' not found");
	}

	@POST
	@Path("{store}/find")
	@AtomLink(rel = "find", target = DataStoreRestBean.class)
	public Response find(@PathParam("store") String storeName, org.bndly.rest.data.beans.Data data, @Meta Context context) {
		Data m = mapToModel(data, null);
		DataStore dataStore = getDataStoreForName(storeName);
		String name = m.getName();
		if (m.getContentType() == null) {
			m = dataStore.findByName(name);
		} else {
			m = dataStore.findByNameAndContentType(name, m.getContentType());
		}
		if (m == null) {
			throw new DataNotFoundException("could not find data " + data.getName());
		}
		ResourceURIBuilder builder = context.createURIBuilder();
		builder
				.pathElement("data")
				.pathElement(dataStore.getName())
				.pathElement("view")
				.pathElement(name);
		return seeOther(builder.build());
	}

	private Response seeOther(ResourceURI uri) {
		return Response.status(StatusWriter.Code.FOUND.getHttpCode()).location(uri.asString());
	}

	@GET
	@Path("{store}/view/{name}")
	@AtomLinks({
		@AtomLink(target = org.bndly.rest.data.beans.Data.class, parameters = @Parameter(name = "store", expression = "${this.dataStoreName}"), isContextExtensionEnabled = false),
		@AtomLink(rel = "data", targetName = "org.bndly.rest.beans.BusinessProcessDefinition", parameters = {
			@Parameter(name = "store", expression = "${this.engineName}"),
			@Parameter(name = "name", expression = "${this.resourceName}")
		}, isContextExtensionEnabled = false),
		@AtomLink(rel = "diagramData", targetName = "org.bndly.rest.beans.BusinessProcessDefinition", parameters = {
			@Parameter(name = "store", expression = "${this.engineName}"),
			@Parameter(name = "name", expression = "${this.diagramResourceName}")
		}, isContextExtensionEnabled = false)
	})
	public Response read(@PathParam("store") String storeName, @PathParam("name") String name, @Meta Context context) {
		name = getDataNameFromContext(context);
		DataStore dataStore = getDataStoreForName(storeName);
		Data d = dataStore.findByName(name);
		if (d == null) {
			return Response.status(404);
		}
		org.bndly.rest.data.beans.Data rb = mapToRestBean(d, dataStore);
		return Response.ok(rb);
	}

	@GET
	@Path("{store}/bin/{name}")
	@AtomLink(rel = "download", target = org.bndly.rest.data.beans.Data.class, parameters = @Parameter(name = "store", expression = "${this.dataStoreName}"), isContextExtensionEnabled = false)
	public Response download(@PathParam("store") String storeName, @PathParam("name") String name, @Meta Context context) {
		name = getDataNameFromContext(context);
		DataStore dataStore = getDataStoreForName(storeName);
		final Data d = dataStore.findByName(name);
		return download(d);
	}

	public Response download(final Data data) {
		if (data == null) {
			return Response.status(404);
		}
		ReplayableInputStream is = data.getInputStream();
		Response r;
		if (is == null) {
			r = Response.NO_CONTENT;
		} else {
			r = Response.ok(is);
			if (data.getContentType() != null) {
				r.contentType(new ContentType() {

					@Override
					public String getName() {
						return data.getContentType();
					}

					@Override
					public String getExtension() {
						String name = data.getName();
						int i = name.lastIndexOf(".");
						if (i < 0) {
							return null;
						}
						return name.substring(i + 1);
					}

				});
			}
		}
		return r;
	}

	private String getDataNameFromContext(Context context) {
		String name;
		ResourceURI uri = context.getURI();
		StringBuffer sb = null;
		ResourceURI.Path p = uri.getPath();
		if (p != null) {
			for (int i = 0; i < p.getElements().size(); i++) {
				String string = p.getElements().get(i);
				// skip the 'data' prefix
				if (i > 2) {
					if (sb == null) {
						sb = new StringBuffer();
					} else {
						sb.append('/');
					}
					sb.append(string);
				}
			}
		}
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (selectors != null) {
			for (ResourceURI.Selector selector : selectors) {
				if (sb == null) {
					sb = new StringBuffer();
				} else {
					sb.append('.');
				}
				sb.append(selector.getName());
			}
		}
		ResourceURI.Extension ext = uri.getExtension();
		if (ext != null) {
			if (sb == null) {
				sb = new StringBuffer();
			} else {
				sb.append('.');
			}
			sb.append(ext.getName());
		}
		name = sb != null ? sb.toString() : null;
		return name;
	}

	@POST
	@Path("{store}/bin/{name}")
	@AtomLink(rel = "upload", target = org.bndly.rest.data.beans.Data.class, parameters = @Parameter(name = "store", expression = "${this.dataStoreName}"), isContextExtensionEnabled = false)
	public Response upload(@PathParam("store") String storeName, @PathParam("name") String name, @Meta Context context) {
		name = getDataNameFromContext(context);
		DataStore dataStore = getDataStoreForName(storeName);
		Data d = dataStore.findByName(name);
		if (d == null) {
			SimpleData sd = new SimpleData(null);
			sd.setCreatedOn(new Date());
			sd.setName(name);
			applyContentTypeToData(context, sd);
			d = dataStore.create(sd);
		}
		return upload((ChangeableData) d, dataStore, context);
	}

	protected void applyContentTypeToData(Context context, ChangeableData sd) {
		ContentType ct = context.getInputContentType();
		String contentType = ct == null ? null : ct.getName();
		if (contentType == null && fileExtensionContentTypeMapper != null) {
			ResourceURI.Extension ext = context.getRequestURI().getExtension();
			if (ext != null) {
				contentType = fileExtensionContentTypeMapper.mapExtensionToContentType(ext.getName());
			}
		}
		sd.setContentType(contentType);
	}

	public Response upload(Data data, DataStore dataStore, Context context) {
		if (data == null) {
			return Response.status(404);
		}
		if (!ChangeableData.class.isInstance(data)) {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setMessage("could not change data because retrieved data object was not changeable");
			errorBean.setName("unchangeableData");
			return Response.status(400).entity(errorBean);
		} else {
			return upload((ChangeableData) data, dataStore, context);
		}
	}

	private Response upload(ChangeableData data, DataStore dataStore, Context context) {
		Finalizer finalizer = loadRequestBytesIntoData(data, context);
		try {
			dataStore.update(data);
			return Response.NO_CONTENT;
		} finally {
			if (finalizer != null) {
				finalizer.doFinally();
			}
		}
	}

	private Finalizer loadRequestBytesIntoData(ChangeableData d, Context context) {
		ContentType ct = context.getInputContentType();
		final String contentType;
		if (ct != null) {
			contentType = ct.getName();
		} else {
			contentType = null;
		}
		if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
			// Create a new file upload handler
			if (factory == null) {
				LOG.error("can not handle upload without a org.apache.commons.fileupload.disk.DiskFileItemFactory");
				return null;
			}
			ServletFileUpload upload = new ServletFileUpload(factory);
			final List<FileItem> items;
			try {
				// Parse the request
				items = upload.parseRequest(buildRequestContext(context, context.getHeaderReader().read("Content-Type")));
			} catch (IOException | FileUploadException ex) {
				LOG.error("file upload failed: " + ex.getMessage(), ex);
				return null;
			}
			if (items != null) {
				Finalizer finalizer = new Finalizer() {

					@Override
					public void doFinally() {
						for (FileItem item : items) {
							item.delete();
						}
					}
				};
				for (FileItem fileItem : items) {
					String fileContentType = fileItem.getContentType();
					if (fileContentType != null) {
						d.setContentType(fileContentType);
					}
					try {
						d.setInputStream(ReplayableInputStream.newInstance(fileItem.getInputStream()));
					} catch (IOException ex) {
						LOG.error("could not retrieve inputstream from uploaded file item");
					}
				}
				return finalizer;
			}
		} else {
			if (contentType != null) {
				d.setContentType(contentType);
			}
			d.setInputStream(getPayloadAsInputStream(context));
		}
		return null;
	}

	private ReplayableInputStream getPayloadAsInputStream(Context context) {
		try {
			ReplayableInputStream is = ReplayableInputStream.newInstance(context.getInputStream());
			return is;
		} catch (IOException ex) {
			LOG.error("failed to get payload as input stream: " + ex.getMessage(), ex);
		}
		return null;
	}

	private org.bndly.rest.data.beans.Data mapToRestBean(Data data, DataStore dataStore) {
		org.bndly.rest.data.beans.Data d = new org.bndly.rest.data.beans.Data();
		d.setName(data.getName());
		d.setContentType(data.getContentType());
		d.setCreatedOn(data.getCreatedOn());
		d.setUpdatedOn(data.getUpdatedOn());
		d.setDataStoreName(dataStore.getName());
		return d;
	}

	private SimpleData mapToModel(org.bndly.rest.data.beans.Data data, LazyLoader lazyLoader) {
		SimpleData d = new SimpleData(lazyLoader);
		d.setName(data.getName());
		d.setContentType(data.getContentType());
		d.setCreatedOn(data.getCreatedOn());
		d.setUpdatedOn(data.getUpdatedOn());
		return d;
	}

	public void setFactory(DiskFileItemFactory factory) {
		this.factory = factory;
	}

}
