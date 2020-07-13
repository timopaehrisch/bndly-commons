package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.controller.api.EntityParser;
import org.bndly.rest.controller.api.EntityRenderer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class JSONGlobalEntityRenderer implements EntityRenderer, EntityParser {

	private final ReadWriteLock contextLock;
	private final Map xmlToJsonNamespaces;
	protected abstract JAXBContext getJAXBContext();

	public JSONGlobalEntityRenderer(ReadWriteLock contextLock, Map<String, String> xmlToJsonNamespaces) {
		this.contextLock = contextLock;
		this.xmlToJsonNamespaces = xmlToJsonNamespaces;
	}
	
	@Override
	public ContentType getSupportedContentType() {
		return ContentType.JSON;
	}

	@Override
	public String getSupportedEncoding() {
		return "UTF-8";
	}

	@Override
	public void render(Object entity, OutputStream os) throws IOException {
		contextLock.readLock().lock();
		try {
			JAXBContext ctx = getJAXBContext();

			Configuration config = new Configuration();
			config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
			MappedNamespaceConvention con = new MappedNamespaceConvention(config);
			Writer writer = new OutputStreamWriter(os, getSupportedEncoding());
			XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer);

			Marshaller marshaller = ctx.createMarshaller();
			marshaller.marshal(entity, xmlStreamWriter);
		} catch (JAXBException ex) {
			throw new IOException("could not create JAXB context while rendering entity", ex);
		} finally {
			contextLock.readLock().unlock();
		}
	}

	@Override
	public Object parse(ReplayableInputStream is, Class<?> requiredType) throws IOException {
		contextLock.readLock().lock();
		try {
			JAXBContext ctx = getJAXBContext();

			Configuration config = new Configuration();
			config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
			MappedNamespaceConvention con = new MappedNamespaceConvention(config);
			String jsonString = IOUtils.readToString(is, "UTF-8");
			JSONObject object = new JSONObject(jsonString);
			XMLStreamReader reader = new MappedXMLStreamReader(object, con);

			Unmarshaller unmarshaller = ctx.createUnmarshaller();
			return unmarshaller.unmarshal(reader);
		} catch (JAXBException ex) {
			throw new IOException("could not create JAXB context while parsing entity", ex);
		} catch (JSONException ex) {
			throw new IOException("could not create json object while parsing entity", ex);
		} catch (XMLStreamException ex) {
			throw new IOException("could not read xml mapped json stream while parsing entity", ex);
		} finally {
			contextLock.readLock().unlock();
		}
	}

}
