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
import java.util.Dictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaBeanBundleTracker extends BundleTracker<Object>{
	
	public SchemaBeanBundleTracker(BundleContext context) {
		super(context, Bundle.ACTIVE, null);
	}

	@Override
	public Object addingBundle(Bundle bundle, BundleEvent event) {
		BundleSchemaBeanProvider schemaBeanProvider = adaptBundleToSchemaBeanProvider(bundle);
		ServiceRegistration[] regs;
		if (schemaBeanProvider != null) {
			// register it
			regs = new ServiceRegistration[1];
			regs[0] = SchemaActivator.registerContainerService(schemaBeanProvider.getSchemaName(), SchemaBeanProvider.class, schemaBeanProvider, context);
		} else {
			regs = new ServiceRegistration[0];
		}
		return regs;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		if (object instanceof ServiceRegistration[]) {
			for (ServiceRegistration reg : (ServiceRegistration[]) object) {
				// remove
				reg.unregister();
			}
		}
	}
	
	private BundleSchemaBeanProvider adaptBundleToSchemaBeanProvider(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring != null) {
			Dictionary<String, String> headers = bundle.getHeaders();
			String schemaName = headers.get("Schema-Name");
			String schemaBeanPackage = headers.get("Schema-Bean-Package");
			if(schemaName == null || schemaBeanPackage == null) {
				return null;
			}
			BundleSchemaBeanProvider simpleSchemaBeanProvider = new BundleSchemaBeanProvider(schemaName, schemaBeanPackage, wiring, bundle);
			return simpleSchemaBeanProvider;
		}
		return null;
	}
	
}
