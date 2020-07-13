package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class Response {
	public static final Response NO_CONTENT = new Response() {

		@Override
		public String getLocation() {
			return null;
		}

		@Override
		public Object getEntity() {
			return null;
		}

		@Override
		public int getStatus() {
			return 204;
		}
		
	};
	
	public static Response ok() {
		Response r = new Response();
		r.setStatus(200);
		return r;
	}
	
	public static Response ok(Object entity) {
		return ok().entity(entity);
	}
	
	public static Response status(int status) {
		if (status < 200 || status >= 600) {
			throw new IllegalArgumentException("status start at 200 and never go to 600");
		}
		Response r = new Response();
		r.setStatus(status);
		return r;
	}
	
	public static Response created(String uri) {
		try {
			return created(new URI(uri));
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	public static Response created(URI uri) {
		if (uri == null) {
			throw new IllegalArgumentException("can not create a 'created' response without an uri");
		}
		Response r = new Response();
		r.setStatus(201);
		r.location(uri);
		return r;
	}

	public static Response seeOther(String uri) {
		Response r = created(uri);
		r.setStatus(303);
		return r;
	}

	private Object entity;
	private int status = 200;
	private String location;
	private Map<String, String> header;
	private ContentType contentType;
	private String encoding;

	public Object getEntity() {
		return entity;
	}

	public void setEntity(Object entity) {
		this.entity = entity;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Map<String, String> getHeaders() {
		return header;
	}

	public Response header(String name, String value) {
		if (name != null && value != null) {
			if (this.header == null) {
				header = new HashMap<>();
			}
			header.put(name, value);
		}
		return this;
	}

	public Response entity(Object entity) {
		setEntity(entity);
		return this;
	}

	public Response location(URI uri) {
		if (uri != null) {
			setLocation(uri.toString());
		} else {
			setLocation(null);
		}
		return this;
	}
	
	@Deprecated
	public Response contentType(ContentType contentType) {
		return contentType(contentType, null);
	}
	
	public Response contentType(ContentType contentType, String encoding) {
		setContentType(contentType, encoding);
		return this;
	}
	
	public Response location(String uri) {
		if (uri == null) {
			return location((URI) null);
		}
		try {
			return location(new URI(uri));
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public ContentType getContentType() {
		return contentType;
	}

	public String getEncoding() {
		return encoding;
	}

	private void setContentType(ContentType contentType, String encoding) {
		this.contentType = contentType;
		this.encoding = encoding;
	}

}
