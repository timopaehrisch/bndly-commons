package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Context {
	ReplayableInputStream getInputStream() throws IOException;
	PathCoder createPathCoder();
	ContentType getInputContentType();
	HeaderWriter getHeaderWriter();
	HeaderReader getHeaderReader();
	StatusWriter getStatusWriter();
	OutputStream getOutputStream() throws IOException;
	@Deprecated
	void setOutputContentType(ContentType contentType);
	void setOutputContentType(ContentType contentType, String charset);
	ContentType getOutputContentType();
	String getOutputEncoding();
	HTTPMethod getMethod();
	CacheContext getCacheContext();
	SecurityContext getSecurityContext();
	void setCacheHandler(CacheHandler cacheHandler);
	void setSecurityHandler(SecurityHandler securityHandler);
	ByteServingContext getByteServingContext();
	ResourceURIBuilder createURIBuilder();
	ResourceURI getURI();
	ResourceURI getRequestURI();
	void setLocation(ResourceURI locationURI);
	Locale getLocale();
	ContentType getDesiredContentType();
	List<QuantifiedContentType> getDesiredContentTypes();
	String getInputEncoding();
	StatusWriter.Code getStatus();
	void setOutputHeader(String name, String value);
	void setOutputContentLanguage(String contentLanguage);
	ResourceURI parseURI(String uriAsString);
	boolean canBeServedFromCache();
	boolean canBeCached();
	void serveFromCache();
	void saveInCache(ReplayableInputStream is);
}
