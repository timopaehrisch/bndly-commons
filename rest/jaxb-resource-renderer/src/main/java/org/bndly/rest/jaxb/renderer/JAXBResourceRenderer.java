package org.bndly.rest.jaxb.renderer;

/*-
 * #%L
 * REST JAXB Resource Renderer
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

import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JAXBResourceRenderer implements ResourceRenderer {

	private static final Logger LOG = LoggerFactory.getLogger(JAXBResourceRenderer.class);
	public static interface JAXBResource {

		Class<?> getRootType();

		Object getRootObject();
	}

	@Override
	public boolean supports(Resource resource, Context context) {
		ResourceURI.Extension ext = resource.getURI().getExtension();
		return ext != null
				&& "xml".equals(ext.getName())
				&& JAXBResource.class.isInstance(resource)
				&& ((JAXBResource) resource).getRootType().isAnnotationPresent(XmlRootElement.class);
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		try {
			JAXBResource jAXBResource = (JAXBResource) resource;
			JAXBContext jaxbContext = JAXBContext.newInstance(jAXBResource.getRootType());
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.marshal(jAXBResource.getRootObject(), new OutputStreamWriter(context.getOutputStream(), "UTF-8"));
			context.setOutputHeader("Content-Type", ContentType.XML.getName());
		} catch (JAXBException ex) {
			// ignore this exception.
			LOG.error("could not render jaxb resource", ex);
		}
	}

}
