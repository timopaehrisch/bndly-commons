package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.fixtures.api.BoundFixtureDeployer;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class BoundFixtureDeployerImpl implements BoundFixtureDeployer {

	private final SchemaBeanFactory schemaBeanFactory;
	private ServiceRegistration<BoundFixtureDeployer> reg;

	public BoundFixtureDeployerImpl(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory == null) {
			throw new IllegalArgumentException("schemaBeanFactory is not allowed to be null");
		}
		this.schemaBeanFactory = schemaBeanFactory;
	}

	@Override
	public String getSchemaName() {
		return schemaBeanFactory.getEngine().getDeployer().getDeployedSchema().getName();
	}

	SchemaBeanFactory getSchemaBeanFactory() {
		return schemaBeanFactory;
	}

	void register(ComponentContext componentContext) {
		if (componentContext != null && reg == null) {
			reg = ServiceRegistrationBuilder.newInstance(BoundFixtureDeployer.class, this)
					.pid(BoundFixtureDeployer.class.getName() + "." + getSchemaName())
					.property("schemaName", getSchemaName())
					.register(componentContext.getBundleContext());
		}
	}

	void unregister() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
	}
	
}
