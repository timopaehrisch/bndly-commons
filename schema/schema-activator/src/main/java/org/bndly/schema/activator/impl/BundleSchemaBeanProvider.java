package org.bndly.schema.activator.impl;

/*-
 * #%L
 * Schema Activator
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

import org.bndly.schema.api.SchemaBeanProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BundleSchemaBeanProvider implements SchemaBeanProvider {
	private final String schemaName;
	private final String schemaBeanPackage;
	private final BundleWiring bundleWiring;
	private final Bundle bundle;

	public BundleSchemaBeanProvider(String schemaName, String schemaBeanPackage, BundleWiring bundleWiring, Bundle bundle) {
		if (schemaName == null) {
			throw new IllegalArgumentException("schemaName is not allowed to be null");
		}
		this.schemaName = schemaName;
		if (schemaBeanPackage == null) {
			throw new IllegalArgumentException("schemaBeanPackage is not allowed to be null");
		}
		this.schemaBeanPackage = schemaBeanPackage;
		if (bundleWiring == null) {
			throw new IllegalArgumentException("bundleWiring is not allowed to be null");
		}
		this.bundleWiring = bundleWiring;
		if (bundle == null) {
			throw new IllegalArgumentException("bundle is not allowed to be null");
		}
		this.bundle = bundle;
	}
	
	
	@Override
	public String getSchemaName() {
		return schemaName;
	}

	@Override
	public String getSchemaBeanPackage() {
		return schemaBeanPackage;
	}

	@Override
	public ClassLoader getSchemaBeanClassLoader() {
		return getBundleWiring().getClassLoader();
	}

	public BundleWiring getBundleWiring() {
		return bundleWiring;
	}

	public Bundle getBundle() {
		return bundle;
	}
	
}
