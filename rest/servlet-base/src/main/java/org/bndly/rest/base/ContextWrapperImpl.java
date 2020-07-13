package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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
import org.bndly.rest.api.ByteServingContext;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.CacheHandler;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.HeaderReader;
import org.bndly.rest.api.HeaderWriter;
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.api.QuantifiedContentType;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.api.SecurityContext;
import org.bndly.rest.api.SecurityHandler;
import org.bndly.rest.api.StatusWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ContextWrapperImpl implements Context {
	private final Context wrapped;

	public ContextWrapperImpl(Context wrapped) {
		if (wrapped == null) {
			throw new IllegalStateException("can not wrap null context");
		}
		this.wrapped = wrapped;
	}

	protected final Context getWrapped() {
		return wrapped;
	}

	@Override
	public ByteServingContext getByteServingContext() {
		return wrapped.getByteServingContext();
	}

	@Override
	public ReplayableInputStream getInputStream() throws IOException {
		return wrapped.getInputStream();
	}

	@Override
	public PathCoder createPathCoder() {
		return wrapped.createPathCoder();
	}

	@Override
	public HeaderWriter getHeaderWriter() {
		return wrapped.getHeaderWriter();
	}

	@Override
	public HeaderReader getHeaderReader() {
		return wrapped.getHeaderReader();
	}

	@Override
	public StatusWriter getStatusWriter() {
		return wrapped.getStatusWriter();
	}

	@Override
	public ContentType getInputContentType() {
		return wrapped.getInputContentType();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return wrapped.getOutputStream();
	}

	@Override
	public void setOutputContentType(ContentType contentType) {
		wrapped.setOutputContentType(contentType);
	}

	@Override
	public void setOutputContentType(ContentType contentType, String encoding) {
		wrapped.setOutputContentType(contentType, encoding);
	}

	@Override
	public void setOutputContentLanguage(String contentLanguage) {
		wrapped.setOutputContentLanguage(contentLanguage);
	}
	
	@Override
	public HTTPMethod getMethod() {
		return wrapped.getMethod();
	}

	@Override
	public CacheContext getCacheContext() {
		return wrapped.getCacheContext();
	}

	@Override
	public void setCacheHandler(CacheHandler cacheHandler) {
		wrapped.setCacheHandler(cacheHandler);
	}

	@Override
	public ResourceURIBuilder createURIBuilder() {
		return wrapped.createURIBuilder();
	}

	@Override
	public ResourceURI getURI() {
		return wrapped.getURI();
	}

	@Override
	public ResourceURI getRequestURI() {
		return wrapped.getRequestURI();
	}

	@Override
	public void setLocation(ResourceURI locationURI) {
		wrapped.setLocation(locationURI);
	}

	@Override
	public Locale getLocale() {
		return wrapped.getLocale();
	}

	@Override
	public ContentType getDesiredContentType() {
		return wrapped.getDesiredContentType();
	}

	@Override
	public List<QuantifiedContentType> getDesiredContentTypes() {
		return wrapped.getDesiredContentTypes();
	}

	@Override
	public String getInputEncoding() {
		return wrapped.getInputEncoding();
	}

	@Override
	public StatusWriter.Code getStatus() {
		return wrapped.getStatus();
	}

	@Override
	public void setOutputHeader(String name, String value) {
		wrapped.setOutputHeader(name, value);
	}

	@Override
	public ResourceURI parseURI(String uriAsString) {
		return wrapped.parseURI(uriAsString);
	}

	@Override
	public boolean canBeServedFromCache() {
		return wrapped.canBeServedFromCache();
	}

	@Override
	public boolean canBeCached() {
		return wrapped.canBeCached();
	}

	@Override
	public void serveFromCache() {
		wrapped.serveFromCache();
	}

	@Override
	public void saveInCache(ReplayableInputStream is) {
		wrapped.saveInCache(is);
	}

	@Override
	public ContentType getOutputContentType() {
		return wrapped.getOutputContentType();
	}

	@Override
	public String getOutputEncoding() {
		return wrapped.getOutputEncoding();
	}

	@Override
	public SecurityContext getSecurityContext() {
		return wrapped.getSecurityContext();
	}

	@Override
	public void setSecurityHandler(SecurityHandler securityHandler) {
		wrapped.setSecurityHandler(securityHandler);
	}
	
}
