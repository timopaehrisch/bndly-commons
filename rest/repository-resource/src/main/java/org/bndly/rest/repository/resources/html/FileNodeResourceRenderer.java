package org.bndly.rest.repository.resources.html;

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

import org.bndly.common.data.io.IOUtils;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.repository.resources.NodeResource;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class)
public class FileNodeResourceRenderer implements ResourceRenderer {

	private static final Logger LOG = LoggerFactory.getLogger(FileNodeResourceRenderer.class);

	@Override
	public boolean supports(Resource resource, Context context) {
		if (!NodeResource.class.isInstance(resource)) {
			return false;
		}
		Node node = ((NodeResource) resource).getNode();
		if (!NodeTypes.FILE.equals(node.getType())) {
			return false;
		}
		if (resource.getURI().getParameter("node") != null) {
			return false;
		}
		String uriAsString = resource.getURI().asString();
		int i = uriAsString.indexOf("?");
		if (i >= 0) {
			uriAsString = uriAsString.substring(0, i);
		}
		return uriAsString.endsWith(node.getPath().toString());
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		NodeResource nr = (NodeResource) resource;
		Node node = nr.getNode();
		try {
			Property prop = node.getProperty("data");
			if (prop.getType() == Property.Type.BINARY) {
				InputStream data = prop.getBinary();
				if (data != null) {
					if (node.isHavingProperty("contentType")) {
						Property contentTypeProp = node.getProperty("contentType");
						if (contentTypeProp.getType() == Property.Type.STRING) {
							final String contentType = contentTypeProp.getString();
							if (contentType != null) {
								context.setOutputContentType(new ContentType() {
									@Override
									public String getName() {
										return contentType;
									}

									@Override
									public String getExtension() {
										return null;
									}
								}, null);
							}
						}
					}
					OutputStream os = context.getOutputStream();
					try (InputStream tmp = data) {
						IOUtils.copy(tmp, os);
						os.flush();
					}
				}
			}
		} catch (PropertyNotFoundException e) {
			// this does not matter
			return;
		} catch (RepositoryException e) {
			LOG.error("could not render repository file node: " + e.getMessage(), e);
			context.getStatusWriter().write(StatusWriter.Code.INTERNAL_SERVER_ERROR);
		}
	}

}
