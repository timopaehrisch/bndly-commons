package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = PayloadToNodeConverter.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MultipartFormDataPayloadToNodeConverter.Configuration.class)
public class MultipartFormDataPayloadToNodeConverter implements PayloadToNodeConverter {
	private static final Logger LOG = LoggerFactory.getLogger(MultipartFormDataPayloadToNodeConverter.class);
	
	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Temp Folder",
				description = "Location to which upload items of mime multi part form data will be put"
		)
		String tempFolder() default "temp";
	}
	
	private DiskFileItemFactory factory;
	
	@Reference
	private ParameterPayloadToNodeConverter parameterPayloadToNodeConverter;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());
		factory = new DiskFileItemFactory();
		Path path = Paths.get(dictionaryAdapter.getString("tempFolder", "temp"));
		factory.setRepository(path.toFile());
	}
	
	@Override
	public void convertPayload(Node node, Context context) throws RepositoryException, IOException {
		ServletFileUpload upload = new ServletFileUpload(factory);
		// Parse the request
		try {
			List<FileItem> items = upload.parseRequest(createRequestContext(context));
			if (items != null) {
				ModificationManagerImpl modificationManagerImpl = new ModificationManagerImpl();
				try {
					for (FileItem item : items) {
						if (item.isFormField()) {
							parameterPayloadToNodeConverter.handleParameter(item.getFieldName(), item.getString(), null, node, modificationManagerImpl);
						} else {
							String name = item.getName();
							if (item.getSize() == 0 && (name == null || name.isEmpty())) {
								parameterPayloadToNodeConverter.handleParameter(item.getFieldName(), null, null, node, modificationManagerImpl);
							} else {
								InputStream is = item.getInputStream();
								parameterPayloadToNodeConverter.handleParameter(item.getFieldName(), null, is, node, modificationManagerImpl);
							}
						}
					}
					modificationManagerImpl.flush();
				} finally {
					for (FileItem item : items) {
						silentlyDeleteItem(item);
					}
				}
			}
		} catch (FileUploadException e) {
			throw new IOException("could not handle form upload: " + e.getMessage(), e);
		}
	}

	@Override
	public ContentType getSupportedContentType() {
		return ContentType.MULTIPART_FORM_DATA;
	}

	private RequestContext createRequestContext(final Context context) {
		final String contentType = context.getHeaderReader().read("Content-Type");
		Object length = context.getHeaderReader().read("Content-Length");
		final int finalLength;
		if (length != null) {
			try {
				length = Integer.valueOf((String) length);
			} catch (NumberFormatException e) {
				length = -1;
			}
			finalLength = (int) length;
		} else {
			finalLength = -1;
		}
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
				return finalLength;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return context.getInputStream().noOp(); // the stream will not be re-read elsewhere
			}
		};
	}

	private void silentlyDeleteItem(FileItem item) {
		try {
			item.delete();
		} catch (Exception e) {
			LOG.error("could not delete upload item: " + e.getMessage(), e);
		}
	}
	
}
