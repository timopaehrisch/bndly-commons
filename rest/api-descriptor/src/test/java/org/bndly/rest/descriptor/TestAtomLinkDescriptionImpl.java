package org.bndly.rest.descriptor;

/*-
 * #%L
 * REST API Descriptor
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

import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.PathParameter;
import org.bndly.rest.atomlink.api.annotation.QueryParameter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TestAtomLinkDescriptionImpl implements AtomLinkDescription {

	private final Class<?> linkedIn;
	private final String rel;
	private final Class<?> returns;

	public TestAtomLinkDescriptionImpl(Class<?> linkedIn, String rel) {
		this.linkedIn = linkedIn;
		this.rel = rel;
		this.returns = null;
	}

	public TestAtomLinkDescriptionImpl(Class<?> linkedIn, String rel, Class<?> returns) {
		this.linkedIn = linkedIn;
		this.rel = rel;
		this.returns = returns;
	}

	@Override
	public Class<?> getLinkedInClass() {
		return linkedIn;
	}

	@Override
	public Class<?> getReturnType() {
		return returns;
	}

	@Override
	public Class<?> getConsumesType() {
		return null;
	}

	@Override
	public String getSegment() {
		return null;
	}

	@Override
	public String getRel() {
		return rel;
	}

	@Override
	public String getConstraint() {
		return "";
	}

	@Override
	public Method getControllerMethod() {
		return null;
	}

	@Override
	public Object getController() {
		return null;
	}

	@Override
	public AtomLink getAtomLink() {
		return null;
	}

	@Override
	public String getHttpMethod() {
		return "GET";
	}

	@Override
	public List<QueryParameter> getQueryParams() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<PathParameter> getPathParams() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public String getHumanReadableDescription() {
		return null;
	}

	@Override
	public String[] getAuthors() {
		return null;
	}

	@Override
	public boolean isContextExtensionEnabled() {
		return false;
	}

	@Override
	public boolean allowSubclasses() {
		return false;
	}
}
