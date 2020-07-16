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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Initializer.class)
public class OSGIServiceAwareInitializer implements Initializer, OSGIServiceAware.ServiceResolver {

	private ComponentContext componentContext;

	@Activate
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
	}

	@Override
	public boolean canInitialize(Class beanType) {
		return OSGIServiceAware.class.isAssignableFrom(beanType);
	}

	@Override
	public void initialize(Object bean) {
		OSGIServiceAware.ServiceResolver serviceResolver = createServiceResolver();
		((OSGIServiceAware) bean).setServiceResolver(serviceResolver);
	}

	private OSGIServiceAware.ServiceResolver createServiceResolver() {
		return this;
	}

	@Override
	public <S> S getService(Class<S> serviceType) {
		ComponentContext tmp = componentContext;
		if (tmp == null) {
			return null;
		}
		BundleContext bc = tmp.getBundleContext();
		if (bc == null) {
			return null;
		}
		ServiceReference<S> sr = bc.getServiceReference(serviceType);
		if (sr == null) {
			return null;
		}
		return bc.getService(sr);
	}

}
