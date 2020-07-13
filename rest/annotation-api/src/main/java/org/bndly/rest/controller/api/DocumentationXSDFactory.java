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

import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import java.io.InputStream;

/**
 * This factory should be used for OSGI services, that are able to provide XSD data for elements, that are not available via {@link JAXBMessageClassProvider} instances.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface DocumentationXSDFactory {
	
	static final String OSGI_PROPERTY_ROOT_ELEMENT = "rootElement";
	static final String OSGI_PROPERTY_DESCRIPTION = "description";
	
	/**
	 * Gets a new input stream, that can be read in order to consume the XSD data.
	 * @return an input stream or null, if the factory is not able to provide the data
	 */
	InputStream getXSDDataAsStream();
}
