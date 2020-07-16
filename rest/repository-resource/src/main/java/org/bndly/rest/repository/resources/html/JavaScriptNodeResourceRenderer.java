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
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.repository.resources.BeanResource;
import org.bndly.rest.repository.resources.NodeResource;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.beans.Bean;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class JavaScriptNodeResourceRenderer implements ResourceRenderer {

	private static final Logger LOG = LoggerFactory.getLogger(JavaScriptNodeResourceRenderer.class);
	
	public static final ContentType JAVASCRIPT = new ContentType() {
		@Override
		public String getName() {
			return "text/javascript";
		}

		@Override
		public String getExtension() {
			return "js";
		}
	};
	
	@Override
	public boolean supports(Resource resource, Context context) {
		if (NodeResource.class.isInstance(resource)) {
			Node node = ((NodeResource) resource).getNode();
			String type = node.getType();
			if (!NodeTypes.FILE.equals(type)) {
				return false;
			}
			try {
				Property contentTypeProperty = node.getProperty("contentType");
				if(contentTypeProperty.getType() != Property.Type.STRING) {
					return false;
				}
				if(!JAVASCRIPT.getName().equals(contentTypeProperty.getString())) {
					return false;
				}
			} catch(RepositoryException e) {
				return false;
			}
		} else if (BeanResource.class.isInstance(resource)) {
			Bean bean = ((BeanResource) resource).getBean();
			// There is no bean for just serving javascript
			return false;
		} else {
			return false;
		}
		ContentType dc = context.getDesiredContentType();
		if (dc == null) {
			ResourceURI.Extension ext = context.getURI().getExtension();
			if (ext == null) {
				return false;
			} else {
				return JAVASCRIPT.getExtension().equals(ext.getName());
			}
		}
		return JAVASCRIPT.getName().equals(dc.getName());
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		OutputStream os = context.getOutputStream();
		if (NodeResource.class.isInstance(resource)) {
			NodeResource nodeResource = (NodeResource) resource;	
			context.setOutputContentType(JAVASCRIPT, "UTF-8");
			try {
				Property prop = nodeResource.getNode().getProperty("data");
				if (prop.getType() == Property.Type.STRING) {
					try (Writer writer = new OutputStreamWriter(os, "UTF-8")) {
						String content = prop.getString();
						if (content != null) {
							writer.write(content);
							writer.flush();
						}
					}
				} else if (prop.getType() == Property.Type.BINARY) {
					try (InputStream content = prop.getBinary()) {
						if (content != null) {
							IOUtils.copy(content, os);
							os.flush();
						}
					}
				}
			} catch (PropertyNotFoundException e) {
				context.getStatusWriter().write(StatusWriter.Code.NOT_FOUND);
			} catch (RepositoryException e) {
				context.getStatusWriter().write(StatusWriter.Code.INTERNAL_SERVER_ERROR);
				LOG.error("could not deliver javascript: " + e.getMessage(), e);
			}
		} else {
			BeanResource beanResource = (BeanResource) resource;
			context.setOutputContentType(JAVASCRIPT, "UTF-8");
			try (Writer writer = new OutputStreamWriter(os, "UTF-8")) {
				Object data = beanResource.getBean().getProperty("data");
				if (String.class.isInstance(data)) {
					String content = (String) data;
					writer.write(content);
					writer.flush();
				} else if (InputStream.class.isInstance(data)) {
					try (InputStream content = (InputStream) data) {
						IOUtils.copy(content, os);
						os.flush();
					}
				}
			}
		}
	}
	
}
