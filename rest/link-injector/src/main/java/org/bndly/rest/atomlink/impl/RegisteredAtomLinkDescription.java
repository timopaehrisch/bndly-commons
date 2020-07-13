package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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

import org.bndly.rest.atomlink.api.LinkFactory;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RegisteredAtomLinkDescription {
	private final AtomLinkDescriptionLinkFactory atomLinkDescriptionLinkFactory;
	private final Class<?> linkedInClass;

	public RegisteredAtomLinkDescription(AtomLinkDescriptionLinkFactory atomLinkDescriptionLinkFactory, Class<?> linkedInClass) {
		if (atomLinkDescriptionLinkFactory == null) {
			throw new IllegalArgumentException("atomLinkDescriptionLinkFactory is not allowed to be null");
		}
		if (linkedInClass == null) {
			throw new IllegalArgumentException("atomLinkDescription is not allowed to be null");
		}
		this.atomLinkDescriptionLinkFactory = atomLinkDescriptionLinkFactory;
		this.linkedInClass = linkedInClass;
	}

	public AtomLinkDescription getAtomLinkDescription() {
		return atomLinkDescriptionLinkFactory.getAtomLinkDescription();
	}

	public LinkFactory getAtomLinkInjector() {
		return atomLinkDescriptionLinkFactory;
	}

	public Class<?> getLinkedInClass() {
		return linkedInClass;
	}
	
}
