package org.bndly.rest.root.controller;

/*-
 * #%L
 * REST Root Controller
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

import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.atomlink.api.AtomLinkInjectorListener;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.common.beans.Services;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = AtomLinkInjectorListener.class)
public class ServicesFlush implements AtomLinkInjectorListener {

	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	
	@Override
	public void addedAtomLink(AtomLinkDescription atomLinkDescription, AtomLinkInjector atomLinkInjector) {
		doFlushIfRequired(atomLinkDescription);
	}

	@Override
	public void removedAtomLink(AtomLinkDescription atomLinkDescription, AtomLinkInjector atomLinkInjector) {
		doFlushIfRequired(atomLinkDescription);
	}

	private void doFlushIfRequired(AtomLinkDescription atomLinkDescription) {
		CacheTransactionFactory factory = cacheTransactionFactory;
		if (factory == null) {
			return;
		}
		if (Services.class.equals(atomLinkDescription.getLinkedInClass())) {
			try (CacheTransaction tx = factory.createCacheTransaction()) {
				tx.flush("/");
				tx.flush("/communicationDescription");
			}
		}
	}

}
