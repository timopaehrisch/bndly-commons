package org.bndly.rest.repository.resources.beans.initializer;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Initializer.class)
public class RESTContextAwareInitializer implements Initializer {

	@Reference
	private ContextProvider contextProvider;

	@Override
	public boolean canInitialize(Class beanType) {
		return RESTContextAware.class.isAssignableFrom(beanType);
	}

	@Override
	public void initialize(Object bean) {
		Context currentContext = contextProvider.getCurrentContext();
		if (currentContext != null) {
			((RESTContextAware) bean).setRESTContext(currentContext);
		}
	}

}
