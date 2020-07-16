package org.bndly.rest.jsonp.renderer;

/*-
 * #%L
 * REST JSON-P Renderer
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

import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.jaxb.renderer.JAXBResourceRenderer;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

public class JSONPMessageProvider implements ResourceRenderer {

	@Override
	public boolean supports(Resource resource, Context context) {
		ResourceURI uri = resource.getURI();
		if (uri.getExtension() != null && "js".equals(uri.getExtension().getName())) {
			return isJaxbResource(resource);
		}
		if (context.getDesiredContentType() != null && "text/javascript".equals(context.getDesiredContentType().getName())) {
			return isJaxbResource(resource);
		}
		return false;
	}

	private boolean isJaxbResource(Resource resource) {
		return JAXBResourceRenderer.JAXBResource.class.isInstance(resource);
	}
	
	@Override
	public void render(Resource resource, Context context) throws IOException {
		JAXBResourceRenderer.JAXBResource jaxbResource = (JAXBResourceRenderer.JAXBResource) resource;
		OutputStreamWriter osw = new OutputStreamWriter(context.getOutputStream(), "UTF-8");
		String callbackName = getCallbackNameFromUri(context.getURI());
		osw.write(callbackName);
		osw.write("(");
		osw.flush();

		Configuration config = new Configuration();
		MappedNamespaceConvention con = new MappedNamespaceConvention(config);
		XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, osw);
		Class<?> type = jaxbResource.getRootType();
		Object t = jaxbResource.getRootObject();
		try {
			JAXBContext jc = JAXBContext.newInstance(type);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.marshal(t, xmlStreamWriter);
			osw.flush();
			osw.write(");");
			osw.flush();
		} catch (JAXBException ex) {
			throw new IOException("failed to render resource with jaxb", ex);
		} finally {
			osw.close();
		}
	}

	private String getCallbackNameFromUri(ResourceURI uri) {
		String callbackName = "define";
		ResourceURI.QueryParameter cb = uri.getParameter("callback");
		if (cb != null && cb.getValue() != null) {
			callbackName = cb.getValue();
		}
		return callbackName;
	}

}
