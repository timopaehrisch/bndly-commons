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

import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.PathCoder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = DefaultCharacterEncodingProvider.class
)
@Designate(ocd = DefaultCharacterEncodingProviderImpl.Configuration.class)
public class DefaultCharacterEncodingProviderImpl implements DefaultCharacterEncodingProvider {

	@ObjectClassDefinition(
			name = "Default Request Character Encoding Provider",
			description = "This provider defines the default character encoding for incoming requests, if no encoding is available."
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Default Character Encoding",
				description = "The default character encoding to use for urls and request data",
				options = {
					@Option(value = "UTF-8", label = "UTF-8")
					,
							@Option(value = "ISO-8859-1", label = "ISO-8859-1")
				}
		)
		String defaultCharacterEncoding() default "UTF-8";
	}
	
	private String defaultCharacterEncoding;
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;
	
	@Activate
	public void activate(Configuration configuration) {
		defaultCharacterEncoding = configuration.defaultCharacterEncoding();
		if (!defaultCharacterEncoding.equalsIgnoreCase("UTF-8") && !defaultCharacterEncoding.equalsIgnoreCase("ISO-8859-1")) {
			defaultCharacterEncoding = "UTF-8";
		}
		if ("UTF-8".equalsIgnoreCase(defaultCharacterEncoding)) {
			defaultCharacterEncodingProvider = createUTF8DefaultCharacterEncodingProvider();
		} else {
			defaultCharacterEncodingProvider = createISO88591DefaultCharacterEncodingProvider();
		}
	}
	@Deactivate
	public void deactivate() {
	}
	
	private DefaultCharacterEncodingProvider createUTF8DefaultCharacterEncodingProvider() {
		return new DefaultCharacterEncodingProvider() {

			@Override
			public String getCharacterEncoding() {
				return "UTF-8";
			}

			@Override
			public PathCoder createPathCoder() {
				return new PathCoder.UTF8();
			}
		};
	}

	private DefaultCharacterEncodingProvider createISO88591DefaultCharacterEncodingProvider() {
		return new DefaultCharacterEncodingProvider() {

			@Override
			public String getCharacterEncoding() {
				return "ISO-8859-1";
			}

			@Override
			public PathCoder createPathCoder() {
				return new PathCoder.ISO88591();
			}
		};
	}
	
	@Override
	public String getCharacterEncoding() {
		return this.defaultCharacterEncodingProvider.getCharacterEncoding();
	}

	@Override
	public PathCoder createPathCoder() {
		return this.defaultCharacterEncodingProvider.createPathCoder();
	}
}
