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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * The SlingURIImpl is a static immutable model instance for a SlingURI.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class ResourceURIImpl implements ResourceURI {

	private final Path path;
	private final List<Selector> selectors;
	private final List<QueryParameter> parameters;
	private final Extension extension;
	private final String suffix;
	private final String fragment;
	private final String scheme;
	private final String host;
	private final Integer port;
	private final Map<String, QueryParameter> parametersByName;

	public ResourceURIImpl(Path path, List<Selector> selectors, List<QueryParameter> parameters, Extension extension, String suffix, String scheme, String host, Integer port, String fragment) {
		final List<String> el = path == null ? null : Collections.unmodifiableList(path.getElements());
		final String ext = extension == null ? null : extension.getName();
		this.path = path == null ? null : new Path() {

			@Override
			public List<String> getElements() {
				return el;
			}

			@Override
			public Iterator<String> iterator() {
				return el.iterator();
			}
		};
		this.selectors = selectors == null ? null : Collections.unmodifiableList(selectors);
		this.parameters = parameters == null ? null : Collections.unmodifiableList(parameters);
		this.parametersByName = new HashMap<>();
		if (parameters != null) {
			for (QueryParameter queryParameter : parameters) {
				parametersByName.put(queryParameter.getName(), queryParameter);
			}
		}
		this.extension = extension == null ? null : new Extension() {

			@Override
			public String getName() {
				return ext;
			}
		};
		this.suffix = suffix;
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.fragment = fragment;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public List<Selector> getSelectors() {
		return selectors;
	}

	@Override
	public List<QueryParameter> getParameters() {
		return parameters;
	}

	@Override
	public Extension getExtension() {
		return extension;
	}

	@Override
	public String getSuffix() {
		return suffix;
	}

	@Override
	public String getFragment() {
		return fragment;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public Integer getPort() {
		return port;
	}

	@Override
	public boolean hasSchemeHostPort() {
		return hasSchemeHost() && port != null;
	}

	@Override
	public boolean hasSchemeHost() {
		return scheme != null && host != null;
	}

	@Override
	public String asString() {
		StringBuffer sb = new StringBuffer();
		if (scheme != null) {
			sb.append(scheme);
		}
		if (host != null) {
			if ("http".equals(scheme) || "https".equals(scheme)) {
				sb.append("://").append(host);
			} else {
				sb.append(":").append(host);
			}
		}
		if (port != null) {
			sb.append(':').append(port);
		}
		if (path != null) {
			for (String element : path.getElements()) {
				String enc = PathCoder.encode(element);
				sb.append('/').append(enc);
			}
		}
		if (selectors != null) {
			for (Selector selector : selectors) {
				sb.append('.').append(selector.getName());
			}
		}
		if (extension != null) {
			sb.append('.').append(extension.getName());
		}
		if (suffix != null) {
			if (selectors == null && extension == null) {
				sb.append('.');
			}
			sb.append(suffix);
		}
		if (parameters != null) {
			sb.append('?');
			boolean isFirst = true;
			for (QueryParameter queryParameter : parameters) {
				if (!isFirst) {
					sb.append('&');
				}
				try {
					String name = URLEncoder.encode(queryParameter.getName(), "UTF-8");
					sb.append(name);
				} catch (UnsupportedEncodingException ex) {
					throw new IllegalStateException("could not encode query parameter name: " + queryParameter.getName(), ex);
				}
				if (queryParameter.getValue() != null) {
					sb.append('=');
					try {
						String value = URLEncoder.encode(queryParameter.getValue(), "UTF-8");
						sb.append(value);
					} catch (UnsupportedEncodingException ex) {
						throw new IllegalStateException("could not encode query parameter name: " + queryParameter.getName(), ex);
					}
				}
				isFirst = false;
			}
		}
		if (fragment != null) {
			sb.append('#').append(fragment);
		}
		return sb.toString();
	}

	@Override
	public String pathAsString() {
		StringBuffer sb = new StringBuffer();
		if (path != null) {
			for (String element : path.getElements()) {
				String enc = PathCoder.encode(element);
				sb.append('/').append(enc);
			}
		}
		return sb.toString();
	}

	@Override
	public QueryParameter getParameter(String name) {
		return parametersByName.get(name);
	}

}
