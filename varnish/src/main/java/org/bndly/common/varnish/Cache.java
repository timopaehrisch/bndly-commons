package org.bndly.common.varnish;

/*-
 * #%L
 * Varnish
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

import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Cache.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = Cache.Configuration.class)
public class Cache {
	
	@ObjectClassDefinition
	public @interface Configuration {
		@AttributeDefinition(
				name = "Name",
				description = "The name to use in order to reference this cache implementation"
		)
		String name() default "default";
		
		@AttributeDefinition(
				name = "Cache URI scheme",
				description = "The URI scheme to contact the cache."
		)
		String scheme() default "http";
		
		@AttributeDefinition(
				name = "Cache URI host",
				description = "The URI host to contact the cache."
		)
		String host() default "localhost";
		
		@AttributeDefinition(
				name = "Cache URI port",
				description = "The URI port to contact the cache."
		)
		int port() default 8079;
		
		@AttributeDefinition(
				name = "HttpClient PID",
				description = "The PID of the HttpClient to use to contact the cache"
		)
		String httpClient_target() default "(service.pid=org.apache.http.client.HttpClient.varnish)";

		@AttributeDefinition(
				name = "Purge request/response handler",
				description = "An OSGI filter expression to access the handler, that builds purge requests and evaluates purge responses."
		)
		String purgeRequestResponseHandler_target() default "(component.name=org.bndly.common.varnish.DefaultPurgeRequestResponseHandler)";
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
	
	private static final PathCoder PATH_CODER = new PathCoder.UTF8();
	
	@Reference(name = "httpClient")
	private HttpClient httpClient;
	@Reference(name = "purgeRequestResponseHandler")
	private PurgeRequestResponseHandler purgeRequestResponseHandler;
	private String scheme;
	private String host;
	private int port;
	private String urlPrefix;

	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter da = new DictionaryAdapter(componentContext.getProperties());
		scheme = da.getString("scheme", "http");
		host = da.getString("host", "localhost");
		port = da.getLong("port", 8079L).intValue();
		urlPrefix = scheme + "://" + host + ":" + port;
	}	
	
	public void flushPaths(Iterable<String> pathsWithExtension) {
		for (String pathToFlush : pathsWithExtension) {
			LOG.debug("flushing {}", pathToFlush);
			String url = urlPrefix + escapeSpecialCharactersToProperURL(pathToFlush);
			try {
				// set the url
				for (final HttpRequestBase purgeRequest : purgeRequestResponseHandler.createPurgeRequests(pathToFlush, url)) {
					Boolean purged = httpClient.execute(purgeRequest, new ResponseHandler<Boolean>() {
						@Override
						public Boolean handleResponse(HttpResponse hr) throws ClientProtocolException, IOException {
							HttpEntity entity = hr.getEntity();
							try {
								return purgeRequestResponseHandler.isPurgeSuccessResponse(hr, purgeRequest);
							} finally {
								if (entity != null) {
									EntityUtils.consumeQuietly(entity);
								}
							}
						}
					});
					if (purged) {
						LOG.debug("flushed {}", pathToFlush);
					} else {
						LOG.warn("could not flush {} for unknown reasons", pathToFlush);
					}
				}
			} catch (IOException e) {
				LOG.error("could not flush cache for path " + pathToFlush + " due to IO issues", e);
			}
		}
	}
	
	/**
	 * This method will parse the path as a resource path and then escapes all special characters in order to get a clean URL. For instance spaces will be escaped to %20.
	 * @param path The path to escape
	 * @return an escaped path
	 */
	public static String escapeSpecialCharactersToProperURL(String path) {
		ResourceURI resourceURI = new ResourceURIParser(PATH_CODER, path).parse().getResourceURI();
		return resourceURI.asString();
	}
}
