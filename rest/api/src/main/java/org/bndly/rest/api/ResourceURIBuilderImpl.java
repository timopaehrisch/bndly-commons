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

import org.bndly.rest.api.ResourceURI.Extension;
import org.bndly.rest.api.ResourceURI.Path;
import org.bndly.rest.api.ResourceURI.Selector;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The ResourceURIBuilder is a stateful builder that should facilitate creating
 * Sling conform URIs. Path elements can be dynamically added without having to
 * bother about the presence of fragments, extensions, suffixes, selectors etc.
 * Additionally this API works with unescaped values. Escaping of values happens
 * in the {@link ResourceURI#asString()} method.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ResourceURIBuilderImpl implements ResourceURIBuilder {
	private final PathCoder pathCoder;
	private List<String> pathElements;
	private List<String> selectors;
	private Map<String, ResourceURI.QueryParameter> parameters;
	private String extension;
	private String suffix;
	private String fragment;
	private String scheme;
	private String host;
	private Integer port;

	public static ResourceURIBuilderImpl uriWithExtensionOrHtml(PathCoder pathCoder, String uri) {
		return uriWithExtensionOrFallback(pathCoder, uri, "html");
	}

	public static ResourceURIBuilderImpl uriWithExtensionOrFallback(PathCoder pathCoder, String uri, String extension) {
		ResourceURIBuilderImpl builder = new ResourceURIBuilderImpl(pathCoder, uri);
		ResourceURI u = builder.build();
		if (u.getExtension() == null && u.getPath() != null) {
			return builder.extension(extension);
		}
		return builder;
	}

	public ResourceURIBuilderImpl(PathCoder pathCoder, String uriAsStringOrResourcePath) {
		this(pathCoder, new ResourceURIParser(pathCoder, uriAsStringOrResourcePath).parse().getResourceURI());
	}

	public ResourceURIBuilderImpl(PathCoder pathCoder, ResourceURI uri) {
		if (uri != null) {
			Path p = uri.getPath();
			if (p != null && p.getElements() != null) {
				for (String string : p.getElements()) {
					pathElement(string);
				}
			}
			List<Selector> s = uri.getSelectors();
			if (s != null) {
				for (Selector selector : s) {
					selector(selector.getName());
				}
			}
			if (uri.getExtension() != null) {
				extension(uri.getExtension().getName());
			}
			suffix(uri.getSuffix());
			fragment(uri.getFragment());
			scheme(uri.getScheme());
			host(uri.getHost());
			port(uri.getPort());
		}
		this.pathCoder = pathCoder;
	}

	public ResourceURIBuilderImpl(PathCoder pathCoder) {
		this(pathCoder, (ResourceURI) null);
	}

	public ResourceURIBuilderImpl uri(URI uri) {
		scheme = null;
		host = null;
		port = null;
		if (uri != null) {
			scheme(uri.getScheme());
			host(uri.getHost());
			port(uri.getPort());
			replace(uri.getPath());
		}
		return this;
	}

	@Override
	public ResourceURIBuilderImpl replace(String pathWithSelectorsExtensionAndSuffix) {
		if (pathWithSelectorsExtensionAndSuffix == null) {
			return replace((ResourceURI) null);
		}
		ResourceURI u = new ResourceURIParser(pathCoder, pathWithSelectorsExtensionAndSuffix).parse().getResourceURI();
		return replace(u);
	}

	@Override
	public ResourceURIBuilderImpl replace(ResourceURI u) {
		if (pathElements != null) {
			pathElements.clear();
		}
		if (u != null) {
			Path p = u.getPath();
			if (p != null) {
				for (String element : p.getElements()) {
					pathElement(element);
				}
			}

		}
		if (selectors != null) {
			selectors.clear();
		}
		if (u != null) {
			if (u.getSelectors() != null) {
				for (Selector selector : u.getSelectors()) {
					selector(selector.getName());
				}
			}
		}
		extension = null;
		if (u != null) {
			if (u.getExtension() != null) {
				extension(u.getExtension().getName());
			}
		}
		suffix = null;
		fragment = null;
		parameters = null;
		if (u != null) {
			suffix(u.getSuffix());
			if (u.getParameters() != null) {
				for (ResourceURI.QueryParameter queryParameter : u.getParameters()) {
					parameter(queryParameter.getName(), queryParameter.getValue());
				}
			}
			fragment(u.getFragment());
		}
		return this;
	}

	/**
	 * Appends a path element to the builder. If <code>/</code> characters are found, the content in between will be added as path elements. e.g. 	 <code>
     * 'foo' -&gt; 'foo' will be added
	 * '/foo' -&gt; 'foo' will be added
	 * '/foo/' -&gt; 'foo' will be added
	 * 'foo/' -&gt; 'foo' will be added
	 * 'foo/bar' -&gt; 'foo' and 'bar' will be added
	 * </code> Note that selectors or extensions are not considered being a part of a path element. This means if 'foo.bar' will be added as a path element, the . will be encoded in the
	 * {@link ResourceURI#asString()} method.
	 *
	 * @param pathElement a string representation of a path element
	 * @return the builder itself
	 */
	@Override
	public ResourceURIBuilderImpl pathElement(String pathElement) {
		if (pathElement == null) {
			return this;
		}
		int start = 0;
		int i;
		while ((i = pathElement.indexOf('/', start)) > -1) {
			String el = pathElement.substring(start, i);
			addPathElement(el, false);
			start = i + 1;
		}
		if (start == 0) {
			addPathElement(pathElement, false);
		} else {
			String el = pathElement.substring(start);
			addPathElement(el, false);
		}
		return this;
	}

	@Override
	public ResourceURIBuilder emptyPathElement() {
		addPathElement("", true);
		return this;
	}

	private void addPathElement(String pathElement, boolean allowEmpty) {
		if ("".equals(pathElement) && allowEmpty) {
			// do nothing
		} else {
			if ("".equals(pathElement)) {
				return;
			}
			if (pathElement.charAt(0) == '/') {
				if (pathElement.length() == 1) {
					return;
				} else {
					pathElement = pathElement.substring(1);
				}
			}
			if ("".equals(pathElement)) {
				return;
			}
		}
		if (pathElements == null) {
			pathElements = new ArrayList<>();
		}
		pathElements.add(pathElement);
	}

	@Override
	public ResourceURIBuilderImpl selector(String selector) {
		if (selector == null) {
			return this;
		}
		if (selectors == null) {
			selectors = new ArrayList<>();
		}
		selectors.add(selector);
		return this;
	}

	@Override
	public ResourceURIBuilderImpl extension(String extension) {
		this.extension = extension;
		return this;
	}

	/**
	 * Adds a query parameter to the URI. If a parameter with the given name already exists, it will be replaces. The value can be left as null, if the parameter has no value. 
	 * The parameter name and value will be encoded with {@link URLEncoder} in {@link ResourceURI#asString()}.
	 *
	 * @param name the name of the parameter
	 * @param value either null or the string value of the parameter
	 * @return the builder itself.
	 */
	@Override
	public ResourceURIBuilderImpl parameter(final String name, final String value) {
		if (name == null) {
			return this;
		}
		if (parameters == null) {
			parameters = new HashMap<>();
		}
		parameters.put(name, new ResourceURI.QueryParameter() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getValue() {
				return value;
			}
		});
		return this;
	}

	/**
	 * Replaces the query parameters with the parameters found in the encoded query string.
	 *
	 * @param encodedQueryString an URL encoded query string. {@link URLEncoder}
	 * @return the builder itself
	 */
	@Override
	public ResourceURIBuilderImpl query(String encodedQueryString) {
		parameters = null;
		if (encodedQueryString != null) {
			// deal with the encoded string
			StringBuffer nameBuf = new StringBuffer();
			StringBuffer valueBuf = null;
			for (int i = 0; i < encodedQueryString.length(); i++) {
				char c = encodedQueryString.charAt(i);
				if ('&' == c) {
					appendQueryParameter(nameBuf, valueBuf);
					// prepare for a new parameter
					nameBuf = new StringBuffer();
					valueBuf = null;
				} else if ('=' == c) {
					if (valueBuf != null) {
						throw new IllegalArgumentException("could not parse encoded query string: " + encodedQueryString);
					}
					valueBuf = new StringBuffer();
				} else {
					if (valueBuf != null) {
						valueBuf.append(c);
					} else {
						nameBuf.append(c);
					}
				}
			}
			appendQueryParameter(nameBuf, valueBuf);
		}
		return this;
	}

	/**
	 * Add the suffix to the builder. If the suffix contains ? or # symbols, the according parts will be added as query parameters {@link #query(java.lang.String)} or as fragment
	 * {@link #fragment(java.lang.String)}.
	 *
	 * @param suffix
	 * @return
	 */
	@Override
	public ResourceURIBuilderImpl suffix(String suffix) {
		this.suffix = suffix;
		if (suffix != null) {
			// look for query
			int queryStart = suffix.indexOf('?');
			// look for fragment
			int fragmentStart = suffix.indexOf('#');
			int endOfSuffix = queryStart;
			if (fragmentStart > -1 && (fragmentStart < queryStart || queryStart < 0)) {
				queryStart = -1;
				endOfSuffix = fragmentStart;
			}
			if (queryStart > -1) {
				String query = suffix.substring(queryStart + 1);
				// apply query
				query(query);
			}

			if (fragmentStart > -1) {
				fragment(suffix.substring(fragmentStart + 1));
			}
			if (endOfSuffix == 0) {
				this.suffix = null;
			} else if (endOfSuffix > -1) {
				this.suffix = suffix.substring(0, endOfSuffix);
			} else {
				this.suffix = suffix;
			}
		}
		return this;
	}

	@Override
	public ResourceURIBuilderImpl fragment(String fragment) {
		this.fragment = fragment;
		return this;
	}

	@Override
	public ResourceURIBuilderImpl scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	@Override
	public ResourceURIBuilderImpl host(String host) {
		this.host = host;
		return this;
	}

	@Override
	public ResourceURIBuilderImpl port(Integer port) {
		this.port = port;
		return this;
	}

	/**
	 * Builds a ResourceURI object that conforms to the values passed into this builder. NOTE: altering the builder after build() has been called might alter the returned ResourceURI.
	 *
	 * @return a ResourceURI object, that is still bound to the builder
	 */
	@Override
	public ResourceURI build() {
		Path p = null;
		if (pathElements != null) {
			p = new Path() {

				@Override
				public List<String> getElements() {
					return pathElements;
				}

				@Override
				public Iterator<String> iterator() {
					return pathElements.iterator();
				}
			};
		}

		List<Selector> s = null;
		if (selectors != null) {
			s = new ArrayList<>(selectors.size());
			for (final String selectorName : selectors) {
				s.add(new Selector() {

					@Override
					public String getName() {
						return selectorName;
					}
				});
			}
		}
		Extension ext = null;
		if (extension != null) {
			ext = new Extension() {

				@Override
				public String getName() {
					return extension;
				}
			};
		}
		List<ResourceURI.QueryParameter> q = null;
		if (parameters != null) {
			q = new ArrayList<>();
			for (ResourceURI.QueryParameter queryParameter : parameters.values()) {
				q.add(queryParameter);
			}
		}
		return new ResourceURIImpl(p, s, q, ext, suffix, scheme, host, port, fragment);
	}

	private void appendQueryParameter(StringBuffer nameBuf, StringBuffer valueBuf) {
		String name = nameBuf.toString();
		if (name.isEmpty()) {
			return;
		}
		String value = null;
		if (valueBuf != null) {
			value = valueBuf.toString();
		}
		try {
			name = URLDecoder.decode(name, "UTF-8");
			if (value != null) {
				value = URLDecoder.decode(value, "UTF-8");
			}
			parameter(name, value);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("could not decode query parameter", ex);
		}
	}

	@Override
	public ResourceURIBuilder clearParameters() {
		if (parameters != null) {
			parameters.clear();
		}
		return this;
	}

	@Override
	public ResourceURIBuilder dropParameter(String name) {
		if (parameters == null || name == null) {
			return this;
		}
		parameters.remove(name);
		return this;
	}
	
}
