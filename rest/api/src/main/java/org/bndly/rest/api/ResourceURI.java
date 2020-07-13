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

import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ResourceURI {

	public interface Path extends Iterable<String> {

		List<String> getElements();
	}

	public interface Selector {

		String getName();
	}

	public interface Extension {

		String getName();
	}

	public interface QueryParameter {

		String getName();

		String getValue();
	}

	public Path getPath();

	public List<Selector> getSelectors();

	public List<QueryParameter> getParameters();

	public Extension getExtension();

	public String getSuffix();

	public String getScheme();

	public String getHost();

	public Integer getPort();

	public String getFragment();

	public String asString();
	
	public String pathAsString();

	public boolean hasSchemeHostPort();

	public boolean hasSchemeHost();

	public QueryParameter getParameter(String name);
}
