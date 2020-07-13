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
import java.util.List;
import java.util.Map;

public class DelegatingAtomLinkDescription implements AtomLinkDescription {

	private final AtomLinkDescription atomLinkDescription;

	public DelegatingAtomLinkDescription(AtomLinkDescription atomLinkDescription) {
		if (atomLinkDescription == null) {
			throw new IllegalArgumentException("a delegating atom link description requires a wrapped description object");
		}
		this.atomLinkDescription = atomLinkDescription;
	}

	@Override
	public Class<?> getLinkedInClass() {
		return atomLinkDescription.getLinkedInClass();
	}

	@Override
	public Class<?> getReturnType() {
		return atomLinkDescription.getReturnType();
	}

	@Override
	public Class<?> getConsumesType() {
		return atomLinkDescription.getConsumesType();
	}

	@Override
	public String getSegment() {
		return atomLinkDescription.getSegment();
	}

	@Override
	public String getConstraint() {
		return atomLinkDescription.getConstraint();
	}

	@Override
	public String getHumanReadableDescription() {
		return atomLinkDescription.getHumanReadableDescription();
	}

	@Override
	public String[] getAuthors() {
		return atomLinkDescription.getAuthors();
	}

	@Override
	public String getRel() {
		return atomLinkDescription.getRel();
	}

	@Override
	public Method getControllerMethod() {
		return atomLinkDescription.getControllerMethod();
	}

	@Override
	public Object getController() {
		return atomLinkDescription.getController();
	}

	@Override
	public AtomLink getAtomLink() {
		return atomLinkDescription.getAtomLink();
	}

	@Override
	public String getHttpMethod() {
		return atomLinkDescription.getHttpMethod();
	}

	@Override
	public List<QueryParameter> getQueryParams() {
		return atomLinkDescription.getQueryParams();
	}

	@Override
	public List<PathParameter> getPathParams() {
		return atomLinkDescription.getPathParams();
	}

	@Override
	public boolean isContextExtensionEnabled() {
		return atomLinkDescription.isContextExtensionEnabled();
	}

	@Override
	public boolean allowSubclasses() {
		return atomLinkDescription.allowSubclasses();
	}
}
