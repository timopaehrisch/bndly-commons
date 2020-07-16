package org.bndly.rest.cache.websocket.impl;

/*-
 * #%L
 * REST Cache Websocket
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = RemoteCacheConfiguration.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = RemoteCacheConfiguration.Configuration.class)
public class RemoteCacheConfiguration {
	
	@ObjectClassDefinition(name = "Remote Cache Configuration")
	public @interface Configuration {
		@AttributeDefinition(
				name = "URL",
				description = "The URL of the remote cache websocket."
		)
		String url();
	}
	
	private String url;

	@Activate
	public void activate(Configuration configuration) {
		url = configuration.url();
	}
	
	public String getUrl() {
		return url;
	}
	
}
