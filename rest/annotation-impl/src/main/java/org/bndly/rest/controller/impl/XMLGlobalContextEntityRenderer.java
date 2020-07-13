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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.controller.api.EntityParser;
import org.bndly.rest.controller.api.EntityRenderer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.locks.ReadWriteLock;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XMLGlobalContextEntityRenderer implements EntityRenderer, EntityParser {
	private static final Logger LOG = LoggerFactory.getLogger(XMLGlobalContextEntityRenderer.class);
	private final ReadWriteLock contextLock;
	protected abstract JAXBContext getJAXBContext();

	public XMLGlobalContextEntityRenderer(ReadWriteLock contextLock) {
		this.contextLock = contextLock;
	}
	
	@Override
	public ContentType getSupportedContentType() {
		return ContentType.XML;
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
			Marshaller marshaller = ctx.createMarshaller();
			try (Writer writer = new OutputStreamWriter(os, getSupportedEncoding())) {
				marshaller.marshal(entity, writer);
				writer.flush();
			} 
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
			JAXBContext localContext = getJAXBContext();
			Unmarshaller unmarshaller = localContext.createUnmarshaller();
			Object unmarshalled = unmarshaller.unmarshal(is);
			if (requiredType.isInstance(unmarshalled)) {
				return requiredType.cast(unmarshalled);
			} else {
				throw new IllegalStateException("parsed data was not of the required type");
			}
		} catch (JAXBException ex) {
			throw new IOException("could not create JAXB context while rendering entity", ex);
		} finally {
			contextLock.readLock().unlock();
		}
	}

}
